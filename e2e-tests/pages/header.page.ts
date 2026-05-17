import { expect, Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Low-level element accessors
// ---------------------------------------------------------------------------

function homeLink(page: Page) {
    return page.locator('a#home-link');
}

function stagesLink(page: Page) {
    return page.locator('a#stages-link');
}

function rulesLink(page: Page) {
    return page.locator('a#rules-link');
}

function togglesLink(page: Page) {
    return page.locator('a#toggles-link');
}

function testerLink(page: Page) {
    return page.locator('a#toggle-tester-link');
}

function helpLink(page: Page) {
    return page.locator('a#help-link');
}

function signoutLink(page: Page) {
    return page.locator('a#signout-link');
}

// ---------------------------------------------------------------------------
// Higher-level navigation functions
// ---------------------------------------------------------------------------

export async function navigateToHome(page: Page): Promise<void> {
    console.log('Header: navigating to Home');
    await homeLink(page).waitFor({ state: 'visible', timeout: 10000 });
    await homeLink(page).click();
    await page.waitForURL(/\/$|\/toggles/, { timeout: 10000 });
    console.log('Header: arrived at Home / Toggles page');
}

export async function navigateToStages(page: Page): Promise<void> {
    console.log('Header: navigating to Stages');
    await stagesLink(page).waitFor({ state: 'visible', timeout: 10000 });
    await stagesLink(page).click();
    await page.waitForURL(/\/stages/, { timeout: 10000 });
    console.log('Header: arrived at Stages page');
}

export async function navigateToRules(page: Page): Promise<void> {
    console.log('Header: navigating to Rules');
    await rulesLink(page).waitFor({ state: 'visible', timeout: 10000 });
    await rulesLink(page).click();
    await page.waitForURL(/\/rules/, { timeout: 10000 });
    console.log('Header: arrived at Rules page');
}

export async function navigateToToggles(page: Page): Promise<void> {
    console.log('Header: navigating to Toggles');
    await togglesLink(page).waitFor({ state: 'visible', timeout: 10000 });
    await togglesLink(page).click();
    await page.waitForURL(/\/toggles/, { timeout: 10000 });
    console.log('Header: arrived at Toggles page');
}

export async function navigateToTester(page: Page): Promise<void> {
    console.log('Header: navigating to Toggle Tester');
    await testerLink(page).waitFor({ state: 'visible', timeout: 10000 });
    await testerLink(page).click();
    await page.waitForURL(/\/toggle-tester/, { timeout: 10000 });
    console.log('Header: arrived at Toggle Tester page');
}

export async function assertHeaderIsVisible(page: Page): Promise<void> {
    console.log('Header: asserting header links are visible');
    await expect(stagesLink(page)).toBeVisible({ timeout: 10000 });
    await expect(rulesLink(page)).toBeVisible({ timeout: 10000 });
    await expect(togglesLink(page)).toBeVisible({ timeout: 10000 });
    await expect(testerLink(page)).toBeVisible({ timeout: 10000 });
    console.log('Header: all links confirmed visible');
}
