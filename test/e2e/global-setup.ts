import { chromium, FullConfig } from '@playwright/test';
import axios from 'axios';
import { getTestAuthToken } from './setup-test-auth';

async function globalSetup(config: FullConfig) {
  console.log('Starting global setup...');

  const baseURL = process.env.BASE_URL || 'http://localhost:4200';
  const apiURL = process.env.API_URL || 'http://localhost:8083';

  // Wait for services to be ready
  console.log('Waiting for services to be ready...');
  
  await waitForService(apiURL + '/actuator/health', 'API Service');
  await waitForService(baseURL, 'UI Service');

  // Get authentication token for setup
  const authToken = await getTestAuthToken();

  // Set up test data
  console.log('Setting up test data...');
  await setupTestData(apiURL, authToken);

  // Perform authentication setup if needed
  const browser = await chromium.launch();
  const context = await browser.newContext();
  
  // Save authentication state
  // await context.storageState({ path: 'auth-state.json' });
  
  await browser.close();

  console.log('Global setup completed');
}

async function waitForService(url: string, serviceName: string) {
  const maxAttempts = 30;
  const delay = 2000;
  
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      await axios.get(url, { timeout: 5000 });
      console.log(`✓ ${serviceName} is ready`);
      return;
    } catch (error) {
      console.log(`⏳ ${serviceName} not ready (attempt ${attempt}/${maxAttempts})`);
      if (attempt === maxAttempts) {
        throw new Error(`${serviceName} failed to start after ${maxAttempts} attempts`);
      }
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}

async function setupTestData(apiURL: string, authToken: string) {
  try {
    // Check if test data already exists
    const countriesResponse = await axios.get(`${apiURL}/v1/countries`, {
      params: { size: 1, codeSystem: 'ISO3166-1' },
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    if (countriesResponse.data.totalElements > 0) {
      console.log('Test data already exists, skipping setup');
      return;
    }

    // Create test countries
    const testCountries = [
      {
        countryCode: 'US',
        countryName: 'United States',
        iso2Code: 'US',
        iso3Code: 'USA',
        numericCode: '840',
        codeSystem: { code: 'ISO3166-1' }
      },
      {
        countryCode: 'CA',
        countryName: 'Canada',
        iso2Code: 'CA',
        iso3Code: 'CAN',
        numericCode: '124',
        codeSystem: { code: 'ISO3166-1' }
      },
      {
        countryCode: 'MX',
        countryName: 'Mexico',
        iso2Code: 'MX',
        iso3Code: 'MEX',
        numericCode: '484',
        codeSystem: { code: 'ISO3166-1' }
      }
    ];

    console.log('Creating test countries...');
    for (const country of testCountries) {
      try {
        await axios.post(`${apiURL}/v1/countries`, country, {
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
          }
        });
        console.log(`✓ Created country: ${country.countryName}`);
      } catch (error: any) {
        if (error.response?.status === 409) {
          console.log(`⚠ Country ${country.countryName} already exists`);
        } else {
          console.error(`✗ Failed to create country ${country.countryName}:`, error.message);
        }
      }
    }

  } catch (error: any) {
    console.error('Failed to setup test data:', error.message);
    // Don't fail the setup if test data creation fails
  }
}

export default globalSetup;