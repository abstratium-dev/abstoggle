import { ToggleDto } from '../model.service';

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

/**
 * Evaluates a single QueryTSRDto row (already sorted by priority by the caller)
 * against a client context. Returns a ToggleResult if a match is found, otherwise null.
 */
export function evaluateToggle(toggle: ToggleDto, clientContext: { [key: string]: string }): ToggleResult {
  const log: EvalLogEntry[] = [];

  if (!toggle.toggleEnabled) {
    log.push({ level: 'info', message: 'Toggle is disabled — resolved to "off"' });
    return {
      name: toggle.toggleName,
      description: toggle.toggleDescription,
      enabled: toggle.toggleEnabled,
      resolvedValue: 'off',
      matchedRule: 'Toggle is disabled',
      evalLog: log,
      showLog: false
    };
  }

  const criteria = toggle.ruleCriteria || [];
  const criteriaCount = criteria.length;
  const ruleNamePart = toggle.ruleName ? ` "${toggle.ruleName}"` : '';
  log.push({ level: 'info', message: `Rule${ruleNamePart} priority=${toggle.priority}, value="${toggle.value}", criteria=${criteriaCount === 0 ? 'none (catch-all)' : criteriaCount}` });

  let matchesAll = true;

  if (criteriaCount === 0) {
    log.push({ level: 'match', message: '  No criteria — catch-all, matches unconditionally' });
  } else {
    for (const criterion of criteria) {
      const clientValue = clientContext[criterion.criterionKey] ?? '';
      const matched = matchesPattern(clientValue, criterion.criterionValue);
      if (matched) {
        log.push({ level: 'match', message: `  ✓ ${criterion.criterionKey}: context="${clientValue}" matches pattern="${criterion.criterionValue}"` });
      } else {
        log.push({ level: 'skip', message: `  ✗ ${criterion.criterionKey}: context="${clientValue}" does not match pattern="${criterion.criterionValue}" — rule skipped` });
        matchesAll = false;
        break;
      }
    }
  }

  if (matchesAll) {
    const criteriaDesc = criteriaCount > 0
      ? ` (criteria: ${criteria.map(c => c.criterionKey + '=' + c.criterionValue).join(', ')})`
      : ' (catch-all)';
    log.push({ level: 'result', message: `→ Matched! Resolved value = "${toggle.value}"` });
    return {
      name: toggle.toggleName,
      description: toggle.toggleDescription,
      enabled: toggle.toggleEnabled,
      resolvedValue: toggle.value ?? 'off',
      matchedRule: `Priority ${toggle.priority}${criteriaDesc}`,
      evalLog: log,
      showLog: false
    };
  }

  log.push({ level: 'result', message: '→ No rule matched — resolved to default "off"' });
  return {
    name: toggle.toggleName,
    description: toggle.toggleDescription,
    enabled: toggle.toggleEnabled,
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
