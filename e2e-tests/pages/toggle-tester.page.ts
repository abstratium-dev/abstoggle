import { expect, Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Low-level element accessors
// ---------------------------------------------------------------------------

function stageSelect(page: Page) {
    return page.locator('select#stageSelect');
}

function contextInput(page: Page) {
    return page.locator('abs-autocomplete#contextInput input.autocomplete-input');
}

function nameFilterInput(page: Page) {
    return page.locator('input#nameFilter');
}

function runQueryButton(page: Page) {
    return page.locator('button#run-query-btn');
}

function clearCacheButton(page: Page) {
    return page.locator('button#clear-cache-btn');
}

function resetContextButton(page: Page) {
    return page.locator('button.btn-secondary', { hasText: 'Reset Context' });
}

/**
 * Returns the existing context entry row for a given key.
 * Each row is a `.tester-context-row` containing two inputs (key, value).
 */
function contextEntryRowByKey(page: Page, key: string) {
    // Match any tester-context-row that is NOT the add-row and whose first input has the given key value
    return page.locator('.tester-context-row:not(.tester-context-add)').filter({
        has: page.locator('input[placeholder="Key"]')
    }).filter({
        // The first input in the row should have the value equal to `key`
        has: page.locator(`input[value="${key}"]`)
    });
}

/** The second input (value) within a context entry row. */
function contextEntryValueInput(row: ReturnType<Page['locator']>) {
    return row.locator('input').nth(1);
}

/** Key input in the "add new context entry" row. */
function newContextKeyInput(page: Page) {
    return page.locator('input[name="newCtxKey"]');
}

/** Value input in the "add new context entry" row. */
function newContextValueInput(page: Page) {
    return page.locator('input[name="newCtxVal"]');
}

/** "+ Add" button in the new-context-entry row. */
function addContextEntryButton(page: Page) {
    return page.locator('.tester-context-add button.btn-add-small', { hasText: '+ Add' });
}

function resultsHeading(page: Page) {
    return page.locator('h2', { hasText: /Results for stage/ });
}

function resultsTable(page: Page) {
    return page.locator('table.standard-table').filter({ has: page.locator('th', { hasText: 'Resolved Value' }) });
}

/** Returns the results table row for a given toggle name. */
function resultRowByToggleName(page: Page, toggleName: string) {
    return resultsTable(page).locator('tbody tr').filter({
        has: page.locator('td strong', { hasText: toggleName })
    });
}

function queryErrorBox(page: Page) {
    return page.locator('.error-box');
}

// ---------------------------------------------------------------------------
// Higher-level functions
// ---------------------------------------------------------------------------

/**
 * Waits for the Toggle Tester page to be ready (form is visible).
 */
export async function waitForTesterPageReady(page: Page): Promise<void> {
    console.log('Tester: waiting for page to be ready');
    await stageSelect(page).waitFor({ state: 'visible', timeout: 10000 });
    console.log('Tester: page ready');
}

/**
 * Selects a stage from the Stage dropdown.
 */
export async function selectStage(page: Page, stageName: string): Promise<void> {
    console.log(`Tester: selecting stage "${stageName}"`);
    await stageSelect(page).waitFor({ state: 'visible', timeout: 10000 });
    await stageSelect(page).selectOption({ label: stageName });
    console.log(`Tester: stage "${stageName}" selected`);
}

/**
 * Fills in the Context field.
 */
export async function setContext(page: Page, context: string): Promise<void> {
    console.log(`Tester: setting context to "${context}"`);
    await contextInput(page).fill(context);
}

/**
 * Fills in the Toggle Name Filter (leave empty to query all toggles).
 */
export async function setNameFilter(page: Page, filter: string): Promise<void> {
    console.log(`Tester: setting name filter to "${filter}"`);
    await nameFilterInput(page).fill(filter);
}

/**
 * Clears the Toggle Name Filter.
 */
export async function clearNameFilter(page: Page): Promise<void> {
    console.log('Tester: clearing name filter');
    await nameFilterInput(page).fill('');
}

/**
 * Updates the value of an existing context criterion identified by its key.
 * The key input must already exist as a row in the context table (e.g. from a
 * previous run or after calling addContextEntry).
 *
 * Strategy: look for an input whose current value matches `key` in the key
 * position, then set the adjacent value input to `value`.
 */
export async function setExistingContextEntry(page: Page, key: string, value: string): Promise<void> {
    console.log(`Tester: setting context entry "${key}" = "${value}"`);

    // Find all non-add context rows
    const rows = page.locator('.tester-context-row:not(.tester-context-add)');
    const rowCount = await rows.count();

    let found = false;
    for (let i = 0; i < rowCount; i++) {
        const row = rows.nth(i);
        const keyInput = row.locator('input').nth(0);
        const currentKey = await keyInput.inputValue();
        if (currentKey === key) {
            const valueInput = row.locator('input').nth(1);
            await valueInput.fill(value);
            found = true;
            console.log(`Tester: updated context entry "${key}" = "${value}"`);
            break;
        }
    }

    if (!found) {
        throw new Error(`Tester: context entry with key "${key}" not found. Use addContextEntry() to add it first.`);
    }
}

/**
 * Adds a new context key/value entry (or updates it if it already exists via
 * the "+ Add" button in the add row).
 */
export async function addContextEntry(page: Page, key: string, value: string): Promise<void> {
    console.log(`Tester: adding context entry "${key}" = "${value}"`);
    await newContextKeyInput(page).fill(key);
    await newContextValueInput(page).fill(value);
    await addContextEntryButton(page).click();
    console.log(`Tester: context entry added`);
}

/**
 * Resets the client context to the application defaults.
 */
export async function resetContext(page: Page): Promise<void> {
    console.log('Tester: resetting context');
    await resetContextButton(page).click();
}

/**
 * Clicks "Clear Cache" to evict the current query's result from the server-side cache.
 * Waits for the button to stop showing "Clearing..." before returning.
 */
export async function clearCache(page: Page): Promise<void> {
    console.log('Tester: clearing cache');
    await clearCacheButton(page).waitFor({ state: 'visible', timeout: 10000 });
    await clearCacheButton(page).click();
    // Wait until the button stops loading
    await expect(clearCacheButton(page)).not.toHaveText(/Clearing/, { timeout: 10000 });
    console.log('Tester: cache cleared');
}

/**
 * Clicks "Run Query" and waits for results to appear.
 */
export async function runQuery(page: Page): Promise<void> {
    console.log('Tester: running query');
    await runQueryButton(page).click();
    // Wait for either results or an error
    await Promise.race([
        resultsHeading(page).waitFor({ state: 'visible', timeout: 15000 }),
        queryErrorBox(page).waitFor({ state: 'visible', timeout: 15000 })
    ]);
    const errorVisible = await queryErrorBox(page).isVisible();
    if (errorVisible) {
        const errorText = await queryErrorBox(page).textContent();
        throw new Error(`Tester: query failed with error: ${errorText}`);
    }
    console.log('Tester: query completed, results visible');
}

/**
 * Asserts the results heading shows the expected stage name.
 */
export async function assertResultsHeading(page: Page, stageName: string): Promise<void> {
    console.log(`Tester: asserting results heading for stage "${stageName}"`);
    await expect(resultsHeading(page)).toContainText(`Results for stage "${stageName}"`, { timeout: 10000 });
    console.log(`Tester: results heading confirmed`);
}

/**
 * Asserts that a specific toggle has the expected resolved value in the results table.
 */
export async function assertToggleResult(
    page: Page,
    toggleName: string,
    expectedValue: string
): Promise<void> {
    console.log(`Tester: asserting toggle "${toggleName}" has resolved value "${expectedValue}"`);
    const row = resultRowByToggleName(page, toggleName);
    await expect(row).toBeVisible({ timeout: 10000 });
    // Resolved value is in a span.tester-value-badge (3rd data cell, index 2)
    const valueBadge = row.locator('td').nth(2).locator('span.tester-value-badge');
    await expect(valueBadge).toContainText(expectedValue, { timeout: 5000 });
    console.log(`Tester: toggle "${toggleName}" resolved to "${expectedValue}" ✓`);
}

/**
 * Asserts multiple toggle results at once.
 * @param expectations - Array of { toggleName, expectedValue }
 */
export async function assertToggleResults(
    page: Page,
    expectations: { toggleName: string; expectedValue: string }[]
): Promise<void> {
    console.log(`Tester: asserting ${expectations.length} toggle result(s)`);
    for (const { toggleName, expectedValue } of expectations) {
        await assertToggleResult(page, toggleName, expectedValue);
    }
    console.log('Tester: all toggle result assertions passed');
}

/**
 * Convenience function: select stage, set context, optionally set nameFilter,
 * run query, and assert results in one call.
 */
export async function queryAndAssert(
    page: Page,
    params: {
        stage: string;
        context: string;
        nameFilter?: string;
        contextUpdates?: { key: string; value: string }[];
    },
    expectations: { toggleName: string; expectedValue: string }[]
): Promise<void> {
    console.log(`Tester: queryAndAssert stage="${params.stage}" context="${params.context}"`);
    await selectStage(page, params.stage);
    await setContext(page, params.context);

    if (params.nameFilter !== undefined) {
        await setNameFilter(page, params.nameFilter);
    }

    if (params.contextUpdates) {
        for (const update of params.contextUpdates) {
            await setExistingContextEntry(page, update.key, update.value);
        }
    }

    await runQuery(page);
    await assertResultsHeading(page, params.stage);
    await assertToggleResults(page, expectations);
}
