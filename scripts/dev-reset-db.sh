#!/usr/bin/env bash
# scripts/dev-reset-db.sh
# Script to safely reset dev database schemas without encoding loss
# Strictly forbidden in production

set -euo pipefail

CURRENT_PROFILE="${SPRING_PROFILES_ACTIVE:-${APP_PROFILE:-dev}}"

if [[ "$CURRENT_PROFILE" == "prod" || "$CURRENT_PROFILE" == "production" ]]; then
    echo "CRITICAL: dev-reset-db is strictly forbidden on production profile ($CURRENT_PROFILE)!" >&2
    exit 1
fi

echo "Target Environment: $CURRENT_PROFILE (SAFE)"
echo "Resetting dev databases and re-applying migrations/seeds in UTF-8..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
node "$SCRIPT_DIR/reset_db_core.js"

echo "Database reset successfully completed!"

