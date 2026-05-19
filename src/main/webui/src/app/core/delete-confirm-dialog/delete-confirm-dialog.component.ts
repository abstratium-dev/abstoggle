import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DeleteConfirmDialogService } from './delete-confirm-dialog.service';

@Component({
  selector: 'ux-delete-confirm-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './delete-confirm-dialog.component.html',
  styleUrl: './delete-confirm-dialog.component.scss'
})
export class DeleteConfirmDialogComponent {
  private dialogService = inject(DeleteConfirmDialogService);

  state = this.dialogService.state$;

  confirm(): void {
    this.dialogService.handleConfirm();
  }

  cancel(): void {
    this.dialogService.handleCancel();
  }

  onInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.dialogService.updateChangeNote(input.value);
  }
}
