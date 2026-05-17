import { Component, inject } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeNoteDialogService } from './change-note-dialog.service';

@Component({
  selector: 'ux-change-note-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, NgClass],
  templateUrl: './change-note-dialog.component.html',
  styleUrl: './change-note-dialog.component.scss'
})
export class ChangeNoteDialogComponent {
  private dialogService = inject(ChangeNoteDialogService);

  state = this.dialogService.state$;

  confirm(): void {
    this.dialogService.handleConfirm();
  }

  cancel(): void {
    this.dialogService.handleCancel();
  }

  skip(): void {
    this.dialogService.handleSkip();
  }

  onInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.dialogService.updateChangeNote(input.value);
  }
}
