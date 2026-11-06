#!/usr/bin/env bash
set -e

# ============================================================
# PhongTro Online - 1-Click Deployment Script
# Port: MySQL 3306 | Spring Boot 8085
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  PhongTro Online Backend Deployment"
echo "  MySQL: Port 3306 | Spring Boot: Port 8085"
echo "=========================================="

if [ ! -f .env ]; then
  echo "[+] Tạo file .env từ .env.example..."
  cp .env.example .env
fi

echo "[+] Khởi chạy Docker Compose (MySQL: 3306, Backend: 8085)..."
docker compose down || true
docker compose up -d --build

echo ""
echo "[+] Đang kiểm tra trạng thái containers..."
sleep 5
docker compose ps

echo ""
echo "=========================================="
echo "  Triển khai thành công!"
echo "  - MySQL: localhost:3306"
echo "  - Spring Boot API: http://localhost:8085/api/v1"
echo "  - Health Check: curl http://localhost:8085/api/v1/health"
echo "=========================================="

