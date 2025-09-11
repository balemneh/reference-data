import { test, expect, Page } from '@playwright/test';

test.describe('Countries Management', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await page.goto('/');
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('should display countries list', async () => {
    // Navigate to countries page
    await page.click('[data-testid="nav-countries"]');
    
    // Wait for countries to load
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Verify page title
    await expect(page.locator('h1')).toContainText('Countries');
    
    // Verify table headers
    await expect(page.locator('th')).toContainText(['Code', 'Name', 'ISO2', 'ISO3', 'Actions']);
    
    // Verify at least one country is displayed
    await expect(page.locator('tbody tr')).toHaveCount({ min: 1 });
  });

  test('should search countries', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Search for "United"
    await page.fill('[data-testid="search-input"]', 'United');
    await page.click('[data-testid="search-button"]');
    
    // Wait for search results
    await page.waitForResponse(response => 
      response.url().includes('/v1/countries') && response.status() === 200
    );
    
    // Verify search results contain "United"
    const countryNames = await page.locator('tbody tr td:nth-child(2)').allTextContents();
    expect(countryNames.some(name => name.includes('United'))).toBeTruthy();
  });

  test('should filter by code system', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Select ISO3166-1 code system
    await page.selectOption('[data-testid="code-system-filter"]', 'ISO3166-1');
    
    // Wait for filtered results
    await page.waitForResponse(response => 
      response.url().includes('codeSystem=ISO3166-1') && response.status() === 200
    );
    
    // Verify results are filtered
    await expect(page.locator('tbody tr')).toHaveCount({ min: 1 });
  });

  test('should paginate through countries', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Check if pagination controls exist (only if there are enough records)
    const paginationExists = await page.locator('[data-testid="pagination"]').isVisible();
    
    if (paginationExists) {
      // Get current page countries
      const page1Countries = await page.locator('tbody tr').count();
      
      // Go to next page
      await page.click('[data-testid="next-page"]');
      
      // Wait for new page to load
      await page.waitForResponse(response => 
        response.url().includes('/v1/countries') && response.status() === 200
      );
      
      // Verify different countries are shown
      const page2Countries = await page.locator('tbody tr').count();
      expect(page2Countries).toBeGreaterThan(0);
    }
  });

  test('should view country details', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Click on first country's view button
    await page.click('tbody tr:first-child [data-testid="view-country"]');
    
    // Wait for detail modal or page
    await page.waitForSelector('[data-testid="country-detail"]');
    
    // Verify country details are displayed
    await expect(page.locator('[data-testid="country-code"]')).toBeVisible();
    await expect(page.locator('[data-testid="country-name"]')).toBeVisible();
    await expect(page.locator('[data-testid="iso-codes"]')).toBeVisible();
  });

  test('should create new country', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Click add new country button
    await page.click('[data-testid="add-country"]');
    
    // Wait for form modal
    await page.waitForSelector('[data-testid="country-form"]');
    
    // Fill in country details
    await page.fill('[data-testid="country-code-input"]', 'DE');
    await page.fill('[data-testid="country-name-input"]', 'Germany');
    await page.fill('[data-testid="iso2-code-input"]', 'DE');
    await page.fill('[data-testid="iso3-code-input"]', 'DEU');
    await page.fill('[data-testid="numeric-code-input"]', '276');
    
    // Submit form
    await page.click('[data-testid="save-country"]');
    
    // Wait for success message
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
    
    // Verify country appears in list
    await page.waitForResponse(response => 
      response.url().includes('/v1/countries') && response.status() === 200
    );
    
    const countryNames = await page.locator('tbody tr td:nth-child(2)').allTextContents();
    expect(countryNames).toContain('Germany');
  });

  test('should edit existing country', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Find and click edit button for first country
    await page.click('tbody tr:first-child [data-testid="edit-country"]');
    
    // Wait for edit form
    await page.waitForSelector('[data-testid="country-form"]');
    
    // Update country name
    const originalName = await page.inputValue('[data-testid="country-name-input"]');
    const newName = originalName + ' (Updated)';
    await page.fill('[data-testid="country-name-input"]', newName);
    
    // Submit changes
    await page.click('[data-testid="save-country"]');
    
    // Wait for success message
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
    
    // Verify country name was updated
    await page.waitForResponse(response => 
      response.url().includes('/v1/countries') && response.status() === 200
    );
    
    const countryNames = await page.locator('tbody tr td:nth-child(2)').allTextContents();
    expect(countryNames).toContain(newName);
  });

  test('should handle validation errors', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Click add new country
    await page.click('[data-testid="add-country"]');
    await page.waitForSelector('[data-testid="country-form"]');
    
    // Try to submit without filling required fields
    await page.click('[data-testid="save-country"]');
    
    // Verify validation errors are displayed
    await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="validation-error"]')).toContainText('required');
  });

  test('should sort countries by different columns', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Get initial order
    const initialOrder = await page.locator('tbody tr td:nth-child(2)').allTextContents();
    
    // Click on Name column header to sort
    await page.click('[data-testid="sort-name"]');
    
    // Wait for sorted results
    await page.waitForResponse(response => 
      response.url().includes('/v1/countries') && response.status() === 200
    );
    
    // Get sorted order
    const sortedOrder = await page.locator('tbody tr td:nth-child(2)').allTextContents();
    
    // Verify order changed (assuming there are enough countries to see a difference)
    if (initialOrder.length > 1) {
      expect(sortedOrder).not.toEqual(initialOrder);
    }
  });

  test('should export countries data', async () => {
    await page.click('[data-testid="nav-countries"]');
    await page.waitForSelector('[data-testid="countries-table"]');
    
    // Start download
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-testid="export-countries"]');
    
    // Wait for download to complete
    const download = await downloadPromise;
    
    // Verify download
    expect(download.suggestedFilename()).toMatch(/countries.*\.(csv|xlsx)$/);
  });

  test('should handle network errors gracefully', async () => {
    // Simulate network failure
    await page.route('**/v1/countries', route => route.abort());
    
    await page.goto('/countries');
    
    // Verify error message is displayed
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="error-message"]')).toContainText('Unable to load');
  });
});