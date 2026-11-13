# Architectural Decisions Record (ADR) - Nền tảng Thuê Trọ "Tổ Ấm"

## 1. Bối cảnh & Mục tiêu
Xây dựng nền tảng thuê phòng trọ "Tổ Ấm" thay thế hệ thống cũ, với kiến trúc Microservice hướng miền (DDD) cho backend và Clean Architecture cho frontend (Next.js).

---

## 2. Quyết định về Phiên bản & Công nghệ (Thực tế & Tương thích)

### 2.1. Backend (`to-am-services`)
* **Java:**
  * *Đề xuất ban đầu:* Java 25 LTS.
  * *Khảo sát môi trường thực tế:* Máy trạm phát triển hiện tại cài đặt `Java 23.0.2` (Oracle JDK 23), Docker hỗ trợ `eclipse-temurin:25-jre-alpine` / `eclipse-temurin:23`.
  * *Quyết định:* Cấu hình Gradle `sourceCompatibility = 23` và `targetCompatibility = 23` để đảm bảo biên dịch cục bộ (local build/test) không gặp lỗi thiếu JDK, đồng thời Docker container dùng Java 23/25.
* **Spring Boot & Spring Framework:**
  * *Phiên bản lựa chọn:* `Spring Boot 4.1.2` (Spring Framework 7, Jakarta EE 11).
  * *Spring Cloud:* `2025.1.4` (bản tương thích chính thức với Spring Boot 4.1.x cho `spring-cloud-gateway`).
  * *Web Starter:* Chuyển sang `org.springframework.boot:spring-boot-starter-webmvc` đúng theo chuẩn Spring Boot 4.
  * *JSON & Jackson:* Jackson 3 (`tools.jackson.core` / `tools.jackson.databind`).
* **Bảo mật & JWT:**
  * Sử dụng `spring-security-oauth2-jose` kết hợp JJWT `0.12.7` / Nimbus JOSE, phân quyền RBAC và token blacklist lưu trữ tại Redis.
* **Database & Migration:**
  * *MySQL Engine (LTS):* Thống nhất **`mysql:8.4` (MySQL 8.4 LTS)** cho toàn bộ `docker-compose.yml` và Testcontainers (`MySQL 8.4 LTS`: Premier Support tới 04/2029, Extended Support tới 04/2032). Loại bỏ tình trạng lẫn `9.0`/`9.1`/`9.7`: `9.0`/`9.1` là Innovation Track hết hỗ trợ nhanh, còn `9.7` đã gỡ tham số `innodb_log_file_size` khiến `org.testcontainers:mysql:1.21.4` lỗi khởi động container.
  * *MySQL Connector:* `com.mysql:mysql-connector-j:9.7.0`.
  * *Flyway:* `org.flywaydb:flyway-core:13.12.1` & `flyway-mysql:13.12.1`.
* **Testing & Architecture Rules:**
  * *ArchUnit:* `com.tngtech.archunit:archunit-junit5:1.5.1` để ép buộc quy tắc DDD: tầng `domain` không phụ thuộc vào `application`, `infrastructure`, hay `interfaces`.
  * *Testcontainers:* `org.testcontainers:mysql:1.21.4`.
* **OpenAPI Documentation:**
  * `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0` (tương thích Spring Boot 4).

### 2.2. Frontend (`to-am-web`)
* **Next.js & React:**
  * `next: 16.2.12` (Next.js 16.2 App Router).
  * `react: 19.3.1`, `react-dom: 19.3.1`.
* **Styling & Design System:**
  * `tailwindcss: 4.3.5` (sử dụng `@theme` CSS-first).
  * `lucide-react: 1.70.0` (tuyệt đối không sử dụng emoji trong UI và code).
  * Cài đặt và tích hợp `taste-skill` (`design-taste-frontend` 2.5.0) đảm bảo giao diện độc bản, tinh tế, tỷ lệ chuẩn, responsive 1440/1024/768/390.
* **State & Data Fetching:**
  * Server State: `@tanstack/react-query: 5.107.1`.
  * Client/UI State: `zustand: 5.1.0`.
  * Form & Schema Validation: `react-hook-form: 7.91.0` + `@hookform/resolvers: 7.0.3` + `zod: 4.6.7`.
  * Đa ngôn ngữ: `next-intl: 4.14.9` (mặc định tiếng Việt `vi`, hỗ trợ tiếng Anh `en`).
* **Kiến trúc & Kiểm soát phụ thuộc:**
  * Clean Architecture: `presentation -> application -> domain`; `infrastructure -> application, domain`.
  * Ép luật phụ thuộc bằng `eslint-plugin-boundaries: 7.4.2`.

---

## 3. Quyết định về Ranh giới Bounded Context (Microservices)

1. **`api-gateway` (Port 8080):** Reverse proxy, CORS, JWT validation filter, rate limiter, OpenAPI gateway aggregator.
2. **`identity-service` (Port 8081):** Quản lý người dùng, đăng ký, đăng nhập, refresh token rotation, KYC (CCCD), hồ sơ.
3. **`listing-service` (Port 8082):** Danh mục địa giới hành chính (DVHCVN), phòng trọ, loại phòng, tiện nghi, nội quy, ảnh, đánh giá, phòng yêu thích.
4. **`booking-service` (Port 8083):** Lịch hẹn xem phòng, yêu cầu thuê phòng, hợp đồng điện tử và snapshot dữ liệu phòng.
5. **`billing-service` (Port 8084):** Chỉ số điện nước (đồng hồ), hóa đơn định kỳ, lịch sử giao dịch và thanh toán.
6. **`communication-service` (Port 8085):** Hội thoại chat trực tiếp, tin nhắn văn bản/ảnh, thông báo in-app.
7. **`admin-service` (Port 8086):** Bảng điều khiển quản trị, kiểm duyệt bài đăng/KYC, báo cáo vi phạm, nhật ký kiểm toán (audit logs).

---

## 4. Quyết định về Phạm vi Giai đoạn 1

* **Chạy thật 100%:** Luồng đăng ký, đăng nhập, refresh token, thông tin cá nhân `/me`, đổi mật khẩu; frontend có middleware bảo vệ route và phân luồng đúng theo Role (Tenant, Landlord, Moderator, Admin).
* **Dữ liệu hiển thị (Read-only APIs):** Toàn bộ các trang công khai và dashboard đều hiển thị dữ liệu thật được seed từ backend qua Flyway (không hardcode dữ liệu trong frontend).
* **Thao tác ghi khác:** Được thiết kế đầy đủ UI (modal, validation, preview) và hiển thị toast thông báo qua i18n ("Tính năng sẽ ra mắt").


---

## 5. Quyết định về Tách Biệt 2 Repository Độc Lập & Docker Compose

* **Bối cảnh:** Hai repository `accommodation-service` (Backend Microservices) và `accommodation-web` (Next.js Frontend) hoạt động độc lập, mỗi repo có Git remote, CI, Dockerfile riêng biệt.
* **Quyết định Docker Compose:**
  * Tại `accommodation-service`, file `docker-compose.yml` mặc định khởi chạy toàn bộ hạ tầng Backend (MySQL 8.4 LTS, Redis, Gateway port 8080 và các microservice).
  * Đồng thời bổ sung service `web` với Docker Compose profile `full` trỏ tới `../accommodation-web`.
  * Khi phát triển độc lập: Backend chạy `./gradlew bootRun` hoặc `docker compose up` (cổng 8080/8081); Frontend chạy `npm run dev` (cổng 3000) gọi API qua `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`.
  * Khi cần khởi chạy trọn gói cả hệ thống bằng một lệnh: `docker compose --profile full up --build`.

---

## 6. Quyết Định Thiết Kế LANDLORD-04 & LANDLORD-02

### 6.1. Xử lý ảnh WebP và Bảo mật File Upload (Java 23/25)
* **Thư viện**: Tích hợp `com.twelvemonkeys.imageio:imageio-webp:3.12.0` và `net.coobird:thumbnailator:0.4.20` vào `listing-service/build.gradle`.
* **Quy trình xử lý an toàn**:
  1. Kiểm tra kích thước file (< 5MB) và tổng số lượng ảnh của phòng (<= 10 ảnh).
  2. Kiểm tra định dạng bằng Magic Bytes và `ImageIO.read()` (chỉ chấp nhận JPEG, PNG, WebP).
  3. Giới hạn độ phân giải pixel tối đa 8000x8000px để chống tấn công Decompression Bomb.
  4. Đọc lại pixel và vẽ sang canvas mới để triệt tiêu hoàn toàn mã độc Polyglot và siêu dữ liệu EXIF/GPS.
  5. Xuất bản 3 kích cỡ WebP chuẩn hóa: Thumb (320px), Medium (960px), Large (1600px).
* **Lưu trữ**: Port `ImageStoragePort`, adapter `FileSystemImageStorageAdapter` lưu file vào thư mục ngoài web (`./storage/media/{key}`), phục vụ qua endpoint `/api/v1/media/{key}` kèm header `Cache-Control: public, max-age=31536000, immutable` và `X-Content-Type-Options: nosniff`.

### 6.2. Ràng buộc duy nhất 1 ảnh bìa ở Database
* MySQL không hỗ trợ partial index `WHERE is_cover = TRUE`. Do đó, sử dụng giải pháp Generated Stored Column:
  `cover_room_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_cover = 1 THEN room_id ELSE NULL END) STORED`
  kèm `UNIQUE KEY uq_room_images_cover_per_room (cover_room_id)`.
