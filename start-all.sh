#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "  🚀 KHỞI ĐỘNG TOÀN BỘ COZYNEST BACKEND MICROSERVICES"
echo "=========================================================="

# 1. Đảm bảo hạ tầng (MySQL, Redis, Mailpit) đang chạy
echo "📦 [1/3] Kiểm tra hạ tầng Docker (MySQL, Redis, Mailpit)..."
if ! nc -z localhost 6379 2>/dev/null; then
  docker compose up -d redis 2>/dev/null || true
else
  echo "   -> Redis đã sẵn sàng trên cổng 6379."
fi

if ! docker ps --format '{{.Names}}' | grep -q "^cozynest_mysql$"; then
  docker compose up -d mysql
fi

if ! docker ps --format '{{.Names}}' | grep -q "^cozynest_mailpit$"; then
  docker compose up -d mailpit
fi

# Phát hiện cổng MySQL đang mở (5555, 3307, hoặc 3306)
if nc -z localhost 5555 2>/dev/null; then
    ACTIVE_DB_PORT=5555
elif nc -z localhost 3307 2>/dev/null; then
    ACTIVE_DB_PORT=3307
elif nc -z localhost 3306 2>/dev/null; then
    ACTIVE_DB_PORT=3306
else
    ACTIVE_DB_PORT=5555
fi
echo "   -> Kết nối MySQL tại cổng: $ACTIVE_DB_PORT"

# 2. Biên dịch JAR nếu chưa có hoặc có thay đổi
echo "🔨 [2/3] Biên dịch các file JAR microservice..."
./gradlew bootJar -x test --parallel

# 3. Khởi chạy 7 microservices
echo "🚀 [3/3] Đang khởi chạy 7 microservices trong nền..."
mkdir -p logs pids

export DB_PORT=$ACTIVE_DB_PORT
export DB_HOST=localhost
export DB_USER=root
export DB_PASSWORD=rootpassword123
export REDIS_HOST=localhost
export REDIS_PORT=6379

SERVICES=(
  "api-gateway:8080:api-gateway/build/libs/api-gateway-*.jar"
  "identity-service:8081:identity-service/build/libs/identity-service-*.jar"
  "listing-service:8082:listing-service/build/libs/listing-service-*.jar"
  "booking-service:8083:booking-service/build/libs/booking-service-*.jar"
  "billing-service:8084:billing-service/build/libs/billing-service-*.jar"
  "communication-service:8085:communication-service/build/libs/communication-service-*.jar"
  "admin-service:8086:admin-service/build/libs/admin-service-*.jar"
)

for ENTRY in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT JAR_PATTERN <<< "$ENTRY"

  # Kiểm tra nếu service đã chạy trên port
  EXISTING_PID=$(lsof -ti :$PORT 2>/dev/null | head -n1 || true)
  if [ -n "$EXISTING_PID" ]; then
    echo "⚠️  $NAME đã chạy sẵn trên cổng $PORT (PID: $EXISTING_PID)"
  else
    JAR_FILE=$(ls $JAR_PATTERN 2>/dev/null | head -n1 || true)
    if [ -f "$JAR_FILE" ]; then
      echo "▶️  Khởi động $NAME (port $PORT)..."
      if [ "$NAME" = "api-gateway" ]; then
        nohup java -Dserver.port=$PORT \
                   -DREDIS_HOST=localhost \
                   -DREDIS_PORT=6379 \
                   -jar "$JAR_FILE" > "logs/$NAME.log" 2>&1 &
      else
        DB_NAME="cozynest_${NAME%-service}"
        nohup java -Dserver.port=$PORT \
                   -DDB_HOST=localhost \
                   -DDB_PORT=$ACTIVE_DB_PORT \
                   -DDB_NAME=$DB_NAME \
                   -DDB_USER=root \
                   -DDB_PASSWORD=rootpassword123 \
                   -DREDIS_HOST=localhost \
                   -DREDIS_PORT=6379 \
                   -Dspring.datasource.url="jdbc:mysql://localhost:$ACTIVE_DB_PORT/$DB_NAME?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci" \
                   -Dspring.datasource.username=root \
                   -Dspring.datasource.password=rootpassword123 \
                   -Dspring.flyway.enabled=false \
                   -jar "$JAR_FILE" > "logs/$NAME.log" 2>&1 &
      fi
      PID=$!
      echo $PID > "pids/$NAME.pid"
      echo "   PID: $PID | Log: logs/$NAME.log"
    else
      echo "❌ Không tìm thấy JAR cho $NAME: $JAR_PATTERN"
    fi
  fi
done

echo ""
echo "=========================================================="
echo "  🎉 Tất cả dịch vụ đã được kích hoạt!"
echo "=========================================================="
echo "  • API Gateway:          http://localhost:8080"
echo "  • Identity Service:     http://localhost:8081"
echo "  • Listing Service:      http://localhost:8082"
echo "  • Booking Service:      http://localhost:8083"
echo "  • Billing Service:      http://localhost:8084"
echo "  • Communication Service:http://localhost:8085"
echo "  • Admin Service:        http://localhost:8086"
echo "  • Mailpit (Web UI):     http://localhost:8025"
echo "  • Swagger UI (Gộp):     http://localhost:8080/docs"
echo "=========================================================="
echo "💡 Lệnh hữu ích:"
echo "  - Kiểm tra trạng thái:  ./status.sh"
echo "  - Dừng tất cả dịch vụ:  ./stop-all.sh"
echo "  - Xem log thời gian thực: tail -f logs/<tên-service>.log"
echo "=========================================================="
