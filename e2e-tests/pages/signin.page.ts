import { expect, Page } from '@playwright/test';

const TEST_EMAIL = 'test@abstratium.dev';
const TEST_PASSWORD = 'secretLong';

// ---------------------------------------------------------------------------
// Low-level element accessors
// ---------------------------------------------------------------------------

function signInButton(page: Page) {
    // The "Sign In" button on the signed-out page
    return page.locator('button', { hasText: 'Sign In' });
}

function emailInput(page: Page) {
    return page.locator('input[type="email"], textbox[name="Email"], input#email').or(
        page.getByPlaceholder('Email')
    );
}

function passwordInput(page: Page) {
    return page.locator('input[type="password"]').or(
        page.getByPlaceholder('Password')
    );
}

function submitSignInButton(page: Page) {
    // The "Sign in" submit button on the OAuth server form
    return page.locator('button', { hasText: /^Sign in$/ });
}

function approveButton(page: Page) {
    return page.locator('button', { hasText: 'Approve' });
}

// ---------------------------------------------------------------------------
// Higher-level functions
// ---------------------------------------------------------------------------

/**
 * Signs in as the test user from any starting state.
 * Handles:
 *   1. /signed-out page → clicks "Sign In"
 *   2. OAuth server sign-in form → fills credentials
 *   3. OAuth approval screen → clicks "Approve"
 *   4. Waits until back on the app (localhost)
 */
export async function signIn(page: Page): Promise<void> {
    console.log('SignIn: starting sign-in flow');

    // The app may show "Sign In Required" with a "Sign In" button while still at
    // localhost:8087/ (Angular client-side routing). Wait for either the Sign In
    // button (unauthenticated app) or the OAuth email input to appear.
    console.log('SignIn: waiting for sign-in button or OAuth form');
    await Promise.race([
        signInButton(page).waitFor({ state: 'visible', timeout: 15000 }),
        emailInput(page).waitFor({ state: 'visible', timeout: 15000 }),
    ]);

    const signInBtnVisible = await signInButton(page).isVisible();
    if (signInBtnVisible) {
        console.log('SignIn: clicking "Sign In" button on the app');
        await signInButton(page).click();
    } else {
        console.log('SignIn: already on OAuth server sign-in form');
    }

    // Wait for the OAuth server sign-in form
    console.log('SignIn: waiting for OAuth sign-in form');
    await emailInput(page).waitFor({ state: 'visible', timeout: 15000 });
    await emailInput(page).fill(TEST_EMAIL);
    await passwordInput(page).fill(TEST_PASSWORD);
    await submitSignInButton(page).click();

    // May show an approval screen
    console.log('SignIn: checking for approval screen');
    try {
        await approveButton(page).waitFor({ state: 'visible', timeout: 5000 });
        console.log('SignIn: approval screen found, approving');
        await approveButton(page).click();
    } catch {
        console.log('SignIn: no approval screen, continuing');
    }

    // Wait until we are back on the app (OAuth redirect).
    console.log('SignIn: waiting to return to app');
    await page.waitForURL(/localhost:8087/, { timeout: 15000 });
    console.log(`SignIn: redirected back to ${page.url()}`);

    // After OAuth callback the app may land on /signed-out if it was the originating page.
    // Navigate to root so the auth guard routes us to the authenticated home page.
    if (page.url().includes('/signed-out')) {
        console.log('SignIn: landed on /signed-out after OAuth, navigating to /');
        await page.goto('/');
    }

    // Wait for Angular to finish bootstrapping and the auth guard to resolve
    await page.locator('a#toggles-link').waitFor({ state: 'visible', timeout: 20000 });
    console.log(`SignIn: fully authenticated, now at ${page.url()}`);
}

/**
 * Signs in as the test admin and navigates to the home page.
 * Use this as the first call in any test.
 */
export async function signInAsAdmin(page: Page): Promise<void> {
    console.log('SignIn: signInAsAdmin');
    await page.goto('/');
    await page.waitForLoadState('load');
    console.log(`SignIn: after goto('/'), URL is ${page.url()}`);

    // If the toggles link is not yet visible, we need to go through the sign-in flow.
    // This handles all cases: /signed-out page, direct OAuth redirect, etc.
    const alreadySignedIn = await page.locator('a#toggles-link').isVisible();
    if (!alreadySignedIn) {
        console.log('SignIn: not yet signed in, starting sign-in flow');
        await signIn(page);
    } else {
        console.log('SignIn: already signed in');
    }

    // Final confirmation
    await expect(page.locator('a#toggles-link')).toBeVisible({ timeout: 10000 });
    console.log('SignIn: confirmed signed in and on app');
}
