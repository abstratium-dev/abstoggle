import { expect, Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Change Note Dialog - Page Object Model
// ---------------------------------------------------------------------------

/**
 * Fills in the change note dialog and confirms the action.
 * This dialog appears after the confirmation dialog when editing or deleting entities.
 *
 * @param page - Playwright page
 * @param changeNote - The change note text to enter
 * @param confirmButtonText - Text of the confirm button (e.g., "Delete", "Update", "Create")
 */
export async function fillChangeNoteDialog(
    page: Page,
    changeNote: string,
    confirmButtonText: string
): Promise<void> {
    console.log(`ChangeNoteDialog: filling with "${changeNote}" and confirming "${confirmButtonText}"`);

    // Wait for the dialog overlay to be visible
    const dialog = page.locator('ux-change-note-dialog .dialog-overlay');
    await dialog.waitFor({ state: 'visible', timeout: 10000 });

    // Fill in the change note input
    const input = dialog.locator('input#changeNote');
    await input.waitFor({ state: 'visible', timeout: 5000 });
    await input.fill(changeNote);

    // Click the confirm button
    const confirmButton = dialog.locator('button', { hasText: confirmButtonText });
    await expect(confirmButton).toBeVisible({ timeout: 5000 });
    await confirmButton.click();

    // Wait for dialog to close
    await dialog.waitFor({ state: 'hidden', timeout: 10000 });
    console.log(`ChangeNoteDialog: confirmed with "${confirmButtonText}"`);
}

/**
 * Cancels the change note dialog.
 *
 * @param page - Playwright page
 */
export async function cancelChangeNoteDialog(page: Page): Promise<void> {
    console.log('ChangeNoteDialog: cancelling');

    const dialog = page.locator('ux-change-note-dialog .dialog-overlay');
    await dialog.waitFor({ state: 'visible', timeout: 10000 });

    const cancelButton = dialog.locator('button', { hasText: 'Cancel' });
    await expect(cancelButton).toBeVisible({ timeout: 5000 });
    await cancelButton.click();

    await dialog.waitFor({ state: 'hidden', timeout: 10000 });
    console.log('ChangeNoteDialog: cancelled');
}
