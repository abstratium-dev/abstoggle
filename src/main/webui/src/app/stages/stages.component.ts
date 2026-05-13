import { Component, inject, OnInit, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { Stage, ModelService } from '../model.service';
import { Controller } from '../controller';

@Component({
  selector: 'app-stages',
  imports: [CommonModule, FormsModule],
  templateUrl: './stages.component.html',
  styleUrl: './stages.component.scss'
})
export class StagesComponent implements OnInit {
  private modelService = inject(ModelService);
  private controller = inject(Controller);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmDialogService);

  stages: Signal<Stage[]> = this.modelService.stages$;
  loading: Signal<boolean> = this.modelService.stagesLoading$;
  error: Signal<string | null> = this.modelService.stagesError$;

  // Form state
  showAddForm = false;
  editingStage: Stage | null = null;
  formSubmitting = false;
  formError: string | null = null;

  // Form fields
  stageName = '';
  stageDescription = '';
  stageDisplayOrder = 0;
  stageParentName = '';

  ngOnInit(): void {
    this.controller.loadStages();
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (this.showAddForm) {
      this.resetForm();
    }
  }

  startEdit(stage: Stage): void {
    this.editingStage = stage;
    this.stageName = stage.name;
    this.stageDescription = stage.description || '';
    this.stageDisplayOrder = stage.displayOrder;
    this.stageParentName = stage.parentStageName || '';
    this.showAddForm = true;
    this.formError = null;
  }

  cancelEdit(): void {
    this.editingStage = null;
    this.showAddForm = false;
    this.resetForm();
  }

  resetForm(): void {
    this.stageName = '';
    this.stageDescription = '';
    this.stageDisplayOrder = 0;
    this.stageParentName = '';
    this.formError = null;
  }

  onRetry(): void {
    this.controller.loadStages();
  }

  async onSubmit(): Promise<void> {
    if (!this.stageName.trim()) {
      this.formError = 'Stage name is required';
      return;
    }

    this.formSubmitting = true;
    this.formError = null;

    try {
      const parentName = this.stageParentName.trim() || undefined;

      if (this.editingStage) {
        await this.controller.updateStage(
          this.editingStage.name,
          this.stageName.trim(),
          this.stageDescription.trim(),
          this.stageDisplayOrder,
          parentName
        );
        this.toastService.success('Stage updated successfully');
      } else {
        await this.controller.createStage(
          this.stageName.trim(),
          this.stageDescription.trim(),
          this.stageDisplayOrder,
          parentName
        );
        this.toastService.success('Stage created successfully');
      }
      this.showAddForm = false;
      this.editingStage = null;
      this.resetForm();
    } catch (err: any) {
      const problem = err.error;
      this.formError = problem?.detail || problem?.title || 'Failed to save stage. Please try again.';
    } finally {
      this.formSubmitting = false;
    }
  }

  async deleteStage(stage: Stage): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Delete Stage',
      message: `Are you sure you want to delete the stage "${stage.name}"? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmClass: 'btn-danger'
    });

    if (!confirmed) {
      return;
    }

    try {
      await this.controller.deleteStage(stage.name);
      this.toastService.success('Stage deleted successfully');
    } catch (err: any) {
      const problem = err.error;
      const errorMessage = problem?.detail || problem?.title || 'Failed to delete stage';
      this.toastService.error(errorMessage);
    }
  }

  getParentStageName(stage: Stage): string {
    return stage.parentStageName || '-';
  }

  getAvailableParentStages(currentStage?: Stage): Stage[] {
    return this.stages().filter(s => {
      // Cannot be own parent
      if (currentStage && s.name === currentStage.name) return false;
      // Prevent circular reference - simple check
      if (currentStage && s.parentStageName === currentStage.name) return false;
      return true;
    });
  }
}
