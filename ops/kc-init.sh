#!/bin/sh
# kc-init.sh — run in a separate init container alongside a running Keycloak.
# Uses only stock tools (sh, grep, cut, head, tr, sed) + /opt/keycloak/bin/kcadm.sh.
# IMPORTANT: Do NOT start Keycloak here. This script only provisions.

set -eu

# -----------------------------
# Config (override via env)
# -----------------------------
KC_URL="${KEYCLOAK_URL:-http://keycloak:8080}"

REALM="${KEYCLOAK_REALM:-reference-data}"

# Backend (confidential) client for your API (use internal URL seen by containers)
API_CLIENT_ID="${KEYCLOAK_API_CLIENT_ID:-reference-api}"
API_ROOT_URL="${KEYCLOAK_API_ROOT_URL:-http://reference-api:8080}"

# Frontend (public) client for your UI
UI_CLIENT_ID="${KEYCLOAK_UI_CLIENT_ID:-reference-admin-ui}"
UI_ROOT_URL="${KEYCLOAK_UI_ROOT_URL:-http://localhost:4200}"
UI_REDIRECTS="${KEYCLOAK_UI_REDIRECTS:-http://localhost:4200/* http://admin-ui/*}"  # space-separated
UI_WEB_ORIGINS="${KEYCLOAK_UI_WEB_ORIGINS:-*}"

# E2E test user
TEST_USER="${KEYCLOAK_TEST_USER:-testuser}"
TEST_PASS="${KEYCLOAK_TEST_PASS:-testpass}"

# Where to write the confidential client's secret (mount this path on host)
SECRET_OUT="${KEYCLOAK_SECRET_OUT:-/secrets/reference-api.secret}"

# Admin credentials (prefer new bootstrap envs; fall back to legacy)
ADMIN_USER="${KEYCLOAK_ADMIN:-${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin}}"

ts() { date '+%F %T'; }

# -----------------------------
# Wait for Keycloak admin API by looping kcadm login
# -----------------------------
echo "$(ts) | ⏳ waiting for Keycloak (kcadm login loop) at ${KC_URL} ..."
attempt=0
max_attempts=240   # ~8 minutes (240 * 2s), adjust if DB init is slow
while :; do
  if /opt/keycloak/bin/kcadm.sh config credentials \
      --server "${KC_URL}" \
      --realm master \
      --user "${ADMIN_USER}" \
      --password "${ADMIN_PASS}" >/dev/null 2>&1; then
    echo "$(ts) | ✅ kcadm login OK"
    break
  fi
  attempt=$((attempt + 1))
  [ "$attempt" -ge "$max_attempts" ] && { echo "$(ts) | ❌ timeout waiting for Keycloak admin"; exit 1; }
  sleep 2
done

echo "$(ts) | 🔧 enforcing sslRequired=NONE on master realm"
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE >/dev/null


# -----------------------------
# JSON helpers (BusyBox-safe, tolerate spaces like "key" : "value")
# -----------------------------
split_objects() {
  # flatten to one line, then split JSON array objects to lines
  tr -d '\n' | sed 's/},[[:space:]]*{/\n/g'
}

extract_first_json_field() {
  # stdin JSON -> first value for given field (handles spaces around :)
  # usage: echo '{"id" : "abc"}' | extract_first_json_field id
  field="$1"
  grep -E -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" 2>/dev/null \
    | head -n1 \
    | cut -d: -f2 \
    | tr -d '" '
}

