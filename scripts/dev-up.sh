#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$ROOT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    if [ -f "$ROOT_DIR/.env.example" ]; then
        cp "$ROOT_DIR/.env.example" "$ENV_FILE"
        echo "Created .env from .env.example"
    else
        touch "$ENV_FILE"
    fi
fi

if ! grep -q "INTERNAL_SERVICE_KEY=" "$ENV_FILE" || grep -q "INTERNAL_SERVICE_KEY=CozyNestInternalServiceKeySecret2026" "$ENV_FILE"; then
    RANDOM_KEY=$(openssl rand -hex 32 2>/dev/null || date +%s%N | sha256sum | head -c 64)
    if grep -q "INTERNAL_SERVICE_KEY=" "$ENV_FILE"; then
        sed -i.bak "s/INTERNAL_SERVICE_KEY=.*/INTERNAL_SERVICE_KEY=$RANDOM_KEY/" "$ENV_FILE" && rm -f "$ENV_FILE.bak"
    else
        echo "INTERNAL_SERVICE_KEY=$RANDOM_KEY" >> "$ENV_FILE"
    fi
    echo "Generated fresh random INTERNAL_SERVICE_KEY in .env"
fi

echo "Starting CozyNest infrastructure containers..."
docker compose -f "$ROOT_DIR/docker-compose.yml" --profile core up -d

