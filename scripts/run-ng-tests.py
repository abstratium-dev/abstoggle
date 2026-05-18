#!/usr/bin/env python3
"""
Run Angular tests via npm and extract key information for LLM consumption.
Mimics how Maven runs tests: sources nvm, uses node v24.11.1, runs ChromeHeadless with coverage.
"""

import subprocess
import re
import os
import sys
import glob
from datetime import datetime
from pathlib import Path

# Configuration
PROJECT_ROOT = Path(__file__).parent.parent.resolve()
WEBUI_DIR = PROJECT_ROOT / "src" / "main" / "webui"
TMP_DIR = PROJECT_ROOT / "tmp"
MAX_TMP_FILES = 10

# Patterns to skip (noise reduction)
SKIP_PATTERNS = [
    re.compile(r'^\s*chunk-.*\.js\s*\|'),  # Chunk files table
    re.compile(r'^\s*polyfills\.js\s*\|'),
    re.compile(r'^\s*main\.js\s*\|'),
    re.compile(r'^\s*styles\.css\s*\|'),
    re.compile(r'^\s*-\s*\|.*\|.*\|'),  # Separator lines in bundle table
    re.compile(r'^\s*Initial chunk files\s*\|'),
    re.compile(r'^\s*Initial total\s*\|'),
    re.compile(r'^\s*Application bundle generation complete\.'),
    re.compile(r'^\d{2}\s+\d{2}\s+\d{4}\s+\d{2}:\d{2}:\d{2}\.\d+:(WARN|INFO)\s+\[karma'),  # Karma server logs
    re.compile(r'^\s*DEBUG:\s*\'\[AUTH\]'),  # Auth debug messages
    re.compile(r'^\s*Chrome\s+\d+\.\d+\.\d+\.\d+.*: Executed \d+ of \d+ SUCCESS'),  # Successful tests
    re.compile(r'^\s*ERROR:\s*\'\[AUTH\]'),  # Auth error logs (usually expected in tests)
    re.compile(r'^\s*$'),  # Empty lines
]

# Patterns that indicate coverage summary
COVERAGE_PATTERNS = [
    re.compile(r'^\s*Code coverage\s*[:\s]*', re.IGNORECASE),
    re.compile(r'^\s*Statements\s*[:\s]*\d+', re.IGNORECASE),
    re.compile(r'^\s*Branches\s*[:\s]*\d+', re.IGNORECASE),
    re.compile(r'^\s*Functions\s*[:\s]*\d+', re.IGNORECASE),
    re.compile(r'^\s*Lines\s*[:\s]*\d+', re.IGNORECASE),
    re.compile(r'^\s*Filename\s*\|\s*%\s*Stmts', re.IGNORECASE),
    re.compile(r'^[-]+\|[-]+'),  # Coverage table separator
]

# Pattern for FAILED tests
FAILED_TEST_PATTERN = re.compile(r'^(Chrome\s+\d+\.\d+\.\d+\.\d+.*)\s+FAILED')

# Pattern for stack trace frames - keep only source code frames (not node_modules)
SOURCE_CODE_FRAME_PATTERN = re.compile(r'^\s+at\s+.*\(src/.*:\d+:\d+\)')
NODE_MODULES_FRAME_PATTERN = re.compile(r'^\s+at\s+.*\(node_modules/')
GENERIC_FRAME_PATTERN = re.compile(r'^\s+at\s+')

# Pattern for overall result
OVERALL_SUCCESS_PATTERN = re.compile(r'^\s*TOTAL:\s+(\d+)\s+SUCCESS')
OVERALL_FAILURE_PATTERN = re.compile(r'^\s*TOTAL:\s+(\d+)\s+FAILED')


def cleanup_old_tmp_files():
    """Remove oldest files if there are more than MAX_TMP_FILES in tmp directory."""
    if not TMP_DIR.exists():
        TMP_DIR.mkdir(parents=True, exist_ok=True)
        return

    files = sorted(TMP_DIR.glob("*.txt"), key=lambda p: p.stat().st_mtime, reverse=True)
    if len(files) > MAX_TMP_FILES:
        for old_file in files[MAX_TMP_FILES:]:
            try:
                old_file.unlink()
                print(f"[cleanup] Removed old file: {old_file.name}")
            except OSError as e:
                print(f"[cleanup] Error removing {old_file}: {e}")


def run_tests():
    """Run npm test and capture all output."""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = TMP_DIR / f"ng-test-{timestamp}.txt"

    # Ensure tmp directory exists
    TMP_DIR.mkdir(parents=True, exist_ok=True)

    # Command to run - same as Maven: source nvm, use node v24.11.1, run npm test
    cmd = [
        "bash", "-c",
        "source ~/.nvm/nvm.sh && nvm use v24.11.1 && npm test -- --browsers=ChromeHeadless --code-coverage --watch=false"
    ]

    print(f"[run] Executing: {' '.join(cmd)}")
    print(f"[run] Working directory: {WEBUI_DIR}")
    print(f"[run] Full output will be saved to: {output_file}")
    print()

    try:
        with open(output_file, "w") as f:
            process = subprocess.Popen(
                cmd,
                cwd=WEBUI_DIR,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
                universal_newlines=True
            )

            # Write all output to file in real-time
            for line in process.stdout:
                f.write(line)
                f.flush()

            process.wait()
            return output_file, process.returncode

    except Exception as e:
        print(f"[error] Failed to run tests: {e}")
        sys.exit(1)


def should_skip_line(line):
    """Check if a line should be skipped (noise reduction)."""
    for pattern in SKIP_PATTERNS:
        if pattern.match(line):
            return True
    return False


