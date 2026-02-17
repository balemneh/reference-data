(function (window) {
  window.__env__ = window.__env__ || {};
  
  // Environment variables
  window.__env__.apiUrl = '${API_URL}';
  window.__env__.keycloakUrl = '${KEYCLOAK_URL}';
  window.__env__.keycloakRealm = '${KEYCLOAK_REALM}';
  window.__env__.keycloakClientId = '${KEYCLOAK_CLIENT_ID}';
  window.__env__.requireHttps = '${REQUIRE_HTTPS}';
  
}(this));
