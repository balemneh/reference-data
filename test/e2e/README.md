# End-to-End Tests for CBP Reference Data Service

This directory contains end-to-end tests for the CBP Reference Data Service using Playwright.

## Setup

1. Install dependencies:
   ```bash
   cd test/e2e
   npm install
   ```

2. Install browser binaries:
   ```bash
   npm run install-browsers
   ```

3. Ensure the application services are running:
   ```bash
   # From the project root
   make setup-dev
   ```

## Running Tests

### All Tests
```bash
npm test
```

### UI Mode (Interactive)
```bash
npm run test:ui
```

### Specific Browser
```bash
npx playwright test --project=chromium
```

### API Tests Only
```bash
npx playwright test --project=api
```

### Debug Mode
```bash
npm run test:debug
```

### Headed Mode (See Browser)
```bash
npm run test:headed
```

## Test Structure

- `tests/` - UI end-to-end tests
- `tests/api/` - API integration tests
- `global-setup.ts` - Global test setup
- `global-teardown.ts` - Global test cleanup
- `playwright.config.ts` - Playwright configuration

## Test Scenarios Covered

### UI Tests
- Countries list display
- Search and filtering
- Pagination
- CRUD operations
- Form validation
- Error handling
- Export functionality

### API Tests
- REST endpoint functionality
- Request/response validation
- Error handling
- Pagination
- Caching (ETags)
- Concurrent operations
- Performance

## Writing New Tests

### UI Test Example
```typescript
test('should do something', async ({ page }) => {
  await page.goto('/');
  await page.click('[data-testid="some-button"]');
  await expect(page.locator('[data-testid="result"]')).toBeVisible();
});
```

### API Test Example
```typescript
test('should call API', async ({ request }) => {
  const response = await request.get('/v1/countries');
  expect(response.status()).toBe(200);
  const data = await response.json();
  expect(data).toHaveProperty('content');
});
```

## Configuration

### Environment Variables
- `BASE_URL` - UI application URL (default: http://localhost:4200)
- `API_URL` - API service URL (default: http://localhost:8080)
- `CI` - Set to true in CI environment
- `NODE_ENV` - Environment (test/development/production)

### Browser Configuration
Tests run against:
- Chromium (Desktop)
- Firefox (Desktop) 
- WebKit/Safari (Desktop)
- Mobile Chrome (Android)
- Mobile Safari (iOS)

## Reports

After running tests, reports are available:
- HTML report: `npm run test:report`
- JUnit XML: `test-results/results.xml`
- JSON: `test-results/results.json`

## Troubleshooting

### Services Not Starting
If tests fail with connection errors:
1. Check services are running: `make status`
2. Restart services: `make restart`
3. Check ports: `make ports`

### Test Data Issues
If tests fail due to missing data:
1. Reset database: `make db-reset`
2. Seed test data: `make seed`

### Performance Issues
If tests are slow:
1. Run fewer workers: `npx playwright test --workers=1`
2. Run specific tests: `npx playwright test tests/countries.spec.ts`
3. Use headed mode to debug: `npm run test:headed`

## CI Integration

Tests are designed to run in CI environments:
- Automatic service startup
- Retry on failure
- Proper cleanup
- Detailed reporting

Example GitHub Actions:
```yaml
- name: Run E2E Tests
  run: |
    cd test/e2e
    npm ci
    npm run install-browsers
    npm test
```