import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { InfoButtonComponent } from '../core/info-button/info-button.component';
import { Controller } from '../controller';
import { ModelService, Stage, ToggleDto } from '../model.service';
import { ToggleResult, evaluateToggle } from './toggle-evaluator';

interface ContextEntry {
  key: string;
  value: string;
}

const DEFAULT_CONTEXT: { [key: string]: string } = {
  userId: '10042',
  country: 'DE',
  plan: 'premium',
  userAgent: 'Mozilla/5.0...'
};

@Component({
  selector: 'app-toggle-tester',
  imports: [CommonModule, FormsModule, RouterModule, InfoButtonComponent],
  templateUrl: './toggle-tester.component.html',
  styleUrl: './toggle-tester.component.scss'
})
export class ToggleTesterComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private controller = inject(Controller);
  private modelService = inject(ModelService);

  stages: Signal<Stage[]> = this.modelService.stages$;

  selectedStage = '';
  selectedContext = '';
  nameFilter = '';
  contextEntries: ContextEntry[] = [];
  newContextKey = '';
  newContextValue = '';

  querying = false;
  queryError: string | null = null;
  results: ToggleResult[] | null = null;
  queriedStage = '';

  ngOnInit(): void {
    this.controller.loadStages();

    this.contextEntries = Object.entries(DEFAULT_CONTEXT).map(([key, value]) => ({ key, value }));

    this.route.paramMap.subscribe(params => {
      const stage = params.get('stage');
      const toggleName = params.get('toggleName');
      if (stage) {
        this.selectedStage = stage;
      }
      if (toggleName && toggleName !== '_') {
        this.nameFilter = toggleName;
      }
    });
  }

  addContextEntry(): void {
    if (this.newContextKey.trim()) {
      const existing = this.contextEntries.find(e => e.key === this.newContextKey.trim());
      if (existing) {
        existing.value = this.newContextValue;
      } else {
        this.contextEntries.push({ key: this.newContextKey.trim(), value: this.newContextValue });
      }
      this.newContextKey = '';
      this.newContextValue = '';
    }
  }

  removeContextEntry(index: number): void {
    this.contextEntries.splice(index, 1);
  }

  resetContext(): void {
    this.contextEntries = Object.entries(DEFAULT_CONTEXT).map(([key, value]) => ({ key, value }));
  }

  async runQuery(): Promise<void> {
    if (!this.selectedStage) {
      this.queryError = 'Please select a stage';
      return;
    }

    if (!this.selectedContext.trim()) {
      this.queryError = 'Context is required';
      return;
    }

    this.querying = true;
    this.queryError = null;
    this.results = null;

    const clientContext: { [key: string]: string } = {};
    for (const entry of this.contextEntries) {
      if (entry.key.trim()) {
        clientContext[entry.key.trim()] = entry.value;
      }
    }

    try {
      const response = await this.controller.queryToggles(
        this.selectedStage,
        this.selectedContext.trim(),
        this.nameFilter.trim() || undefined
      );

      this.queriedStage = this.selectedStage;
      this.results = response.toggles.map(toggle => evaluateToggle(toggle, clientContext));

      const name = this.nameFilter.trim();
      if (name) {
        this.router.navigate(['/toggle-tester', name, this.selectedStage], { replaceUrl: true });
      } else {
        this.router.navigate(['/toggle-tester'], { replaceUrl: true });
      }
    } catch (err: any) {
      const problem = err.error;
      this.queryError = problem?.detail || problem?.title || 'Failed to query toggles';
    } finally {
      this.querying = false;
    }
  }

  toggleLog(result: ToggleResult): void {
    result.showLog = !result.showLog;
  }

  getValueClass(value: string): string {
    if (value === 'on') return 'value-on';
    if (value === 'off') return 'value-off';
    return 'value-custom';
  }
}
