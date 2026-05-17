import { Injectable, signal, Signal } from '@angular/core';

/**
 * Represents a deployment stage (e.g., "dev", "test", "prod").
 */
export interface Stage {
  /** v4 UUID. */
  id: string;
  /** Stage identifier. Must be unique. */
  name: string;
  /** Human-readable description. */
  description?: string;
  /** UI presentation order. */
  displayOrder: number;
  /** Optional parent stage name for inheritance chain. */
  parentStageName?: string;
}

/**
 * Application configuration returned by the public config endpoint.
 */
export interface Config {
  /** Current log level. */
  logLevel: string;
  /** Warning message to display, or "-" to suppress. */
  warningMessage: string;
}

/**
 * Lightweight toggle metadata used in list views.
 */
export interface Toggle {
  /** v4 UUID. */
  id: string;
  /** Toggle identifier. */
  name: string;
  /** Human-readable description. */
  description?: string;
  /** Master switch to enable or disable this toggle. */
  enabled?: boolean;
  /** Context string for this toggle. */
  context?: string;
}

/**
 * A single criterion key/value pair, as returned by the backend CriterionDto.
 */
export interface Criterion {
  /** v4 UUID. */
  id?: string;
  /** Key for matching (e.g., "userId", "country"). */
  criterionKey: string;
  /** Value or regex pattern to match. */
  criterionValue: string;
  /** v4 UUID of the parent rule. */
  ruleId?: string;
}

/**
 * Full toggle data transfer object returned by toggle query endpoints,
 * including stage-specific rules ordered by priority.
 */
export interface ToggleDto {
  /** Toggle name. */
  toggleName: string;
  /** Toggle description. */
  toggleDescription?: string;
  /** Toggle enabled flag. */
  toggleEnabled?: boolean;
  /** Toggle context. */
  toggleContext?: string;
  /** Stage name. */
  stageName: string;
  /** Rule name. */
  ruleName?: string;
  /** Rule description. */
  ruleDescription?: string;
  /** Rule criteria list. */
  ruleCriteria: Criterion[];
  /** Evaluation order (lower = first). */
  priority: number;
  /** Toggle value from the assignment. */
  value: string;
}

/**
 * Response payload for toggle query endpoints (matches QueryResponse Java record).
 */
export interface ToggleQueryResponse {
  /** Toggles matching the requested stage and optional name filter. */
  toggles: ToggleDto[];
  /** Metadata about the query. */
  queryMetadata?: { stage?: string; nameFilter?: string; count?: number; cacheHit?: boolean };
}

/**
 * Represents an assignment of a rule to a toggle within a specific stage.
 * Matches ToggleStageRuleDto from the backend.
 */
export interface ToggleStageRule {
  /** v4 UUID of the assignment. */
  id: string;
  /** v4 UUID of the toggle. */
  toggleId: string;
  /** v4 UUID of the stage. */
  stageId: string;
  /** v4 UUID of the reusable rule. */
  ruleId: string;
  /** Toggle value if criteria match ("off" or custom). */
  ruleValue: string;
  /** Evaluation order (lower = first). */
  priority: number;
}

/**
 * Represents a reusable rule definition (criteria template).
 * Returned by /api/rules; has no value because the value is set per-assignment.
 * Matches RuleDto from the backend.
 */
export interface Rule {
  /** v4 UUID of the rule. */
  id: string;
  /** Unique human-readable name for this rule. */
  name?: string;
  /** Human-readable description explaining the criteria. */
  description?: string;
  /** List of criterion pairs for client-side matching. Empty list means catch-all. */
  criteria: Criterion[];
}

@Injectable({
  providedIn: 'root',
})
export class ModelService {

  private config = signal<Config | null>(null);
  private warningMessage = signal<string>('');
  private stages = signal<Stage[]>([]);
  private stagesLoading = signal<boolean>(false);
  private stagesError = signal<string | null>(null);
  private toggles = signal<Toggle[]>([]);
  private togglesLoading = signal<boolean>(false);
  private togglesError = signal<string | null>(null);
  private rules = signal<Rule[]>([]);
  private rulesLoading = signal<boolean>(false);
  private rulesError = signal<string | null>(null);

  config$: Signal<Config | null> = this.config.asReadonly();
  warningMessage$: Signal<string> = this.warningMessage.asReadonly();
  stages$: Signal<Stage[]> = this.stages.asReadonly();
  stagesLoading$: Signal<boolean> = this.stagesLoading.asReadonly();
  stagesError$: Signal<string | null> = this.stagesError.asReadonly();
  toggles$: Signal<Toggle[]> = this.toggles.asReadonly();
  togglesLoading$: Signal<boolean> = this.togglesLoading.asReadonly();
  togglesError$: Signal<string | null> = this.togglesError.asReadonly();
  rules$: Signal<Rule[]> = this.rules.asReadonly();
  rulesLoading$: Signal<boolean> = this.rulesLoading.asReadonly();
  rulesError$: Signal<string | null> = this.rulesError.asReadonly();

  setConfig(config: Config) {
    this.config.set(config);
    if (config.warningMessage === '-') {
      this.warningMessage.set('');
    } else {
      this.warningMessage.set(config.warningMessage || '');
    }
  }

  setStages(stages: Stage[]) {
    this.stages.set(stages);
  }

  setStagesLoading(loading: boolean) {
    this.stagesLoading.set(loading);
  }

  setStagesError(error: string | null) {
    this.stagesError.set(error);
  }

  setToggles(toggles: Toggle[]) {
    this.toggles.set(toggles);
  }

  setTogglesLoading(loading: boolean) {
    this.togglesLoading.set(loading);
  }

  setTogglesError(error: string | null) {
    this.togglesError.set(error);
  }

  setRules(rules: Rule[]) {
    this.rules.set(rules);
  }

  setRulesLoading(loading: boolean) {
    this.rulesLoading.set(loading);
  }

  setRulesError(error: string | null) {
    this.rulesError.set(error);
  }
}
