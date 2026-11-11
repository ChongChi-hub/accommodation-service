#!/usr/bin/env bash

echo "🛑 Đang dừng toàn bộ Cozynest Backend Microservices..."
PORTS=(8080 8081 8082 8083 8084 8085 8086)

for PORT in "${PORTS[@]}"; do
  PIDS=$(lsof -ti :$PORT 2>/dev/null || true)
  if [ -n "$PIDS" ]; then
    echo "  - Đang tắt tiến trình trên cổng $PORT (PID: $PIDS)..."
    kill -9 $PIDS 2>/dev/null || true
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
rm -rf "$SCRIPT_DIR/pids"/*.pid 2>/dev/null || true

echo "✅ Đã dừng toàn bộ 7 microservices."

