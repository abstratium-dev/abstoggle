import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { Toggle, ModelService, Stage, Rule } from '../model.service';
import { Controller } from '../controller';
import { AuthService } from '../core/auth.service';

interface ToggleStageInfo {
  stageName: string;
  rules: Rule[];
  loadingRules: boolean;
  showAddRule: boolean;
  editingRule: Rule | null;
  ruleForm: {
    priority: number;
    value: string;
    description: string;
    criteriaKey: string;
    criteriaValue: string;
    criteria: { [key: string]: string };
  };
}

@Component({
  selector: 'app-toggles',
  imports: [CommonModule, FormsModule],
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

  // Expose Object for template use
  Object = Object;

  toggles: Signal<Toggle[]> = this.modelService.toggles$;
  loading: Signal<boolean> = this.modelService.togglesLoading$;
  error: Signal<string | null> = this.modelService.togglesError$;
  stages: Signal<Stage[]> = this.modelService.stages$;

  // Form state
  showAddForm = false;
  editingToggle: Toggle | null = null;
  formSubmitting = false;
  formError: string | null = null;

  // Form fields
  toggleName = '';
  toggleDescription = '';
  toggleEnabled = true;

  // Stage management
  managingToggle: Toggle | null = null;
  toggleStages: Map<string, ToggleStageInfo> = new Map();
  selectedStageToAdd = '';
  stageManagementLoading = false;

  ngOnInit(): void {
    this.controller.loadToggles();
    this.controller.loadStages();

    this.route.paramMap.subscribe(params => {
      const toggleName = params.get('toggleName');
      if (toggleName) {
        this.openManageForToggleName(toggleName);
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
      await this.startManageStages(toggle);
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
    this.showAddForm = true;
    this.formError = null;
  }

  cancelEdit(): void {
    this.editingToggle = null;
    this.showAddForm = false;
    this.resetForm();
  }

  resetForm(): void {
    this.toggleName = '';
    this.toggleDescription = '';
    this.toggleEnabled = true;
    this.formError = null;
  }

  onRetry(): void {
    this.controller.loadToggles();
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
          this.toggleEnabled
        );
        this.toastService.success('Toggle updated successfully');
      } else {
        await this.controller.createToggle(
          this.toggleName.trim(),
          this.toggleDescription.trim(),
          this.toggleEnabled
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

  // Stage Management
  async startManageStages(toggle: Toggle): Promise<void> {
    this.managingToggle = toggle;
    this.toggleStages.clear();
    this.selectedStageToAdd = '';
    this.stageManagementLoading = true;
    try {
      const stageNames = await this.controller.getStagesForToggle(toggle.name);
      for (const stageName of stageNames) {
        this.toggleStages.set(stageName, {
          stageName,
          rules: [],
          loadingRules: false,
          showAddRule: false,
          editingRule: null,
          ruleForm: this.createEmptyRuleForm()
        });
      }
      await Promise.all(stageNames.map(s => this.loadRulesForStage(s)));
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to load stages';
      this.toastService.error(errorMessage);
    } finally {
      this.stageManagementLoading = false;
    }
  }

  cancelManageStages(): void {
    this.managingToggle = null;
    this.toggleStages.clear();
    this.selectedStageToAdd = '';
  }

  getAvailableStagesForToggle(): Stage[] {
    const assignedStages = new Set(this.toggleStages.keys());
    return this.stages().filter(stage => !assignedStages.has(stage.name));
  }

  async addStageToToggle(): Promise<void> {
    if (!this.managingToggle || !this.selectedStageToAdd) {
      return;
    }

    this.stageManagementLoading = true;
    try {
      await this.controller.addStageToToggle(this.managingToggle.name, this.selectedStageToAdd);
      this.toggleStages.set(this.selectedStageToAdd, {
        stageName: this.selectedStageToAdd,
        rules: [],
        loadingRules: false,
        showAddRule: false,
        editingRule: null,
        ruleForm: this.createEmptyRuleForm()
      });
      this.selectedStageToAdd = '';
      this.toastService.success('Stage added to toggle');
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to add stage';
      this.toastService.error(errorMessage);
    } finally {
      this.stageManagementLoading = false;
    }
  }

  async removeStageFromToggle(stageName: string): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    const confirmed = await this.confirmService.confirm({
      title: 'Remove Stage',
      message: `Remove stage "${stageName}" from this toggle?`,
      confirmText: 'Remove',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    this.stageManagementLoading = true;
    try {
      await this.controller.removeStageFromToggle(this.managingToggle.name, stageName);
      this.toggleStages.delete(stageName);
      this.toastService.success('Stage removed from toggle');
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to remove stage';
      this.toastService.error(errorMessage);
    } finally {
      this.stageManagementLoading = false;
    }
  }

  // Rule Management
  private createEmptyRuleForm() {
    return {
      priority: 1,
      value: 'on',
      description: '',
      criteriaKey: '',
      criteriaValue: '',
      criteria: {}
    };
  }

  async loadRulesForStage(stageName: string): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    const stageInfo = this.toggleStages.get(stageName);
    if (!stageInfo) {
      return;
    }

    stageInfo.loadingRules = true;
    try {
      const rules = await this.controller.getRulesForToggle(this.managingToggle.name, stageName);
      stageInfo.rules = rules.sort((a, b) => a.priority - b.priority);
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to load rules';
      this.toastService.error(errorMessage);
    } finally {
      stageInfo.loadingRules = false;
    }
  }

  toggleAddRuleForm(stageName: string): void {
    const stageInfo = this.toggleStages.get(stageName);
    if (stageInfo) {
      stageInfo.showAddRule = !stageInfo.showAddRule;
      if (stageInfo.showAddRule) {
        stageInfo.editingRule = null;
        stageInfo.ruleForm = this.createEmptyRuleForm();
      }
    }
  }

  startEditRule(stageName: string, rule: Rule): void {
    const stageInfo = this.toggleStages.get(stageName);
    if (stageInfo) {
      stageInfo.editingRule = rule;
      stageInfo.showAddRule = true;
      const criteriaEntries = Object.entries(rule.criteria);
      stageInfo.ruleForm = {
        priority: rule.priority,
        value: rule.value,
        description: rule.description || '',
        criteriaKey: criteriaEntries[0]?.[0] || '',
        criteriaValue: criteriaEntries[0]?.[1] || '',
        criteria: { ...rule.criteria }
      };
    }
  }

  cancelEditRule(stageName: string): void {
    const stageInfo = this.toggleStages.get(stageName);
    if (stageInfo) {
      stageInfo.showAddRule = false;
      stageInfo.editingRule = null;
      stageInfo.ruleForm = this.createEmptyRuleForm();
    }
  }

  addCriteriaEntry(stageName: string): void {
    const stageInfo = this.toggleStages.get(stageName);
    if (stageInfo && stageInfo.ruleForm.criteriaKey && stageInfo.ruleForm.criteriaValue) {
      stageInfo.ruleForm.criteria[stageInfo.ruleForm.criteriaKey] = stageInfo.ruleForm.criteriaValue;
      stageInfo.ruleForm.criteriaKey = '';
      stageInfo.ruleForm.criteriaValue = '';
    }
  }

  removeCriteriaEntry(stageName: string, key: string): void {
    const stageInfo = this.toggleStages.get(stageName);
    if (stageInfo) {
      delete stageInfo.ruleForm.criteria[key];
    }
  }

  async saveRule(stageName: string): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    const stageInfo = this.toggleStages.get(stageName);
    if (!stageInfo) {
      return;
    }

    // Add current criteria entry if both key and value are filled
    if (stageInfo.ruleForm.criteriaKey && stageInfo.ruleForm.criteriaValue) {
      stageInfo.ruleForm.criteria[stageInfo.ruleForm.criteriaKey] = stageInfo.ruleForm.criteriaValue;
    }

    try {
      if (stageInfo.editingRule) {
        await this.controller.updateRule(
          this.managingToggle.name,
          stageName,
          stageInfo.editingRule.id,
          stageInfo.ruleForm.value,
          stageInfo.ruleForm.priority,
          stageInfo.ruleForm.description,
          stageInfo.ruleForm.criteria
        );
        this.toastService.success('Rule updated successfully');
      } else {
        await this.controller.createRule(
          this.managingToggle.name,
          stageName,
          stageInfo.ruleForm.value,
          stageInfo.ruleForm.priority,
          stageInfo.ruleForm.description,
          stageInfo.ruleForm.criteria
        );
        this.toastService.success('Rule created successfully');
      }

      stageInfo.showAddRule = false;
      stageInfo.editingRule = null;
      stageInfo.ruleForm = this.createEmptyRuleForm();
      await this.loadRulesForStage(stageName);
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to save rule';
      this.toastService.error(errorMessage);
    }
  }

  async deleteRule(stageName: string, rule: Rule): Promise<void> {
    if (!this.managingToggle) {
      return;
    }

    const confirmed = await this.confirmService.confirm({
      title: 'Delete Rule',
      message: 'Are you sure you want to delete this rule?',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    try {
      await this.controller.deleteRule(this.managingToggle.name, stageName, rule.id);
      this.toastService.success('Rule deleted successfully');
      await this.loadRulesForStage(stageName);
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete rule';
      this.toastService.error(errorMessage);
    }
  }

  // Helper method to format criteria as a string
  formatCriteria(criteria: { [key: string]: string }): string {
    return Object.entries(criteria).map(([k, v]) => k + '=' + v).join(', ');
  }

  // Helper method to check if criteria has entries
  hasCriteria(criteria: { [key: string]: string }): boolean {
    return Object.keys(criteria).length > 0;
  }
}