# Robust client lookup; exact clientId match with flexible spacing
get_client_id() {
  realm="$1"; cid="$2"

  try_mode() {
    # $1 = kcadm "get" arg (e.g., 'clients -q "clientId=reference-api"' or 'clients?clientId=...')
    # We pass --fields to keep output small, then exact-match clientId and extract id.
    /opt/keycloak/bin/kcadm.sh get $1 -r "${realm}" --fields id,clientId 2>/dev/null \
      | split_objects \
      | grep -E "\"clientId\"[[:space:]]*:[[:space:]]*\"${cid}\"" \
      | head -n1 \
      | extract_first_json_field id
  }

  # 1) -q clientId=...
  id="$(try_mode 'clients -q "clientId='"${cid}"'"')"
  [ -n "$id" ] && { echo "$id"; return 0; }

  # 2) clients?clientId=...
  id="$(try_mode "clients?clientId=${cid}")"
  [ -n "$id" ] && { echo "$id"; return 0; }

  # 3) clients?search=true&clientId=...
  id="$(try_mode "clients?search=true&clientId=${cid}")"
  [ -n "$id" ] && { echo "$id"; return 0; }

  # 4) last resort: dump some rows for debugging; return empty
  echo "$(ts) | ⚠️  could not find client '${cid}' via API; sample listing follows:" >&2
  /opt/keycloak/bin/kcadm.sh get clients -r "${realm}" --fields id,clientId 2>/dev/null \
    | split_objects | head -n 20 >&2
  echo ""
}

get_client_secret() {
  realm="$1"; id="$2"
  /opt/keycloak/bin/kcadm.sh get "clients/${id}/client-secret" -r "${realm}" 2>/dev/null \
    | extract_first_json_field value
}

# -----------------------------
# Ensure realm (sslRequired=NONE for HTTP dev)
# -----------------------------
if /opt/keycloak/bin/kcadm.sh get "realms/${REALM}" >/dev/null 2>&1; then
  echo "$(ts) | ➡️  realm '${REALM}' exists; enforcing sslRequired=NONE"
  /opt/keycloak/bin/kcadm.sh update "realms/${REALM}" -s sslRequired=NONE >/dev/null
else
  echo "$(ts) | ✨ creating realm '${REALM}' (sslRequired=NONE)"
  /opt/keycloak/bin/kcadm.sh create realms \
    -s realm="${REALM}" \
    -s enabled=true \
    -s sslRequired=NONE >/dev/null
fi

# -----------------------------
# Ensure confidential API client + ALWAYS write a secret
# -----------------------------
API_ID="$(get_client_id "${REALM}" "${API_CLIENT_ID}")"
if [ -z "${API_ID}" ]; then
  echo "$(ts) | ✨ creating confidential client '${API_CLIENT_ID}'"
  /opt/keycloak/bin/kcadm.sh create clients -r "${REALM}" \
    -s clientId="${API_CLIENT_ID}" \
    -s enabled=true \
    -s protocol=openid-connect \
    -s publicClient=false \
    -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s 'redirectUris=["'"${API_ROOT_URL}"'/*"]' \
    -s 'webOrigins=["'"${API_ROOT_URL}"'"]' \
    -s 'attributes."client_credentials.use_refresh_tokens"=false' >/dev/null 2>&1 || true
  # Re-fetch ID regardless (handles "already exists" 409)
  API_ID="$(get_client_id "${REALM}" "${API_CLIENT_ID}")"
fi

if [ -z "${API_ID}" ]; then
  echo "$(ts) | ❌ still could not obtain client id for ${API_CLIENT_ID}; see sample listing above."
  exit 1
fi

echo "$(ts) | 🔑 obtaining secret for '${API_CLIENT_ID}'"
API_SECRET="$(get_client_secret "${REALM}" "${API_ID}")"
if [ -z "${API_SECRET}" ]; then
  echo "$(ts) | (no secret found — creating one)"
  /opt/keycloak/bin/kcadm.sh create "clients/${API_ID}/client-secret" -r "${REALM}" >/dev/null 2>&1 || true
  API_SECRET="$(get_client_secret "${REALM}" "${API_ID}")"
fi
mkdir -p "$(dirname "${SECRET_OUT}")"
printf "%s" "${API_SECRET}" > "${SECRET_OUT}"
echo "$(ts) | ✅ wrote client secret to ${SECRET_OUT}"

