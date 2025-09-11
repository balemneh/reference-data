import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';

// Load environment variables
dotenv.config({ path: '../../.env' });

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
    ['json', { outputFile: 'test-results/results.json' }]
  ],
  /* Shared settings for all the projects below. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: process.env.BASE_URL || 'http://localhost:4200',
    
    /* API URL for API testing */
    extraHTTPHeaders: {
      'Accept': 'application/json',
    },
    
    /* Collect trace when retrying the failed test. */
    trace: 'on-first-retry',
    
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',
    
    /* Record video on failure */
    video: 'retain-on-failure',
    
    /* Global timeout for all tests */
    actionTimeout: 30000,
    navigationTimeout: 30000,
  },

  /* Global setup and teardown */
  globalSetup: require.resolve('./global-setup.ts'),
  globalTeardown: require.resolve('./global-teardown.ts'),

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],
    },

    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
      dependencies: ['setup'],
    },

    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
      dependencies: ['setup'],
    },

    /* Test against mobile viewports. */
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
      dependencies: ['setup'],
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
      dependencies: ['setup'],
    },

    /* API Testing */
    {
      name: 'api',
      testDir: './tests/api',
      use: {
        baseURL: process.env.API_URL || 'http://localhost:8083',
      },
    },
  ],

  /* Test timeout */
  timeout: 60000,
  expect: {
    /* Timeout for expect() assertions */
    timeout: 10000,
  },

  /* Run your local dev server before starting the tests */
  // Commented out since services are already running
  // webServer: [
  //   {
  //     command: 'make up-dev',
  //     port: 5432, // Wait for PostgreSQL to be ready
  //     timeout: 120000,
  //     reuseExistingServer: !process.env.CI,
  //   },
  //   {
  //     command: 'cd ../../admin-ui && npm start',
  //     port: 4200,
  //     timeout: 120000,
  //     reuseExistingServer: !process.env.CI,
  //     env: {
  //       'NODE_ENV': 'development',
  //     },
  //   },
  //   {
  //     command: 'cd ../.. && make run',
  //     port: 8083,
  //     timeout: 120000,
  //     reuseExistingServer: !process.env.CI,
  //   },
  // ],

  /* Output directory */
  outputDir: 'test-results/',
});