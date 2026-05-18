import { expect, Page } from '@playwright/test';
import { confirmDialog, assertErrorToast, dismissErrorToast } from './shared.page';

// ---------------------------------------------------------------------------
// Low-level element accessors
// ---------------------------------------------------------------------------

function addRuleButton(page: Page) {
    return page.locator('button.btn-add', { hasText: '+ Add Rule' });
}

function ruleNameInput(page: Page) {
    return page.locator('input#ruleName');
}

function ruleDescriptionInput(page: Page) {
    return page.locator('textarea#ruleDescription');
}

function criteriaKeyInput(page: Page) {
    // First input in the criteria-input-row (Key field)
    return page.locator('.criteria-input-row input').nth(0);
}

function criteriaValueInput(page: Page) {
    // Second input in the criteria-input-row (Value field)
    return page.locator('.criteria-input-row input').nth(1);
}

function addCriterionButton(page: Page) {
    return page.locator('.criteria-input-row button.btn-add-small', { hasText: 'Add' });
}

function criteriaTagList(page: Page) {
    return page.locator('.criteria-list .criteria-tag');
}

function changeNoteInput(page: Page) {
    return page.locator('input#changeNote');
}

function createRuleSubmitButton(page: Page) {
    return page.locator('button.btn-primary', { hasText: /Create Rule|Update Rule/ });
}

/** Returns the table row locator for a rule by its name. */
function ruleRowByName(page: Page, ruleName: string) {
    return page.locator('table.standard-table tbody tr').filter({ has: page.locator('td strong', { hasText: ruleName }) });
}

/** Returns the delete button within a rule row. */
function deleteButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon-danger[title="Delete rule"]');
}

/** Returns the edit button within a rule row. */
function editButtonInRow(row: ReturnType<Page['locator']>) {
    return row.locator('button.btn-icon[title="Edit rule"]');
}

// ---------------------------------------------------------------------------
// Higher-level functions
// ---------------------------------------------------------------------------

/**
 * Waits for the rules page to finish loading.
 */
export async function waitForRulesPageReady(page: Page): Promise<void> {
    console.log('Rules: waiting for page to be ready');
    await expect(page.locator('.loading')).not.toBeVisible({ timeout: 15000 });
    console.log('Rules: page ready');
}

/**
 * Creates a new rule, optionally with criteria entries.
 * Assumes the user is already on the Rules page.
 *
 * @param criteria - Array of { key, value } pairs to add as criteria
 */
export async function createRule(
    page: Page,
    name: string,
    options: {
        description?: string;
        criteria?: { key: string; value: string }[];
    } = {}
): Promise<void> {
    console.log(`Rules: creating rule "${name}"`);
    await waitForRulesPageReady(page);

    await addRuleButton(page).waitFor({ state: 'visible', timeout: 10000 });
    await addRuleButton(page).click();

    await ruleNameInput(page).waitFor({ state: 'visible', timeout: 10000 });
    await ruleNameInput(page).fill(name);

    if (options.description) {
        await ruleDescriptionInput(page).fill(options.description);
    }

    if (options.criteria && options.criteria.length > 0) {
        for (const criterion of options.criteria) {
            console.log(`Rules: adding criterion ${criterion.key} = ${criterion.value}`);
            await criteriaKeyInput(page).fill(criterion.key);
            await criteriaValueInput(page).fill(criterion.value);
            await addCriterionButton(page).click();
            // Wait for the tag to appear in the list
            const tag = criteriaTagList(page).filter({ hasText: criterion.key });
            await expect(tag).toBeVisible({ timeout: 5000 });
        }
    }

    // Fill in change note (required field)
    await changeNoteInput(page).fill(`Created rule ${name}`);

    await createRuleSubmitButton(page).click();

    // Wait for success: form closes
    await expect(ruleNameInput(page)).not.toBeVisible({ timeout: 10000 });
    console.log(`Rules: rule "${name}" created`);
}

/**
 * Attempts to delete a rule that is still used in assignments, confirms the dialog,
 * then asserts an error toast containing the expected partial text is shown.
 * Dismisses the toast before returning so subsequent interactions are clean.
 */
export async function tryDeleteRuleAndAssertError(
    page: Page,
    ruleName: string,
    expectedErrorText: string
): Promise<void> {
    console.log(`Rules: attempting to delete "${ruleName}" (expecting error)`);
    await waitForRulesPageReady(page);
    const row = ruleRowByName(page, ruleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    await deleteButtonInRow(row).click();
    await confirmDialog(page, 'Delete');
    await assertErrorToast(page, expectedErrorText);
    await dismissErrorToast(page);
    await expect(row).toBeVisible({ timeout: 5000 });
    console.log(`Rules: deletion of "${ruleName}" correctly rejected`);
}

/**
 * Deletes a rule by name if it exists.
 * Asserts that it is no longer in the table after deletion.
 * If the rule does not exist, this function is a no-op.
 */
export async function deleteRuleIfExists(page: Page, ruleName: string): Promise<void> {
    console.log(`Rules: checking if rule "${ruleName}" exists for deletion`);
    await waitForRulesPageReady(page);

    const row = ruleRowByName(page, ruleName);
    const count = await row.count();
    if (count === 0) {
        console.log(`Rules: rule "${ruleName}" not found, skipping deletion`);
        return;
    }

    console.log(`Rules: deleting rule "${ruleName}"`);
    await deleteButtonInRow(row).click();
    await confirmDialog(page, 'Delete');
    await assertRuleAbsent(page, ruleName);
    console.log(`Rules: rule "${ruleName}" deleted and confirmed absent`);
}

/**
 * Asserts that a rule with the given name IS present in the table.
 */
export async function assertRulePresent(
    page: Page,
    ruleName: string,
    expectedCriteriaSummary?: string
): Promise<void> {
    console.log(`Rules: asserting rule "${ruleName}" is present`);
    const row = ruleRowByName(page, ruleName);
    await expect(row).toBeVisible({ timeout: 10000 });

    if (expectedCriteriaSummary !== undefined) {
        const criteriaCell = row.locator('td .criteria-summary');
        await expect(criteriaCell).toContainText(expectedCriteriaSummary, { timeout: 5000 });
    }
    console.log(`Rules: rule "${ruleName}" confirmed present`);
}

/**
 * Asserts that a rule with the given name is NOT present in the table.
 */
export async function assertRuleAbsent(page: Page, ruleName: string): Promise<void> {
    console.log(`Rules: asserting rule "${ruleName}" is absent`);
    const row = ruleRowByName(page, ruleName);
    await expect(row).toHaveCount(0, { timeout: 10000 });
    console.log(`Rules: rule "${ruleName}" confirmed absent`);
}
