import { Injectable, signal, Signal } from '@angular/core';

export interface ChangeNoteDialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmClass?: string;
  placeholder?: string;
  /** If true, allows skipping the change note (returns empty string). */
  optional?: boolean;
  skipText?: string;
}

interface ChangeNoteDialogState {
  isOpen: boolean;
  config: ChangeNoteDialogConfig | null;
  changeNote: string;
  resolve: ((value: string | null) => void) | null;
  isOptional: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ChangeNoteDialogService {
  private state = signal<ChangeNoteDialogState>({
    isOpen: false,
    config: null,
    changeNote: '',
    resolve: null,
    isOptional: false,
  });

  state$: Signal<ChangeNoteDialogState> = this.state.asReadonly();

  prompt(config: ChangeNoteDialogConfig): Promise<string | null> {
    return new Promise((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          ...config,
          confirmText: config.confirmText || 'Confirm',
          cancelText: config.cancelText || 'Cancel',
          confirmClass: config.confirmClass || 'btn-primary',
          placeholder: config.placeholder || 'Enter change note...',
          skipText: config.skipText || 'Skip',
        },
        changeNote: '',
        resolve,
        isOptional: config.optional ?? false,
      });
    });
  }

  /**
   * Prompts for a change note, but if optional mode is enabled and user skips,
   * returns an empty string instead of null.
   * Use this when you want to always proceed but capture notes when provided.
   */
  async promptWithOptional(config: ChangeNoteDialogConfig & { optional: boolean }): Promise<string> {
    const result = await this.prompt(config);
    if (result === null) {
      // User cancelled
      return '';
    }
    return result;
  }

  handleConfirm(): void {
    const currentState = this.state();
    if (currentState.resolve) {
      const note = currentState.changeNote.trim();
      currentState.resolve(note || null);
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

  handleSkip(): void {
    const currentState = this.state();
    if (currentState.resolve) {
      currentState.resolve('');
    }
    this.close();
  }

  private close(): void {
    this.state.set({
      isOpen: false,
      config: null,
      changeNote: '',
      resolve: null,
      isOptional: false,
    });
  }
}
