# Tổ Ấm - Backend Microservices Architecture (`accommodation-service`)

Hệ thống Backend Microservices cho nền tảng thuê trọ **Tổ Ấm**, thiết kế theo nguyên lý **Domain-Driven Design (DDD)** trên nền **Java 25 LTS & Spring Boot 4.1.2**.

---

## 🚀 Danh Sách Microservices & Cổng Port

| Service | Port | Database Schema | Chức Năng Chính |
|---|---|---|---|
| **`api-gateway`** | `8080` | - | Định tuyến trung tâm, CORS, Rate Limit, gộp OpenAPI `/docs` |
| **`identity-service`** | `8081` | `toam_identity` | Tài khoản, vai trò (RBAC), Đăng ký, Đăng nhập, JWT, Refresh Token, KYC |
| **`listing-service`** | `8082` | `toam_listing` | Phòng trọ, loại phòng, địa giới DVHCVN, tiện nghi, ảnh, đánh giá |
| **`booking-service`** | `8083` | `toam_booking` | Lịch hẹn xem phòng, yêu cầu thuê, hợp đồng điện tử |
| **`billing-service`** | `8084` | `toam_billing` | Chỉ số điện nước (công tơ), hóa đơn hàng tháng, thanh toán |
| **`communication-service`** | `8085` | `toam_communication` | Tin nhắn hội thoại thời gian thực, thông báo in-app |
| **`admin-service`** | `8086` | `toam_admin` | Thống kê bảng điều khiển, kiểm duyệt tin/KYC, báo cáo vi phạm |

---

## 🔑 Tài Khoản Demo (Môi trường Dev)

| Vai trò | Email | Mật khẩu |
|---|---|---|
| **Người thuê (Tenant)** | `tenant@toam.vn` | `password123` |
| **Chủ trọ (Landlord)** | `landlord@toam.vn` | `password123` |
| **Kiểm duyệt viên (Moderator)** | `moderator@toam.vn` | `password123` |
| **Quản trị viên (Admin)** | `admin@toam.vn` | `password123` |

---

## ⚙️ Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Yêu cầu hệ thống
* **Java:** Java 25 LTS (Gradle Toolchain sẽ tự động nhận diện hoặc tải Temurin 25).
* **MySQL:** 9.7 LTS hoặc 8.0+ (Port `3306`).
* **Redis:** 7.x (Port `6379`).

### 2. Biên dịch & Kiểm thử kiến trúc DDD
```bash
# Biên dịch toàn bộ các service
./gradlew clean build -x test

# Chạy kiểm thử tự động (Unit Test & ArchUnit DDD)
./gradlew test
```

### 3. Chạy từng service độc lập
```bash
# Chạy API Gateway
./gradlew :api-gateway:bootRun

# Chạy Identity Service
./gradlew :identity-service:bootRun
```

### 4. Khởi chạy bằng Docker Compose
```bash
docker compose up -d --build
```

---

## 📑 Tài Liệu Kỹ Thuật
* **Quy chuẩn phát triển:** Xem file [`AGENT.md`](./AGENT.md).
* **Tài liệu đặc tả (SRS):** Xem tại [`docs/SRS.md`](./docs/SRS.md).
* **Quyết định kiến trúc (ADR):** Xem tại [`docs/DECISIONS.md`](./docs/DECISIONS.md).
* **Schema cũ tham chiếu:** Xem tại [`docs/legacy/schema.sql`](./docs/legacy/schema.sql).
