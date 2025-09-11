import { test, expect, APIRequestContext } from '@playwright/test';
import { getTestAuthToken } from '../../setup-test-auth';

test.describe('Countries API', () => {
  let apiContext: APIRequestContext;
  let authToken: string;

  test.beforeAll(async ({ playwright }) => {
    // Get authentication token
    authToken = await getTestAuthToken();
    
    apiContext = await playwright.request.newContext({
      baseURL: process.env.API_URL || 'http://localhost:8083',
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${authToken}`
      }
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('GET /v1/countries should return paginated countries', async () => {
    const response = await apiContext.get('/v1/countries', {
      params: {
        page: '0',
        size: '10',
        codeSystem: 'ISO3166-1'
      }
    });

    expect(response.status()).toBe(200);
    
    const data = await response.json();
    expect(data).toHaveProperty('content');
    expect(data).toHaveProperty('totalElements');
    expect(data).toHaveProperty('totalPages');
    expect(Array.isArray(data.content)).toBeTruthy();
  });

  test('GET /v1/countries/{code} should return specific country', async () => {
    const response = await apiContext.get('/v1/countries/US', {
      params: { codeSystem: 'ISO3166-1' }
    });

    expect(response.status()).toBe(200);
    
    const country = await response.json();
    expect(country).toHaveProperty('countryCode', 'US');
    expect(country).toHaveProperty('countryName');
    expect(country).toHaveProperty('iso2Code');
    expect(country).toHaveProperty('iso3Code');
    expect(country).toHaveProperty('codeSystem');
  });

  test('GET /v1/countries/{code} should return 404 for non-existent country', async () => {
    const response = await apiContext.get('/v1/countries/NONEXISTENT', {
      params: { codeSystem: 'ISO3166-1' }
    });

    expect(response.status()).toBe(404);
    
    const error = await response.json();
    expect(error).toHaveProperty('title');
    expect(error).toHaveProperty('detail');
  });

  test('POST /v1/countries should create new country', async () => {
    const newCountry = {
      countryCode: 'TEST01',
      countryName: 'Test Country API',
      iso2Code: 'TC',
      iso3Code: 'TST',
      numericCode: '999',
      codeSystem: { code: 'ISO3166-1' }
    };

    const response = await apiContext.post('/v1/countries', {
      data: newCountry
    });

    expect(response.status()).toBe(201);
    expect(response.headers()['location']).toBeTruthy();
    
    const createdCountry = await response.json();
    expect(createdCountry.countryCode).toBe(newCountry.countryCode);
    expect(createdCountry.countryName).toBe(newCountry.countryName);
  });

  test('POST /v1/countries should validate required fields', async () => {
    const invalidCountry = {
      countryCode: '',
      countryName: ''
    };

    const response = await apiContext.post('/v1/countries', {
      data: invalidCountry
    });

    expect(response.status()).toBe(400);
    
    const error = await response.json();
    expect(error).toHaveProperty('violations');
    expect(Array.isArray(error.violations)).toBeTruthy();
  });

  test('PUT /v1/countries/{code} should update existing country', async () => {
    // First create a country to update
    const testCountry = {
      countryCode: 'TEST02',
      countryName: 'Test Country Update',
      iso2Code: 'TU',
      iso3Code: 'TSU',
      numericCode: '998',
      codeSystem: { code: 'ISO3166-1' }
    };

    await apiContext.post('/v1/countries', { data: testCountry });

    // Now update it
    const updatedCountry = {
      ...testCountry,
      countryName: 'Test Country Updated'
    };

    const response = await apiContext.put('/v1/countries/TEST02', {
      params: { codeSystem: 'ISO3166-1' },
      data: updatedCountry
    });

    expect(response.status()).toBe(200);
    
    const result = await response.json();
    expect(result.countryName).toBe('Test Country Updated');
  });

  test('DELETE /v1/countries/{code} should soft delete country', async () => {
    // First create a country to delete
    const testCountry = {
      countryCode: 'TEST03',
      countryName: 'Test Country Delete',
      iso2Code: 'TD',
      iso3Code: 'TSD',
      numericCode: '997',
      codeSystem: { code: 'ISO3166-1' }
    };

    await apiContext.post('/v1/countries', { data: testCountry });

    // Now delete it
    const response = await apiContext.delete('/v1/countries/TEST03', {
      params: { codeSystem: 'ISO3166-1' }
    });

    expect(response.status()).toBe(204);

    // Verify it's marked as inactive/deleted
    const getResponse = await apiContext.get('/v1/countries/TEST03', {
      params: { codeSystem: 'ISO3166-1' }
    });
    
    // Should either return 404 or show as inactive
    expect([404, 200].includes(getResponse.status())).toBeTruthy();
    
    if (getResponse.status() === 200) {
      const deletedCountry = await getResponse.json();
      expect(deletedCountry.active).toBeFalsy();
    }
  });

  test('GET /v1/countries/search should support full-text search', async () => {
    const response = await apiContext.get('/v1/countries/search', {
      params: {
        q: 'United',
        codeSystem: 'ISO3166-1'
      }
    });

    expect(response.status()).toBe(200);
    
    const data = await response.json();
    expect(data).toHaveProperty('content');
    expect(Array.isArray(data.content)).toBeTruthy();
    
    // Verify search results contain the search term
    if (data.content.length > 0) {
      const hasMatch = data.content.some((country: any) => 
        country.countryName.toLowerCase().includes('united')
      );
      expect(hasMatch).toBeTruthy();
    }
  });

  test('GET /v1/countries should support filtering by active status', async () => {
    const response = await apiContext.get('/v1/countries', {
      params: {
        active: 'true',
        codeSystem: 'ISO3166-1',
        size: '5'
      }
    });

    expect(response.status()).toBe(200);
    
    const data = await response.json();
    expect(data).toHaveProperty('content');
    
    // Verify all returned countries are active
    if (data.content.length > 0) {
      const allActive = data.content.every((country: any) => country.active !== false);
      expect(allActive).toBeTruthy();
    }
  });

  test('GET /v1/countries should support as-of date queries', async () => {
    const asOfDate = '2024-01-01';
    
    const response = await apiContext.get('/v1/countries', {
      params: {
        asOf: asOfDate,
        codeSystem: 'ISO3166-1',
        size: '5'
      }
    });

    expect(response.status()).toBe(200);
    
    const data = await response.json();
    expect(data).toHaveProperty('content');
    expect(Array.isArray(data.content)).toBeTruthy();
  });

  test('should handle pagination correctly', async () => {
    const page1Response = await apiContext.get('/v1/countries', {
      params: {
        page: '0',
        size: '2',
        codeSystem: 'ISO3166-1'
      }
    });

    expect(page1Response.status()).toBe(200);
    
    const page1Data = await page1Response.json();
    expect(page1Data.content.length).toBeLessThanOrEqual(2);
    expect(page1Data.number).toBe(0);
    
    if (page1Data.totalPages > 1) {
      const page2Response = await apiContext.get('/v1/countries', {
        params: {
          page: '1',
          size: '2',
          codeSystem: 'ISO3166-1'
        }
      });

      const page2Data = await page2Response.json();
      expect(page2Data.number).toBe(1);
      
      // Ensure different content between pages
      const page1Codes = page1Data.content.map((c: any) => c.countryCode);
      const page2Codes = page2Data.content.map((c: any) => c.countryCode);
      
      expect(page1Codes).not.toEqual(page2Codes);
    }
  });

  test('should support ETags for caching', async () => {
    const response1 = await apiContext.get('/v1/countries/US', {
      params: { codeSystem: 'ISO3166-1' }
    });

    expect(response1.status()).toBe(200);
    
    const etag = response1.headers()['etag'];
    if (etag) {
      // Make second request with ETag
      const response2 = await apiContext.get('/v1/countries/US', {
        params: { codeSystem: 'ISO3166-1' },
        headers: { 'If-None-Match': etag }
      });

      expect(response2.status()).toBe(304);
    }
  });

  test('should handle concurrent requests without conflicts', async () => {
    const countryCode = 'CONCURRENT_TEST';
    const baseCountry = {
      countryCode,
      countryName: 'Concurrent Test Country',
      iso2Code: 'CT',
      iso3Code: 'CON',
      numericCode: '995',
      codeSystem: { code: 'ISO3166-1' }
    };

    // Create initial country
    await apiContext.post('/v1/countries', { data: baseCountry });

    // Make concurrent update requests
    const updatePromises = Array.from({ length: 3 }, (_, i) => 
      apiContext.put(`/v1/countries/${countryCode}`, {
        params: { codeSystem: 'ISO3166-1' },
        data: {
          ...baseCountry,
          countryName: `Concurrent Test Country ${i + 1}`
        }
      })
    );

    const responses = await Promise.all(updatePromises);
    
    // At least one should succeed (200), others might fail with 409 (conflict)
    const successCount = responses.filter(r => r.status() === 200).length;
    const conflictCount = responses.filter(r => r.status() === 409).length;
    
    expect(successCount).toBeGreaterThanOrEqual(1);
    expect(successCount + conflictCount).toBe(3);
  });

  test('should return proper error formats', async () => {
    // Test 400 Bad Request
    const badResponse = await apiContext.post('/v1/countries', {
      data: { invalidField: 'test' }
    });
    
    expect(badResponse.status()).toBe(400);
    const badError = await badResponse.json();
    expect(badError).toHaveProperty('title');
    expect(badError).toHaveProperty('type');
    expect(badError).toHaveProperty('status', 400);

    // Test 404 Not Found
    const notFoundResponse = await apiContext.get('/v1/countries/NOTFOUND', {
      params: { codeSystem: 'ISO3166-1' }
    });
    
    expect(notFoundResponse.status()).toBe(404);
    const notFoundError = await notFoundResponse.json();
    expect(notFoundError).toHaveProperty('title');
    expect(notFoundError).toHaveProperty('type');
    expect(notFoundError).toHaveProperty('status', 404);
  });

  test('should handle large datasets efficiently', async () => {
    const startTime = Date.now();
    
    const response = await apiContext.get('/v1/countries', {
      params: {
        size: '100',
        codeSystem: 'ISO3166-1'
      }
    });

    const endTime = Date.now();
    const responseTime = endTime - startTime;

    expect(response.status()).toBe(200);
    expect(responseTime).toBeLessThan(5000); // Should respond within 5 seconds
    
    const data = await response.json();
    expect(data).toHaveProperty('content');
    expect(data.content.length).toBeLessThanOrEqual(100);
  });
});