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
 * Full toggle data transfer object returned by toggle query endpoints,
 * including stage-specific rules ordered by priority.
 */
export interface ToggleDto {
  /** Toggle identifier. */
  name: string;
  /** Stage that matched the query. */
  stage: string;
  /** Human-readable description. */
  description?: string;
  /** Master switch to enable or disable this toggle. */
  enabled?: boolean;
  /** Context string for this toggle. */
  context?: string;
  /** List of rules ordered by evaluation priority. */
  rules: Rule[];
}

/**
 * Response payload for toggle query endpoints.
 */
export interface ToggleQueryResponse {
  /** Toggles matching the requested stage and optional name filter. */
  toggles: ToggleDto[];
  /** Metadata about the query, such as stage, nameFilter, count, and cacheHit. */
  queryMetadata?: { [key: string]: any };
}

/**
 * Represents an assignment of a rule to a toggle within a specific stage.
 */
export interface ToggleStageRule {
  /** v4 UUID of the assignment. */
  id: string;
  /** Name of the toggle this rule is assigned to. */
  toggleName: string;
  /** Name of the stage this rule is assigned within. */
  stageName: string;
  /** v4 UUID of the reusable rule being assigned. */
  ruleId: string;
  /** Name of the reusable rule being assigned. */
  ruleName: string;
  /** Toggle value if criteria match ("off" or custom). */
  ruleValue: string;
  /** Human-readable description of the rule. */
  description?: string;
  /** Evaluation order (lower = first). */
  priority: number;
  /** Key/value pairs for client-side matching. */
  criteria: { [key: string]: string };
}

/**
 * Represents a rule with criteria for client-side matching.
 * Rules are evaluated in ascending priority order; the first matching rule wins.
 */
export interface Rule {
  /** v4 UUID of the rule. */
  id: string;
  /** Unique human-readable name for this rule. */
  name?: string;
  /** Evaluation order (lower = first). */
  priority: number;
  /** Toggle value if criteria match ("off" or custom). */
  value: string;
  /** Human-readable description explaining the criteria. */
  description?: string;
  /** Key/value pairs for client-side matching. Empty object means catch-all. */
  criteria: { [key: string]: string };
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