# -----------------------------
# Ensure public UI client
# -----------------------------
UI_ID="$(get_client_id "${REALM}" "${UI_CLIENT_ID}")"
if [ -z "${UI_ID}" ]; then
  echo "$(ts) | ✨ creating public client '${UI_CLIENT_ID}'"
  # Convert space-separated redirects to JSON array
  # shellcheck disable=SC2086
  set -- ${UI_REDIRECTS}
  redirects_json=""
  for r in "$@"; do
    [ -n "${redirects_json}" ] && redirects_json="${redirects_json},"
    redirects_json="${redirects_json}\"${r}\""
  done
  /opt/keycloak/bin/kcadm.sh create clients -r "${REALM}" \
    -s clientId="${UI_CLIENT_ID}" \
    -s enabled=true \
    -s protocol=openid-connect \
    -s publicClient=true \
    -s standardFlowEnabled=true \
    -s directAccessGrantsEnabled=false \
    -s 'attributes."pkce.code.challenge.method"="S256"' \
    -s "rootUrl=${UI_ROOT_URL}" \
    -s "webOrigins=[\"${UI_WEB_ORIGINS}\"]" \
    -s "redirectUris=[${redirects_json}]" >/dev/null 2>&1 || true
else
  echo "$(ts) | ➡️  client '${UI_CLIENT_ID}' exists"
fi

# -----------------------------
# Ensure client-level roles for entity permissions
# -----------------------------
UI_ID_ROLES="$(get_client_id "${REALM}" "${UI_CLIENT_ID}")"
if [ -n "${UI_ID_ROLES}" ]; then
  echo "$(ts) | 👤 creating client roles for '${UI_CLIENT_ID}'"
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/country-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=country-owner
  fi
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/airport-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=airport-owner
  fi
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/port-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=port-owner
  fi
fi

# -----------------------------
# Ensure E2E user
# -----------------------------
UI_ID_ROLES="$(get_client_id "${REALM}" "${UI_CLIENT_ID}")"
if [ -n "${UI_ID_ROLES}" ]; then
  echo "$(ts) | 👤 creating client roles for '${UI_CLIENT_ID}'"
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/country-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=country-owner
  fi
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/airport-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=airport-owner
  fi
  if ! /opt/keycloak/bin/kcadm.sh get "clients/${UI_ID_ROLES}/roles/port-owner" -r "${REALM}" >/dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create "clients/${UI_ID_ROLES}/roles" -r "${REALM}" -s name=port-owner
  fi
fi

# -----------------------------
# Ensure E2E user
# -----------------------------
if ! /opt/keycloak/bin/kcadm.sh get "users?username=${TEST_USER}" -r "${REALM}" 2>/dev/null \
     | grep -q "\"username\"[[:space:]]*:[[:space:]]*\"${TEST_USER}\""; then
  echo "$(ts) | 👤 creating user '${TEST_USER}'"
  USER_ID="$(/opt/keycloak/bin/kcadm.sh create users -r "${REALM}" -s username="${TEST_USER}" -s enabled=true -i)"
  /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${USER_ID}" --new-password "${TEST_PASS}" --temporary=false
else
  echo "$(ts) | ➡️  user '${TEST_USER}' exists"
fi

# -----------------------------
# Ensure roles
# -----------------------------
if ! /opt/keycloak/bin/kcadm.sh get roles/DATA_STEWARD -r "${REALM}" >/dev/null 2>&1; then
  echo "$(ts) | 👤 creating role 'DATA_STEWARD'"
  /opt/keycloak/bin/kcadm.sh create roles -r "${REALM}" -s name=DATA_STEWARD
fi

if ! /opt/keycloak/bin/kcadm.sh get roles/ADMIN -r "${REALM}" >/dev/null 2>&1; then
  echo "$(ts) | 👤 creating role 'ADMIN'"
  /opt/keycloak/bin/kcadm.sh create roles -r "${REALM}" -s name=ADMIN
fi

