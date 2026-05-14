import { Injectable, signal, Signal } from '@angular/core';

export interface Stage {
  id: string;
  name: string;
  description?: string;
  displayOrder: number;
  parentStageName?: string;
}

export interface Config {
  logLevel: string;
  warningMessage: string;
}

export interface Toggle {
  name: string;
  description?: string;
  enabled?: boolean;
  context?: string;
}

export interface ToggleDto {
  name: string;
  stage: string;
  description?: string;
  enabled?: boolean;
  context?: string;
  rules: Rule[];
}

export interface ToggleQueryResponse {
  toggles: ToggleDto[];
  queryMetadata?: { [key: string]: any };
}

export interface ToggleStageRule {
  id: string;
  toggleName: string;
  stageName: string;
  ruleName: string;
  priority: number;
}

export interface Rule {
  id: string;
  priority: number;
  value: string;
  description?: string;
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

  config$: Signal<Config | null> = this.config.asReadonly();
  warningMessage$: Signal<string> = this.warningMessage.asReadonly();
  stages$: Signal<Stage[]> = this.stages.asReadonly();
  stagesLoading$: Signal<boolean> = this.stagesLoading.asReadonly();
  stagesError$: Signal<string | null> = this.stagesError.asReadonly();
  toggles$: Signal<Toggle[]> = this.toggles.asReadonly();
  togglesLoading$: Signal<boolean> = this.togglesLoading.asReadonly();
  togglesError$: Signal<string | null> = this.togglesError.asReadonly();

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
}
