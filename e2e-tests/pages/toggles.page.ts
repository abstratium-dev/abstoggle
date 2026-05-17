import { expect, Page } from '@playwright/test';
import { confirmDialog, selectAutocompleteOption, assertErrorToast, dismissErrorToast } from './shared.page';

// ---------------------------------------------------------------------------
// Low-level element accessors – Toggle form
// ---------------------------------------------------------------------------

function addToggleButton(page: Page) {
    return page.locator('button.btn-add', { hasText: '+ Add Toggle' });
}

function toggleNameInput(page: Page) {
    return page.locator('input#toggleName');
}

function toggleDescriptionInput(page: Page) {
    return page.locator('textarea#toggleDescription');
}

function toggleContextAutocomplete(page: Page) {
    return page.locator('abs-autocomplete#toggleContext input.autocomplete-input');
}

function toggleEnabledCheckbox(page: Page) {
    return page.locator('input#toggleEnabled');
}

function createToggleSubmitButton(page: Page) {
    return page.locator('button.btn-primary', { hasText: /Create Toggle|Update Toggle/ });
}

function cancelToggleFormButton(page: Page) {
    return page.locator('button.btn-secondary', { hasText: 'Cancel' });
}

/** Returns the table row for a toggle by its name (in the main toggles table). */
function toggleRowByName(page: Page, toggleName: string) {
    return page.locator('table.standard-table tbody tr').filter({
        has: page.locator('td strong', { hasText: toggleName })
    });
}

function deleteToggleButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon-danger[title="Delete toggle"]');
}

function editToggleButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon[title="Edit toggle"]');
}

function manageAssignmentsButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon[title="Manage"]');
}

// ---------------------------------------------------------------------------
// Low-level element accessors – Assignments panel
// ---------------------------------------------------------------------------

function assignmentsPanelHeader(page: Page, toggleName: string) {
    return page.locator('.form-container h2', { hasText: `Manage Stage & Rule Assignments for "${toggleName}"` });
}

function closeAssignmentsPanelButton(page: Page) {
    return page.locator('.section-header button.btn-secondary', { hasText: 'Close' });
}

function addAssignmentButton(page: Page) {
    return page.locator('button.btn-add', { hasText: '+ Add Assignment' });
}

function stageAutocompleteInAssignmentForm(page: Page) {
    return page.locator('abs-autocomplete#stageName');
}

function ruleAutocompleteInAssignmentForm(page: Page) {
    return page.locator('abs-autocomplete#ruleId');
}

function assignmentValueInput(page: Page) {
    return page.locator('input#ruleValue');
}

function assignmentPriorityInput(page: Page) {
    return page.locator('input#priority');
}

function saveAssignmentButton(page: Page) {
    return page.locator('button.btn-add', { hasText: /Add Assignment|Update/ });
}

function cancelAssignmentFormButton(page: Page) {
    return page.locator('button.btn-secondary', { hasText: 'Cancel' });
}

/** Returns a row in the assignments table matching stage + rule names. */
function assignmentRowByStageAndRule(page: Page, stageName: string, ruleName: string) {
    return page.locator('.form-container table.standard-table tbody tr').filter({
        hasText: stageName
    }).filter({
        hasText: ruleName
    });
}

function deleteAssignmentButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon-danger[title="Delete assignment"]');
}

function editAssignmentButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon[title="Edit priority"]');
}

/** All rows in the assignments table (scoped to the managing panel). */
function allAssignmentRows(page: Page) {
    return page.locator('.form-container table.standard-table tbody tr');
}

// ---------------------------------------------------------------------------
// Higher-level functions – Toggle CRUD
// ---------------------------------------------------------------------------

/**
 * Waits for the toggles page to finish loading.
 */
export async function waitForTogglesPageReady(page: Page): Promise<void> {
    console.log('Toggles: waiting for page to be ready');
    await expect(page.locator('.loading')).not.toBeVisible({ timeout: 15000 });
    console.log('Toggles: page ready');
}

/**
 * Creates a new toggle.
 * Assumes the user is already on the Toggles page.
 */
export async function createToggle(
    page: Page,
    name: string,
    options: { description?: string; context?: string; enabled?: boolean } = {}
): Promise<void> {
    console.log(`Toggles: creating toggle "${name}"`);
    await waitForTogglesPageReady(page);

    await addToggleButton(page).waitFor({ state: 'visible', timeout: 10000 });
    await addToggleButton(page).click();

    await toggleNameInput(page).waitFor({ state: 'visible', timeout: 10000 });
    await toggleNameInput(page).fill(name);

    if (options.description !== undefined) {
        await toggleDescriptionInput(page).fill(options.description);
    }

    if (options.context !== undefined) {
        const contextInput = toggleContextAutocomplete(page);
        await contextInput.waitFor({ state: 'visible', timeout: 10000 });
        await contextInput.fill(options.context);
    }

    if (options.enabled === false) {
        const checkbox = toggleEnabledCheckbox(page);
        if (await checkbox.isChecked()) {
            await checkbox.uncheck();
        }
    }

    await createToggleSubmitButton(page).click();

    // Wait for success: form closes
    await expect(toggleNameInput(page)).not.toBeVisible({ timeout: 10000 });
    console.log(`Toggles: toggle "${name}" created`);
}