# -----------------------------
# Create testuser and assign ADMIN role
# -----------------------------
if ! /opt/keycloak/bin/kcadm.sh get "users?username=testuser" -r "${REALM}" 2>/dev/null | grep -q "\"username\"[[:space:]]*:[[:space:]]*\"testuser\""; then
  echo "$(ts) | 👤 creating user 'testuser'"
  TESTUSER_ID="$(/opt/keycloak/bin/kcadm.sh create users -r "${REALM}" -s username="testuser" -s enabled=true -s firstName="Test" -s lastName="User" -s email="testuser@example.com" -i)"
  /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${TESTUSER_ID}" --new-password "testpass" --temporary=false
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${TESTUSER_ID}" --rolename ADMIN
  echo "$(ts) | ✅ assigned ADMIN role and details to testuser"
else
  echo "$(ts) | ➡️  user 'testuser' exists, ensuring details and role"
  TESTUSER_ID=$(/opt/keycloak/bin/kcadm.sh get "users?username=testuser" -r "${REALM}" | extract_first_json_field id)
  /opt/keycloak/bin/kcadm.sh update "users/${TESTUSER_ID}" -r "${REALM}" -s firstName="Test" -s lastName="User" -s email="testuser@example.com"
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${TESTUSER_ID}" --rolename ADMIN >/dev/null 2>&1 || true
fi

# -----------------------------
# Create country-steward and assign DATA_STEWARD and country-owner roles
# -----------------------------
UI_ID_ROLES="$(get_client_id "${REALM}" "${UI_CLIENT_ID}")"
if ! /opt/keycloak/bin/kcadm.sh get "users?username=country-steward" -r "${REALM}" 2>/dev/null | grep -q "\"username\"[[:space:]]*:[[:space:]]*\"country-steward\""; then
  echo "$(ts) | 👤 creating user 'country-steward'"
  COUNTRY_STEWARD_ID="$(/opt/keycloak/bin/kcadm.sh create users -r "${REALM}" -s username="country-steward" -s enabled=true -i)"
  /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${COUNTRY_STEWARD_ID}" --new-password "password" --temporary=false
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${COUNTRY_STEWARD_ID}" --rolename DATA_STEWARD
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${COUNTRY_STEWARD_ID}" --cclientid "${UI_CLIENT_ID}" --rolename country-owner
  echo "$(ts) | ✅ assigned DATA_STEWARD and country-owner roles to country-steward"
fi

# -----------------------------
# Create airport-steward and assign DATA_STEWARD and airport-owner roles
# -----------------------------
if ! /opt/keycloak/bin/kcadm.sh get "users?username=airport-steward" -r "${REALM}" 2>/dev/null | grep -q "\"username\"[[:space:]]*:[[:space:]]*\"airport-steward\""; then
  echo "$(ts) | 👤 creating user 'airport-steward'"
  AIRPORT_STEWARD_ID="$(/opt/keycloak/bin/kcadm.sh create users -r "${REALM}" -s username="airport-steward" -s enabled=true -i)"
  /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${AIRPORT_STEWARD_ID}" --new-password "password" --temporary=false
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${AIRPORT_STEWARD_ID}" --rolename DATA_STEWARD
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${AIRPORT_STEWARD_ID}" --cclientid "${UI_CLIENT_ID}" --rolename airport-owner
  echo "$(ts) | ✅ assigned DATA_STEWARD and airport-owner roles to airport-steward"
fi

# -----------------------------
# Create port-steward and assign DATA_STEWARD and port-owner roles
# -----------------------------
if ! /opt/keycloak/bin/kcadm.sh get "users?username=port-steward" -r "${REALM}" 2>/dev/null | grep -q "\"username\"[[:space:]]*:[[:space:]]*\"port-steward\""; then
  echo "$(ts) | 👤 creating user 'port-steward'"
  PORT_STEWARD_ID="$(/opt/keycloak/bin/kcadm.sh create users -r "${REALM}" -s username="port-steward" -s enabled=true -i)"
  /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${PORT_STEWARD_ID}" --new-password "password" --temporary=false
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${PORT_STEWARD_ID}" --rolename DATA_STEWARD
  /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${PORT_STEWARD_ID}" --cclientid "${UI_CLIENT_ID}" --rolename port-owner
  echo "$(ts) | ✅ assigned DATA_STEWARD and port-owner roles to port-steward"
fi

echo "$(ts) | ✅ kc-init complete"
