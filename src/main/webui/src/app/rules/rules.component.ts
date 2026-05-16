import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { InfoButtonComponent } from '../core/info-button/info-button.component';
import { Rule, ModelService } from '../model.service';
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
  criteriaEntries: { key: string; value: string }[] = [];

  ngOnInit(): void {
    this.controller.loadRules();
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
    this.criteriaEntries = Object.entries(rule.criteria || {}).map(([key, value]) => ({ key, value }));
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
    const key = this.criteriaKey.trim();
    const value = this.criteriaValue.trim();
    if (!key || !value) {
      return;
    }
    if (this.criteriaEntries.some(e => e.key === key)) {
      this.criteriaError = `Criterion "${key}" is already defined. Use a regular expression that matches all required values (e.g., "^(value1|value2)$"), or create and assign a separate rule.`;
      return;
    }
    this.criteriaError = null;
    this.criteriaEntries.push({ key, value });
    this.criteriaKey = '';
    this.criteriaValue = '';
  }

  removeCriterionEntry(index: number): void {
    this.criteriaEntries.splice(index, 1);
    this.criteriaError = null;
  }

  private buildCriteriaMap(): { [key: string]: string } {
    const map: { [key: string]: string } = {};
    for (const entry of this.criteriaEntries) {
      map[entry.key] = entry.value;
    }
    return map;
  }

  async onSubmit(): Promise<void> {
    if (!this.ruleName.trim()) {
      this.formError = 'Rule name is required';
      return;
    }

    this.formSubmitting = true;
    this.formError = null;

    try {
      const criteria = this.buildCriteriaMap();

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

  formatCriteria(criteria: { [key: string]: string }): string {
    if (!criteria || Object.keys(criteria).length === 0) {
      return 'None (catch-all)';
    }
    return Object.entries(criteria)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ');
  }

  goToToggles(rule: Rule): void {
    this.router.navigate(['/toggles'], { queryParams: { filterRule: rule.name || rule.id } });
  }
}
