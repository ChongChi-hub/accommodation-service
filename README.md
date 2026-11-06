# PhongTro Online - Spring Boot Service (Microservice Architecture)

Hệ thống Backend chuyển đổi từ Node.js (Express) sang **Java Spring Boot 3.3.5 / Java 21 LTS**, thiết kế theo cấu trúc chuẩn Modular Microservice, đóng gói và quản lý phụ thuộc bằng **Gradle**.

---

## 🚀 Công nghệ sử dụng
- **Ngôn ngữ**: Java 21 (Temurin / LTS)
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Gradle 9.8.2 (Wrapper `./gradlew`)
- **Bảo mật & Xác thực**: Spring Security, JJWT 0.12.6 (HMAC-SHA256 Stateless Token)
- **Database**: MySQL 8.0 (`room_rental`)
- **ORM / Data Access**: Spring Data JPA & Spring JDBC Template
- **Validation**: Jakarta Validation API (`@Valid`, Hibernate Validator)
- **Lombok**: Giảm boilerplate code (`@Getter`, `@Setter`, `@Builder`, `@Data`)

---

## 📂 Cấu trúc thư mục chuẩn (`com.service.*`)
```
service/
├── build.gradle
├── gradlew
├── gradlew.bat
├── src/
│   └── main/
│       ├── java/com/service/
│       │   ├── ServiceApplication.java
│       │   ├── common/             # DTO chung (ApiResponse, PaginationResponse), Exception handler
│       │   ├── security/           # JwtUtils, JwtAuthFilter, SecurityConfig (CORS, RBAC)
│       │   ├── config/             # WebMvcConfig (Static file mapping /uploads/**)
│       │   ├── auth/               # Domain 1: Auth & User Management (JWT, KYC, Profiles)
│       │   ├── room/               # Domain 2: Room, Location, Amenity, Review, Bookmark, Upload
│       │   ├── booking/            # Domain 3: Appointment, Rental Request, Contract
│       │   ├── payment/            # Domain 4: Invoice, Payment, Utility Readings
│       │   ├── notification/       # Domain 5: Notification, Realtime Chat (Conversations, Messages)
│       │   └── admin/              # Domain 6: Admin Dashboard Stats, User/Room Moderation, Reports
│       └── resources/
│           └── application.yml     # Cấu hình Datasource, JPA, JWT, Server Port
```

---

## ⚙️ Hướng dẫn cài đặt & Khởi chạy

### 1. Yêu cầu môi trường
- Java 21 LTS (Eclipse Temurin 21)
- MySQL 8.0+ đang chạy cổng `3306`

### 2. Thiết lập biến môi trường JDK 21
```bash
export JAVA_HOME="/Users/trongtri/Library/Java/JavaVirtualMachines/temurin-21.0.11/Contents/Home"
```

### 3. Build dự án
```bash
cd service
./gradlew build -x test
```

### 4. Chạy service
```bash
./gradlew bootRun
```
*Service sẽ lắng nghe trên cổng: `http://localhost:8080`.*

---

## 🐳 Hướng dẫn Deploy lên VPS bằng Docker & Docker Compose

Dự án đã được cấu hình sẵn **Multi-stage Dockerfile** (sử dụng Temurin JRE 21 siêu nhẹ, bảo mật, chạy non-root user) và **`docker-compose.yml`**.

### Các bước triển khai trên VPS:
```bash
# 1. Di chuyển vào thư mục service
cd service

# 2. Khởi chạy toàn bộ hệ thống (MySQL 8.0 + Spring Boot Service)
docker compose up -d --build

# 3. Kiểm tra log khởi động
docker compose logs -f backend

# 4. Kiểm tra sức khỏe hệ thống
curl http://localhost:8080/api/v1/health
```

---

## 📑 Danh mục API Endpoints (Tương thích 100% với Frontend)

### 0. Healthcheck
- `GET /api/v1/health`: Kiểm tra trạng thái hoạt động của server (uptime & version).

### 1. Authentication & Users (`/api/v1/auth`, `/api/v1/users`)
- `POST /api/v1/auth/register`: Đăng ký tài khoản mới.
- `POST /api/v1/auth/login`: Đăng nhập (Email + Password), trả về JWT token + Refresh token.
- `POST /api/v1/auth/google-login`: Đăng nhập bằng Google OAuth Access Token.
- `GET /api/v1/users`: Quản lý danh sách người dùng (Admin).
- `POST /api/v1/auth/forgot-password`: Yêu cầu mã OTP đặt lại mật khẩu.
- `POST /api/v1/auth/verify-reset-otp`: Xác thực mã OTP.
- `POST /api/v1/auth/reset-password`: Đổi mật khẩu qua OTP.
- `GET /api/v1/users/me`: Xem thông tin cá nhân.
- `PUT /api/v1/users/me`: Cập nhật thông tin cá nhân.
- `PUT /api/v1/users/change-password`: Đổi mật khẩu khi đang đăng nhập.
- `POST /api/v1/users/kyc`: Nộp hồ sơ định danh KYC (CCCD 2 mặt).
- `GET /api/v1/users/kyc/pending`: Danh sách hồ sơ KYC chờ duyệt (Admin).
- `PATCH /api/v1/users/kyc/{id}/review`: Phê duyệt hoặc từ chối KYC (Admin).