* Khi `is_cover = 0`, giá trị là `NULL` (MySQL cho phép nhiều NULL trong UNIQUE index). Khi `is_cover = 1`, giá trị là `room_id`, do đó chỉ duy nhất 1 ảnh của mỗi phòng được phép có `is_cover = 1`.

### 6.3. Bảo vệ phòng RENTED & Toàn vẹn dữ liệu
* Xóa phòng là Xóa mềm (`deleted_at = NOW()`).
* Tạo interface `RoomUsagePort`. Adapter tạm thời kiểm tra trạng thái phòng (`status == 'RENTED'`), nếu đang cho thuê thì chặn xóa với lỗi nghiệp vụ `ROOM_CURRENTLY_RENTED` (HTTP 400).
* Ẩn/Hiện phòng: Tin bị kiểm duyệt/Admin ẩn (`hidden_by IN ('MODERATOR', 'ADMIN')`) thì chủ trọ không có quyền tự hiện lại, phải liên hệ hỗ trợ hoặc sửa theo yêu cầu.

### 6.4. Chiến lược quản lý ảnh ở Frontend
* **Màn hình Đăng mới**: Quản lý ảnh cục bộ qua Object URLs (nhớ revoke khi unmount). Khi nhấn "Đăng tin", hệ thống tạo phòng -> upload file multipart -> cập nhật ảnh bìa và thứ tự -> gửi duyệt.
* **Màn hình Chỉnh sửa**: Tương tác API trực tiếp cho các hành động đặt bìa (`PATCH .../cover`), xóa (`DELETE .../images/{id}`), và đổi thứ tự (`PUT .../images/order`) để đảm bảo tính nguyên tử và phản hồi tức thì.


## 7. Quyết Định Về Cập Nhật Địa Giới Hành Chính Theo Mô Hình 2 Cấp (Từ 01/07/2025)

* **Căn cứ pháp lý & Nguồn dữ liệu:**
  * Nghị quyết số 202/2025/QH15 của Quốc hội về việc sắp xếp đơn vị hành chính cấp tỉnh và chuyển đổi sang mô hình hành chính 2 cấp (Tỉnh/Thành phố trực thuộc Trung ương -> Xã/Phường, bãi bỏ cấp quận/huyện từ 01/07/2025).
  * Danh mục chuẩn hóa địa giới hành chính của Tổng cục Thống kê (GSO), tra cứu và đối soát ngày 07/11/2026.
* **Cấu trúc 34 Đơn vị Hành chính Cấp Tỉnh (28 Tỉnh, 6 Thành phố trực thuộc Trung ương):**
  * **6 Thành phố trực thuộc Trung ương:**
    1. Thành phố Hà Nội (`HN`)
    2. Thành phố Hồ Chí Minh (`HCM`)
    3. Thành phố Hải Phòng (`HP`)
    4. Thành phố Đà Nẵng (`DNG`)
    5. Thành phố Cần Thơ (`CT`)
    6. Thành phố Huế (`HUE`) (chính thức trực thuộc Trung ương)
  * **28 Tỉnh:**
    An Giang (`AG`), Bắc Giang (`BG`), Bắc Kạn (`BK`), Bạc Liêu (`BL`), Bắc Ninh (`BN`), Bến Tre (`BT`), Bình Định (`BD`), Bình Dương (`BDG`), Bình Phước (`BP`), Bình Thuận (`BTH`), Cà Mau (`CM`), Cao Bằng (`CB`), Đắk Lắk (`DL`), Đắk Nông (`DKN`), Điện Biên (`DB`), Đồng Nai (`DNA`), Đồng Tháp (`DT`), Gia Lai (`GL`), Hà Giang (`HG`), Hà Nam (`HNA`), Hà Tĩnh (`HT`), Hải Dương (`HD`), Hậu Giang (`HGI`), Hòa Bình (`HB`), Hưng Yên (`HY`), Khánh Hòa (`KH`), Kiên Giang (`KG`), Kon Tum (`KT`).
* **Chiến lược Schema & Dữ liệu:**
  * Cấp chính quy chuẩn: `provinces` (cấp tỉnh) và `wards` (cấp xã/phường).
  * Bổ sung cột `province_code` vào bảng `wards` và index `idx_ward_province_code`. Cột `district_code` chuyển thành nullable (dữ liệu legacy tùy chọn).
  * Bổ sung cột `ward_id` vào bảng `rooms` (FK tới `wards.id`). Cột `rooms.district_code` chuyển thành nullable.
  * Seed đầy đủ xã/phường cho 6 thành phố trực thuộc Trung ương trọng điểm (Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ, Huế); 28 tỉnh còn lại được quản lý ở cấp tỉnh và mở rộng xã/phường theo lộ trình.
  * Toàn bộ 78 phòng demo trong seed được cập nhật địa chỉ chuẩn hóa theo định dạng 2 cấp: `"Số nhà, Tên đường, Phường/Xã, Tỉnh/Thành phố"`, gán đúng `ward_id` và giữ nguyên tọa độ định vị GPS chính xác.
* **Giao diện Frontend:**
  * Loại bỏ dropdown Quận/Huyện trên bộ lọc tìm kiếm và form đăng tin. Chuyển thành quy trình chọn 2 bước: `Tỉnh/Thành phố` -> `Phường/Xã`.
  * Chuẩn hóa định dạng chuỗi địa chỉ hiển thị trên toàn bộ UI và email thông báo.

---

## 8. Cơ chế Xác thực Phiên, BFF HttpOnly Cookie Proxy & Xoay vòng Khóa RSA (Session Cookies & Key Rotation)

### 8.1 Định danh và Bản chất Cookie `cozynest_token` (BFF HttpOnly Proxy)
* **Loại Token:** JWT ký mật mã bằng thuật toán bất đối xứng **RS256** (RSA Signature with SHA-256).
* **Payload quy chuẩn:**
  * `sub`: UUID v7 của người dùng (khóa chính `users.id` / `users.user_uuid`).
  * `email`: Địa chỉ email xác thực.
  * `roles`: Danh sách vai trò (`ROLE_TENANT`, `ROLE_LANDLORD`, `ROLE_MODERATOR`, `ROLE_ADMIN`).
  * `iss`: `cozynest-identity` (được thẩm định bởi Resource Server).
  * `exp`: Thời hạn sống của Access Token là **15 phút** (900 giây — xác nhận thực tế tại `identity-service` `application.yml` `jwt.expiration-ms: 900000` và `JwtTokenProvider.java`, đồng bộ với Quy tắc R7).
  * `kid`: Key ID tương ứng trong JWKS Keyset để phục vụ xác thực và xoay vòng khóa.
* **Thời hạn sống của Refresh Token:** Token ngẫu nhiên bảo mật cao được lưu mã hóa SHA-256 trong bảng `refresh_tokens` có hạn **30 ngày** (2,592,000 giây) kèm cơ chế phát hiện tái sử dụng (Reuse Detection Revocation).

### 8.2 Cơ chế Làm mới Token qua BFF (`HttpOnly` Cookie Proxy)
* **Không lưu token ở JS trình duyệt:** Mã nguồn `accommodation-web` tuyệt đối không trả `accessToken` / `refreshToken` trong payload JSON về trình duyệt và không lưu `cozynest_refresh_token` ở `localStorage`. Các Next.js BFF Route Handlers (`/api/auth/*` và `/api/v1/[...path]/route.ts`) nắm giữ và xoay vòng token thông qua cookie `HttpOnly`.
* **Luồng làm mới tự động:** Khi Access Token hết hạn (HTTP 401), BFF hoặc client gọi `/api/auth/refresh` (trên server đọc cookie `HttpOnly` `cozynest_refresh_token` và gọi `/api/v1/auth/refresh-token`), cập nhật cookie `HttpOnly` mới với cờ `Path=/`, `SameSite=Lax` và `Secure` ở production.
* **Xử lý tranh chấp (Race Condition):** Cơ chế khóa chống đua (single-flight refresh lock / mutex) ghép các request đồng thời khi access token hết hạn để tránh kích hoạt thu hồi do tái sử dụng refresh token.

### 8.3 Chính sách An toàn Cookie & CSRF ở BFF
* **Cờ `HttpOnly`:** Bắt buộc trên mọi cookie xác thực; JavaScript trình duyệt (`document.cookie`) không thể đọc được JWT hoặc refresh token.
* **Cờ `Secure`:** Tự động kích hoạt khi `process.env.NODE_ENV === 'production'`, bắt buộc cookie chỉ truyền qua HTTPS.
* **Cờ `SameSite=Lax` + Kiểm tra `Origin`:** BFF proxy kiểm tra header `Origin`/`Referer` hợp lệ cho các request ghi dữ liệu (`POST`/`PUT`/`PATCH`/`DELETE`) nhằm chống CSRF.
* **Đường dẫn `Path=/`:** Phạm vi hợp lệ trên toàn bộ ứng dụng web và tuyến BFF `/api/*`.

