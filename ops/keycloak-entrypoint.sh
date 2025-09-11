#!/bin/sh
set -euo pipefail

# Start Keycloak with HTTP enabled for local/dev
exec /opt/keycloak/bin/kc.sh start \
  --http-enabled=true \
  --http-port=8080 \
  --hostname-strict=false
