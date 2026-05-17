import { test, expect, Page } from '@playwright/test';
import { signInAsAdmin } from '../pages/signin.page';
import { navigateToStages, navigateToRules, navigateToToggles, navigateToTester, assertHeaderIsVisible } from '../pages/header.page';
import { waitForStagesPageReady, createStage, deleteStageIfExists, assertStagePresent, assertStageAbsent, tryDeleteStageAndAssertError } from '../pages/stages.page';
import { waitForRulesPageReady, createRule, deleteRuleIfExists, assertRulePresent, assertRuleAbsent, tryDeleteRuleAndAssertError } from '../pages/rules.page';
import {
    waitForTogglesPageReady,
    createToggle,
    assertTogglePresent,
    assertToggleAbsent,
    openManageAssignments,
    closeAssignmentsPanel,
    addAssignment,
    deleteAllAssignments,
    deleteAllAssignmentsAndToggle,
    editAssignmentValue,
    assertAssignmentPresent,
    assertNoAssignments,
    tryDeleteToggleAndAssertError,
} from '../pages/toggles.page';
import {
    waitForTesterPageReady,
    queryAndAssert,
    selectStage,
    setContext,
    clearNameFilter,
    runQuery,
    clearCache,
    assertResultsHeading,
    assertToggleResults,
    setExistingContextEntry,
} from '../pages/toggle-tester.page';