### 8.4 Nguồn Khóa Riêng RSA & Quản lý Bí mật
* **Tuyệt đối không commit:** Khóa riêng tư RSA (`JWT_PRIVATE_KEY` / `private_key.pem`) không bao giờ được lưu trong kho mã nguồn git hay Docker image.
* **Nạp từ biến môi trường/Secret Manager:** Khóa riêng được nạp qua biến môi trường `JWT_PRIVATE_KEY` (hoặc Kubernetes Secret / AWS Secrets Manager).
* **Hướng dẫn tạo cặp khóa RSA 2048-bit cho môi trường phát triển (Dev/Demo):**
  ```bash
  # Sinh khóa riêng RSA 2048-bit PKCS#8
  openssl genpkey -algorithm RSA -out rsa_private.pem -pkeyopt rsa_keygen_bits:2048
  # Trích xuất khóa công khai tương ứng chuẩn X.509
  openssl rsa -in rsa_private.pem -pubout -out rsa_public.pem
  ```

### 8.5 JWKS Caching và Cơ chế Xoay vòng Khóa (Key Rotation by `kid`)
* Identity service công khai bộ khóa công khai tại endpoint tiêu chuẩn `/.well-known/jwks.json`.
* Mỗi khóa trong JWKS chứa `kid` duy nhất (ví dụ `cozynest-key-2026-v1`).
* Các Resource Server (`listing-service`, `billing-service`, `booking-service`, `admin-service`) cache JWKS trong bộ nhớ với thời gian sống TTL 24 giờ. Khi gặp token mang `kid` mới chưa có trong cache, Resource Server tự động fetch lại JWKS để cập nhật khóa mới mà không cần khởi động lại dịch vụ (Zero-downtime key rotation).

---

## 9. Chiến lược Định danh Thực thể: Rà soát UUID v7 và BIGINT, Trade-offs & Lộ trình Di chuyển (`ADR-007`)

> **Mã định danh:** `ADR-007`  
> **Quyết định chuẩn hóa (`HARDEN-01`):** Duy trì Dual-ID (`BIGINT` nội bộ + `UUID v7` công khai) cho các bảng lõi di sản (`rooms`, `users`); **tất cả các bảng giao dịch mới (Sprint 3+: `ledger_*`, `*_outbox`, `escrow_holds`, `contracts`, `contract_amendments`, `refund_requests`, `payout_requests`) dùng UUID v7** (`CHAR(36)`/`VARCHAR(36)` hoặc `BINARY(16)`), ánh xạ `UUID`/`String` UUID v7 trên API.

### 9.1 Hiện trạng Rà soát Schema (`rooms`, `room_images`, `users`)
* **Bảng `rooms`:**
  * Khóa chính nội bộ (PK): `id BIGINT AUTO_INCREMENT`.
  * Khóa định danh ngoại vi (Public UUID): `room_uuid VARCHAR(36) NOT NULL UNIQUE` (lưu trữ chuỗi UUID v7).
* **Bảng `room_images`:**
  * Khóa chính nội bộ (PK): `id BIGINT AUTO_INCREMENT`.
  * Khóa ngoại quan hệ: `room_id BIGINT` (tham chiếu tới `rooms.id`).
  * Khóa lưu trữ vật lý: `storage_key`, `thumb_key`, `medium_key`, `large_key` (được đặt tên có chứa UUID ngẫu nhiên tránh trùng lặp).
* **Bảng `users`:**
  * Khóa chính nội bộ (PK): `id BIGINT AUTO_INCREMENT`.
  * Khóa định danh ngoại vi (Public UUID): `user_uuid VARCHAR(36) NOT NULL UNIQUE`.
  * Dữ liệu hạt giống dev/demo: Còn tồn tại bí danh chuỗi thân thiện con người (`u-landlord-01`, `u-tenant-01`, `u-admin-01`) dùng cho test fixture.

### 9.2 Lý do Áp dụng Mô hình Khóa Kép (Dual-ID Pattern) & Trade-offs
1. **Hiệu năng B-tree Clustered Index của InnoDB (MySQL):**
   * *Ưu điểm của BIGINT PK:* InnoDB lưu trữ bảng theo chỉ mục phân cụm (Clustered Index B+Tree). Mọi chỉ mục phụ (Secondary Index) đều phải lưu trữ giá trị của PK làm con trỏ hàng. Khóa chính `BIGINT` (8 bytes) giúp kích thước node chỉ mục nhỏ gọn hơn 50-75% so với chuỗi UUID (36 bytes), giảm phân mảnh trang (page split) và tối ưu bộ nhớ đệm InnoDB Buffer Pool.
   * *Nhược điểm của BIGINT đơn lẻ:* Dễ bị quét ID (ID Enumeration Attack), làm lộ tổng số lượng khách hàng hoặc số lượng phòng của nền tảng.
2. **Bảo mật và Phân tán với UUID v7:**
   * *Ưu điểm của UUID v7:* Có chứa 48-bit timestamp ở phần đầu (Time-ordered), giúp việc chèn mới không bị nhảy ngẫu nhiên khắp cây B-tree như UUID v4, giảm thiểu phân tán IOPS. Không thể đoán trước ID của người dùng hay bài đăng khác.
   * *Trade-off:* Tốn 36 bytes dạng chuỗi (hoặc 16 bytes dạng `BINARY(16)`), chi phí join giữa các bảng đa dịch vụ cao hơn so với số nguyên 64-bit.
3. **Đánh giá quyết định:** Mô hình kết hợp (`BIGINT AUTO_INCREMENT` làm PK nội bộ + `UUID v7` làm Public Resource ID ở URL và API) là giải pháp cân bằng tối ưu nhất cho hệ thống vừa đạt thông lượng xử lý cao, vừa bảo vệ tính riêng tư.

### 9.3 Lộ trình Di chuyển Chuẩn hóa (Migration Roadmap)
* **Giai đoạn 1 (Hiện tại):**
  * Duy trì Dual-ID pattern trên toàn bộ các service.
  * API công khai ưu tiên nhận dạng qua `room_uuid` và `user_uuid`. Các API nội bộ giữa các microservice có thể dùng surrogate `BIGINT` ID để join tốc độ cao.
  * Các bí danh tiền tố demo như `u-landlord-01` được cô lập hoàn toàn trong hồ sơ test fixture/dev seed, không bao giờ xuất hiện ở môi trường production.
* **Giai đoạn 2 (Tối ưu hóa Storage & Memory):**
  * Chuyển đổi định dạng lưu trữ cột `room_uuid` và `user_uuid` từ `VARCHAR(36)` sang `BINARY(16)`.
  * Áp dụng hàm tích hợp sẵn của MySQL 8+: `UUID_TO_BIN(uuid_str, 1)` (cờ 1 đổi vị trí byte thời gian lên đầu) và `BIN_TO_UUID(bin_uuid, 1)` khi truy vấn.
* **Giai đoạn 3 (Pure Distributed Identity):**
  * Khi mở rộng sang kiến trúc sharding đa trung tâm dữ liệu (multi-region database sharding), chuyển đổi khóa chính sang UUID v7 thuần túy (`BINARY(16) PRIMARY KEY`) với bộ sinh UUID v7 tập trung hoặc phân tán ở tầng Application (Java 25).


---

## 10. Kiến trúc Quản lý Lịch hẹn Xem phòng ([APPT-02])

> Ngày quyết định: 07/11/2026  
> Áp dụng cho: `booking-service` và `accommodation-web`

### 10.1 Máy trạng thái (State Machine)
Lịch hẹn xem phòng (`ViewingAppointment`) tuân thủ nghiêm ngặt máy trạng thái trong domain model (không dùng setter công khai):
* `PENDING` -> `CONFIRMED`: Chủ trọ chấp thuận lịch hẹn. Hệ thống kiểm tra trùng giờ; nếu trùng giờ với lịch đã `CONFIRMED` khác của cùng chủ trọ, trả về `ERR_APPT_TIME_CONFLICT` (HTTP 409). Chủ trọ có thể gửi kèm cờ `force: true` để chấp thuận đè.
* `PENDING` -> `DECLINED`: Chủ trọ từ chối lịch hẹn, bắt buộc có lý do (`reasonCode`).
* `CONFIRMED` -> `CANCELLED`: Hủy lịch hẹn đã xác nhận, bắt buộc có lý do (`reasonCode`).
* `CONFIRMED` -> `COMPLETED`: Đánh dấu đã gặp khách thành công. Chỉ cho phép sau thời điểm `scheduled_start`.
* `CONFIRMED` -> `NO_SHOW`: Đánh dấu khách không đến xem phòng. Chỉ cho phép sau thời điểm `scheduled_start`.
* `PENDING` -> `EXPIRED`: Hệ thống định kỳ quét và đánh dấu hết hạn đối với các lịch chờ chưa được xử lý khi đã qua giờ hẹn.
* Mọi chuyển đổi trạng thái trái phép trả về lỗi nghiệp vụ ổn định `ERR_APPT_INVALID_TRANSITION` (HTTP 409).

### 10.2 Xử lý Trùng giờ (Time Conflict Policy)
* Khi chủ trọ xác nhận lịch, `booking-service` quét các lịch `CONFIRMED` của cùng `landlord_id` trong khung giờ giao nhau `[scheduled_start, scheduled_end)`.
* Nếu phát hiện trùng, trả về mã lỗi `ERR_APPT_TIME_CONFLICT` kèm danh sách lịch trùng.
* Nếu client gửi cờ `force: true`, hệ thống cho phép xác nhận và ghi vết vào `appointment_status_history` với lý do "Xác nhận đè lịch trùng".

