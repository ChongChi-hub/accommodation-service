# Cozynest (Tổ Ấm) - Backend Microservices Architecture (`accommodation-service`)

Hệ thống Backend Microservices cho nền tảng thuê trọ **Cozynest (Tổ Ấm)**, thiết kế theo nguyên lý **Domain-Driven Design (DDD)** và **Hexagonal Architecture (Ports & Adapters)** trên nền **Java 25 LTS & Spring Boot 4.1.2**.

---

## 1. Danh Sách Microservices & Cổng Port

| Service | Port | Database Schema | Công Nghệ & Trách Nhiệm Chính |
|---|---|---|---|
| **`api-gateway`** | `8080` | - | Định tuyến trung tâm, Reactive Gateway, CORS duy nhất, Rate Limiting (Redis), truyền dẫn `X-Request-ID` / `traceparent`, gộp OpenAPI Swagger `/docs`. |
| **`identity-service`** | `8081` | `cozynest_identity` | Tài khoản, xác thực JWT (RS256 với JWKS), Refresh Token Rotation, BCrypt(12) với dynamic dummy hash chống timing attack, Google GIS OAuth2, luồng đăng ký Tenant-first, eKYC CCCD & khuôn mặt (AES-256 GCM). |
| **`listing-service`** | `8082` | `cozynest_listing` | Quản lý phòng trọ, danh mục tiện nghi, bảng phí dịch vụ, vị trí bản đồ, bookmark phòng yêu thích (idempotent), cập nhật trạng thái phòng (Available, Rented, Maintenance). |
| **`booking-service`** | `8083` | `cozynest_booking` | Quản lý lịch hẹn xem phòng ([APPT-01], [APPT-02] phê duyệt/từ chối kèm lý do), yêu cầu thuê phòng, hợp đồng điện tử. |
| **`billing-service`** | `8084` | `cozynest_billing` | Quản lý chỉ số điện nước, chu kỳ hóa đơn, đối soát công nợ và tích hợp mã thanh toán VietQR. |
| **`communication-service`** | `8085` | `cozynest_communication` | Hội thoại tin nhắn giữa Người thuê và Chủ trọ, thông báo in-app, cảnh báo email. |
| **`admin-service`** | `8086` | `cozynest_admin` | Thống kê số liệu sàn, kiểm duyệt phòng trọ & hồ sơ KYC, xử lý khiếu nại tranh chấp, Audit Logs. |

---

## 2. Tài Khoản Thử Nghiệm Mặc Định (Môi Trường Dev)

> Mọi mật khẩu tài khoản demo đều được thiết lập chuẩn: `password123`.

| Vai trò | Email | Mật khẩu | Mô tả quyền hạn |
|---|---|---|---|
| **Người thuê (Tenant)** | `tenant@cozynest.vn` | `password123` | Tìm kiếm phòng, lưu phòng yêu thích, đặt lịch hẹn, gửi yêu cầu thuê, theo dõi hợp đồng và hóa đơn. |
| **Chủ trọ (Landlord)** | `landlord@cozynest.vn` | `password123` | Đăng tin phòng trọ, quản lý phòng, duyệt/hủy lịch hẹn xem phòng ([APPT-02]), tạo hợp đồng. |
| **Kiểm duyệt (Moderator)** | `moderator@cozynest.vn` | `password123` | Kiểm duyệt hồ sơ eKYC (CCCD 2 mặt, sinh trắc khuôn mặt), phê duyệt tin đăng phòng mới. |
| **Quản trị (Admin)** | `admin@cozynest.vn` | `password123` | Quản trị toàn sàn, phân quyền người dùng, xem nhat ký kiểm toán và trạng thái hệ thống. |

---

## 3. Kiến Trúc & Tiêu Chuẩn Bảo Mật

