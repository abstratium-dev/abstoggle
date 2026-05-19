import { Injectable, signal, Signal } from '@angular/core';

export interface DeleteConfirmDialogConfig {
  title: string;
  message: string;
  itemName: string;
  confirmText?: string;
  cancelText?: string;
  /** If true, the change note is required (mandatory). */
  changeNoteMandatory: boolean;
  placeholder?: string;
}

interface DeleteConfirmDialogState {
  isOpen: boolean;
  config: DeleteConfirmDialogConfig | null;
  changeNote: string;
  resolve: ((value: string | null) => void) | null;
}

@Injectable({
  providedIn: 'root',
})
export class DeleteConfirmDialogService {
  private state = signal<DeleteConfirmDialogState>({
    isOpen: false,
    config: null,
    changeNote: '',
    resolve: null,
  });

  state$: Signal<DeleteConfirmDialogState> = this.state.asReadonly();

  /**
   * Opens the delete confirmation dialog with integrated change note input.
   * Returns a promise that resolves to:
   * - The change note string if user confirmed
   * - null if user cancelled
   */
  confirm(config: DeleteConfirmDialogConfig): Promise<string | null> {
    return new Promise((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          ...config,
          confirmText: config.confirmText || 'Delete',
          cancelText: config.cancelText || 'Cancel',
          placeholder: config.placeholder || 'Enter change note...',
        },
        changeNote: '',
        resolve,
      });
    });
  }

  handleConfirm(): void {
    const currentState = this.state();
    if (currentState.resolve) {
      const note = currentState.changeNote.trim();
      // If change note is mandatory and empty, don't confirm
      if (currentState.config?.changeNoteMandatory && !note) {
        return;
      }
      currentState.resolve(note || '');
    }
    this.close();
  }

  handleCancel(): void {
    const currentState = this.state();
    if (currentState.resolve) {
      currentState.resolve(null);
    }
    this.close();
  }

  updateChangeNote(value: string): void {
    this.state.update(s => ({ ...s, changeNote: value }));
  }

  private close(): void {
    this.state.set({
      isOpen: false,
      config: null,
      changeNote: '',
      resolve: null,
    });
  }
}
