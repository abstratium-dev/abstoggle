import { Component, inject, OnInit, Signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { ChangeNoteDialogService } from '../core/change-note-dialog/change-note-dialog.service';
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
  protected modelService = inject(ModelService);
  private controller = inject(Controller);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmDialogService);
  private changeNoteDialog = inject(ChangeNoteDialogService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  toggles: Signal<Toggle[]> = this.modelService.toggles$;
  loading: Signal<boolean> = this.modelService.togglesLoading$;
  error: Signal<string | null> = this.modelService.togglesError$;
  stages: Signal<Stage[]> = this.modelService.stages$;
  rules: Signal<Rule[]> = this.modelService.rules$;
  toggleContexts: Signal<string[]> = this.modelService.toggleContexts$;

  @ViewChild('contextAutocomplete') contextAutocomplete?: AutocompleteComponent;

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
  changeNote = '';

  // Toggle Stage Rule management
  managingToggle: Toggle | null = null;
  toggleStageRules: ToggleStageRule[] = [];
  stageRulesLoading = false;
  showAddStageRuleForm = false;
  editingStageRule: ToggleStageRule | null = null;

  // Add form fields - stage and rule selected by ID
  selectedStageId = '';
  selectedRuleId = '';
  newRulePriority = 100;
  newToggleValue = 'off';
  newAssignmentChangeNote = '';
  editRulePriority = 100;
  editToggleValue = 'off';
  editAssignmentChangeNote = '';

  // Filter fields
  filterStageName: string | null = null;
  filterRuleName: string | null = null;

  fetchContextOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.toggleContexts()
      .filter(ctx => ctx.toLowerCase().includes(term))
      .map(ctx => ({ value: ctx, label: ctx }));
  };

  fetchStageOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.stages()
      .filter(stage => stage.name.toLowerCase().includes(term))
      .map(stage => ({ value: stage.name, label: stage.name }));
  };

  fetchRuleOptions = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.rules()
      .filter(rule => {
        const name = (rule.name || '').toLowerCase();
        const desc = (rule.description || '').toLowerCase();
        return name.includes(term) || desc.includes(term);
      })
      .map(rule => ({ value: rule.name || rule.id, label: rule.name || rule.id }));
  };

  fetchStageOptionsForAssignment = async (searchTerm: string): Promise<AutocompleteOption[]> => {
    const term = searchTerm.toLowerCase();
    return this.stages()
      .filter(stage => stage.name.toLowerCase().includes(term))
      .map(stage => ({ value: stage.id, label: stage.name }));
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
        label: `${rule.name || rule.description || rule.id}`
      }));
  };

  ngOnInit(): void {
    this.controller.loadToggles();
    this.controller.loadStages();
    this.controller.loadRules();
    this.controller.loadToggleContexts();

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
      this.cancelManageStageRules();
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
    this.changeNote = '';
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
      const context = (this.contextAutocomplete?.searchTerm() ?? this.toggleContext).trim();
      if (this.editingToggle) {
        await this.controller.updateToggle(
          this.editingToggle.id,
          this.toggleName.trim(),
          this.toggleDescription.trim(),
          this.toggleEnabled,
          context,
          this.changeNote
        );
        this.toastService.success('Toggle updated successfully');
      } else {
        await this.controller.createToggle(
          this.toggleName.trim(),
          this.toggleDescription.trim(),
          this.toggleEnabled,
          context,
          this.changeNote
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

    const isMandatory = this.modelService.config$()?.changeNoteMandatory ?? true;
    const changeNote = await this.changeNoteDialog.prompt({
      title: 'Delete Toggle',
      message: isMandatory
        ? `Enter a change note for deleting toggle "${toggle.name}":`
        : `Enter a change note for deleting toggle "${toggle.name}" (optional):`,
      confirmText: 'Delete',
      confirmClass: 'btn-danger',
      optional: !isMandatory
    });
    if (changeNote === null) return;

    try {
      await this.controller.deleteToggle(toggle.id, changeNote);
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
    this.selectedStageId = '';
    this.selectedRuleId = '';
    this.newRulePriority = 100;
    this.showAddStageRuleForm = false;
    this.editingStageRule = null;
    this.stageRulesLoading = true;
    this.controller.loadRules();
    try {
      const rules = await this.controller.getToggleStageRules(toggle.id);
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
    this.selectedStageId = '';
    this.selectedRuleId = '';
    this.showAddStageRuleForm = false;
    this.editingStageRule = null;
  }

  toggleAddStageRuleForm(): void {
    this.showAddStageRuleForm = !this.showAddStageRuleForm;
    if (this.showAddStageRuleForm) {
      this.editingStageRule = null;
      this.selectedStageId = '';
      this.selectedRuleId = '';
      this.newRulePriority = 100;
      this.newToggleValue = 'off';
      this.newAssignmentChangeNote = '';
    }
  }

  startEditStageRule(stageRule: ToggleStageRule): void {
    this.editingStageRule = stageRule;
    this.editRulePriority = stageRule.priority;
    this.editToggleValue = stageRule.toggleValue;
    this.editAssignmentChangeNote = '';
    this.showAddStageRuleForm = true;
  }

  cancelEditStageRule(): void {
    this.editingStageRule = null;
    this.showAddStageRuleForm = false;
    this.newAssignmentChangeNote = '';
    this.editAssignmentChangeNote = '';
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
          this.editingStageRule.id,
          this.editToggleValue,
          this.editRulePriority,
          this.editAssignmentChangeNote
        );
        this.toastService.success('Assignment updated successfully');
      } else {
        if (!this.selectedStageId || !this.selectedRuleId) {
          this.toastService.error('Stage and rule are required');
          return;
        }
        await this.controller.createToggleStageRule(
          this.managingToggle.id,
          this.selectedStageId,
          this.selectedRuleId,
          this.newRulePriority,
          this.newToggleValue,
          this.newAssignmentChangeNote
        );
        this.toastService.success('Assignment created successfully');
      }
      this.showAddStageRuleForm = false;
      this.editingStageRule = null;
      this.selectedStageId = '';
      this.selectedRuleId = '';
      this.newAssignmentChangeNote = '';
      this.editAssignmentChangeNote = '';
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

    const stageName = this.getStageName(stageRule.stageId);
    const ruleName = this.getRuleName(stageRule.ruleId);
    const confirmed = await this.confirmService.confirm({
      title: 'Delete Assignment',
      message: `Remove the assignment for stage "${stageName}" and rule "${ruleName}"?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    const isMandatory = this.modelService.config$()?.changeNoteMandatory ?? true;
    const changeNote = await this.changeNoteDialog.prompt({
      title: 'Delete Assignment',
      message: isMandatory
        ? `Enter a change note for deleting this assignment:`
        : `Enter a change note for deleting this assignment (optional):`,
      confirmText: 'Delete',
      confirmClass: 'btn-danger',
      optional: !isMandatory
    });
    if (changeNote === null) return;

    try {
      await this.controller.deleteToggleStageRule(stageRule.id, changeNote);
      this.toastService.success('Assignment deleted successfully');
      await this.reloadStageRules();
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete assignment';
      this.toastService.error(errorMessage);
    }
  }

  getStageName(stageId: string): string {
    return this.stages().find(s => s.id === stageId)?.name || stageId;
  }

  getRuleName(ruleId: string): string {
    return this.rules().find(r => r.id === ruleId)?.name || ruleId;
  }

  getStage(stageId: string): Stage | undefined {
    return this.stages().find(s => s.id === stageId);
  }

  getRule(ruleId: string): Rule | undefined {
    return this.rules().find(r => r.id === ruleId);
  }

  goToStage(stageId: string): void {
    const stage = this.getStage(stageId);
    if (stage) {
      this.router.navigate(['/stages'], { queryParams: { filterName: stage.name } });
    }
  }

  goToRule(ruleId: string): void {
    const rule = this.getRule(ruleId);
    if (rule) {
      const filterValue = rule.name || rule.id;
      this.router.navigate(['/rules'], { queryParams: { filterName: filterValue } });
    }
  }

  goToToggleHistory(toggle: Toggle): void {
    this.router.navigate(['/history'], { queryParams: { entityType: 'Toggle', entityId: toggle.id } });
  }

  goToAssignmentHistory(stageRule: ToggleStageRule): void {
    this.router.navigate(['/history'], { queryParams: { entityType: 'ToggleStageRule', entityId: stageRule.id } });
  }

  private async reloadStageRules(): Promise<void> {
    if (!this.managingToggle) {
      return;
    }
    this.stageRulesLoading = true;
    try {
      const rules = await this.controller.getToggleStageRules(this.managingToggle.id);
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
