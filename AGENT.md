# AGENT.md - Quy Chuẩn Kỹ Thuật Backend Microservices (accommodation-service)

## 1. Mục Tiêu Dự Án & Bối Cảnh Nghiệp Vụ
Hệ thống backend "Tổ Ấm" là nền tảng quản lý và tìm kiếm phòng trọ/nhà ở tại Việt Nam, phục vụ người thuê, chủ trọ, kiểm duyệt viên và quản trị viên. Hệ thống được kiến trúc theo mô hình Microservices độc lập kết hợp Domain-Driven Design (DDD), phân tách rõ ràng giữa nghiệp vụ thuần (Domain) và hạ tầng kỹ thuật (Infrastructure). Mỗi service sở hữu một schema MySQL riêng biệt, bảo đảm tính tự chủ, bảo mật cao và sẵn sàng mở rộng quy mô.

## 2. Công Nghệ & Phiên Bản Chuẩn
- **Ngôn ngữ:** Java 25 LTS (Microsoft / Temurin toolchain).
- **Framework:** Spring Boot 4.1.2 (Spring Framework 7, Jakarta EE 11), `spring-boot-starter-webmvc`.
- **API Gateway:** Spring Cloud Gateway 2025.1.4 (Port 8080).
- **Cơ sở dữ liệu:** MySQL 9.7 LTS, charset `utf8mb4`, collation `utf8mb4_unicode_ci`.
- **Migration:** Flyway 13.12.1 (V* cho cấu trúc, seed riêng cho dev/demo).
- **Bảo mật:** Spring Security + JJWT 0.12.7 (HMAC-SHA256 Stateless Token), Argon2id/BCrypt.
- **Tài liệu API:** `springdoc-openapi` 3.2.0 (tự sinh OpenAPI tại `/docs` qua gateway).
- **Mapping & Boilerplate:** MapStruct 1.6.3, Lombok 1.18.48.
- **Kiểm thử & Ràng buộc kiến trúc:** JUnit 5, Testcontainers MySQL 1.21.4, ArchUnit 1.5.1.
- **Lệnh thực thi:**
  - Build toàn bộ: `./gradlew build -x test`
  - Chạy test & kiểm tra luật DDD: `./gradlew test`
  - Chạy service cụ thể: `./gradlew :identity-service:bootRun`
  - Chạy Gateway: `./gradlew :api-gateway:bootRun`

## 3. Bản Đồ Bounded Context & Danh Sách Microservices

| Service | Port | Schema DB | Bounded Context & Trách nhiệm |
|---|---|---|---|
| `api-gateway` | 8080 | - | Reverse proxy, định tuyến, rate limit, CORS, gộp OpenAPI `/docs` |
| `identity-service` | 8081 | `cozynest_identity` | Tài khoản, vai trò (RBAC), đăng ký, đăng nhập, refresh token, KYC, hồ sơ |
| `listing-service` | 8082 | `cozynest_listing` | Phòng trọ, loại phòng, địa giới DVHCVN, tiện nghi, ảnh, đánh giá, yêu thích |
| `booking-service` | 8083 | `cozynest_booking` | Lịch hẹn xem phòng, yêu cầu thuê, hợp đồng điện tử, snapshot phòng |
| `billing-service` | 8084 | `cozynest_billing` | Chỉ số điện nước (đồng hồ), hóa đơn hàng tháng, lịch sử thanh toán |
| `communication-service` | 8085 | `cozynest_communication` | Hội thoại chat trực tiếp, tin nhắn thời gian thực, thông báo in-app |
| `admin-service` | 8086 | `cozynest_admin` | Bảng thống kê dashboard, kiểm duyệt phòng/KYC, báo cáo vi phạm, audit logs |

### Luật Giao Tiếp Liên Service
1. **Tuyệt đối không dùng Foreign Key liên schema:** Tham chiếu giữa các service CHỈ bằng ID (UUID chuỗi 36 ký tự hoặc `BINARY(16)`).
2. **Không chia sẻ Entity JPA giữa các service:** Mỗi service có Domain Model và JPA Entity riêng. Dữ liệu trao đổi qua HTTP REST client bằng Contract DTO.
3. **Domain Event nội bộ:** Dùng `ApplicationEventPublisher`. Chuẩn bị sẵn interface cổng (Port) cho Message Broker trong tương lai.

## 4. Cấu Trúc Thư Mục Chuẩn DDD Phân Tầng Trong MỖI Service
Package gốc: `vn.cozynest.<service>`

