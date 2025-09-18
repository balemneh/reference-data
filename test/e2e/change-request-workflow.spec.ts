import { test, expect, Page } from '@playwright/test';

/**
 * End-to-End tests for the complete Change Request Workflow.
 * These tests verify the entire user journey from submission to approval to implementation.
 */

interface TestUser {
  username: string;
  password: string;
  role: 'requester' | 'approver' | 'admin';
}

const testUsers: Record<string, TestUser> = {
  requester: { username: 'test-requester', password: 'test123', role: 'requester' },
  approver: { username: 'test-approver', password: 'test123', role: 'approver' },
  admin: { username: 'test-admin', password: 'test123', role: 'admin' }
};

test.describe('Change Request Workflow E2E Tests', () => {
  let changeRequestId: string;

  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto('/');

    // Wait for the application to load
    await expect(page.locator('h1')).toContainText('CBP Reference Data Service');
  });

  test.describe('Complete CREATE Workflow', () => {
    test('should complete full country creation workflow', async ({ page, context }) => {
      // Step 1: Login as requester and create change request
      await loginAsUser(page, testUsers.requester);
      await navigateToCountries(page);

      // Create new country change request
      await page.click('[data-testid="add-country-btn"]');
      await fillCountryForm(page, {
        countryCode: 'E2E',
        countryName: 'End-to-End Test Country',
        iso2Code: 'E2',
        iso3Code: 'E2E',
        numericCode: '901'
      });

      // Submit change request
      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, 'Creating test country for E2E workflow validation');

      const submitButton = page.locator('[data-testid="submit-change-request-btn"]');
      await expect(submitButton).toBeEnabled();
      await submitButton.click();

      // Verify change request was created
      await expect(page.locator('.usa-alert--success')).toContainText('Change request submitted successfully');

      // Extract change request ID from success message
      const successText = await page.locator('.usa-alert--success').textContent();
      const match = successText?.match(/Request ID: ([A-Za-z0-9-]+)/);
      expect(match).toBeTruthy();
      changeRequestId = match![1];

      // Verify navigation to change request details
      await expect(page).toHaveURL(new RegExp(`/change-requests/${changeRequestId}`));

      // Step 2: Login as approver in new context and approve request
      const approverPage = await context.newPage();
      await loginAsUser(approverPage, testUsers.approver);
      await navigateToChangeRequests(approverPage);

      // Find and open the pending change request
      await approverPage.fill('[data-testid="search-change-requests"]', changeRequestId);
      await approverPage.press('[data-testid="search-change-requests"]', 'Enter');

      await expect(approverPage.locator(`[data-change-request-id="${changeRequestId}"]`)).toBeVisible();
      await approverPage.click(`[data-change-request-id="${changeRequestId}"] [data-testid="view-details-btn"]`);

      // Verify change request details
      await expect(approverPage.locator('[data-testid="cr-status"]')).toHaveText('PENDING');
      await expect(approverPage.locator('[data-testid="cr-operation"]')).toHaveText('CREATE');
      await expect(approverPage.locator('[data-testid="cr-data-type"]')).toHaveText('COUNTRY');

      // Approve the change request
      await approverPage.click('[data-testid="approve-btn"]');
      await approverPage.fill('[data-testid="approval-comments"]', 'Approved for E2E test execution');
      await approverPage.click('[data-testid="confirm-approval-btn"]');

      // Verify approval was successful
      await expect(approverPage.locator('.usa-alert--success')).toContainText('Change request approved');
      await expect(approverPage.locator('[data-testid="cr-status"]')).toHaveText('APPROVED');

      // Step 3: Execute the change request (automatic or manual trigger)
      await approverPage.click('[data-testid="execute-btn"]');
      await approverPage.click('[data-testid="confirm-execute-btn"]');

      // Verify implementation was successful
      await expect(approverPage.locator('.usa-alert--success')).toContainText('Change request implemented');
      await expect(approverPage.locator('[data-testid="cr-status"]')).toHaveText('APPLIED');

      // Step 4: Verify the country was actually created
      await navigateToCountries(approverPage);
      await approverPage.fill('[data-testid="search-countries"]', 'E2E');
      await approverPage.press('[data-testid="search-countries"]', 'Enter');

      await expect(approverPage.locator('[data-testid="countries-table"]')).toContainText('End-to-End Test Country');
      await expect(approverPage.locator('[data-country-code="E2E"]')).toBeVisible();

      await approverPage.close();
    });

    test('should handle change request rejection workflow', async ({ page }) => {
      // Step 1: Create change request as requester
      await loginAsUser(page, testUsers.requester);
      await navigateToCountries(page);

      await page.click('[data-testid="add-country-btn"]');
      await fillCountryForm(page, {
        countryCode: 'REJ',
        countryName: 'Rejection Test Country',
        iso2Code: 'RJ',
        iso3Code: 'REJ',
        numericCode: '902'
      });

      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, 'Test country for rejection workflow');
      await page.click('[data-testid="submit-change-request-btn"]');

      // Extract change request ID
      const successText = await page.locator('.usa-alert--success').textContent();
      const rejectionRequestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];
      expect(rejectionRequestId).toBeTruthy();

      // Step 2: Login as approver and reject request
      await loginAsUser(page, testUsers.approver);
      await navigateToChangeRequests(page);

      await page.fill('[data-testid="search-change-requests"]', rejectionRequestId!);
      await page.press('[data-testid="search-change-requests"]', 'Enter');
      await page.click(`[data-change-request-id="${rejectionRequestId}"] [data-testid="view-details-btn"]`);

      // Reject the change request
      await page.click('[data-testid="reject-btn"]');
      await page.fill('[data-testid="rejection-reason"]', 'Business requirements not clearly defined');
      await page.click('[data-testid="confirm-rejection-btn"]');

      // Verify rejection was successful
      await expect(page.locator('.usa-alert--success')).toContainText('Change request rejected');
      await expect(page.locator('[data-testid="cr-status"]')).toHaveText('REJECTED');

      // Verify the country was NOT created
      await navigateToCountries(page);
      await page.fill('[data-testid="search-countries"]', 'REJ');
      await page.press('[data-testid="search-countries"]', 'Enter');

      await expect(page.locator('[data-testid="no-results-message"]')).toBeVisible();
    });
  });

  test.describe('UPDATE Workflow', () => {
    test('should complete country update workflow', async ({ page }) => {
      // Prerequisites: Ensure a country exists to update
      await loginAsUser(page, testUsers.admin);
      await ensureCountryExists(page, {
        countryCode: 'UPD',
        countryName: 'Update Test Country',
        iso2Code: 'UP',
        iso3Code: 'UPD',
        numericCode: '903'
      });

      // Step 1: Create update change request
      await navigateToCountries(page);
      await page.fill('[data-testid="search-countries"]', 'UPD');
      await page.press('[data-testid="search-countries"]', 'Enter');

      await page.click('[data-country-code="UPD"] [data-testid="edit-btn"]');
      await page.fill('[data-testid="country-name-input"]', 'Updated Test Country Name');

      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, 'Updating country name for clarity');
      await page.click('[data-testid="submit-change-request-btn"]');

      // Extract change request ID
      const successText = await page.locator('.usa-alert--success').textContent();
      const updateRequestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];

      // Step 2: Approve and execute update
      await navigateToChangeRequests(page);
      await page.fill('[data-testid="search-change-requests"]', updateRequestId!);
      await page.press('[data-testid="search-change-requests"]', 'Enter');
      await page.click(`[data-change-request-id="${updateRequestId}"] [data-testid="view-details-btn"]`);

      // Verify update details
      await expect(page.locator('[data-testid="cr-operation"]')).toHaveText('UPDATE');
      await expect(page.locator('[data-testid="proposed-changes"]')).toContainText('Updated Test Country Name');

      // Approve and execute
      await page.click('[data-testid="approve-btn"]');
      await page.fill('[data-testid="approval-comments"]', 'Approved country name update');
      await page.click('[data-testid="confirm-approval-btn"]');

      await page.click('[data-testid="execute-btn"]');
      await page.click('[data-testid="confirm-execute-btn"]');

      // Step 3: Verify update was applied
      await navigateToCountries(page);
      await page.fill('[data-testid="search-countries"]', 'UPD');
      await page.press('[data-testid="search-countries"]', 'Enter');

      await expect(page.locator('[data-country-code="UPD"]')).toContainText('Updated Test Country Name');
    });
  });

  test.describe('DELETE (Deactivation) Workflow', () => {
    test('should complete country deactivation workflow', async ({ page }) => {
      // Prerequisites: Ensure a country exists to deactivate
      await loginAsUser(page, testUsers.admin);
      await ensureCountryExists(page, {
        countryCode: 'DEL',
        countryName: 'Delete Test Country',
        iso2Code: 'DL',
        iso3Code: 'DEL',
        numericCode: '904'
      });

      // Step 1: Create delete change request
      await navigateToCountries(page);
      await page.fill('[data-testid="search-countries"]', 'DEL');
      await page.press('[data-testid="search-countries"]', 'Enter');

      await page.click('[data-country-code="DEL"] [data-testid="delete-btn"]');
      await page.click('[data-testid="confirm-delete-btn"]');

      await fillJustificationModal(page, 'Country is no longer valid and should be deactivated');
      await page.click('[data-testid="submit-change-request-btn"]');

      // Extract change request ID
      const successText = await page.locator('.usa-alert--success').textContent();
      const deleteRequestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];

      // Step 2: Approve and execute deletion
      await navigateToChangeRequests(page);
      await page.fill('[data-testid="search-change-requests"]', deleteRequestId!);
      await page.press('[data-testid="search-change-requests"]', 'Enter');
      await page.click(`[data-change-request-id="${deleteRequestId}"] [data-testid="view-details-btn"]`);

      // Verify delete details
      await expect(page.locator('[data-testid="cr-operation"]')).toHaveText('DELETE');

      // Approve and execute
      await page.click('[data-testid="approve-btn"]');
      await page.fill('[data-testid="approval-comments"]', 'Approved for deactivation');
      await page.click('[data-testid="confirm-approval-btn"]');

      await page.click('[data-testid="execute-btn"]');
      await page.click('[data-testid="confirm-execute-btn"]');

      // Step 3: Verify soft delete was applied
      await navigateToCountries(page);

      // Should not appear in active countries
      await page.fill('[data-testid="search-countries"]', 'DEL');
      await page.press('[data-testid="search-countries"]', 'Enter');
      await expect(page.locator('[data-testid="no-results-message"]')).toBeVisible();

      // Should appear in inactive countries
      await page.selectOption('[data-testid="active-filter"]', 'inactive');
      await page.fill('[data-testid="search-countries"]', 'DEL');
      await page.press('[data-testid="search-countries"]', 'Enter');

      await expect(page.locator('[data-country-code="DEL"]')).toBeVisible();
      await expect(page.locator('[data-country-code="DEL"]')).toContainText('Inactive');
    });
  });

  test.describe('Bulk Operations', () => {
    test('should handle bulk approval workflow', async ({ page }) => {
      // Step 1: Create multiple change requests
      await loginAsUser(page, testUsers.requester);
      const bulkRequestIds: string[] = [];

      for (let i = 1; i <= 3; i++) {
        await navigateToCountries(page);
        await page.click('[data-testid="add-country-btn"]');
        await fillCountryForm(page, {
          countryCode: `BLK${i}`,
          countryName: `Bulk Test Country ${i}`,
          iso2Code: `B${i}`,
          iso3Code: `BLK${i}`,
          numericCode: `90${i}`
        });

        await page.click('[data-testid="save-country-btn"]');
        await fillJustificationModal(page, `Bulk operation test country ${i}`);
        await page.click('[data-testid="submit-change-request-btn"]');

        const successText = await page.locator('.usa-alert--success').textContent();
        const requestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];
        if (requestId) bulkRequestIds.push(requestId);
      }

      // Step 2: Bulk approve as approver
      await loginAsUser(page, testUsers.approver);
      await navigateToChangeRequests(page);

      // Select multiple change requests
      for (const requestId of bulkRequestIds) {
        await page.check(`[data-change-request-id="${requestId}"] input[type="checkbox"]`);
      }

      // Bulk approve
      await page.click('[data-testid="bulk-approve-btn"]');
      await page.fill('[data-testid="bulk-approval-comments"]', 'Bulk approval for testing');
      await page.click('[data-testid="confirm-bulk-approval-btn"]');

      // Verify all requests were approved
      await expect(page.locator('.usa-alert--success')).toContainText('requests approved');

      // Step 3: Verify individual statuses
      for (const requestId of bulkRequestIds) {
        await page.click(`[data-change-request-id="${requestId}"] [data-testid="view-details-btn"]`);
        await expect(page.locator('[data-testid="cr-status"]')).toHaveText('APPROVED');
        await page.goBack();
      }
    });
  });

  test.describe('Scheduled Implementation', () => {
    test('should schedule change request for future implementation', async ({ page }) => {
      // Step 1: Create and approve change request
      await loginAsUser(page, testUsers.admin);
      await navigateToCountries(page);

      await page.click('[data-testid="add-country-btn"]');
      await fillCountryForm(page, {
        countryCode: 'SCH',
        countryName: 'Scheduled Test Country',
        iso2Code: 'SC',
        iso3Code: 'SCH',
        numericCode: '905'
      });

      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, 'Country for scheduled implementation testing');
      await page.click('[data-testid="submit-change-request-btn"]');

      const successText = await page.locator('.usa-alert--success').textContent();
      const scheduledRequestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];

      // Approve the request
      await navigateToChangeRequests(page);
      await page.fill('[data-testid="search-change-requests"]', scheduledRequestId!);
      await page.press('[data-testid="search-change-requests"]', 'Enter');
      await page.click(`[data-change-request-id="${scheduledRequestId}"] [data-testid="view-details-btn"]`);

      await page.click('[data-testid="approve-btn"]');
      await page.fill('[data-testid="approval-comments"]', 'Approved for scheduled implementation');
      await page.click('[data-testid="confirm-approval-btn"]');

      // Step 2: Schedule implementation
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 7);
      const futureDateString = futureDate.toISOString().split('T')[0];

      await page.click('[data-testid="schedule-btn"]');
      await page.fill('[data-testid="effective-date"]', futureDateString);
      await page.click('[data-testid="confirm-schedule-btn"]');

      // Verify scheduling was successful
      await expect(page.locator('.usa-alert--success')).toContainText('Change request scheduled');
      await expect(page.locator('[data-testid="cr-status"]')).toHaveText('SCHEDULED');
      await expect(page.locator('[data-testid="effective-date"]')).toContainText(futureDateString);
    });
  });

  test.describe('Error Scenarios', () => {
    test('should handle concurrent modification conflicts', async ({ page, context }) => {
      // Prerequisites: Create a country to update
      await loginAsUser(page, testUsers.admin);
      await ensureCountryExists(page, {
        countryCode: 'CON',
        countryName: 'Concurrency Test Country',
        iso2Code: 'CN',
        iso3Code: 'CON',
        numericCode: '906'
      });

      // Step 1: User 1 starts editing
      await navigateToCountries(page);
      await page.fill('[data-testid="search-countries"]', 'CON');
      await page.press('[data-testid="search-countries"]', 'Enter');
      await page.click('[data-country-code="CON"] [data-testid="edit-btn"]');

      // Step 2: User 2 creates competing change request
      const user2Page = await context.newPage();
      await loginAsUser(user2Page, testUsers.requester);
      await navigateToCountries(user2Page);
      await user2Page.fill('[data-testid="search-countries"]', 'CON');
      await user2Page.press('[data-testid="search-countries"]', 'Enter');
      await user2Page.click('[data-country-code="CON"] [data-testid="edit-btn"]');

      await user2Page.fill('[data-testid="country-name-input"]', 'Updated by User 2');
      await user2Page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(user2Page, 'User 2 update');
      await user2Page.click('[data-testid="submit-change-request-btn"]');

      // Step 3: User 1 tries to submit conflicting change
      await page.fill('[data-testid="country-name-input"]', 'Updated by User 1');
      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, 'User 1 update');
      await page.click('[data-testid="submit-change-request-btn"]');

      // Should show conflict warning or create separate change request
      await expect(page.locator('.usa-alert--warning, .usa-alert--info')).toBeVisible();

      await user2Page.close();
    });

    test('should handle validation errors gracefully', async ({ page }) => {
      await loginAsUser(page, testUsers.requester);
      await navigateToCountries(page);

      // Try to create country with invalid data
      await page.click('[data-testid="add-country-btn"]');

      // Leave required fields empty
      await page.fill('[data-testid="country-code-input"]', '');
      await page.fill('[data-testid="country-name-input"]', '');
      await page.click('[data-testid="save-country-btn"]');

      // Should show validation errors
      await expect(page.locator('[data-testid="country-code-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="country-name-error"]')).toBeVisible();

      // Fill with invalid ISO codes
      await page.fill('[data-testid="country-code-input"]', 'INVALID');
      await page.fill('[data-testid="country-name-input"]', 'Test');
      await page.fill('[data-testid="iso2-code-input"]', 'TOOLONG');
      await page.fill('[data-testid="iso3-code-input"]', 'XX');
      await page.fill('[data-testid="numeric-code-input"]', 'abc');

      await page.click('[data-testid="save-country-btn"]');

      // Should show format validation errors
      await expect(page.locator('[data-testid="iso2-code-error"]')).toContainText('exactly 2 characters');
      await expect(page.locator('[data-testid="iso3-code-error"]')).toContainText('exactly 3 characters');
      await expect(page.locator('[data-testid="numeric-code-error"]')).toContainText('exactly 3 digits');
    });
  });

  // Helper functions
  async function loginAsUser(page: Page, user: TestUser) {
    await page.goto('/login');
    await page.fill('[data-testid="username"]', user.username);
    await page.fill('[data-testid="password"]', user.password);
    await page.click('[data-testid="login-btn"]');
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
  }

  async function navigateToCountries(page: Page) {
    await page.click('[data-testid="countries-nav"]');
    await expect(page.locator('h1')).toContainText('Countries');
  }

  async function navigateToChangeRequests(page: Page) {
    await page.click('[data-testid="change-requests-nav"]');
    await expect(page.locator('h1')).toContainText('Change Requests');
  }

  async function fillCountryForm(page: Page, country: {
    countryCode: string;
    countryName: string;
    iso2Code: string;
    iso3Code: string;
    numericCode: string;
  }) {
    await page.fill('[data-testid="country-code-input"]', country.countryCode);
    await page.fill('[data-testid="country-name-input"]', country.countryName);
    await page.fill('[data-testid="iso2-code-input"]', country.iso2Code);
    await page.fill('[data-testid="iso3-code-input"]', country.iso3Code);
    await page.fill('[data-testid="numeric-code-input"]', country.numericCode);
  }

  async function fillJustificationModal(page: Page, justification: string) {
    await expect(page.locator('[data-testid="justification-modal"]')).toBeVisible();
    await page.fill('[data-testid="business-justification"]', justification);
  }

  async function ensureCountryExists(page: Page, country: {
    countryCode: string;
    countryName: string;
    iso2Code: string;
    iso3Code: string;
    numericCode: string;
  }) {
    // Check if country exists, create if not
    await navigateToCountries(page);
    await page.fill('[data-testid="search-countries"]', country.countryCode);
    await page.press('[data-testid="search-countries"]', 'Enter');

    const existingCountry = page.locator(`[data-country-code="${country.countryCode}"]`);

    if (!(await existingCountry.isVisible())) {
      // Create the country
      await page.click('[data-testid="add-country-btn"]');
      await fillCountryForm(page, country);
      await page.click('[data-testid="save-country-btn"]');
      await fillJustificationModal(page, `Setup country for testing: ${country.countryName}`);
      await page.click('[data-testid="submit-change-request-btn"]');

      // Extract and approve the change request
      const successText = await page.locator('.usa-alert--success').textContent();
      const setupRequestId = successText?.match(/Request ID: ([A-Za-z0-9-]+)/)?.[1];

      if (setupRequestId) {
        await navigateToChangeRequests(page);
        await page.fill('[data-testid="search-change-requests"]', setupRequestId);
        await page.press('[data-testid="search-change-requests"]', 'Enter');
        await page.click(`[data-change-request-id="${setupRequestId}"] [data-testid="view-details-btn"]`);

        await page.click('[data-testid="approve-btn"]');
        await page.fill('[data-testid="approval-comments"]', 'Auto-approved for test setup');
        await page.click('[data-testid="confirm-approval-btn"]');

        await page.click('[data-testid="execute-btn"]');
        await page.click('[data-testid="confirm-execute-btn"]');
      }
    }
  }
});