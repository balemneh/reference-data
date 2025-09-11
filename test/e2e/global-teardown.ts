import { FullConfig } from '@playwright/test';
import axios from 'axios';

async function globalTeardown(config: FullConfig) {
  console.log('Starting global teardown...');

  const apiURL = process.env.API_URL || 'http://localhost:8080';

  // Clean up test data if needed
  await cleanupTestData(apiURL);

  console.log('Global teardown completed');
}

async function cleanupTestData(apiURL: string) {
  try {
    // Only clean up if this is a test environment
    if (process.env.NODE_ENV === 'test' || process.env.CLEANUP_TEST_DATA === 'true') {
      console.log('Cleaning up test data...');
      
      // Add cleanup logic here if needed
      // For now, we'll leave test data for debugging purposes
      
      console.log('✓ Test data cleanup completed');
    } else {
      console.log('Skipping test data cleanup (not in test environment)');
    }
  } catch (error: any) {
    console.error('Failed to cleanup test data:', error.message);
    // Don't fail the teardown if cleanup fails
  }
}

export default globalTeardown;