#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
FRONTEND_DIR="${PROJECT_DIR}"
DIST_DIR="${FRONTEND_DIR}/dist"

cd "${FRONTEND_DIR}"

if ! [ -d "./node_modules" ]; then
    npm install
fi

npm run build

# Move the content of dist/frontend to dist
mv ${DIST_DIR}/frontend/* ${DIST_DIR}/
rm -rf ${DIST_DIR}/frontend

echo "Frontend build complete. Artifacts are in ${DIST_DIR}"
