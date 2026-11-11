#!/usr/bin/env bash

SERVICES=(
  "API Gateway:8080"
  "Identity Service:8081"
  "Listing Service:8082"
  "Booking Service:8083"
  "Billing Service:8084"
  "Communication Service:8085"
  "Admin Service:8086"
)

echo "=========================================================="
echo "  📊 TRẠNG THÁI COZYNEST BACKEND MICROSERVICES"
echo "=========================================================="

ALL_OK=true
for ENTRY in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$ENTRY"
  PID=$(lsof -ti :$PORT 2>/dev/null | head -n1 || true)
  if [ -n "$PID" ]; then
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:$PORT/actuator/health" 2>/dev/null || echo "DOWN")
    if [ "$HTTP_CODE" = "200" ]; then
      printf "  🟢 %-24s (Port %-4s): HEALTHY  (PID: %s)\n" "$NAME" "$PORT" "$PID"
    else
      printf "  🟡 %-24s (Port %-4s): STARTING (PID: %s, HTTP: %s)\n" "$NAME" "$PORT" "$PID" "$HTTP_CODE"
    fi
  else
    printf "  🔴 %-24s (Port %-4s): STOPPED\n" "$NAME" "$PORT"
    ALL_OK=false
  fi
done

echo "----------------------------------------------------------"
# Kiểm tra Docker infra
for CONTAINER in cozynest_mysql cozynest_redis cozynest_mailpit; do
  STATUS=$(docker inspect --format='{{.State.Status}}' $CONTAINER 2>/dev/null || echo "not running")
  if [ "$STATUS" = "running" ]; then
    printf "  🟢 Docker Infra %-14s: RUNNING\n" "[$CONTAINER]"
  else
    printf "  🔴 Docker Infra %-14s: %s\n" "[$CONTAINER]" "$STATUS"
  fi
done
echo "=========================================================="

