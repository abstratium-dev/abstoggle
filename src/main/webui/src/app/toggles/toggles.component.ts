import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { InfoButtonComponent } from '../core/info-button/info-button.component';
import { AutocompleteComponent, AutocompleteOption } from '../core/autocomplete/autocomplete.component';
import { Toggle, ModelService, Stage, Rule, ToggleStageRule } from '../model.service';
import { Controller } from '../controller';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-toggles',
  imports: [CommonModule, FormsModule, InfoButtonComponent, AutocompleteComponent],
  templateUrl: './toggles.component.html',
  styleUrl: './toggles.component.scss'
})
export class TogglesComponent implements OnInit {
  private modelService = inject(ModelService);
  private controller = inject(Controller);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmDialogService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);

  toggles: Signal<Toggle[]> = this.modelService.toggles$;
  loading: Signal<boolean> = this.modelService.togglesLoading$;
  error: Signal<string | null> = this.modelService.togglesError$;
  stages: Signal<Stage[]> = this.modelService.stages$;
  rules: Signal<Rule[]> = this.modelService.rules$;

  // Form state
  showAddForm = false;
  editingToggle: Toggle | null = null;
  formSubmitting = false;
  formError: string | null = null;

  // Form fields
  toggleName = '';
  toggleDescription = '';
  toggleEnabled = true;
  toggleContext = '';

  // Toggle Stage Rule management
  managingToggle: Toggle | null = null;
  toggleStageRules: ToggleStageRule[] = [];
  stageRulesLoading = false;
  showAddStageRuleForm = false;
  editingStageRule: ToggleStageRule | null = null;

  // Add form fields
  selectedStageName = '';
  selectedRuleId = '';
  newRulePriority = 100;
  editRulePriority = 100;

  // Filter fields
  filterStageName: string | null = null;
  filterRuleName: string | null = null;

  fetchStageOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.stages()
      .filter(stage => stage.name.toLowerCase().includes(term))
      .map(stage => ({ value: stage.name, label: stage.name }));
  };

  fetchRuleOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.rules()
      .filter(rule => (rule.name || '').toLowerCase().includes(term))
      .map(rule => ({ value: rule.name || '', label: rule.name || rule.value }));
  };

  fetchRuleOptionsForAssignment = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.rules()
      .filter(rule => {
        const name = (rule.name || '').toLowerCase();
        const desc = (rule.description || '').toLowerCase();
        return name.includes(term) || desc.includes(term);
      })
      .map(rule => ({
        value: rule.id,
        label: `${rule.name || rule.description || rule.value} (${rule.value})`
      }));
  };

  ngOnInit(): void {
    this.controller.loadToggles();
    this.controller.loadStages();
    this.controller.loadRules();

    this.route.paramMap.subscribe(params => {
      const toggleName = params.get('toggleName');
      if (toggleName) {
        this.openManageForToggleName(toggleName);
      }
    });

    this.route.queryParamMap.subscribe(params => {
      const filterRule = params.get('filterRule');
      if (filterRule) {
        this.filterRuleName = filterRule;
        this.onFilterChange();
      }
      const filterStage = params.get('filterStage');
      if (filterStage) {
        this.filterStageName = filterStage;
        this.onFilterChange();
      }
    });
  }

  private async openManageForToggleName(toggleName: string): Promise<void> {
    const waitForToggles = (): Promise<Toggle[]> => {
      return new Promise(resolve => {
        const check = () => {
          const current = this.toggles();
          if (current.length > 0) {
            resolve(current);
          } else {
            setTimeout(check, 50);
          }
        };
        check();
      });
    };

    const toggles = await waitForToggles();
    const toggle = toggles.find(t => t.name === toggleName);
    if (toggle) {
      await this.startManageStageRules(toggle);
    }
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (this.showAddForm) {
      this.resetForm();
    }
  }

  startEdit(toggle: Toggle): void {
    this.editingToggle = toggle;
    this.toggleName = toggle.name;
    this.toggleDescription = toggle.description || '';
    this.toggleEnabled = toggle.enabled ?? true;
    this.toggleContext = toggle.context || '';
    this.showAddForm = true;
    this.formError = null;
  }

  cancelEdit(): void {
    this.editingToggle = null;
    this.showAddForm = false;
    this.resetForm();
  }

  resetForm(): void {
    this.editingToggle = null;
    this.toggleName = '';
    this.toggleDescription = '';
    this.toggleEnabled = true;
    this.toggleContext = '';
    this.formError = null;
  }

  onRetry(): void {
    this.controller.loadToggles(this.filterStageName || undefined, this.filterRuleName || undefined);
  }

  onFilterChange(): void {
    this.controller.loadToggles(this.filterStageName || undefined, this.filterRuleName || undefined);
  }

  clearFilters(): void {
    this.filterStageName = null;
    this.filterRuleName = null;
    this.onFilterChange();
  }

  async onSubmit(): Promise<void> {
    if (!this.toggleName.trim()) {
      this.formError = 'Toggle name is required';
      return;
    }

    this.formSubmitting = true;
    this.formError = null;

    try {
      if (this.editingToggle) {
        await this.controller.updateToggle(
          this.editingToggle.name,
          this.toggleDescription.trim(),
          this.toggleEnabled,
          this.toggleContext.trim()
        );
        this.toastService.success('Toggle updated successfully');
      } else {
        await this.controller.createToggle(
          this.toggleName.trim(),
          this.toggleDescription.trim(),
          this.toggleEnabled,
          this.toggleContext.trim()
        );
        this.toastService.success('Toggle created successfully');
      }
      this.showAddForm = false;
      this.editingToggle = null;
      this.resetForm();
    } catch (err: any) {
      const problem = err.error;
      this.formError = problem?.detail || problem?.title || 'Failed to save toggle. Please try again.';
    } finally {
      this.formSubmitting = false;
    }
  }

  async deleteToggle(toggle: Toggle): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Delete Toggle',
      message: `Are you sure you want to delete the toggle "${toggle.name}"? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    try {
      await this.controller.deleteToggle(toggle.name);
      this.toastService.success('Toggle deleted successfully');
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete toggle';
      this.toastService.error(errorMessage);
    }
  }

  getEnabledStatus(toggle: Toggle): string {
    return toggle.enabled ? 'Yes' : 'No';
  }

  // Toggle Stage Rule Management
  async startManageStageRules(toggle: Toggle): Promise<void> {
    this.managingToggle = toggle;
    this.toggleStageRules = [];
    this.selectedStageName = '';
    this.selectedRuleId = '';
    this.newRulePriority = 100;
    this.showAddStageRuleForm = false;
    this.editingStageRule = null;
    this.stageRulesLoading = true;
    this.controller.loadRules();
    try {
      const rules = await this.controller.getToggleStageRules(toggle.name);
      this.toggleStageRules = rules;
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to load stage rules';
      this.toastService.error(errorMessage);
    } finally {
      this.stageRulesLoading = false;
    }
  }

  cancelManageStageRules(): void {
    this.managingToggle = null;
    this.toggleStageRules = [];
    this.selectedStageName = '';
    this.selectedRuleId = '';
    this.showAddStageRuleForm = false;
    this.editingStageRule = null;
  }

  toggleAddStageRuleForm(): void {
    this.showAddStageRuleForm = !this.showAddStageRuleForm;
    if (this.showAddStageRuleForm) {
      this.editingStageRule = null;
      this.selectedStageName = '';
      this.selectedRuleId = '';
      this.newRulePriority = 100;
    }
  }

  startEditStageRule(stageRule: ToggleStageRule): void {
    this.editingStageRule = stageRule;
    this.editRulePriority = stageRule.priority;
    this.showAddStageRuleForm = true;
  }

  cancelEditStageRule(): void {
    this.editingStageRule = null;
    this.showAddStageRuleForm = false;
  }

  onPriorityChange(value: number): void {
    if (this.editingStageRule) {
      this.editRulePriority = value;
    } else {
      this.newRulePriority = value;
    }
  }

  async saveStageRule(): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    try {
      if (this.editingStageRule) {
        await this.controller.updateToggleStageRule(
          this.managingToggle.name,
          this.editingStageRule.id,
          this.editRulePriority
        );
        this.toastService.success('Assignment updated successfully');
      } else {
        if (!this.selectedStageName || !this.selectedRuleId) {
          this.toastService.error('Stage and rule are required');
          return;
        }
        await this.controller.createToggleStageRule(
          this.managingToggle.name,
          this.selectedStageName,
          this.selectedRuleId,
          this.newRulePriority
        );
        this.toastService.success('Assignment created successfully');
      }
      this.showAddStageRuleForm = false;
      this.editingStageRule = null;
      this.selectedStageName = '';
      this.selectedRuleId = '';
      await this.reloadStageRules();
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to save assignment';
      this.toastService.error(errorMessage);
    }
  }

  async deleteStageRule(stageRule: ToggleStageRule): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    const confirmed = await this.confirmService.confirm({
      title: 'Delete Assignment',
      message: `Remove the assignment for stage "${stageRule.stageName}" and rule "${stageRule.ruleName}"?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    try {
      await this.controller.deleteToggleStageRule(this.managingToggle.name, stageRule.id);
      this.toastService.success('Assignment deleted successfully');
      await this.reloadStageRules();
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete assignment';
      this.toastService.error(errorMessage);
    }
  }

  private async reloadStageRules(): Promise<void> {
    if (!this.managingToggle) {
      return;
    }
    this.stageRulesLoading = true;
    try {
      const rules = await this.controller.getToggleStageRules(this.managingToggle.name);
      this.toggleStageRules = rules;
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to reload stage rules';
      this.toastService.error(errorMessage);
    } finally {
      this.stageRulesLoading = false;
    }
  }
}
