# HƯỚNG DẪN TRIỂN KHAI BACKEND SPRING BOOT (PHONGTRO ONLINE)

Hệ thống đã được thiết lập sẵn sàng 100% với cấu hình cổng theo yêu cầu:
- **MySQL Database**: Cổng **`3306`**
- **Spring Boot Backend**: Cổng **`8085`**
- **Đầy đủ 76/76 REST API endpoints**: Tương thích 100% với Frontend React.

---

## 📦 Bộ file triển khai đã đóng gói
File nén **`deploy.tar.gz`** (nằm ngay tại thư mục `service/`) đã bao gồm đầy đủ:
```text
deploy.tar.gz
├── docker-compose.yml              # Cấu hình container (MySQL 3306, Backend 8085, giới hạn RAM)
├── Dockerfile                      # Temurin JRE 21 Alpine siêu nhẹ
├── .env.example / .env             # Biến môi trường chuẩn
├── database/schema.sql             # Toàn bộ cấu trúc bảng và 5 tài khoản Admin mặc định
├── build/libs/service-0.0.1-SNAPSHOT.jar # File thực thi Java 21 Spring Boot
├── deploy.sh                       # Script chạy 1-click
└── systemd/phongtro.service        # Template chạy nền không cần Docker (siêu tiết kiệm RAM)
```

---

## CÁCH 1: Triển khai bằng Docker Compose (Khuyên dùng)

### Bước 1: Copy file lên server
Từ máy local của bạn, chạy lệnh copy file nén lên thư mục bất kỳ trên VPS (ví dụ `/opt/phongtro` hoặc `~/phongtro`):
```bash
scp -P 22 deploy.tar.gz devopsg5@103.72.57.148:/home/devopsg5/
```

### Bước 2: Giải nén
Trên server VPS:
```bash
mkdir -p /home/devopsg5/phongtro
cd /home/devopsg5/phongtro
tar -xzvf /home/devopsg5/deploy.tar.gz
```

### Bước 3: Khởi chạy dịch vụ
Bạn chỉ cần chạy 1 lệnh duy nhất:
```bash
./deploy.sh
```
*Hoặc chạy trực tiếp:*
```bash
docker compose up -d --build
```

> **Ghi chú về phân bổ RAM cho VPS 4GB:**
> `docker-compose.yml` đã được cấu hình tối ưu hiệu năng cao cho VPS 4GB:
> - MySQL: `mem_limit: 1g` + `--innodb-buffer-pool-size=256M` + `--max_connections=150`
> - Spring Boot: `mem_limit: 1.2g` + `-Xms256m -Xmx768m`
> Giúp truy vấn nhanh, GC mượt mà và luôn chừa lại ~1.5GB cho hệ điều hành Linux.

---

## CÁCH 2: Triển khai Native JAR (Dành cho VPS cực ít RAM < 500MB)

Nếu VPS của bạn đang chạy nhiều dự án khác và không muốn tốn RAM cho Docker, bạn có thể chạy file JAR trực tiếp:

1. **Yêu cầu:** Cài Java 21:
   ```bash
   sudo apt update && sudo apt install -y openjdk-21-jre-headless
   ```

2. **Chạy trực tiếp Spring Boot:**
   ```bash
   PORT=8085 \
   DB_HOST=127.0.0.1 \
   DB_PORT=3306 \
   DB_NAME=room_rental \
   DB_USER=phongtro_user \
   DB_PASSWORD=phongtro_pass123 \
   java -Xms128m -Xmx350m -jar build/libs/service-0.0.1-SNAPSHOT.jar &
   ```

3. **Hoặc cấu hình chạy nền tự khởi động (Systemd):**
   ```bash
   sudo cp systemd/phongtro.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable phongtro
   sudo systemctl start phongtro
   sudo systemctl status phongtro
   ```

---

## 🔍 Kiểm tra dịch vụ sau khi deploy

### 1. Kiểm tra Health Endpoint
```bash
curl http://localhost:8085/api/v1/health
```
**Kết quả mong đợi:**
```json
{"status":"UP","timestamp":"2026-11-06T12:00:00"}
```

### 2. Kiểm tra Đăng nhập Admin
```bash
curl -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@phongtro.com","password":"admin123"}'
```
**Kết quả mong đợi:**
```json
{
  "message": "Đăng nhập thành công",
  "data": {
    "user": {
      "email": "admin@phongtro.com",
      "role": "admin",
      "fullName": "Quản trị viên 1 (Super Admin)"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6..."
  }
}
```

---

## 🌐 Kết nối Frontend (`/src`) với Backend

Trong thư mục gốc dự án Frontend, mở file `.env` (hoặc tạo `.env.production`) và cấu hình trỏ về cổng **`8085`**:
```env
VITE_API_URL=http://103.72.57.148:8085/api/v1
```
Sau đó build Frontend:
```bash
npm run build
```
Mọi API gọi từ React (Auth, Phòng trọ, Hợp đồng, Thanh toán, Admin dashboard...) sẽ tự động kết nối mượt mà tới Spring Boot backend trên cổng 8085!

