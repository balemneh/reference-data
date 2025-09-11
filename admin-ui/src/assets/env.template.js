(function (window) {
  window.__env__ = window.__env__ || {};

  // Environment variables will be substituted at deployment time
  // Use ${VAR_NAME} syntax for substitution
  
  window.__env__.apiUrl = '${API_URL}';
  window.__env__.keycloakUrl = '${KEYCLOAK_URL}';
  window.__env__.keycloakRealm = '${KEYCLOAK_REALM}';
  window.__env__.keycloakClientId = '${KEYCLOAK_CLIENT_ID}';
  
}(this));