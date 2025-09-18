#!/bin/sh

# Replace environment variables in env.js at container startup
# This allows configuration without rebuilding the Docker image

# Set defaults if not provided
API_URL=${API_URL:-""}
KEYCLOAK_URL=${KEYCLOAK_URL:-""}
KEYCLOAK_REALM=${KEYCLOAK_REALM:-"reference-data"}
KEYCLOAK_CLIENT_ID=${KEYCLOAK_CLIENT_ID:-"reference-ui"}

# Note: Since we're running as non-root, env.js should be writable
# The build process should ensure proper permissions
# If the file is not writable, skip the update and use defaults
if [ -w "/usr/share/nginx/html/assets/env.js" ]; then
  # Create env.js from template with actual values
  cat > /usr/share/nginx/html/assets/env.js <<EOF
(function (window) {
  window.__env__ = window.__env__ || {};
  
  // Runtime configuration
  window.__env__.apiUrl = '${API_URL}';
  window.__env__.keycloakUrl = '${KEYCLOAK_URL}';
  window.__env__.keycloakRealm = '${KEYCLOAK_REALM}';
  window.__env__.keycloakClientId = '${KEYCLOAK_CLIENT_ID}';
  
}(this));
EOF

  echo "Environment configuration updated:"
  echo "  API_URL: ${API_URL}"
  echo "  KEYCLOAK_URL: ${KEYCLOAK_URL}"
  echo "  KEYCLOAK_REALM: ${KEYCLOAK_REALM}"
  echo "  KEYCLOAK_CLIENT_ID: ${KEYCLOAK_CLIENT_ID}"
else
  echo "Warning: env.js is not writable, using default configuration"
fi

# Start nginx
nginx -g 'daemon off;'