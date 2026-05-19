import { expect, Page } from '@playwright/test';
import { confirmDialog, confirmDeleteDialog, assertErrorToast, dismissErrorToast } from './shared.page';

// ---------------------------------------------------------------------------
// Low-level element accessors
// ---------------------------------------------------------------------------

function addStageButton(page: Page) {
    return page.locator('button.btn-add', { hasText: '+ Add Stage' });
}

function stageNameInput(page: Page) {
    return page.locator('input#stageName');
}

function stageDescriptionInput(page: Page) {
    return page.locator('textarea#stageDescription');
}

function stageDisplayOrderInput(page: Page) {
    return page.locator('input#stageDisplayOrder');
}

function stageParentNameSelect(page: Page) {
    return page.locator('select#stageParentName');
}

function changeNoteInput(page: Page) {
    return page.locator('input#changeNote');
}

function createStageSubmitButton(page: Page) {
    return page.locator('button.btn-primary', { hasText: /Create Stage|Update Stage/ });
}

function cancelFormButton(page: Page) {
    return page.locator('button.btn-secondary', { hasText: 'Cancel' });
}

function formErrorBox(page: Page) {
    return page.locator('.form-container .error-box');
}

/** Returns the table row locator for a stage by its name (substring match). */
function stageRowByName(page: Page, stageName: string) {
    return page.locator('table.standard-table tbody tr').filter({ has: page.locator('td strong', { hasText: stageName }) });
}

/** Returns the table row locator for a stage by its exact name only. */
function stageRowByNameExact(page: Page, stageName: string) {
    return page.locator('table.standard-table tbody tr').filter({
        has: page.locator('td strong').getByText(stageName, { exact: true })
    });
}

/** Returns the delete button within a stage row. */
function deleteButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon-danger[title="Delete stage"]');
}

/** Returns the edit button within a stage row. */
function editButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon[title="Edit stage"]');
}

// ---------------------------------------------------------------------------
// Higher-level functions
// ---------------------------------------------------------------------------

/**
 * Waits for the stages table to be visible (i.e. at least one row present OR
 * the "No stages found" message is shown).
 */
export async function waitForStagesPageReady(page: Page): Promise<void> {
    console.log('Stages: waiting for page to be ready');
    // Wait until neither the loading indicator nor an error is shown
    await expect(page.locator('.loading')).not.toBeVisible({ timeout: 15000 });
    console.log('Stages: page ready');
}

/**
 * Creates a new stage.
 * Assumes the user is already on the Stages page.
 */
export async function createStage(
    page: Page,
    name: string,
    options: { description?: string; displayOrder?: number; parentName?: string } = {}
): Promise<void> {
    console.log(`Stages: creating stage "${name}"`);
    await waitForStagesPageReady(page);

    await addStageButton(page).waitFor({ state: 'visible', timeout: 10000 });
    await addStageButton(page).click();

    await stageNameInput(page).waitFor({ state: 'visible', timeout: 10000 });
    await stageNameInput(page).fill(name);

    if (options.description !== undefined) {
        await stageDescriptionInput(page).fill(options.description);
    }

    if (options.displayOrder !== undefined) {
        await stageDisplayOrderInput(page).fill(String(options.displayOrder));
    }

    if (options.parentName) {
        await stageParentNameSelect(page).selectOption({ label: options.parentName });
    }

    // Fill in change note (required field)
    await changeNoteInput(page).fill(`Created stage ${name}`);

    await createStageSubmitButton(page).click();

    // Wait for success: form closes and stage appears in table
    await expect(stageNameInput(page)).not.toBeVisible({ timeout: 10000 });
    console.log(`Stages: stage "${name}" created`);
}

/**
 * Attempts to delete a stage that is still used in toggle assignments, confirms
 * the dialog, then asserts an error toast containing the expected partial text.
 * Dismisses the toast before returning so subsequent interactions are clean.
 */
export async function tryDeleteStageAndAssertError(
    page: Page,
    stageName: string,
    expectedErrorText: string
): Promise<void> {
    console.log(`Stages: attempting to delete "${stageName}" (expecting error)`);
    await waitForStagesPageReady(page);
    const row = stageRowByNameExact(page, stageName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await deleteButtonInRow(row).click();
    await confirmDeleteDialog(page, `Attempted to delete "${stageName}"`, 'Delete');
    await assertErrorToast(page, expectedErrorText);
    await dismissErrorToast(page);
    await expect(row).toBeVisible({ timeout: 5000 });
    console.log(`Stages: deletion of "${stageName}" correctly rejected`);
}

/**
 * Deletes a stage by name if it exists.
 * Asserts that it is no longer in the table after deletion.
 * If the stage does not exist, this function is a no-op.
 */
export async function deleteStageIfExists(page: Page, stageName: string): Promise<void> {
    console.log(`Stages: checking if stage "${stageName}" exists for deletion`);
    await waitForStagesPageReady(page);

    const row = stageRowByName(page, stageName);
    const count = await row.count();
    if (count === 0) {
        console.log(`Stages: stage "${stageName}" not found, skipping deletion`);
        return;
    }

    console.log(`Stages: deleting stage "${stageName}"`);
    await deleteButtonInRow(row).click();
    await confirmDeleteDialog(page, `Deleted stage "${stageName}"`, 'Delete');

    // Check for a toast error (e.g. referential integrity prevented deletion)
    await page.waitForTimeout(500);
    const errorToast = page.locator('.toast-error');
    if (await errorToast.isVisible()) {
        const msg = await errorToast.textContent();
        throw new Error(`Stages: server rejected deletion of "${stageName}": ${msg?.trim()}`);
    }

    await assertStageAbsent(page, stageName);
    console.log(`Stages: stage "${stageName}" deleted and confirmed absent`);
}

/**
 * Asserts that a stage with the given name IS present in the table
 * and returns its row for further assertions.
 */
export async function assertStagePresent(
    page: Page,
    stageName: string,
    expectedParent?: string
): Promise<void> {
    console.log(`Stages: asserting stage "${stageName}" is present`);
    const row = stageRowByName(page, stageName);
    await expect(row).toBeVisible({ timeout: 10000 });

    if (expectedParent !== undefined) {
        // Parent stage is the 4th <td> in the row
        const parentCell = row.locator('td').nth(3);
        await expect(parentCell).toHaveText(expectedParent, { timeout: 5000 });
    }
    console.log(`Stages: stage "${stageName}" confirmed present`);
}

/**
 * Asserts that a stage with the given name is NOT present in the table.
 */
export async function assertStageAbsent(page: Page, stageName: string): Promise<void> {
    console.log(`Stages: asserting stage "${stageName}" is absent`);
    const row = stageRowByName(page, stageName);
    await expect(row).toHaveCount(0, { timeout: 10000 });
    console.log(`Stages: stage "${stageName}" confirmed absent`);
}