### 10.3 Transactional Outbox Pattern & Event Publishing
* Khi trạng thái thay đổi (`CONFIRMED`, `DECLINED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`, `EXPIRED`), một domain event tương ứng được ghi vào bảng `appointment_events_outbox` trong cùng giao dịch database (ACID transaction).
* *TODO*: `communication-service` hiện chưa có API gửi SMS/Email thời gian thực; khi `communication-service` hoàn thiện, outbox worker hoặc Debezium CDC sẽ gửi event lên message broker (Kafka/RabbitMQ) để bắn thông báo cho khách thuê và chủ trọ.

### 10.4 Bảo vệ Quyền riêng tư Số điện thoại (Phone Privacy Protection)
* Bảo mật dữ liệu người thuê: Thuộc tính `contactPhone` chỉ được trả về đầy đủ khi lịch hẹn ở trạng thái `CONFIRMED` hoặc `COMPLETED`.
* Với các trạng thái `PENDING`, `DECLINED`, `CANCELLED`, `EXPIRED`: số điện thoại được che một phần tại tầng Application/DTO (ví dụ `090••••567`). Không bao giờ gửi số điện thoại thô qua mạng khi chưa xác nhận.
* Nghiêm cấm ghi số điện thoại khách hàng vào log hệ thống.


---

## 11. Kiến trúc Hồ sơ Người dùng (USER-01) & Xác minh Danh tính Chủ trọ (KYC-01)

> Ngày quyết định: 07/11/2026  
> Áp dụng cho: `identity-service`, `listing-service`, `common`, `api-gateway`, và `accommodation-web`