def is_coverage_line(line):
    """Check if a line is part of coverage summary."""
    for pattern in COVERAGE_PATTERNS:
        if pattern.match(line):
            return True
    return False


def is_source_code_frame(line):
    """Check if a stack trace frame is from source code (not node_modules)."""
    return SOURCE_CODE_FRAME_PATTERN.match(line) is not None


def is_node_modules_frame(line):
    """Check if a stack trace frame is from node_modules."""
    return NODE_MODULES_FRAME_PATTERN.match(line) is not None


def is_any_frame(line):
    """Check if line is any stack frame."""
    return GENERIC_FRAME_PATTERN.match(line) is not None


def parse_output(output_file):
    """Parse the test output file and extract relevant information."""
    failed_tests = []
    coverage_lines = []
    current_failed_test = None
    in_failed_test = False
    total_tests = 0
    success_tests = 0
    failed_count = 0
    overall_success = False

    with open(output_file, "r") as f:
        lines = f.readlines()

    i = 0
    while i < len(lines):
        line = lines[i]

        # Check for overall result
        success_match = OVERALL_SUCCESS_PATTERN.search(line)
        if success_match:
            overall_success = True
            total_tests = int(success_match.group(1))
            success_tests = total_tests
            i += 1
            continue

        failure_match = OVERALL_FAILURE_PATTERN.search(line)
        if failure_match:
            overall_success = False
            failed_count = int(failure_match.group(1))
            i += 1
            continue

        # Check for failed test start
        failed_match = FAILED_TEST_PATTERN.match(line)
        if failed_match:
            if current_failed_test:
                failed_tests.append(current_failed_test)
            current_failed_test = {
                "name": failed_match.group(1).strip(),
                "error": "",
                "stack_frames": []
            }
            in_failed_test = True
            i += 1
            continue

        # If in failed test section
        if in_failed_test:
            # Check for next test or empty line that ends this test
            if FAILED_TEST_PATTERN.match(line) or (line.strip() == "" and i + 1 < len(lines) and not is_any_frame(lines[i + 1])):
                if current_failed_test:
                    failed_tests.append(current_failed_test)
                    current_failed_test = None
                in_failed_test = False
                continue

            # Check if this is an error message line (starts with "Error:")
            if line.strip().startswith("Error:") or line.strip().startswith("Expected"):
                current_failed_test["error"] += line.strip() + "\n"
                i += 1
                continue

            # Check for stack frames - keep only source code frames
            if is_source_code_frame(line):
                frame = line.strip()
                # Clean up the frame to be more readable
                frame = re.sub(r'^\s+at\s+', '  at ', frame)
                current_failed_test["stack_frames"].append(frame)
            # Skip node_modules frames silently
            elif is_node_modules_frame(line):
                pass
            # Include other error context lines
            elif line.strip() and not is_any_frame(line):
                current_failed_test["error"] += line.strip() + "\n"

            i += 1
            continue

        # Check for coverage lines
        if is_coverage_line(line):
            coverage_lines.append(line.rstrip())
            i += 1
            continue

        i += 1

    # Don't forget the last failed test
    if current_failed_test:
        failed_tests.append(current_failed_test)

    return {
        "failed_tests": failed_tests,
        "coverage": coverage_lines,
        "overall_success": overall_success,
        "total_tests": total_tests,
        "success_tests": success_tests,
        "failed_count": failed_count
    }


def print_summary(results, output_file):
    """Print a concise summary of test results."""
    print("=" * 60)
    print("ANGULAR TEST RESULTS SUMMARY")
    print("=" * 60)
    print()

    # Overall status
    if results["overall_success"] and not results["failed_tests"]:
        print("[STATUS] ALL TESTS PASSED")
    elif results["overall_success"] and results["failed_tests"]:
        print("[STATUS] PARTIAL SUCCESS - Some tests passed but failures detected")
    else:
        print("[STATUS] TESTS FAILED")

    if results["total_tests"] > 0:
        print(f"[SUMMARY] {results['success_tests']}/{results['total_tests']} tests passed")
    if results["failed_count"] > 0:
        print(f"[SUMMARY] {results['failed_count']} tests failed")

    print()

    # Coverage info
    if results["coverage"]:
        print("-" * 40)
        print("CODE COVERAGE:")
        print("-" * 40)
        for line in results["coverage"]:
            print(line)
        print()

    # Failed tests
    if results["failed_tests"]:
        print("-" * 40)
        print(f"FAILED TESTS ({len(results['failed_tests'])}):")
        print("-" * 40)
        for test in results["failed_tests"]:
            print(f"\n[FAILED] {test['name']}")
            if test["error"]:
                # Deduplicate error messages
                error_lines = [l for l in test["error"].strip().split('\n') if l]
                seen = set()
                for err_line in error_lines:
                    if err_line not in seen:
                        print(f"  Error: {err_line}")
                        seen.add(err_line)
            if test["stack_frames"]:
                print("  Stack trace (source code only):")
                for frame in test["stack_frames"]:
                    print(f"    {frame}")
        print()

    # File location hint
    print("=" * 60)
    print(f"Full output saved to: {output_file}")
    print(f"[HINT] Use grep to search for specific errors:")
    print(f"  grep -n 'ERROR\\|FAILED\\|Error' {output_file}")
    print("=" * 60)


def main():
    cleanup_old_tmp_files()
    output_file, return_code = run_tests()
    results = parse_output(output_file)
    print_summary(results, output_file)

    # Exit with appropriate code
    sys.exit(return_code)


if __name__ == "__main__":
    main()