### 2. Rooms, Locations & Amenities (`/api/v1/rooms`, `/api/v1/locations`, `/api/v1/amenities`, `/api/v1/upload`)
- `GET /api/v1/locations/provinces`: Danh sách Tỉnh / Thành phố.
- `GET /api/v1/locations/districts`: Danh sách Quận / Huyện (hỗ trợ `?provinceId=...`).
- `GET /api/v1/locations/wards`: Danh sách Phường / Xã (hỗ trợ `?districtId=...`).
- `POST /api/v1/locations/seed`: Nạp dữ liệu địa giới hành chính tự động (Admin).
- `GET /api/v1/amenities`: Danh sách tiện ích phòng trọ.
- `GET /api/v1/amenities/room-types`: Danh sách loại hình phòng trọ.
- `GET /api/v1/rooms`: Tìm kiếm và lọc danh sách phòng trọ có phân trang.
- `POST /api/v1/rooms`: Đăng tin phòng trọ mới (Yêu cầu Chủ trọ đã KYC).
- `PUT /api/v1/rooms/{id}`: Cập nhật thông tin phòng trọ (Chủ trọ hoặc Admin).
- `GET /api/v1/rooms/my-rooms`: Danh sách phòng trọ của chính chủ trọ.
- `GET /api/v1/rooms/{id}`: Xem chi tiết phòng trọ.
- `DELETE /api/v1/rooms/{id}`: Xoá tin đăng phòng trọ.
- `PATCH /api/v1/rooms/{id}/status`: Cập nhật trạng thái phòng (`available`, `rented`, `hidden`).
- `POST /api/v1/upload`: Upload hình ảnh (hỗ trợ single & multiple images).

### 3. Bookings, Appointments & Contracts (`/api/v1/appointments`, `/api/v1/rental-requests`, `/api/v1/contracts`)
- `POST /api/v1/appointments`: Đặt lịch hẹn xem phòng trực tiếp.
- `GET /api/v1/appointments`: Danh sách lịch hẹn xem phòng (phân trang).
- `PATCH /api/v1/appointments/{id}/confirm`: Chủ trọ xác nhận lịch hẹn.
- `PATCH /api/v1/appointments/{id}/cancel`: Hủy lịch hẹn xem phòng.
- `POST /api/v1/rental-requests`: Gửi yêu cầu thuê phòng.
- `GET /api/v1/rental-requests`: Danh sách yêu cầu thuê phòng.
- `PATCH /api/v1/rental-requests/{id}/accept`: Chấp nhận yêu cầu và tự động khởi tạo hợp đồng điện tử.
- `PATCH /api/v1/rental-requests/{id}/reject`: Từ chối yêu cầu thuê phòng.
- `PATCH /api/v1/rental-requests/{id}/cancel`: Người thuê hủy yêu cầu.
- `GET /api/v1/contracts`: Danh sách hợp đồng thuê phòng.
- `GET /api/v1/contracts/{id}`: Chi tiết hợp đồng thuê phòng.
- `PATCH /api/v1/contracts/{id}/sign`: Ký kết hợp đồng điện tử.
- `PATCH /api/v1/contracts/{id}/terminate`: Chấm dứt hợp đồng.

### 4. Payments & Invoices (`/api/v1/payments`)
- `POST /api/v1/payments/invoices`: Chủ nhà tạo hóa đơn hàng tháng (tiền phòng + điện nước).
- `GET /api/v1/payments/invoices`: Danh sách hóa đơn hàng tháng.
- `GET /api/v1/payments/invoices/{id}`: Chi tiết hóa đơn.
- `POST /api/v1/payments`: Thanh toán hóa đơn (hỗ trợ nhiều phương thức).
- `GET /api/v1/payments`: Lịch sử thanh toán.
- `POST /api/v1/payments/utility-readings`: Ghi nhận chỉ số điện nước theo kỳ.
- `GET /api/v1/payments/utility-readings`: Danh sách chỉ số điện nước theo hợp đồng.

### 5. Chat & Notifications (`/api/v1/chat`, `/api/v1/notifications`)
- `POST /api/v1/chat/conversations`: Tạo hoặc lấy cuộc trò chuyện giữa Tenant và Landlord.
- `GET /api/v1/chat/conversations`: Danh sách các cuộc trò chuyện gần nhất.
- `GET /api/v1/chat/conversations/{id}/messages`: Danh sách tin nhắn trong cuộc trò chuyện.
- `POST /api/v1/chat/conversations/{id}/messages`: Gửi tin nhắn mới.
- `GET /api/v1/notifications`: Danh sách thông báo in-app kèm số lượng chưa đọc.
- `PATCH /api/v1/notifications/{id}/read`: Đánh dấu thông báo đã đọc.
- `PATCH /api/v1/notifications/read-all`: Đánh dấu tất cả thông báo đã đọc.

### 6. Admin & Reports (`/api/v1/admin`, `/api/v1/reports`)
- `GET /api/v1/admin/stats`: Thống kê Dashboard (tổng người dùng, tổng phòng, doanh thu, hoạt động mới).
- `GET /api/v1/admin/rooms`: Quản lý danh sách toàn bộ phòng trọ.
- `GET /api/v1/admin/users`: Quản lý danh sách người dùng và trạng thái KYC.
- `PATCH /api/v1/admin/users/{id}/role`: Phân quyền người dùng (`tenant`, `landlord`, `admin`).
- `GET /api/v1/admin/users/{id}/kyc`: Xem chi tiết hồ sơ xác minh KYC.
- `GET /api/v1/admin/reports/target/{type}/{id}`: Xem đối tượng bị báo cáo vi phạm.
- `POST /api/v1/reports`: Gửi báo cáo vi phạm (phòng trọ, người dùng, đánh giá).
- `GET /api/v1/reports`: Danh sách báo cáo vi phạm (Admin).
- `PATCH /api/v1/reports/{id}`: Xử lý báo cáo (`resolved`, `dismissed`).
