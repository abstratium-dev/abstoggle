import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { InfoButtonComponent } from '../core/info-button/info-button.component';
import { Criterion, Rule, ModelService } from '../model.service';
import { Controller } from '../controller';

@Component({
  selector: 'app-rules',
  imports: [CommonModule, FormsModule, InfoButtonComponent],
  templateUrl: './rules.component.html',
  styleUrl: './rules.component.scss'
})
export class RulesComponent implements OnInit {
  private modelService = inject(ModelService);
  private controller = inject(Controller);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmDialogService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  rules: Signal<Rule[]> = this.modelService.rules$;
  loading: Signal<boolean> = this.modelService.rulesLoading$;
  error: Signal<string | null> = this.modelService.rulesError$;

  // Form state
  showAddForm = false;
  editingRule: Rule | null = null;
  formSubmitting = false;
  formError: string | null = null;
  criteriaError: string | null = null;

  // Form fields
  ruleName = '';
  ruleDescription = '';
  criteriaKey = '';
  criteriaValue = '';
  criteriaEntries: { criterionKey: string; criterionValue: string }[] = [];

  // Filter fields
  filterName: string | null = null;

  ngOnInit(): void {
    this.controller.loadRules();

    this.route.queryParamMap.subscribe(params => {
      const filterName = params.get('filterName');
      if (filterName) {
        this.filterName = filterName;
      }
    });
  }

  /**
   * Returns filtered rules based on the current filterName.
   */
  filteredRules(): Rule[] {
    const allRules = this.rules();
    if (!this.filterName) {
      return allRules;
    }
    const term = this.filterName.toLowerCase();
    return allRules.filter(rule => {
      const name = (rule.name || '').toLowerCase();
      const desc = (rule.description || '').toLowerCase();
      return name.includes(term) || desc.includes(term);
    });
  }

  clearFilter(): void {
    this.filterName = null;
    this.router.navigate(['/rules']);
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (this.showAddForm) {
      this.resetForm();
    }
  }

  startEdit(rule: Rule): void {
    this.editingRule = rule;
    this.ruleName = rule.name || '';
    this.ruleDescription = rule.description || '';
    this.criteriaEntries = (rule.criteria || []).map(c => ({ criterionKey: c.criterionKey, criterionValue: c.criterionValue }));
    this.showAddForm = true;
    this.formError = null;
    this.criteriaError = null;
  }

  cancelEdit(): void {
    this.editingRule = null;
    this.showAddForm = false;
    this.resetForm();
  }

  resetForm(): void {
    this.ruleName = '';
    this.ruleDescription = '';
    this.criteriaKey = '';
    this.criteriaValue = '';
    this.criteriaEntries = [];
    this.formError = null;
    this.criteriaError = null;
  }


  onRetry(): void {
    this.controller.loadRules();
  }

  addCriterionEntry(): void {
    const criterionKey = this.criteriaKey.trim();
    const criterionValue = this.criteriaValue.trim();
    if (!criterionKey || !criterionValue) {
      return;
    }
    this.criteriaError = null;
    this.criteriaEntries.push({ criterionKey, criterionValue });
    this.criteriaKey = '';
    this.criteriaValue = '';
  }

  removeCriterionEntry(index: number): void {
    this.criteriaEntries.splice(index, 1);
    this.criteriaError = null;
  }

  private buildCriteriaList(): Criterion[] {
    return this.criteriaEntries.map(entry => ({
      criterionKey: entry.criterionKey,
      criterionValue: entry.criterionValue
    }));
  }

  async onSubmit(): Promise<void> {
    if (!this.ruleName.trim()) {
      this.formError = 'Rule name is required';
      return;
    }

    this.formSubmitting = true;
    this.formError = null;

    try {
      const criteria = this.buildCriteriaList();

      if (this.editingRule) {
        await this.controller.updateStandaloneRule(
          this.editingRule.id,
          this.ruleName.trim(),
          this.ruleDescription.trim(),
          criteria
        );
        this.toastService.success('Rule updated successfully');
      } else {
        await this.controller.createStandaloneRule(
          this.ruleName.trim(),
          this.ruleDescription.trim(),
          criteria
        );
        this.toastService.success('Rule created successfully');
      }
      this.showAddForm = false;
      this.editingRule = null;
      this.resetForm();
    } catch (err: any) {
      const problem = err.error;
      this.formError = problem?.detail || problem?.title || 'Failed to save rule. Please try again.';
    } finally {
      this.formSubmitting = false;
    }
  }

  async deleteRule(rule: Rule): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Delete Rule',
      message: `Are you sure you want to delete the rule "${rule.name || rule.id}"? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    try {
      await this.controller.deleteStandaloneRule(rule.id);
      this.toastService.success('Rule deleted successfully');
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete rule';
      this.toastService.error(errorMessage);
    }
  }

  formatCriteria(criteria: Criterion[]): string {
    if (!criteria || criteria.length === 0) {
      return 'None (catch-all)';
    }
    return criteria
      .map(c => `${c.criterionKey}: ${c.criterionValue}`)
      .join(', ');
  }

  goToToggles(rule: Rule): void {
    this.router.navigate(['/toggles'], { queryParams: { filterRule: rule.name || rule.id } });
  }
}
