import axios from 'axios';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Setup authentication for e2e tests using Keycloak
 * Uses the reference-api client with credentials from ops/secrets/reference-api.secret
 */

// Keycloak configuration
const KEYCLOAK_URL = 'http://localhost:8085';
const REALM = 'reference-data';
const CLIENT_ID = 'reference-api';
const TOKEN_ENDPOINT = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

// Read client secret from file
function getClientSecret(): string {
  try {
    const secretPath = path.join(__dirname, '../../ops/secrets/reference-api.secret');
    const secret = fs.readFileSync(secretPath, 'utf-8').trim();
    return secret;
  } catch (error) {
    console.error('Failed to read client secret:', error);
    // Fallback to known secret if file doesn't exist
    return 'pzX5C4IsiKnh6cENso5fySoTOAxkj047';
  }
}

export async function getTestAuthToken(): Promise<string> {
  const clientSecret = getClientSecret();
  
  try {
    // Use client credentials grant for service-to-service authentication
    const response = await axios.post(
      TOKEN_ENDPOINT,
      new URLSearchParams({
        grant_type: 'client_credentials',
        client_id: CLIENT_ID,
        client_secret: clientSecret,
        scope: 'openid profile'
      }),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        timeout: 5000
      }
    );
    
    if (response.data?.access_token) {
      console.log('✓ Got Keycloak access token for reference-api client');
      return response.data.access_token;
    }
  } catch (error: any) {
    console.error('Failed to get Keycloak token:', error.message);
    
    // Try with password grant as fallback (requires test user)
    try {
      const passwordResponse = await axios.post(
        TOKEN_ENDPOINT,
        new URLSearchParams({
          grant_type: 'password',
          client_id: CLIENT_ID,
          client_secret: clientSecret,
          username: 'test-user',
          password: 'test-password',
          scope: 'openid profile'
        }),
        {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          timeout: 5000
        }
      );
      
      if (passwordResponse.data?.access_token) {
        console.log('✓ Got Keycloak token using password grant');
        return passwordResponse.data.access_token;
      }
    } catch (passwordError: any) {
      console.warn('Password grant also failed:', passwordError.message);
    }
  }
  
  // Return a mock token for testing if Keycloak is not available
  console.warn('⚠ Using mock token - tests may fail against secured endpoints');
  const mockToken = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJuYW1lIjoiVGVzdCBVc2VyIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk5OTk5OTk5OTksInJvbGVzIjpbImFkbWluIiwidXNlciJdfQ.test';
  
  return mockToken;
}

export async function setupAuthHeaders(token?: string): Promise<Record<string, string>> {
  const authToken = token || await getTestAuthToken();
  
  return {
    'Authorization': `Bearer ${authToken}`,
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };
}