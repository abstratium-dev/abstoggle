import { Injectable, signal, Signal } from '@angular/core';

export interface Demo {
  id: string;
}

export interface Stage {
  id: string;
  name: string;
  description?: string;
  displayOrder: number;
  parentStageName?: string;
  createdAt: string;
}

export interface Config {
  logLevel: string;
  warningMessage: string;
}

@Injectable({
  providedIn: 'root',
})
export class ModelService {

  private demos = signal<Demo[]>([]);
  private demosLoading = signal<boolean>(false);
  private demosError = signal<string | null>(null);
  private config = signal<Config | null>(null);
  private warningMessage = signal<string>('');
  private stages = signal<Stage[]>([]);
  private stagesLoading = signal<boolean>(false);
  private stagesError = signal<string | null>(null);

  demos$: Signal<Demo[]> = this.demos.asReadonly();
  demosLoading$: Signal<boolean> = this.demosLoading.asReadonly();
  demosError$: Signal<string | null> = this.demosError.asReadonly();
  config$: Signal<Config | null> = this.config.asReadonly();
  warningMessage$: Signal<string> = this.warningMessage.asReadonly();
  stages$: Signal<Stage[]> = this.stages.asReadonly();
  stagesLoading$: Signal<boolean> = this.stagesLoading.asReadonly();
  stagesError$: Signal<string | null> = this.stagesError.asReadonly();

  setDemos(demos: Demo[]) {
    this.demos.set(demos);
  }

  setDemosLoading(loading: boolean) {
    this.demosLoading.set(loading);
  }

  setDemosError(error: string | null) {
    this.demosError.set(error);
  }

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
}
