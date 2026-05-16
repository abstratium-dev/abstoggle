import { ToggleDto, ToggleQueryRule } from '../model.service';

export type EvalLogLevel = 'info' | 'match' | 'skip' | 'result';

export interface EvalLogEntry {
  level: EvalLogLevel;
  message: string;
}

export interface ToggleResult {
  name: string;
  description?: string;
  enabled?: boolean;
  resolvedValue: string;
  matchedRule: string | null;
  evalLog: EvalLogEntry[];
  showLog: boolean;
}

export function evaluateToggle(toggle: ToggleDto, clientContext: { [key: string]: string }): ToggleResult {
  const log: EvalLogEntry[] = [];

  if (!toggle.enabled) {
    log.push({ level: 'info', message: 'Toggle is disabled — resolved to "off"' });
    return { name: toggle.name, description: toggle.description, enabled: toggle.enabled, resolvedValue: 'off', matchedRule: 'Toggle is disabled', evalLog: log, showLog: false };
  }

  const sortedRules = [...(toggle.rules || [])].sort((a, b) => a.priority - b.priority);
  log.push({ level: 'info', message: `Evaluating ${sortedRules.length} rule(s) in priority order` });

  for (const rule of sortedRules) {
    const criteriaCount = Object.keys(rule.criteria).length;
    const ruleNamePart = rule.name ? ` "${rule.name}"` : '';
    log.push({ level: 'info', message: `Rule${ruleNamePart} priority=${rule.priority}, value="${rule.value}", criteria=${criteriaCount === 0 ? 'none (catch-all)' : criteriaCount}` });

    let matchesAll = true;

    if (criteriaCount === 0) {
      log.push({ level: 'match', message: '  No criteria — catch-all, matches unconditionally' });
    } else {
      for (const [criterionKey, pattern] of Object.entries(rule.criteria)) {
        const clientValue = clientContext[criterionKey] ?? '';
        const matched = matchesPattern(clientValue, pattern);
        if (matched) {
          log.push({ level: 'match', message: `  ✓ ${criterionKey}: context="${clientValue}" matches pattern="${pattern}"` });
        } else {
          log.push({ level: 'skip', message: `  ✗ ${criterionKey}: context="${clientValue}" does not match pattern="${pattern}" — rule skipped` });
          matchesAll = false;
          break;
        }
      }
    }

    if (matchesAll) {
      const criteriaDesc = criteriaCount > 0
        ? ` (criteria: ${Object.entries(rule.criteria).map(([k, v]) => k + '=' + v).join(', ')})`
        : ' (catch-all)';
      log.push({ level: 'result', message: `→ Matched! Resolved value = "${rule.value}"` });
      return {
        name: toggle.name,
        description: toggle.description,
        enabled: toggle.enabled,
        resolvedValue: rule.value ?? 'off',
        matchedRule: `Priority ${rule.priority}${criteriaDesc}`,
        evalLog: log,
        showLog: false
      };
    }
  }

  log.push({ level: 'result', message: '→ No rule matched — resolved to default "off"' });
  return {
    name: toggle.name,
    description: toggle.description,
    enabled: toggle.enabled,
    resolvedValue: 'off',
    matchedRule: 'No matching rule — default',
    evalLog: log,
    showLog: false
  };
}

export function matchesPattern(value: string, pattern: string): boolean {
  try {
    const slashRegex = /^\/(.+)\/([gimsuy]*)$/;
    const match = slashRegex.exec(pattern);
    if (match) {
      return new RegExp(match[1], match[2]).test(value);
    }
    return new RegExp(pattern).test(value);
  } catch {
    return value === pattern;
  }
}