// ---------------------------------------------------------------------------
// Reusable teardown: deletes all test data created by this spec
// ---------------------------------------------------------------------------
async function deleteAllTestData(page: Page): Promise<void> {
    console.log('=== deleteAllTestData: START ===');

    // Navigate to Toggles
    await navigateToToggles(page);
    await waitForTogglesPageReady(page);

    // Delete "feature-123" assignments + toggle
    await deleteAllAssignmentsAndToggle(page, 'feature-123');

    // Delete "maintenance-window" assignments + toggle
    await deleteAllAssignmentsAndToggle(page, 'maintenance-window');

    // artm-123 is pre-existing demo data that may also reference temp/temp-child stages.
    // Clear its assignments only (do NOT delete the toggle itself).
    const artm123Row = page.locator('table.standard-table tbody tr').filter({
        has: page.locator('td strong', { hasText: 'artm-123' })
    });
    if (await artm123Row.count() > 0) {
        console.log('deleteAllTestData: clearing artm-123 assignments to allow stage deletion');
        await openManageAssignments(page, 'artm-123');
        await deleteAllAssignments(page);
        await closeAssignmentsPanel(page);
    }

    // Navigate to Rules
    await navigateToRules(page);
    await waitForRulesPageReady(page);
    await deleteRuleIfExists(page, 'new-feature-users');
    await deleteRuleIfExists(page, 'catch-all');

    // Navigate to Stages – delete child before parent
    await navigateToStages(page);
    await waitForStagesPageReady(page);
    await deleteStageIfExists(page, 'temp-child');
    await deleteStageIfExists(page, 'temp');

    console.log('=== deleteAllTestData: END ===');
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------

test.describe('Full feature-toggle lifecycle (001)', () => {

    test('create, assign, evaluate, modify, and clean up toggles', async ({ page }) => {

        // =====================================================================
        // Bootstrap: navigate to the app
        // =====================================================================
        console.log('--- Boot: signing in ---');
        await signInAsAdmin(page);
        await assertHeaderIsVisible(page);

        // =====================================================================
        // STEP 1 – Pre-condition cleanup
        // =====================================================================
        console.log('--- Step 1: Pre-condition cleanup ---');
        await deleteAllTestData(page);

        // Verify absence after cleanup
        await navigateToToggles(page);
        await assertToggleAbsent(page, 'feature-123');
        await assertToggleAbsent(page, 'maintenance-window');

        await navigateToRules(page);
        await assertRuleAbsent(page, 'new-feature-users');
        await assertRuleAbsent(page, 'catch-all');

        await navigateToStages(page);
        await assertStageAbsent(page, 'temp-child');
        await assertStageAbsent(page, 'temp');

        // =====================================================================
        // STEP 2 – Create stages
        // =====================================================================
        console.log('--- Step 2: Create stages ---');
        await navigateToStages(page);

        await createStage(page, 'temp');
        await assertStagePresent(page, 'temp', '-');

        await createStage(page, 'temp-child', { parentName: 'temp' });
        await assertStagePresent(page, 'temp-child', 'temp');

        // =====================================================================
        // STEP 3 – Create rules
        // =====================================================================
        console.log('--- Step 3: Create rules ---');
        await navigateToRules(page);

        await createRule(page, 'catch-all');
        await assertRulePresent(page, 'catch-all', 'None (catch-all)');

        await createRule(page, 'new-feature-users', {
            criteria: [{ key: 'userId', value: '(alice|bob)' }]
        });
        await assertRulePresent(page, 'new-feature-users', 'userId: (alice|bob)');

        // =====================================================================
        // STEP 4 – Create toggle "maintenance-window" with assignment
        // =====================================================================
        console.log('--- Step 4: Create maintenance-window toggle ---');
        await navigateToToggles(page);

        await createToggle(page, 'maintenance-window', { context: 'public' });
        await assertTogglePresent(page, 'maintenance-window', 'public');

        await openManageAssignments(page, 'maintenance-window');
        await addAssignment(page, 'temp', 'catch-all', 'off');
        await assertAssignmentPresent(page, 'temp', 'catch-all', 'off');
        await closeAssignmentsPanel(page);

        // =====================================================================
        // STEP 5 – Create toggle "feature-123" with two assignments
        // =====================================================================
        console.log('--- Step 5: Create feature-123 toggle ---');
        await createToggle(page, 'feature-123', { context: 'public' });
        await assertTogglePresent(page, 'feature-123', 'public');

        await openManageAssignments(page, 'feature-123');

        await addAssignment(page, 'temp', 'new-feature-users', 'off');
        await assertAssignmentPresent(page, 'temp', 'new-feature-users', 'off');

        await addAssignment(page, 'temp-child', 'new-feature-users', 'on');
        await assertAssignmentPresent(page, 'temp-child', 'new-feature-users', 'on');

        await closeAssignmentsPanel(page);

        // =====================================================================
        // STEP 6 – Toggle Tester: initial evaluations
        // =====================================================================
        console.log('--- Step 6: Toggle Tester initial evaluations ---');
        await navigateToTester(page);
        await waitForTesterPageReady(page);

        // We need context entries in place. The tester loads default context entries.
        // Set context "public" and clear any name filter.
        await setContext(page, 'public');
        await clearNameFilter(page);

        // --- Query 6a: stage=temp, userId=bob ---
        console.log('--- Query 6a: temp / bob ---');
        await queryAndAssert(
            page,
            { stage: 'temp', context: 'public', contextUpdates: [{ key: 'userId', value: 'bob' }] },
            [
                { toggleName: 'maintenance-window', expectedValue: 'off' },
                { toggleName: 'feature-123', expectedValue: 'off' },
            ]
        );

        // --- Query 6b: stage=temp, userId=alice ---
        console.log('--- Query 6b: temp / alice ---');
        await setExistingContextEntry(page, 'userId', 'alice');
        await runQuery(page);
        await assertResultsHeading(page, 'temp');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'off' },
            { toggleName: 'feature-123', expectedValue: 'off' },
        ]);

        // --- Query 6c: stage=temp, userId=charlie ---
        console.log('--- Query 6c: temp / charlie ---');
        await setExistingContextEntry(page, 'userId', 'charlie');
        await runQuery(page);
        await assertResultsHeading(page, 'temp');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'off' },
            { toggleName: 'feature-123', expectedValue: 'off' },
        ]);

        // --- Query 6d: stage=temp-child, userId=bob ---
        console.log('--- Query 6d: temp-child / bob ---');
        await queryAndAssert(
            page,
            { stage: 'temp-child', context: 'public', contextUpdates: [{ key: 'userId', value: 'bob' }] },
            [
                { toggleName: 'maintenance-window', expectedValue: 'off' },
                { toggleName: 'feature-123', expectedValue: 'on' },
            ]
        );

        // --- Query 6e: stage=temp-child, userId=alice ---
        console.log('--- Query 6e: temp-child / alice ---');
        await setExistingContextEntry(page, 'userId', 'alice');
        await runQuery(page);
        await assertResultsHeading(page, 'temp-child');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'off' },
            { toggleName: 'feature-123', expectedValue: 'on' },
        ]);

        // --- Query 6f: stage=temp-child, userId=charlie ---
        console.log('--- Query 6f: temp-child / charlie ---');
        await setExistingContextEntry(page, 'userId', 'charlie');
        await runQuery(page);
        await assertResultsHeading(page, 'temp-child');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'off' },
            { toggleName: 'feature-123', expectedValue: 'off' },
        ]);

        // =====================================================================
        // STEP 7 – Modify maintenance-window assignment value to "on"
        // =====================================================================
        console.log('--- Step 7: Modify maintenance-window assignment ---');
        await navigateToToggles(page);
        await waitForTogglesPageReady(page);

        await openManageAssignments(page, 'maintenance-window');
        await editAssignmentValue(page, 'temp', 'catch-all', 'on');
        await assertAssignmentPresent(page, 'temp', 'catch-all', 'on');
        await closeAssignmentsPanel(page);

        // =====================================================================
        // STEP 8 – Toggle Tester: re-evaluate after modification
        // =====================================================================
        console.log('--- Step 8: Toggle Tester re-evaluation (cache cleared) ---');
        await navigateToTester(page);
        await waitForTesterPageReady(page);

        await setContext(page, 'public');
        await clearNameFilter(page);

        // --- Re-query 8a: stage=temp ---
        console.log('--- Re-query 8a: temp ---');
        await selectStage(page, 'temp');
        await clearCache(page);
        await runQuery(page);
        await assertResultsHeading(page, 'temp');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'on' },
        ]);

        // --- Re-query 8b: stage=temp-child ---
        console.log('--- Re-query 8b: temp-child ---');
        await selectStage(page, 'temp-child');
        await clearCache(page);
        await runQuery(page);
        await assertResultsHeading(page, 'temp-child');
        await assertToggleResults(page, [
            { toggleName: 'maintenance-window', expectedValue: 'on' },
        ]);

        // =====================================================================
        // STEP 9 – Referential integrity: verify deletion is blocked while
        // maintenance-window still has an assignment
        // =====================================================================
        console.log('--- Step 8.5: Referential integrity checks ---');

        // Try to delete the toggle while it still has an assignment
        await navigateToToggles(page);
        await waitForTogglesPageReady(page);
        await tryDeleteToggleAndAssertError(
            page,
            'maintenance-window',
            'Cannot delete toggle: it is still used by'
        );

        // Try to delete the catch-all rule while it is still assigned
        await navigateToRules(page);
        await waitForRulesPageReady(page);
        await tryDeleteRuleAndAssertError(
            page,
            'catch-all',
            'is still assigned to'
        );

        // Try to delete the temp-child stage while it still has inherited assignments
        await navigateToStages(page);
        await waitForStagesPageReady(page);
        await tryDeleteStageAndAssertError(
            page,
            'temp-child',
            'Cannot delete stage: it is still assigned to'
        );

        // =====================================================================
        // STEP 10 – Post-condition cleanup
        // =====================================================================
        console.log('--- Step 9: Post-condition cleanup ---');
        await deleteAllTestData(page);

        // Final absence assertions
        await navigateToToggles(page);
        await assertToggleAbsent(page, 'feature-123');
        await assertToggleAbsent(page, 'maintenance-window');

        await navigateToRules(page);
        await assertRuleAbsent(page, 'new-feature-users');
        await assertRuleAbsent(page, 'catch-all');

        await navigateToStages(page);
        await assertStageAbsent(page, 'temp-child');
        await assertStageAbsent(page, 'temp');

        console.log('=== Test complete ===');
    });
});
