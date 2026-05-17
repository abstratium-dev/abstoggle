import { Component, inject, OnInit, Signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { InfoButtonComponent } from '../core/info-button/info-button.component';
import { AutocompleteComponent, AutocompleteOption } from '../core/autocomplete/autocomplete.component';
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

const STORAGE_KEY = 'toggleTesterConfigs';
const DEFAULT_CONFIG_NAME = 'default';

interface StoredState {
  selectedStage: string;
  selectedContext: string;
  nameFilter: string;
  contextEntries: ContextEntry[];
}

type Configurations = Record<string, StoredState>;

@Component({
  selector: 'app-toggle-tester',
  imports: [CommonModule, FormsModule, RouterModule, InfoButtonComponent, AutocompleteComponent],
  templateUrl: './toggle-tester.component.html',
  styleUrl: './toggle-tester.component.scss'
})
export class ToggleTesterComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private controller = inject(Controller);
  private modelService = inject(ModelService);

  stages: Signal<Stage[]> = this.modelService.stages$;
  toggleContexts: Signal<string[]> = this.modelService.toggleContexts$;

  @ViewChild('contextAutocomplete') contextAutocomplete?: AutocompleteComponent;

  fetchContextOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.toggleContexts()
      .filter(ctx => ctx.toLowerCase().includes(term))
      .map(ctx => ({ value: ctx, label: ctx }));
  };

  selectedStage = '';
  selectedContext = '';
  nameFilter = '';
  contextEntries: ContextEntry[] = [];
  newContextKey = '';
  newContextValue = '';
  configName = '';
  savedConfigs: string[] = [];
  configExpanded = false;

  querying = false;
  clearingCache = false;
  queryError: string | null = null;
  results: ToggleResult[] | null = null;
  queriedStage = '';

  ngOnInit(): void {
    this.controller.loadStages();
    this.controller.loadToggleContexts();

    this.loadState();

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

    // Read query parameters (URL takes precedence over localStorage)
    this.route.queryParamMap.subscribe(params => {
      const stage = params.get('stage');
      const context = params.get('context');
      const nameFilter = params.get('filter');

      if (stage) {
        this.selectedStage = stage;
      }
      if (context) {
        this.selectedContext = context;
      }
      if (nameFilter) {
        this.nameFilter = nameFilter;
      }

      // Load context entries from query params (ctx.key=value format)
      const entries: ContextEntry[] = [];
      params.keys.forEach(key => {
        if (key.startsWith('ctx.')) {
          const ctxKey = key.substring(4);
          const value = params.get(key) ?? '';
          if (ctxKey) {
            entries.push({ key: ctxKey, value });
          }
        }
      });

      if (entries.length > 0) {
        this.contextEntries = entries;
      }
    });
  }

  private getAllConfigs(): Configurations {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : {};
    } catch {
      return {};
    }
  }

  private loadConfig(name: string): void {
    const configs = this.getAllConfigs();
    const state = configs[name];

    if (state) {
      this.selectedStage = state.selectedStage ?? '';
      this.selectedContext = state.selectedContext ?? '';
      this.nameFilter = state.nameFilter ?? '';
      this.contextEntries = state.contextEntries?.length
        ? state.contextEntries
        : Object.entries(DEFAULT_CONTEXT).map(([key, value]) => ({ key, value }));
    } else {
      // No saved config - use defaults
      this.selectedStage = '';
      this.selectedContext = '';
      this.nameFilter = '';
      this.contextEntries = Object.entries(DEFAULT_CONTEXT).map(([key, value]) => ({ key, value }));
    }
    this.configName = name;
    this.updateUrlWithCurrentState();
  }

  updateUrlWithCurrentState(): void {
    const queryParams: { [key: string]: string | null } = {
      stage: this.selectedStage || null,
      context: (this.contextAutocomplete?.searchTerm() ?? this.selectedContext) || null,
      filter: this.nameFilter.trim() || null
    };

    for (const entry of this.contextEntries) {
      if (entry.key.trim()) {
        queryParams[`ctx.${entry.key.trim()}`] = entry.value || null;
      }
    }

    const name = this.nameFilter.trim();
    if (name) {
      this.router.navigate(['/toggle-tester', name, this.selectedStage], {
        replaceUrl: true,
        queryParams
      });
    } else {
      this.router.navigate(['/toggle-tester'], {
        replaceUrl: true,
        queryParams
      });
    }

    // Always save to default config when fields change
    this.saveDefaultState();
  }

  private saveDefaultState(): void {
    try {
      this.saveConfig(DEFAULT_CONFIG_NAME);
    } catch (error) {
      console.error("Failed to save default state", error);
    }
  }

  private saveConfig(name: string): void {
    const configs = this.getAllConfigs();
    configs[name] = {
      selectedStage: this.selectedStage,
      selectedContext: this.selectedContext,
      nameFilter: this.nameFilter,
      contextEntries: this.contextEntries
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(configs));
    this.refreshSavedConfigs();
  }

  private refreshSavedConfigs(): void {
    this.savedConfigs = Object.keys(this.getAllConfigs());
  }

  private loadState(): void {
    this.refreshSavedConfigs();
    this.loadConfig(this.configName || DEFAULT_CONFIG_NAME);
  }

  private saveState(): void {
    try {
      this.saveConfig(this.configName || DEFAULT_CONFIG_NAME);
    } catch (error) {
      console.error("Failed to save state", error);
    }
  }

  saveCurrentConfig(): void {
    const name = this.configName.trim() || DEFAULT_CONFIG_NAME;
    this.saveConfig(name);
    this.configName = name;
  }

  loadNamedConfig(): void {
    const name = this.configName.trim() || DEFAULT_CONFIG_NAME;
    this.loadConfig(name);
    this.configName = name;
  }

  deleteConfig(): void {
    const name = this.configName.trim() || DEFAULT_CONFIG_NAME;
    if (!name) return;

    const configs = this.getAllConfigs();
    if (configs[name]) {
      delete configs[name];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(configs));
      this.refreshSavedConfigs();
      // Reset to default config
      this.loadConfig(DEFAULT_CONFIG_NAME);
    }
  }

  toggleConfigExpanded(): void {
    this.configExpanded = !this.configExpanded;
  }

  onConfigSelect(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const name = select.value;
    if (name) {
      this.loadConfig(name);
    }
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
      this.saveState();
      this.saveDefaultState();
      this.updateUrlWithCurrentState();
    }
  }

  removeContextEntry(index: number): void {
    this.contextEntries.splice(index, 1);
    this.saveState();
    this.saveDefaultState();
    this.updateUrlWithCurrentState();
  }

  resetContext(): void {
    this.contextEntries = Object.entries(DEFAULT_CONTEXT).map(([key, value]) => ({ key, value }));
    this.saveState();
    this.saveDefaultState();
    this.updateUrlWithCurrentState();
  }

  async runQuery(): Promise<void> {
    this.saveState();

    if (!this.selectedStage) {
      this.queryError = 'Please select a stage';
      return;
    }

    const resolvedContext = (this.contextAutocomplete?.searchTerm() ?? this.selectedContext).trim();
    if (!resolvedContext) {
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
      const response = await this.controller.queryTogglesManagement(
        this.selectedStage,
        resolvedContext,
        this.nameFilter.trim() || undefined
      );

      this.queriedStage = this.selectedStage;
      this.results = response.toggles.map(toggle => evaluateToggle(toggle, clientContext));

      // Build query params for sharing
      const queryParams: { [key: string]: string | null } = {
        stage: this.selectedStage || null,
        context: this.selectedContext.trim() || null,
        filter: this.nameFilter.trim() || null
      };

      // Add context entries as ctx.<key>=<value>
      for (const entry of this.contextEntries) {
        if (entry.key.trim()) {
          queryParams[`ctx.${entry.key.trim()}`] = entry.value || null;
        }
      }

      const name = this.nameFilter.trim();
      if (name) {
        this.router.navigate(['/toggle-tester', name, this.selectedStage], {
          replaceUrl: true,
          queryParams
        });
      } else {
        this.router.navigate(['/toggle-tester'], {
          replaceUrl: true,
          queryParams
        });
      }
    } catch (err: any) {
      const problem = err.error;
      this.queryError = problem?.detail || problem?.title || 'Failed to query toggles';
    } finally {
      this.querying = false;
    }
  }

  async clearCache(): Promise<void> {
    const resolvedContext = (this.contextAutocomplete?.searchTerm() ?? this.selectedContext).trim();
    if (!this.selectedStage || !resolvedContext) {
      return;
    }
    this.clearingCache = true;
    try {
      await this.controller.evictCache(
        this.selectedStage,
        resolvedContext,
        this.nameFilter.trim() || undefined
      );
    } catch (err: any) {
      const problem = err.error;
      this.queryError = problem?.detail || problem?.title || 'Failed to clear cache';
    } finally {
      this.clearingCache = false;
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
