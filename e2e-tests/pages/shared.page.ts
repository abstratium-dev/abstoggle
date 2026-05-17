import { expect, Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Shared low-level helpers used across multiple page objects
// ---------------------------------------------------------------------------

/**
 * Clicks the confirm button in the confirmation dialog.
 * The dialog is rendered by ConfirmDialogComponent and has a `.dialog-overlay`.
 *
 * @param page - Playwright page
 * @param confirmButtonText - Text of the confirm button (e.g. "Delete", "Update")
 */
export async function confirmDialog(page: Page, confirmButtonText: string): Promise<void> {
    console.log(`Dialog: waiting for confirmation dialog with button "${confirmButtonText}"`);
    const dialog = page.locator('.dialog-overlay');
    await dialog.waitFor({ state: 'visible', timeout: 10000 });
    const confirmButton = dialog.locator('button', { hasText: confirmButtonText });
    await expect(confirmButton).toBeVisible({ timeout: 5000 });
    await confirmButton.click();
    await dialog.waitFor({ state: 'hidden', timeout: 10000 });
    console.log(`Dialog: confirmed with "${confirmButtonText}"`);
}

/**
 * Dismisses (cancels) the confirmation dialog.
 */
export async function cancelDialog(page: Page): Promise<void> {
    console.log('Dialog: cancelling confirmation dialog');
    const dialog = page.locator('.dialog-overlay');
    await dialog.waitFor({ state: 'visible', timeout: 10000 });
    const cancelButton = dialog.locator('button', { hasText: 'Cancel' });
    await expect(cancelButton).toBeVisible({ timeout: 5000 });
    await cancelButton.click();
    await dialog.waitFor({ state: 'hidden', timeout: 10000 });
    console.log('Dialog: cancelled');
}

/**
 * Asserts that an error toast is visible and contains the expected text.
 * Uses partial (substring) matching so tests don't need to know UUIDs etc.
 */
export async function assertErrorToast(page: Page, partialText: string): Promise<void> {
    console.log(`Toast: asserting error toast contains "${partialText}"`);
    const toast = page.locator('.toast-error');
    await expect(toast).toBeVisible({ timeout: 8000 });
    await expect(toast.locator('.toast-message')).toContainText(partialText, { timeout: 5000 });
    console.log('Toast: error toast confirmed');
}

/**
 * Dismisses the currently visible error toast by clicking its close button.
 */
export async function dismissErrorToast(page: Page): Promise<void> {
    console.log('Toast: dismissing error toast');
    const toast = page.locator('.toast-error');
    await toast.waitFor({ state: 'visible', timeout: 8000 });
    await toast.locator('.toast-close').click();
    await toast.waitFor({ state: 'hidden', timeout: 8000 });
    console.log('Toast: error toast dismissed');
}

/**
 * Selects a value in an abs-autocomplete widget by typing the search term and
 * clicking the matching dropdown option.
 *
 * @param page         - Playwright page
 * @param containerId  - The `id` attribute of the `abs-autocomplete` host element
 * @param searchTerm   - Text to type into the autocomplete input
 * @param optionLabel  - Visible label of the option to select (must be an exact or partial match)
 */
export async function selectAutocompleteOption(
    page: Page,
    containerId: string,
    searchTerm: string,
    optionLabel: string
): Promise<void> {
    console.log(`Autocomplete #${containerId}: typing "${searchTerm}", selecting "${optionLabel}"`);
    const container = page.locator(`abs-autocomplete#${containerId}`);
    const input = container.locator('input.autocomplete-input');
    await input.waitFor({ state: 'visible', timeout: 10000 });
    await input.fill(searchTerm);

    const dropdown = container.locator('.dropdown');
    await dropdown.waitFor({ state: 'visible', timeout: 10000 });

    // Use exact text matching so "temp" does not accidentally match "temp-child"
    const option = dropdown.locator('.dropdown-item').filter({ hasText: new RegExp(`^\\s*${optionLabel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`) });
    await option.waitFor({ state: 'visible', timeout: 10000 });
    await option.click();

    // Dropdown should close after selection
    await dropdown.waitFor({ state: 'hidden', timeout: 10000 });
    // Brief pause to allow Angular to process the selection and re-render dependent fields
    await page.waitForTimeout(300);
    console.log(`Autocomplete #${containerId}: selected "${optionLabel}"`);
}