- **Mật khẩu & Chống Dò Quét**: `BCryptPasswordEncoder(12)`, giới hạn độ dài chuẩn $\le 72$ bytes, tự động re-encode khi nâng cấp cost hash (`upgradeEncoding`). So sánh email không tồn tại bằng `dummyBcryptHash` sinh động khi khởi động để triệt tiêu chênh lệch thời gian (độ lệch thực nghiệm $\le 2.56\%$).
- **Cơ chế CORS Tập Trung**: API Gateway là chủ thể duy nhất quản lý CORS (`AllowedOrigins: http://localhost:3000`), downstream microservices vô hiệu hóa CORS để tránh lỗi trùng lặp `Access-Control-Allow-Origin` trên trình duyệt.
- **Xác Thực Khóa Downstream (JWKS)**: Token JWT ký RS256 bởi `identity-service`, công khai qua `/.well-known/jwks.json`. Downstream xác thực theo ma trận 7 kịch bản: hợp lệ (200), hết hạn (401), chữ ký sai (401), alg=none (401), thiếu kid/aud (401), hỗ trợ xoay khóa đa kid.
- **Cô Lập Cơ Sở Dữ Liệu**: Mỗi microservice sở hữu schema riêng và kết nối qua tài khoản MySQL độc lập (`identity_user`, `listing_user`, v.v.), được chứng minh bằng kiểm thử tự động Testcontainers.
- **Chuẩn Hóa Múi Giờ**: Toàn bộ cơ sở dữ liệu và entities sử dụng chuẩn UTC (`serverTimezone=UTC`, Java `Instant`), kiểm thử qua ranh giới nửa đêm (`23:59:59` -> `00:00:00`).

---

## 4. Hướng Dẫn Cài Đặt & Khởi Chạy

### Yêu Cầu Hệ Thống
- **Java**: Java 25 LTS (Gradle Toolchain tự động nhận diện).
- **MySQL**: 9.7 LTS hoặc 8.0+ (Cổng `3306` hoặc Docker `5555`).
- **Redis**: 7.x (Cổng `6379`).
- **Mailpit**: Cổng SMTP `1025`, Cổng Web UI `8025`.

### Khởi Chạy Nhanh Bằng Docker Compose
```bash
# 1. Khởi chạy toàn bộ hạ tầng (MySQL, Redis, Mailpit)
docker compose --profile infra up -d

# 2. Khởi chạy các dịch vụ cốt lõi (Infra + Gateway, Identity, Listing, Booking)
docker compose --profile core up -d

# 3. Kiểm tra trạng thái sức khỏe các service
curl -i http://localhost:8080/actuator/health
```

### Biên Dịch & Chạy Kiểm Thử Tự Động
```bash
# Chạy toàn bộ 282 tests kiểm thử tự động không dùng build cache
./gradlew clean test --no-build-cache

# Chạy riêng từng service
./gradlew :api-gateway:bootRun
./gradlew :identity-service:bootRun
./gradlew :listing-service:bootRun
./gradlew :booking-service:bootRun
```

---

## 5. Tài Liệu Kỹ Thuật Liên Quan
- **Báo cáo Chuyển đổi Microservices**: [`docs/SERVICE-01_MIGRATION.md`](./docs/SERVICE-01_MIGRATION.md)
- **Quy tắc & Quyết định Kiến trúc (ADR)**: [`docs/DECISIONS.md`](./docs/DECISIONS.md)
- **Đặc tả Yêu cầu Nghiệp vụ (SRS)**: [`docs/SRS.md`](./docs/SRS.md)
- **Báo cáo Đối chiếu & Khắc phục Nền tảng (P-CHECK-A)**: [`docs/audit/P-CHECK-A.md`](./docs/audit/P-CHECK-A.md)
- **Báo cáo eKYC Khuôn Mặt (KYC-02)**: [`docs/audit/KYC-02.md`](./docs/audit/KYC-02.md)
- **Báo cáo Lịch Hẹn Xem Phòng (APPT-02)**: [`docs/audit/APPT-02.md`](./docs/audit/APPT-02.md)