```text
vn.cozynest.<service>/
├── domain/                    # TẦNG MIỀN (Thuần Java, KHÔNG import Spring/JPA)
│   ├── model/                 # Aggregate Root, Entity, Value Object (Money, PhoneNumber...)
│   ├── event/                 # Domain Events
│   ├── repository/            # Interfaces Repository (Cổng Port)
│   ├── service/               # Domain Services (Nghiệp vụ phức tạp đa entity)
│   └── exception/             # Domain Exceptions (Lỗi nghiệp vụ)
├── application/               # TẦNG ỨNG DỤNG (Điều phối nghiệp vụ, CQRS nhẹ)
│   ├── command/               # Use cases thay đổi trạng thái (Command & Handler)
│   ├── query/                 # Use cases đọc dữ liệu (Query & Handler)
│   ├── dto/                   # Request/Response DTOs
│   ├── mapper/                # MapStruct mappers
│   └── port/                  # Outbound Ports (gọi service khác, storage, mailer)
├── infrastructure/            # TẦNG HẠ TẦNG (Chi tiết kỹ thuật)
│   ├── persistence/           # JPA Entities, Spring Data Repositories, Database Adapters
│   ├── client/                # Feign / HTTP Interface clients tới service khác
│   ├── security/              # JWT filter adapter, Security configurations
│   └── storage/               # File upload storage adapters (local/S3)
└── interfaces/                # TẦNG GIAO DIỆN (Cổng vào Inbound)
    └── rest/                  # RestControllers, RestDTOs, GlobalExceptionHandler
```

### Luật Phụ Thuộc Bắt Buộc (Ép Tự Động Bằng ArchUnit)
- `interfaces -> application -> domain`
- `infrastructure -> application, domain`
- `domain` TUYỆT ĐỐI KHÔNG import bất kỳ class nào từ `application`, `infrastructure`, `interfaces`, `org.springframework.*`, hoặc `jakarta.persistence.*`.
- Test kiến trúc ArchUnit sẽ fail toàn bộ build nếu có bất kỳ vi phạm nào.

## 5. Quy Ước Thiết Kế Mã Nguồn & Giới Hạn File
- **Kích thước file:** Mỗi file tối đa 200 dòng (giới hạn cứng 250 dòng). Tách nhỏ class, handler, mapper khi vượt quá.
- **Aggregate Root:** Bảo vệ tính bất biến (invariants). KHÔNG dùng public setter; thay đổi trạng thái thông qua phương thức nghiệp vụ có ý nghĩa (ví dụ: `approve()`, `terminateContract()`, `recordUtility()`).
- **Khóa chính:** UUID v7 (`BINARY(16)` trong MySQL, `UUID` trong Java).
- **Tiền tệ:** Luôn dùng số nguyên `BIGINT` (VND) hoặc Value Object `Money`.
- **Thời gian:** Luôn lưu UTC `Instant` hoặc `OffsetDateTime` (`DATETIME(3)` trong DB).
- **Chuẩn API Response:** Mọi endpoint trả về `ApiResponse<T>`:
  ```json
  {
    "message": "Thành công",
    "data": { ... },
    "pagination": { "page": 1, "limit": 20, "total": 100, "totalPages": 5 },
    "errorCode": null,
    "errors": []
  }
  ```
- **Mã lỗi nghiệp vụ:** Bắt đầu bằng `ERR_<DOMAIN>_<ACTION>`, ví dụ: `ERR_AUTH_INVALID_CREDENTIALS`, `ERR_ROOM_NOT_FOUND`.

## 6. Quy Trình Phát Triển Tính Năng Mới
1. **Domain:** Định nghĩa Aggregate Root, Value Object, Repository Interface và Domain Event.
2. **Application:** Viết Command/Query, DTO và Use Case handler.
3. **Infrastructure:** Viết JPA Entity, Spring Data Repository, Database Adapter hiện thực Repository Interface của Domain.
4. **Interfaces:** Viết RestController, Request Validation `@Valid`, gọi Use Case.
5. **Database Migration:** Tạo file `V{timestamp}__{description}.sql` trong `resources/db/migration`.
6. **Kiểm tra trước khi hoàn tất:**
   - Chạy `./gradlew test` (ArchUnit pass + Unit test pass).
   - Kiểm tra endpoint xuất hiện trong OpenAPI docs.

## 7. Các Luật Cấm Tuyệt Đối (Zero-Tolerance Rules)
- KHÔNG dùng emoji trong code, log, message, tài liệu, commit.
- KHÔNG hardcode dữ liệu (toàn bộ dữ liệu hiển thị phải qua database/API).
- KHÔNG hardcode secret, mật khẩu (dùng `.env` và biến môi trường).
- KHÔNG để lộ JPA Entity ra tầng RestController.
- KHÔNG nuốt ngoại lệ (empty catch block). Mọi lỗi phải được log và bọc trong `AppException`.
- KHÔNG sửa các file Flyway migration đã chạy. Nếu cần thay đổi, tạo file migration mới.

## 8. Quy Chuẩn Git
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`.
- Tiếng Anh, ngắn gọn, súc tích (ví dụ: `feat(identity): add refresh token rotation`).
- Không commit file build (`/build/`, `.gradle/`, `.env`).

## 9. Khu Vực Nhạy Cảm & Yêu Cầu An Toàn
- **Xác thực:** Mã hóa mật khẩu bằng BCrypt cost factor 12. Refresh token phải lưu dạng băm (SHA-256) và thu hồi ngay khi phát hiện sử dụng lại token cũ (Replay Attack detection).
- **Dữ liệu định danh KYC (CCCD):** Đường dẫn ảnh lưu trữ an toàn, chỉ cấp quyền truy cập cho chính chủ tài khoản và Admin/Moderator.
- **Chống IDOR:** Luôn đối chiếu `landlordId` / `tenantId` từ JWT với tài nguyên được thao tác trong tầng Application.

