#!/bin/sh
# Exit immediately if a command exits with a non-zero status.
set -e

# Define a whitelist of variables to be substituted. This prevents accidentally
# replacing other variables in the file that might use a '$'.
# Note: Add any new environment variables to this list.
VARS_TO_SUBSTITUTE='${API_URL} ${KEYCLOAK_URL} ${KEYCLOAK_REALM} ${KEYCLOAK_CLIENT_ID} ${REQUIRE_HTTPS}'

# Set defaults if not provided
API_URL=${API_URL:-""}
KEYCLOAK_URL=${KEYCLOAK_URL:-""}
KEYCLOAK_REALM=${KEYCLOAK_REALM:-"reference-data"}
KEYCLOAK_CLIENT_ID=${KEYCLOAK_CLIENT_ID:-"reference-admin-ui"}
REQUIRE_HTTPS=${REQUIRE_HTTPS:-"false"}

# Create the assets directory if it doesn't exist
mkdir -p /usr/share/nginx/html/assets

# Create env.js from template with actual values
cat > /usr/share/nginx/html/assets/env.js <<EOF
(function (window) {
  window.__env__ = window.__env__ || {};
  
  // Runtime configuration
  window.__env__.apiUrl = '${API_URL}';
  window.__env__.keycloakUrl = '${KEYCLOAK_URL}';
  window.__env__.keycloakRealm = '${KEYCLOAK_REALM}';
  window.__env__.keycloakClientId = '${KEYCLOAK_CLIENT_ID}';
  window.__env__.requireHttps = '${REQUIRE_HTTPS}';
  
}(this));
EOF

  echo "Environment configuration updated:"
  echo "  API_URL: ${API_URL}"
  echo "  KEYCLOAK_URL: ${KEYCLOAK_URL}"
  echo "  KEYCLOAK_REALM: ${KEYCLOAK_REALM}"
  echo "  KEYCLOAK_CLIENT_ID: ${KEYCLOAK_CLIENT_ID}"
  echo "  REQUIRE_HTTPS: ${REQUIRE_HTTPS}"

# Substitute environment variables in nginx configuration
NGINX_CONF_FILE="/etc/nginx/conf.d/default.conf"
if [ -f "$NGINX_CONF_FILE" ]; then
    # Set default values for the variables if they are not already set.
    : "${RESOLVER:=127.0.0.11}" # Default for Docker Compose internal DNS
    : "${REFDATA_API_URL:=http://refdata-api:8080}" # Default for local docker-compose

    # Create a temporary file for the substitution
    TEMP_FILE=$(mktemp)
    
    # Export the variables so envsubst can use them
    export RESOLVER
    export REFDATA_API_URL

    # Substitute the variables. We list them explicitly to avoid substituting
    # nginx's own variables like $host, $uri, etc.
    envsubst '${RESOLVER} ${REFDATA_API_URL}' < "$NGINX_CONF_FILE" > "$TEMP_FILE" && mv "$TEMP_FILE" "$NGINX_CONF_FILE"
    
    echo "Nginx configuration updated with:"
    echo "  Resolver: ${RESOLVER}"
    echo "  Refdata API URL: ${REFDATA_API_URL}"
else
    echo "Warning: Nginx config file $NGINX_CONF_FILE not found. Skipping substitution."
fi


# Execute the command passed to this script (e.g., the CMD from Dockerfile).
exec "$@"