### 11.1 Thư viện Xử lý Ảnh Dùng chung (Shared Image Processing)
* **Vấn đề**: Cả `listing-service` (ảnh phòng) và `identity-service` (avatar người dùng, ảnh CCCD KYC) đều cần kiểm tra an ninh hình ảnh nghiêm ngặt (magic bytes, chống file polyglot, chặn bom pixel $\le 8000$px, tẩy EXIF/GPS, nén sang WebP).
* **Quyết định**: Di chuyển và đưa các thư viện `com.twelvemonkeys.imageio:imageio-webp:3.12.0` và `net.coobird:thumbnailator:0.4.20` vào module `common`.
* Đóng gói tiện ích xử lý ảnh dùng chung trong `vn.cozynest.common.image.ImageProcessingHelper`:
  * Xác thực magic bytes: JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`), WebP (Byte 0-3 `RIFF`, byte 8-11 `WEBP`).
  * Tẩy sạch EXIF, dữ liệu định vị GPS và mã độc bằng cách vẽ lại hoàn toàn sang một `BufferedImage` kiểu `TYPE_INT_RGB` mới.
  * Cắt ảnh vuông từ tâm (center square crop) tự động cho avatar người dùng.
  * Xuất ảnh định dạng WebP tối ưu với các kích cỡ chuẩn: Avatar (64px, 256px), KYC CCCD ($\le 2000$px cạnh dài).

### 11.2 Bảo mật Dữ liệu Nhạy cảm KYC & Mã hóa Lưu trữ
* **Số Căn cước công dân (CCCD)**:
  * `id_number_enc`: Mã hóa đối xứng AES-256-GCM với vector khởi tạo ngẫu nhiên (IV 12 bytes). Khóa mã hóa được cấu hình qua biến môi trường `APP_KYC_SECRET_KEY` (có khóa mặc định 256-bit cho môi trường dev/demo).
  * `id_number_hash`: Băm HMAC-SHA256 sử dụng khóa pepper bí mật từ `APP_KYC_PEPPER`. Thiết lập ràng buộc UNIQUE đối với các hồ sơ có trạng thái `PENDING` hoặc `APPROVED`, đảm bảo một số CCCD chỉ có thể liên kết với một tài khoản duy nhất mà không lưu plaintext trong database.
  * `id_number_last4`: Lưu trữ 4 chữ số cuối để hiển thị dạng che (`••••••••1234`) trên giao diện người dùng và drawer đối soát.
* **Kho lưu trữ Ảnh Riêng tư (Private Storage)**:
  * Ảnh CCCD mặt trước và mặt sau được lưu vào thư mục riêng tư độc lập (Private Storage), tuyệt đối KHÔNG nằm trong thư mục `/media` công khai hoặc thư mục static web.
  * Xem ảnh CCCD yêu cầu xác thực và phân quyền nghiêm ngặt qua endpoint `/api/v1/landlord/kyc/images/{type}` (chính chủ) hoặc `/api/v1/moderation/kyc/{id}/images/{type}` (ADMIN/MODERATOR).
  * Luôn gửi kèm header HTTP: `Cache-Control: no-store, private`, `X-Content-Type-Options: nosniff`. Không phát hành link ký sẵn (presigned URL) dài hạn.
* **Nhật ký Truy cập Bất biến (`kyc_access_logs`)**:
  * Mỗi lượt kiểm duyệt xem ảnh mặt trước (`VIEW_FRONT`), mặt sau (`VIEW_BACK`), xem chi tiết (`VIEW_DETAIL`), phê duyệt (`APPROVE`) hay từ chối (`REJECT`) đều được ghi vào bảng nhật ký `kyc_access_logs` (append-only) kèm `ip`, `actor_id`, `actor_role`.
* **Quy định Không Ghi Log Dữ liệu Nhạy cảm**:
  * Nghiêm cấm tuyệt đối in số CCCD, đường dẫn ảnh lưu trữ nội bộ vào log hệ thống, thông báo ngoại lệ (exception messages) hoặc phản hồi lỗi API.

### 11.3 Chính sách Lưu giữ và Hủy Dữ liệu KYC Bị Từ chối (Retention & Cleanup Policy)
* **Thời hạn lưu trữ và Cấp quyết định**:
  * Thời hạn lưu trữ hiện dùng là **30 ngày** (cấu hình qua `app.kyc.retention-days=30`).
  * Thời hạn này do **Bộ phận Pháp chế & Tuân thủ (Legal & Compliance) phối hợp với Cán bộ Bảo vệ Dữ liệu Cá nhân (Data Protection Officer - DPO)** quyết định, căn cứ theo **Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 và Nghị định 356/2025/NĐ-CP** cùng quy định khiếu nại dân sự của Việt Nam (cho phép người dùng có 30 ngày để khiếu nại quyết định từ chối trước khi dữ liệu nhạy cảm bị tiêu hủy hoàn toàn).
* **Cơ chế An toàn của Scheduled Job (`KycImageCleanupScheduler`)**:
  * Cờ kích hoạt `app.kyc.cleanup-enabled`: **MẶC ĐỊNH LÀ FALSE (TẮT)**. Chỉ khi quản trị viên chủ động bật qua biến môi trường hoặc cấu hình hệ thống, tác vụ mới chạy.
  * **Tuyệt đối KHÔNG xóa hồ sơ PENDING hoặc APPROVED**: Truy vấn cơ sở dữ liệu (`findRejectedBefore(cutoff)`) được lọc chặt chẽ chỉ lấy các bản ghi có trạng thái `REJECTED` và thời điểm bị từ chối cũ hơn 30 ngày. Các hồ sơ đang chờ duyệt (`PENDING`) và hồ sơ đã xác thực thành công (`APPROVED`) không bao giờ bị scheduler chạm tới hay xóa ảnh.
  * Khi quét và xóa file vật lý trên đĩa, đường dẫn ảnh được cập nhật thành giá trị đại diện `PURGED` để đánh dấu hồ sơ đã được thanh tẩy dữ liệu.

### 11.4 Cổng Chặn Đăng Tin (Listing KYC Gate) & Inter-Service Token
* **Quy định**: Chủ trọ chỉ được phép gửi duyệt phòng (`POST /api/v1/landlord/rooms/{id}/submit`) và xuất bản phòng khi KYC ở trạng thái `APPROVED`. Nếu chưa được duyệt, hệ thống từ chối với HTTP 403 và mã lỗi `ERR_KYC_REQUIRED` kèm trạng thái KYC hiện tại (`NONE`, `PENDING`, `REJECTED`). Thao tác lưu nháp (`DRAFT`) vẫn được cho phép.
* **Cơ chế Xác minh Liên Dịch vụ (Inter-Service KYC Verification)**:
  * `identity-service` cung cấp endpoint nội bộ `GET /internal/users/{id}/kyc-status`, không đi qua gateway công khai.
  * Endpoint được bảo vệ bằng Service Token / Secret nội bộ truyền qua header `X-Internal-Service-Key`.
  * `listing-service` gọi qua HTTP Client với thời gian chờ timeout 3 giây, bộ nhớ đệm ngắn $\le 60$ giây.
  * Áp dụng chính sách **FAIL-CLOSED**: Nếu không kết nối được `identity-service` hoặc phản hồi lỗi/hết thời gian, hệ thống tự động từ chối yêu cầu gửi duyệt để bảo vệ an toàn nền tảng.
* **Chính sách Phòng Đã Đăng Khi KYC Bị Thu hồi**:
  * Các phòng đã được `PUBLISHED` trước đó sẽ không bị tự động ẩn để tránh gián đoạn hợp đồng và người thuê đang tìm kiếm, tuy nhiên mọi hành động đăng mới, gửi duyệt lại hoặc mở lại phòng ẩn đều bị chặn cho đến khi KYC được phê duyệt lại.

### 11.5 Đổi Mật khẩu & Thu hồi Phiên Làm việc
* Đổi mật khẩu yêu cầu nhập đúng mật khẩu hiện tại. Mật khẩu mới bắt buộc phải khác mật khẩu cũ và đáp ứng độ phức tạp tối thiểu.
* Áp dụng Rate Limiting qua Redis: Nếu nhập sai mật khẩu cũ 5 lần liên tiếp trong 15 phút, tài khoản bị tạm khóa đổi mật khẩu để chống brute-force.
* Khi đổi mật khẩu thành công: Hệ thống tự động thu hồi tất cả các `refresh_tokens` của các thiết bị khác, chỉ duy trì phiên đăng nhập hiện tại để người dùng không bị văng ra ngoài đột ngột. Đồng thời gửi email cảnh báo bảo mật tới người dùng.


## 12. Quyết định Kiến trúc & Bảo mật Xác thực [AUTH-01, AUTH-03, AUTH-04]

### 12.1 Thuật toán Băm Mật khẩu & Chống Dò Tài khoản (Timing Attack)
* **Thuật toán thật đang sử dụng**: BCrypt với hệ số phức tạp 12 (`BCryptPasswordEncoder(12)`).
* **Lý do & Bảo đảm tiêu chuẩn**: BCrypt hệ số 12 tạo độ trễ tính toán ~80-100ms cho mỗi lần băm/so sánh trên CPU hiện đại, đáp ứng đầy đủ khuyến nghị của OWASP Password Storage Cheat Sheet.
* **Chống Timing Attack (Timing Enumeration)**: Khi người dùng đăng nhập với email không tồn tại trong hệ thống, `LoginUseCase` vẫn kích hoạt việc so sánh mật khẩu với hằng số `DUMMY_BCRYPT_HASH` (`$2a$12$7kPj5aXN62q.7XJjN8wGxeQJv9XW1Nq2g5U1rQ0M5J4L3K2H1G0F.`). Việc này đảm bảo thời gian phản hồi giữa trường hợp "email không tồn tại" và "mật khẩu không đúng" là hoàn toàn xấp xỉ nhau (~80-100ms), ngăn chặn kẻ tấn công đo thời gian để dò email hợp lệ.

### 12.2 Chính sách Khóa Tài khoản Chống DoS (Lockout Policy)
* **Quyết định**: Khóa theo cặp `(email, IP)` thay vì khóa toàn cầu theo `email`.
* **Lý do**: Nếu khóa toàn cục theo email, bất kỳ kẻ tấn công nào biết trước email nạn nhân đều có thể gửi 5 request sai mật khẩu để làm tê liệt tài khoản của nạn nhân (Denial-of-Service). Bằng cách đếm số lần sai theo `(email, ipAddress)`, kẻ tấn công chỉ bị khóa trên chính IP của họ trong 15 phút, trong khi người dùng thực từ địa chỉ IP hợp lệ vẫn có thể đăng nhập bình thường.

### 12.3 Đánh đổi Khi Đăng ký Email Trùng
* **Quyết định**: Khi đăng ký bằng email đã tồn tại, trả về `400 Bad Request` kèm mã lỗi `ERR_AUTH_EMAIL_EXISTS` ("Email đã tồn tại trên hệ thống").
* **Đánh đổi**: Thay vì gửi email ẩn thông báo trùng (gây chậm trễ và khó khăn lớn cho người dùng thực muốn đăng ký nhanh), hệ thống chấp nhận thông báo lỗi trực tiếp để tối ưu trải nghiệm người dùng (UX). Rủi ro brute-force quét email được triệt tiêu thông qua cơ chế Redis Rate Limiting nghiêm ngặt (tối đa 10 lần đăng ký / 1 phút / IP, quá ngưỡng trả về HTTP 429).


## 13. Quyết Định Di Chuyển Node.js Sang Microservices [SERVICE-01]

### 13.1 Phân Chia Microservices & Phạm Vi Nghiệp Vụ (DDD Bounded Contexts)
* Hệ thống phân tách thành 6 microservices độc lập nghiệp vụ và 1 API Gateway:
  1. `api-gateway` (Port 8080): Điểm đón tiếp duy nhất, quản lý CORS, chặn tuyệt đối `/internal/**`.
  2. `identity-service` (Port 8081): Xác thực, người dùng, phân quyền RBAC, hồ sơ KYC CCCD và Sinh trắc học khuôn mặt.
  3. `listing-service` (Port 8082): Tin đăng phòng trọ, danh mục tiện ích, tìm kiếm, lưu phòng (Bookmarks), đánh giá.
  4. `booking-service` (Port 8083): Lịch hẹn xem phòng, yêu cầu thuê, hợp đồng điện tử.
  5. `billing-service` (Port 8084): Chỉ số điện nước, hóa đơn, thanh toán.
  6. `communication-service` (Port 8085): Trò chuyện thời gian thực, thông báo in-app.
  7. `admin-service` (Port 8086): Kiểm duyệt, báo cáo vi phạm, thống kê hệ thống.

### 13.2 Cô Lập Cơ Sở Dữ Liệu Tuyệt Đối (Database Isolation & Zero Cross-Schema FK)
* Mỗi microservice sở hữu CSDL schema riêng biệt trên MySQL 8.4 LTS (`cozynest_identity`, `cozynest_listing`, `cozynest_booking`, `cozynest_billing`, `cozynest_communication`, `cozynest_admin`).
* Cấm hoàn toàn Foreign Key liên schema (xác thực qua truy vấn `information_schema.KEY_COLUMN_USAGE` trả về 0 liên kết).
* Giao tiếp liên dịch vụ thực hiện qua REST API nội bộ hoặc Outbox Pattern, không chia sẻ database connection.

### 13.3 Bỏ Có Chủ Đích Địa Giới Cấp Quận/Huyện
* Áp dụng quy chuẩn địa giới hành chính 2 cấp (Tỉnh/Thành -> Phường/Xã).
* Loại bỏ route `GET /api/v1/location/districts` và bảng `districts` trong nghiệp vụ. Phường/Xã liên kết trực tiếp với mã Tỉnh/Thành qua trường `province_code`.

---

## 14. Quyết Định Kiến Trúc & Bảo Mật Đăng Nhập/Đăng Ký Bằng Google [AUTH-02]

### 14.1 Xác Thực Qua Google Identity Services (GIS) Không Cần Client Secret
* **Mô hình**: Sử dụng ID Token do Google Identity Services SDK phát hành trực tiếp trên trình duyệt, gửi về Backend (`POST /api/v1/auth/google`) để xác thực chữ ký số bằng thuật toán RS256 thông qua Google Public JWKS (`https://www.googleapis.com/oauth2/v3/certs`) với thư viện chuẩn `NimbusJwtDecoder` trong Spring Security OAuth2.
* **Bảo mật Secret**: Tuyệt đối không cấu hình `GOOGLE_CLIENT_SECRET` trên Backend lẫn Frontend. GIS ID token flow là luồng công khai chuẩn được Google khuyến nghị cho Web App, loại bỏ hoàn toàn nguy cơ rò rỉ Client Secret.
* **Chống Replay Attack bằng Nonce một lần**: Mỗi phiên đăng nhập bắt buộc phải lấy một giá trị ngẫu nhiên cryptographically secure qua `GET /api/v1/auth/google/nonce`. Nonce được lưu trong Redis với TTL 5 phút và cơ chế tiêu thụ 1 lần duy nhất (single-use consumption). Nếu nonce đã dùng hoặc không khớp giá trị trong ID token, backend từ chối với `400 Bad Request`.

### 14.2 Chính Sách Khóa Mật Khẩu Đối Với Google OAuth
* **Quy tắc**: Việc tài khoản bị khóa tạm thời 15 phút do nhập sai mật khẩu cục bộ 5 lần liên tiếp (chống brute-force) KHÔNG làm khóa việc đăng nhập bằng Google.
* **Lý do**: Đăng nhập bằng Google là xác thực qua nhà cung cấp danh tính bên thứ ba (Google Identity Provider) đã được xác minh bảo mật nhiều lớp (2FA/OTP của Google). Việc kẻ xấu cố tình gõ sai mật khẩu cục bộ không được phép làm tê liệt quyền truy cập qua kênh xác thực an toàn của người dùng thực.
* **Ngoại lệ duy nhất**: Nếu tài khoản bị Admin gắn cờ đình chỉ vi phạm chính sách (`status = SUSPENDED`) hoặc xóa mềm (`DELETED`), hệ thống sẽ chặn tất cả mọi hình thức đăng nhập kể cả Google OAuth với mã lỗi `403 Forbidden`.

### 14.3 Khả Năng Thiết Lập Mật Khẩu Cục Bộ Cho Tài Khoản Tạo Mới Bằng Google
* **Quy tắc**: Người dùng đăng ký ban đầu hoàn toàn qua Google (`password_hash = NULL`, `phone = NULL`) được phép sử dụng luồng Quên mật khẩu (`/forgot-password` -> OTP qua email) để thiết lập mật khẩu đăng nhập cục bộ bất cứ lúc nào.
* **Lý do**: Tối ưu tính linh hoạt và quyền tự chủ của người dùng khi muốn đăng nhập bằng phương thức email + mật khẩu truyền thống mà không bị phụ thuộc duy nhất vào tài khoản Google.

### 14.4 Kiểm Tra Lệch Client ID Trong Dev Mode Không Lộ Secret
* **Quy tắc**: Endpoint `GET /api/v1/auth/google/status` trả về cờ `enabled: boolean` và `clientIdFingerprint: string` (8 ký tự đầu của chuỗi SHA-256 hash của Client ID).
* **Lý do**: Cho phép Frontend phát hiện ngay lập tức tình trạng cấu hình lệch giữa `.env.local` của Frontend và cấu hình backend trên môi trường dev/staging mà tuyệt đối không để lộ toàn bộ Client ID qua API public.

### 14.5 Bảng user_identities và Quản Lý Liên Kết Danh Tính Đa Phương Thức
* **Mô hình**: Tách biệt danh tính liên kết sang bảng `user_identities` (provider, provider_subject, email_at_link, email_verified, display_name, picture_url, locale, linked_at, last_login_at) với ràng buộc `UNIQUE(provider, provider_subject)` và `UNIQUE(user_id, provider)`. Bảng `users` chỉ giữ `password_hash` (nullable) và suy ra `hasPassword`.
* **Kiểm toán**: Mọi hành vi nhạy cảm (`GOOGLE_LINKED`, `GOOGLE_UNLINKED`, `GOOGLE_LOGIN`, `ACCOUNT_AUTO_LINKED`) đều được ghi nhận vào bảng `identity_audit_logs` kèm actor, IP và User-Agent (tuyệt đối không ghi token hay PII).

### 14.6 Chính Sách Tự Động Liên Kết Tài Khoản (Auto-Linking) & Chống Chiếm Đoạt
* **Email đã xác minh**: Nếu email trùng với tài khoản đã xác minh email (`email_verified_at != NULL`), hệ thống tự động liên kết tài khoản Google mới với tài khoản hiện có, không tạo user rác.
* **Email chưa xác minh (Anti-Account Takeover)**: Nếu email trùng với tài khoản chưa xác minh email (có thể do kẻ tấn công tạo trước bằng email của nạn nhân), khi chủ sở hữu thực sự đăng nhập bằng Google (đã được Google xác minh email), hệ thống:
  1. Thu hồi toàn bộ Refresh Token của phiên cũ.
  2. Xóa `password_hash` của tài khoản cũ.
  3. Đánh dấu `email_verified_at = NOW()`.
  4. Liên kết Google Identity và đăng nhập cho người dùng thật. Muốn dùng lại mật khẩu, người dùng bắt buộc đi qua luồng Quên mật khẩu qua email chính chủ.

### 14.7 Hạn Chế Tài Khoản Nhân Sự Quản Trị (ADMIN, MODERATOR)
* **Quy tắc**: Tài khoản nhân sự (`ADMIN`, `MODERATOR`) mặc định KHÔNG được phép liên kết hoặc đăng nhập qua Google OAuth (trả về mã lỗi 403 `ERR_GOOGLE_NOT_ALLOWED_FOR_STAFF`).
* **Cấu hình**: Có thể bật lại bằng cờ `auth.google.allow-staff=true` (mặc định là `false` để đảm bảo an ninh cấp cao).

### 14.8 Bảo Vệ Phương Thức Đăng Nhập Cuối Cùng (Last Login Method Protection)
* **Quy tắc**: Người dùng không thể hủy liên kết Google (`DELETE /api/v1/me/identities/google`) nếu đây là phương thức đăng nhập duy nhất (chưa có mật khẩu và không còn identity nào khác). Hệ thống trả về `409 Conflict` với mã `ERR_AUTH_LAST_LOGIN_METHOD`.
* **Giao diện**: Nút hủy liên kết bị vô hiệu hóa kèm thông báo hướng dẫn người dùng "Đặt mật khẩu" trước.

### 14.9 Chống Tấn Công SSRF Khi Xử Lý Avatar Google
* **Quy tắc**: Tuyệt đối KHÔNG tải file ảnh từ `picture_url` của Google về server để lưu cục bộ (loại bỏ hoàn toàn nguy cơ Server-Side Request Forgery - SSRF).
* **Hiển thị**: Frontend Next.js sử dụng `next/image` với cấu hình `images.remotePatterns` trỏ tới `lh3.googleusercontent.com` và fallback sang chữ cái đầu nếu lỗi.

### 14.10 Ứng Xử Khi Thiếu Cấu Hình Ở Môi Trường Production
* **Quy tắc**: Nếu thiếu `GOOGLE_CLIENT_ID` hoặc hệ thống bảo mật nonce Redis không khả dụng trên môi trường Production, hệ thống fail-fast hoặc tự động tắt cờ `enabled: false`. Frontend phát hiện trạng thái này sẽ ẩn hoàn toàn nút đăng nhập Google để tránh gây lỗi cho người dùng.




---

## 15. Quyết định Nghiệp vụ và Luồng Xác thực Phê duyệt Chủ Trọ (AUTH-FLOW-A: Quy tắc R1 - R9)

### 15.1. Bối cảnh
Trước đây, hệ thống cấp vai trò `LANDLORD` ngay lập tức khi đăng ký nếu người dùng chọn vai trò "Chủ cho thuê", dẫn đến việc người dùng chưa qua xác minh danh tính (KYC) vẫn có thể truy cập dashboard quản lý phòng và đăng tin. Đồng thời luồng Google Auth bị nhầm lẫn điều hướng giữa người thuê và chủ trọ.

### 15.2. Các quy tắc cốt lõi (R1 - R9)
1. **R1 (Quyền mặc định an toàn):** Mọi tài khoản tự đăng ký (qua email/mật khẩu hoặc Google) chỉ nhận vai trò `TENANT` và có thể hoạt động như người thuê phòng ngay lập tức.
2. **R2 (Không cấp LANDLORD tức thì):** Lựa chọn "Chủ cho thuê" khi đăng ký không cấp vai trò `LANDLORD`. Thay vào đó, hệ thống tạo bản ghi hồ sơ chủ trọ trong bảng `landlord_profiles` với trạng thái `AWAITING_KYC`. Chỉ khi hồ sơ KYC tổng thể được nhân sự có quyền `kyc:review` (`ADMIN`, `MODERATOR`) phê duyệt, hệ thống mới cấp thêm vai trò `LANDLORD` (giữ nguyên vai trò `TENANT`) trong CÙNG một giao dịch cơ sở dữ liệu, cập nhật `landlord_profiles.approval_status = APPROVED`, và phát sự kiện `LandlordApproved` qua outbox.
3. **R3 (Máy trạng thái hồ sơ thuần Domain):**
   - Vòng đời: `AWAITING_KYC` -> `PENDING_REVIEW` (khi nộp KYC) -> `APPROVED` | `REJECTED`.
   - Nộp lại: `REJECTED` -> `PENDING_REVIEW` (khi nộp bổ sung giấy tờ hợp lệ).
   - Mọi chuyển đổi trạng thái không hợp lệ khác bị từ chối bằng ngoại lệ Domain `ERR_LANDLORD_INVALID_STATE_TRANSITION` (HTTP 409 Conflict).
4. **R4 (Nguồn sự thật duy nhất - Single Source of Truth):**
   - Trạng thái phê duyệt chủ trọ được lưu tại `landlord_profiles.approval_status`. Bảng `user_verifications` (KYC) đóng vai trò là bằng chứng pháp lý và hồ sơ đính kèm.
   - Việc phê duyệt KYC đồng nghĩa với việc phê duyệt tư cách chủ trọ vì theo quy định pháp luật và chính sách Tổ Ấm, chủ nhà bắt buộc phải xác thực CCCD chính chủ trước khi kinh doanh lưu trú.
   - Tính nhất quán được đảm bảo tuyệt đối bằng việc cập nhật cả 2 bảng trong cùng một `@Transactional` và cơ chế khóa lạc quan (`@Version` trên `landlord_profiles` và `user_verifications`).
5. **R5 (Đăng ký sau cho Người thuê hiện tại):**
   - Người dùng đã có tài khoản `TENANT` có thể nộp đơn xin làm chủ trọ qua `POST /api/v1/me/landlord-application` (yêu cầu email đã xác minh, hành động idempotent) và tra cứu tiến độ qua `GET /api/v1/me/landlord-application`.
6. **R6 (Phân quyền và Ngoại lệ có chủ đích):**
   - Mọi endpoint `/api/v1/landlord/**` thuộc các nghiệp vụ quản lý phòng, đặt lịch, tài chính đòi hỏi vai trò `LANDLORD` trong JWT. Nếu người dùng chưa có vai trò này, hệ thống trả về mã lỗi 403 `ERR_LANDLORD_NOT_APPROVED` để Frontend tự động điều hướng sang trang KYC onboarding.
   - Ngoại lệ: Endpoint nộp và theo dõi KYC của chính mình (`/api/v1/landlord/kyc`, `/api/v1/landlord/kyc/face`, `/api/v1/me/**`) mở quyền cho cả `TENANT` và `LANDLORD`.
7. **R7 (Cập nhật quyền qua Refresh Token):**
   - JWT access token là bất biến trong thời gian sống (15 phút). Sau khi hồ sơ được duyệt, khi người dùng gọi `POST /api/v1/auth/refresh-token`, hệ thống tải lại vai trò tươi mới nhất từ bảng `user_roles` trong DB và cấp token mới chứa đầy đủ `["TENANT", "LANDLORD"]`.
8. **R8 (Tài khoản nhân sự và Chống tự duyệt):**
   - Tài khoản `ADMIN`, `MODERATOR` chỉ mang vai trò nhân sự, không có `TENANT`.
   - Kiểm duyệt viên bị cấm tuyệt đối việc tự duyệt hoặc tự từ chối hồ sơ của chính mình (`ERR_KYC_SELF_REVIEW_FORBIDDEN`, HTTP 403/409).
9. **R9 (Toàn vẹn dữ liệu mẫu - Seed Integrity):**
   - Tất cả chủ trọ sở hữu phòng trong seed data (`u-landlord-01` đến `u-landlord-06`) đều ở trạng thái `APPROVED` và sở hữu đồng thời cả 2 vai trò `TENANT` và `LANDLORD`.

---

## 16. Quyết định Kiến trúc và Bảo vệ Dữ liệu Sinh trắc học (KYC-02: Xác thực khuôn mặt)

### 16.1. Không tự xây dựng thuật toán nhận diện khuôn mặt
* **Quyết định:** Nền tảng tuyệt đối **không tự viết thuật toán so khớp sinh trắc học** hay lưu trữ vector embedding / đặc trưng khuôn mặt của người dùng.
* **Lý do:**
  1. Bảo vệ dữ liệu cá nhân nhạy cảm theo **Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 và Nghị định 356/2025/NĐ-CP**. Việc lưu trữ embedding sinh trắc học đòi hỏi hạ tầng đạt chuẩn an toàn thông tin cấp độ cao và tiềm ẩn rủi ro lộ lọt nghiêm trọng.
  2. Quyết định phê duyệt tính khớp giữa khuôn mặt và CCCD do **Kiểm duyệt viên con người** đối chiếu (`ManualReviewProvider`, mặc định) hoặc cổng cắm nhà cung cấp eKYC được cấp phép (`VendorFaceVerificationProvider`).

### 16.2. Vai trò của Bộ phát hiện khuôn mặt trên Trình duyệt
* **Quy tắc:** Bộ quét webcam và khung elip hướng dẫn trên trình duyệt **chỉ đóng vai trò hỗ trợ người dùng thực hiện đúng động tác**, cấm tuyệt đối mô tả nó là biện pháp bảo mật hay "chống giả mạo".
* **Lý do:** Client-side JavaScript có thể bị can thiệp bởi DevTools, camera ảo hoặc extensions. Do đó cờ `liveness_client_flag` chỉ là dữ liệu bổ trợ để kiểm duyệt viên tham khảo.

### 16.3. Điều kiện Phê duyệt KYC Tổng thể và Cổng Đăng tin
* **Quy tắc:** Khi biến môi trường `FACE_REQUIRED=true` (mặc định), trạng thái KYC tổng thể `APPROVED` đòi hỏi đồng thời:
  1. Giấy tờ tùy thân CCCD đã được duyệt (`user_verifications.status = APPROVED`).
  2. Khuôn mặt sinh trắc học đã đạt (`face_verifications.status = PASSED`).
* Cổng kiểm tra đăng tin `listing-service` thông qua endpoint nội bộ `/internal/users/{id}/kyc-status` bắt buộc `isApproved=true` mới cho phép đăng tin phòng trọ.

### 16.4. Chính sách Lưu trữ An toàn và Tự động Tiêu hủy (Data Retention & Purge)
* **Mã hóa:** Tất cả các khung hình chụp (`neutral`, `action_1`, `action_2`) đều được mã hóa bằng thuật toán `AES-GCM 256-bit` với khóa riêng `FACE_ENCRYPTION_KEY` trước khi lưu vào kho lưu trữ riêng tư.
* **Thời hạn lưu trữ:** Khung hình của hồ sơ đã có kết quả (`PASSED` hoặc `FAILED`) chỉ được lưu trữ tối đa `FACE_RETENTION_DAYS=30` ngày. Hết hạn, tác vụ định kỳ `FaceCleanupScheduler` sẽ xóa vĩnh viễn tệp ảnh mã hóa khỏi ổ đĩa và cập nhật `deleted_at` trong cơ sở dữ liệu.
* **Quyền rút đồng ý:** Người dùng có quyền xóa ngay lập tức mọi khung hình đã lưu thông qua nút "Xóa dữ liệu ảnh sinh trắc học" hoặc gọi API `DELETE /api/v1/me/kyc/face/consent`.
* **Nhật ký kiểm toán vĩnh viễn:** Bảng `face_access_logs` (append-only) ghi lại từng lượt truy cập, stream khung hình hoặc quyết định của kiểm duyệt viên (actorId, role, frameId, timestamp, IP, User-Agent) và không bao giờ bị xóa.



---

## 17. Quyết định Pháp lý & Mô hình Dòng tiền (LEGAL-02 / ADR-008)

> **Mã định danh:** `ADR-008`  
> **Chủ đề:** Mô hình Kiến trúc Dòng tiền, Giữ cọc (Escrow) & Trách nhiệm Thuế  
> **Trạng thái:** `HUMAN` — **Đề xuất kỹ thuật, CHỜ luật sư và chuyên gia thuế thẩm định, CHƯA phải kết luận pháp lý chính thức**  
> **Ngày ghi nhận:** 09/11/2026 (cập nhật 12/11/2026)  
> **Tài liệu tham chiếu:** `docs/legal/LEGAL-02-PAYMENT-TAX-LEGAL-ASSESSMENT.md`

### 17.1. Bối cảnh & Căn cứ Pháp lý Cần Tham vấn (`LEGAL-02`)
1. Tổ Ấm vận hành sàn thương mại điện tử kết nối cho thuê nhà ở. Các giao dịch có giá trị lớn bao gồm: Tiền đặt cọc hợp đồng, Tiền thuê phòng hàng tháng, Tiền dịch vụ tiện ích, Phí gói VIP và Phí dịch vụ sàn.
2. Theo **Nghị định 52/2024/NĐ-CP** (hiệu lực 01/07/2024), việc nhận tiền của khách hàng vào tài khoản của doanh nghiệp, lưu giữ và chi trả theo lệnh có thể cấu thành Dịch vụ trung gian thanh toán (thu hộ, chi hộ, ví điện tử) cần Giấy phép NHNN nếu nền tảng trực tiếp giữ tiền.
3. Về nghĩa vụ thuế đối với cá nhân kinh doanh/cho thuê qua sàn TMĐT: cần luật sư và chuyên gia thuế đối chiếu đồng thời **Nghị định 117/2025/NĐ-CP**, **Luật Quản lý thuế số 56/2024/QH15** và **Thông tư 40/2021/TT-BTC** (lưu ý: nội dung miễn/khấu trừ thuế nêu trong bản nháp trước là giả thuyết tham chiếu, KHÔNG phải kết luận pháp lý miễn trừ thuế cho sàn hay chủ trọ).

### 17.2. Định hướng Kiến trúc Kỹ thuật Đề xuất (Gated Sandbox)
1. **KHÔNG BẬT MÔ HÌNH TỰ GIỮ CỌC TIỀN THẬT (MÔ HÌNH D):**  
   Không dùng tài khoản Công ty Tổ Ấm để nhận và giữ tiền cọc thật của người dùng; toàn bộ module lưu ký (`Escrow`, ví nội bộ, rút tiền thật) phải bị khóa sau feature flag `money.custody.enabled=false` và `fintech.mode=SANDBOX_ONLY`.
2. **ĐỀ XUẤT MÔ HÌNH CHUYỂN KHOẢN TRỰC TIẾP P2P (MÔ HÌNH A) LÀM MẶC ĐỊNH AN TOÀN:**  
   - Người thuê chuyển khoản tiền cọc/thuê trực tiếp tới STK ngân hàng chính chủ đã xác minh của Chủ trọ qua mã VietQR động hoặc cổng đối tác có phép.
   - Hệ thống sinh mã tham chiếu chuẩn `TOAM-REF-xxxxxx` để đối soát trạng thái chứng từ mà không nắm giữ dòng tiền thuê.
   - Phần mã nguồn escrow/wallet/payout/refund đã xây trong Sprint 3 được giữ nguyên để thử nghiệm sandbox dưới cờ `money.custody.enabled=false` (`SANDBOX_ONLY`).
3. **ĐỊNH HƯỚNG MÔ HÌNH B (ĐỐI TÁC TRUNG GIAN THANH TOÁN CÓ GIẤY PHÉP NHNN):**  
   Chỉ kích hoạt giải pháp Escrow tiền thật sau khi ký hợp đồng hợp tác với Tổ chức Trung gian thanh toán/Ngân hàng có giấy phép NHNN và hoàn tất toàn bộ câu hỏi `LEGAL-02`.

---

## 18. Quyết định Bằng chứng Pháp lý Hợp đồng Điện tử & Ký số (LEGAL-02 / ADR-010)

> **Mã định danh:** `ADR-010`  
> **Chủ đề:** Bằng chứng Hợp đồng Điện tử, Tính toàn vẹn Dữ liệu & Ký xác thực OTP  
> **Trạng thái:** `HUMAN` — Đề xuất tiêu chuẩn kỹ thuật, chờ luật sư duyệt mẫu hợp đồng và giá trị chứng cứ theo `LEGAL-02`  
> **Ngày ghi nhận:** 09/11/2026  
> **Tài liệu tham chiếu:** `docs/legal/LEGAL-02-PAYMENT-TAX-LEGAL-ASSESSMENT.md`

### 18.1. Căn cứ Pháp lý Tham chiếu
1. **Luật Nhà ở 2023 (Điều 163, 164):** Hợp đồng thuê nhà ở lập thành văn bản; cần luật sư xác nhận các trường hợp đặc thù về công chứng/chứng thực nếu có.
2. **Luật Giao dịch điện tử 2023 (Điều 9, 12, 22):** Thông điệp dữ liệu và phương thức xác thực điện tử (OTP qua SMS/Email) phải được luật sư thẩm định mức độ đáp ứng chữ ký điện tử trong quy trình cụ thể của Tổ Ấm.

### 18.2. Chuẩn hóa Kỹ thuật Lưu vết Chứng cứ (Technical Evidentiary Standard)
1. **Tạo Snapshot Bất biến:** Tại thời điểm các bên xác nhận ký, hệ thống kết xuất toàn văn nội dung hợp đồng và lưu trữ dưới dạng bản chụp bất biến (Immutable Snapshot).
2. **Mã băm SHA-256:** Mỗi bản chụp hợp đồng được băm bằng thuật toán `SHA-256` trên toàn bộ nội dung văn bản kèm thông tin phiên bản để chứng minh văn bản không bị can thiệp sau khi ký.
3. **Nhật ký Ký số (Append-only Signature Audit Trail):** Bảng `contract_signatures` bắt buộc lưu trữ:
   - `contract_id`, `version`, `document_hash` (SHA-256).
   - `signer_id`, `signer_role` (`TENANT` / `LANDLORD`).
   - `auth_method` (`OTP_SMS` / `OTP_EMAIL`).
   - `ip_address`, `user_agent`, `signed_at` (UTC timestamp).
   - Tuyệt đối không lưu mã OTP thô (chỉ lưu mã băm OTP hoặc trạng thái xác thực).
4. **Định danh Chủ thể trước khi ký:** Cả Người thuê và Chủ trọ đều phải hoàn thành xác thực danh tính (eKYC CCCD) và số điện thoại trước khi được phép kích hoạt luồng ký hợp đồng điện tử.

---

## 19. Quy chuẩn Kỹ thuật & Kiến trúc Hệ thống Sprint 3 (HARDEN-01, SAGA-01, LEDGER-02)

### 19.1. Chuẩn hóa Khóa chính và Định dạng Tiền tệ (HARDEN-01 / ADR-007)
* **Khóa chính:** Toàn bộ bảng giao dịch mới sử dụng UUID v7 (`VARCHAR(36)` hoặc `BINARY(16)`), đại diện bằng `UUID` trong Java và chuỗi UUID trên API; các bảng di sản duy trì Dual-ID (`BIGINT` nội bộ + `UUID v7` công khai) theo ADR-007.
* **Tiền tệ:** Toàn bộ số tiền đều dùng kiểu số nguyên `BIGINT` (đơn vị Việt Nam Đồng - VND). Tuyệt đối không dùng `FLOAT` hay `DOUBLE` để tránh sai số làm tròn.
* **Thời gian:** Mọi mốc thời gian lưu trữ ở dạng UTC `DATETIME(3)` trong DB và `Instant` trong Java.

### 19.2. Hạch toán Kế toán Kép (LEDGER-02)
* Mọi giao dịch nội bộ được hạch toán qua Sổ Cái Kép (Double-entry Ledger Engine) với ràng buộc bảo toàn số dư nghiêm ngặt (`Sum(DEBIT) == Sum(CREDIT)`).

### 19.3. Giao dịch Xuyên Service (SAGA-01) & Transactional Outbox (BROKER-01)
* Vòng đời đặt thuê khép kín: `Rental Request` -> `Holding Deposit` -> `Electronic Contract` -> `Room RENTED`.
* Áp dụng Transactional Outbox Pattern kết hợp Saga Orchestrator đảm bảo tính nhất quán cuối cùng (Eventual Consistency) và các bước bù trừ tự động khi gặp sự cố mạng hoặc hết hạn giao dịch.

---

## 20. Sprint 3 Fintech Architecture, Messaging Topology (`ADR-009`) & Legal Gates (2026-11-12)

### 20.1. Hardened Fintech Core (`LEDGER-01/02`, `PAY-01/02`, `ESCROW-01`, `FIN-RISK-01`, `REFUND-01`)
* **Double-Entry Engine (`billing-service`):**
  * Wallet locking follows deterministic ordered `SELECT ... FOR UPDATE` across participating accounts before persisting immutable `ledger_entries` + `ledger_lines`.
  * MySQL 8.4 LTS triggers (`V6__sprint3_billing_audit_hardening.sql`) block `UPDATE` and `DELETE` on posted `ledger_entries` and `ledger_lines`; enforced in MySQL 8.4 LTS Testcontainers (`LedgerImmutabilityTestcontainersTest`) and Docker (`--log-bin-trust-function-creators=1`).
  * Admin ledger exports (`/api/v1/billing/ledger/export/*` & `/api/v1/billing/dashboard/export`) enforce bounded date ranges (`<= 366` days), bounded rows (`<= 5000`), UTF-8 BOM, CSV formula neutralization (`=`, `+`, `-`, `@`), and immutable access audits.
* **Payment Gateway, Escrow & Risk Guards:**
  * Webhook intake verifies `X-Webhook-Signature` HMAC-SHA256 (`401` on invalid/missing signature), trusted IP whitelist (`403`), and replay timestamp window; client simulation is separated behind authenticated sandbox endpoint `POST /api/v1/billing/payments/sandbox/simulate-webhook`.
  * `EscrowController` gates release behind two-party move-in confirmation and freezes disbursement while an open dispute exists (`money.custody.enabled=false` default per ADR-008 Model A).
  * `RefundService` verifies live OTP via `identity-service`, matches account holder against verified KYC name, enforces `cumulativeRefund <= settledPaidAmount`, and routes failed bank payouts to `FAILED` retry queue.

### 20.2. RabbitMQ Outbox/DLQ (`ADR-009`) & Cross-Service Booking Saga (`BROKER-01`, `SAGA-01`, `CONTRACT-BE/02`)
> **Mã định danh:** `ADR-009` — Message Broker Topology, Outbox & Lifecycle Audit
> * **Phiên bản RabbitMQ đang chạy thực tế:** `3.13.7` (`rabbitmq:3.13-management-alpine`).
> * **Đối chiếu vòng đời hỗ trợ chính thức:** RabbitMQ 3.13 đã **hết Community Support từ 2024-09-30** (`3.13.7` là bản vá community cuối cùng; Commercial Extended Support qua Broadcom/Tanzu tới **2029-12-31**). Hệ thống đánh dấu mục tiêu nâng cấp lên nhánh LTS/mới còn Community Support (RabbitMQ `4.2 LTS` / `4.x`) trước khi triển khai production chính thức.
```mermaid
flowchart LR
  BKG["booking-service\n(Outbox + Saga)"] -->|Topic Exchange\ncozynest.events| RMQ["RabbitMQ 3.13.7 Broker\ncozynest.communication.events.queue"]
  BIL["billing-service\n(Outbox + Recon)"] -->|Topic Exchange| RMQ
  RMQ -->|Retry Exhausted| DLX["DLX Exchange\ncozynest.dlx"]
  DLX --> DLQ["DLQ\ncozynest.events.dlq"]
  RMQ --> COM["communication-service\n(EventInbox Dedupe)"]
  DLQ --> ADM["admin-service\n(DeadLetter Inspect & Redrive)"]
```

```mermaid
sequenceDiagram
  autonumber
  participant T as Tenant (Web BFF /api/v1)
  participant B as booking-service (Saga + Contract)
  participant L as listing-service (/internal/rooms)
  participant P as billing-service (Escrow + Ledger)

  T->>B: POST /booking/rental-requests (availability & maxOccupants check)
  B->>L: POST /internal/rooms/{id}/hold (X-Internal-Service-Key)
  T->>B: POST /booking/contracts/{id}/sign-tenant (6-digit OTP + SHA-256)
  B->>P: Deposit Escrow + Ledger Posting
  alt Deposit Timeout / Cancel / Down
    B->>L: POST /internal/rooms/{id}/release (Saga Compensate)
    B->>P: Compensation Refund (or MANUAL_INTERVENTION_REQUIRED queue)
  end
```

### 20.3. BFF HttpOnly Auth Proxy (`DASH-FIX-1`) & Compliance Gate Status (`LEGAL-02`)
* Browser JavaScript no longer receives, reads, or persists `accessToken` (`cozynest_token`) or `refreshToken` (`cozynest_refresh_token`): `/api/auth/*` strips tokens from JSON responses and stores them solely in `HttpOnly` cookies, while Next.js `/api/v1/[...path]/route.ts` injects `Authorization: Bearer` server-side after `verifyCsrfOrigin(req)`.
* **Production Financial Blockers (`LEGAL-02 = HUMAN`):**
  * Platform strictly runs with `fintech.mode=SANDBOX_ONLY`, `money.custody.enabled=false`, `live-custody-enabled=false`, and `live-enabled=false`.
  * Real-money activation remains blocked until external legal counsel & tax advisor sign-off (`NĐ 52/2024/NĐ-CP`, `NĐ 117/2025/NĐ-CP`, `Luật 56/2024/QH15`, `Luật GDĐT 2023`), NHNN-licensed intermediary contract, and e-contract audit acceptance are completed.