/**
 * Attempts to delete a toggle that still has assignments, confirms the dialog,
 * then asserts an error toast containing the expected partial text is shown.
 * Dismisses the toast before returning so subsequent interactions are clean.
 */
export async function tryDeleteToggleAndAssertError(
    page: Page,
    toggleName: string,
    expectedErrorText: string
): Promise<void> {
    console.log(`Toggles: attempting to delete "${toggleName}" (expecting error)`);
    await waitForTogglesPageReady(page);
    const row = toggleRowByName(page, toggleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await deleteToggleButtonInRow(row).click();
    await confirmDialog(page, 'Delete');
    await assertErrorToast(page, expectedErrorText);
    await dismissErrorToast(page);
    await expect(row).toBeVisible({ timeout: 5000 });
    console.log(`Toggles: deletion of "${toggleName}" correctly rejected`);
}

/**
 * Deletes a toggle by name if it exists (does NOT delete assignments first –
 * use deleteAllAssignmentsForToggle() before calling this if needed).
 * Asserts that it is no longer in the table after deletion.
 */
export async function deleteToggleIfExists(page: Page, toggleName: string): Promise<void> {
    console.log(`Toggles: checking if toggle "${toggleName}" exists for deletion`);
    await waitForTogglesPageReady(page);

    const row = toggleRowByName(page, toggleName);
    const count = await row.count();
    if (count === 0) {
        console.log(`Toggles: toggle "${toggleName}" not found, skipping deletion`);
        return;
    }

    console.log(`Toggles: deleting toggle "${toggleName}"`);
    await deleteToggleButtonInRow(row).click();
    await confirmDialog(page, 'Delete');
    await assertToggleAbsent(page, toggleName);
    console.log(`Toggles: toggle "${toggleName}" deleted and confirmed absent`);
}

/**
 * Asserts that a toggle with the given name IS present in the main toggles table.
 */
export async function assertTogglePresent(
    page: Page,
    toggleName: string,
    expectedContext?: string
): Promise<void> {
    console.log(`Toggles: asserting toggle "${toggleName}" is present`);
    const row = toggleRowByName(page, toggleName);
    await expect(row).toBeVisible({ timeout: 10000 });

    if (expectedContext !== undefined) {
        // Context is the 2nd <td> (index 1)
        const contextCell = row.locator('td').nth(1);
        await expect(contextCell).toHaveText(expectedContext, { timeout: 5000 });
    }
    console.log(`Toggles: toggle "${toggleName}" confirmed present`);
}

/**
 * Asserts that a toggle with the given name is NOT present in the table.
 */
export async function assertToggleAbsent(page: Page, toggleName: string): Promise<void> {
    console.log(`Toggles: asserting toggle "${toggleName}" is absent`);
    const row = toggleRowByName(page, toggleName);
    await expect(row).toHaveCount(0, { timeout: 10000 });
    console.log(`Toggles: toggle "${toggleName}" confirmed absent`);
}

// ---------------------------------------------------------------------------
// Higher-level functions – Assignment management
// ---------------------------------------------------------------------------

/**
 * Opens the "Manage Stage & Rule Assignments" panel for a toggle.
 * Assumes the user is on the Toggles page.
 */
export async function openManageAssignments(page: Page, toggleName: string): Promise<void> {
    console.log(`Toggles: opening assignments panel for "${toggleName}"`);
    const row = toggleRowByName(page, toggleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await manageAssignmentsButtonInRow(row).click();
    await expect(assignmentsPanelHeader(page, toggleName)).toBeVisible({ timeout: 10000 });
    console.log(`Toggles: assignments panel opened for "${toggleName}"`);
}

/**
 * Closes the assignments panel.
 */
export async function closeAssignmentsPanel(page: Page): Promise<void> {
    console.log('Toggles: closing assignments panel');
    await closeAssignmentsPanelButton(page).click();
    await expect(closeAssignmentsPanelButton(page)).not.toBeVisible({ timeout: 10000 });
    console.log('Toggles: assignments panel closed');
}

/**
 * Adds a new assignment (stage + rule + value) to the currently open toggle.
 * The assignments panel must already be open.
 */
export async function addAssignment(
    page: Page,
    stageName: string,
    ruleName: string,
    value: string,
    priority?: number
): Promise<void> {
    console.log(`Toggles: adding assignment stage="${stageName}" rule="${ruleName}" value="${value}"`);

    await addAssignmentButton(page).waitFor({ state: 'visible', timeout: 10000 });
    await addAssignmentButton(page).click();

    // Select stage via autocomplete
    await selectAutocompleteOption(page, 'stageName', stageName, stageName);

    // Select rule via autocomplete
    await selectAutocompleteOption(page, 'ruleId', ruleName, ruleName);

    if (priority !== undefined) {
        await assignmentPriorityInput(page).fill(String(priority));
    }

    // Clear and fill value
    await assignmentValueInput(page).fill(value);

    await saveAssignmentButton(page).click();

    // Wait for form to close (Add Assignment button reappears)
    await expect(addAssignmentButton(page)).toBeVisible({ timeout: 10000 });
    console.log(`Toggles: assignment added for stage="${stageName}" rule="${ruleName}"`);
}

/**
 * Deletes all assignments currently visible in the open assignments panel.
 * Asserts that the panel shows "No stage & rule assignments yet" afterwards.
 */
export async function deleteAllAssignments(page: Page): Promise<void> {
    console.log('Toggles: deleting all assignments in panel');

    // Repeat until no assignment rows remain
    let deleted = 0;
    while (true) {
        const rows = allAssignmentRows(page);
        const count = await rows.count();
        if (count === 0) {
            break;
        }
        console.log(`Toggles: ${count} assignment(s) remaining, deleting first`);
        await deleteAssignmentButtonInRow(rows.first()).click();
        await confirmDialog(page, 'Delete');
        deleted++;
        // Small wait for DOM update
        await page.waitForTimeout(300);
    }

    if (deleted > 0) {
        // Only wait for the empty-state message if we actually deleted something
        await expect(page.locator('.form-container .info-message', { hasText: 'No stage & rule assignments yet' })).toBeVisible({ timeout: 10000 });
    }
    console.log(`Toggles: all assignments deleted (${deleted} removed)`);
}

/**
 * Deletes all assignments for a toggle and then deletes the toggle itself.
 * Navigates to the Toggles page first if the toggle row is visible.
 * Asserts the toggle is absent after deletion.
 */
export async function deleteAllAssignmentsAndToggle(page: Page, toggleName: string): Promise<void> {
    console.log(`Toggles: full deletion of toggle "${toggleName}" and its assignments`);
    await waitForTogglesPageReady(page);

    const row = toggleRowByName(page, toggleName);
    const count = await row.count();
    if (count === 0) {
        console.log(`Toggles: toggle "${toggleName}" not found, skipping`);
        return;
    }

    await openManageAssignments(page, toggleName);
    await deleteAllAssignments(page);
    await closeAssignmentsPanel(page);
    await deleteToggleIfExists(page, toggleName);
}

/**
 * Edits an existing assignment (by stage + rule) to change its value.
 * The assignments panel must already be open.
 */
export async function editAssignmentValue(
    page: Page,
    stageName: string,
    ruleName: string,
    newValue: string
): Promise<void> {
    console.log(`Toggles: editing assignment stage="${stageName}" rule="${ruleName}" → value="${newValue}"`);
    const row = assignmentRowByStageAndRule(page, stageName, ruleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await editAssignmentButtonInRow(row).click();

    // Value input is now editable (edit mode)
    await assignmentValueInput(page).waitFor({ state: 'visible', timeout: 10000 });
    await assignmentValueInput(page).fill(newValue);

    await saveAssignmentButton(page).click();

    // Wait for the row to reflect the new value
    await expect(row.locator('td code', { hasText: newValue })).toBeVisible({ timeout: 10000 });
    console.log(`Toggles: assignment edited to value="${newValue}"`);
}

/**
 * Asserts that an assignment row with the given stage + rule + value is present
 * in the open assignments panel.
 */
export async function assertAssignmentPresent(
    page: Page,
    stageName: string,
    ruleName: string,
    value: string
): Promise<void> {
    console.log(`Toggles: asserting assignment stage="${stageName}" rule="${ruleName}" value="${value}"`);
    const row = assignmentRowByStageAndRule(page, stageName, ruleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row.locator('td code', { hasText: value })).toBeVisible({ timeout: 5000 });
    console.log(`Toggles: assignment confirmed present`);
}

/**
 * Asserts that no assignment rows are present in the open assignments panel.
 */
export async function assertNoAssignments(page: Page): Promise<void> {
    console.log('Toggles: asserting no assignments are present');
    await expect(
        page.locator('.form-container .info-message', { hasText: 'No stage & rule assignments yet' })
    ).toBeVisible({ timeout: 10000 });
    console.log('Toggles: confirmed no assignments');
}
