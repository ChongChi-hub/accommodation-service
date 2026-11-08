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
  * Tại `accommodation-service`, file `docker-compose.yml` mặc định khởi chạy toàn bộ hạ tầng Backend (MySQL 9.7, Redis, Gateway port 8080 và các microservice).
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

## 9. Cơ chế Xác thực Phiên, Quản lý Cookie & Xoay vòng Khóa RSA (Session Cookies & Key Rotation)

### 9.1 Định danh và Bản chất Cookie `cozynest_token`
* **Loại Token:** JWT ký mật mã bằng thuật toán bất đối xứng **RS256** (RSA Signature with SHA-256).
* **Payload quy chuẩn:**
  * `sub`: UUID v7 của người dùng (khóa chính `users.id` / `users.user_uuid`).
  * `email`: Địa chỉ email xác thực.
  * `roles`: Danh sách vai trò (`ROLE_TENANT`, `ROLE_LANDLORD`, `ROLE_MODERATOR`, `ROLE_ADMIN`).
  * `iss`: `cozynest-identity` (được thẩm định bởi Resource Server).
  * `exp`: Thời hạn sống của Access Token là **24 giờ** (86,400 giây).
  * `kid`: Key ID tương ứng trong JWKS Keyset để phục vụ xác thực và xoay vòng khóa.
* **Thời hạn sống của Refresh Token:** Token ngẫu nhiên bảo mật cao được lưu mã hóa SHA-256 trong bảng `refresh_tokens` có hạn **30 ngày** (2,592,000 giây) kèm cơ chế phát hiện tái sử dụng (Reuse Detection Revocation).

### 9.2 Cơ chế Làm mới Token (Silent Refresh Flow)
* **Kênh truyền:** Khi Access Token hết hạn (nhận mã HTTP 401 Unauthorized từ gateway hoặc backend), HTTP client tại frontend (`ApiClient`) chặn request và tự động gọi endpoint `/api/v1/auth/refresh-token` với `cozynest_refresh_token` từ `localStorage`.
* **Cập nhật liền mạch:** Khi cấp thành công cặp token mới, `cozynest_token` trong cookie và `cozynest_refresh_token` trong `localStorage` được cập nhật đồng thời mà không bắt người dùng phải đăng nhập lại hoặc gián đoạn trải nghiệm người dùng.
* **Xử lý tranh chấp (Race Condition):** `ApiClient` duy trì một `refreshPromise` singleton duy nhất để ghép các request đồng thời, ngăn chặn việc gọi refresh token nhiều lần làm kích hoạt cơ chế thu hồi do nghi ngờ tái sử dụng token.

### 9.3 Chính sách An toàn Cookie ở Production
* **Cờ `Secure`:** Tự động kích hoạt khi `process.env.NODE_ENV === 'production'`, bắt buộc cookie chỉ được truyền tải qua HTTPS.
* **Cờ `SameSite=Lax`:** Ngăn chặn tấn công CSRF trên các request chéo domain trong khi vẫn cho phép chuyển hướng hợp lệ từ các liên kết bên ngoài.
* **Đường dẫn `Path=/`:** Phạm vi hợp lệ trên toàn bộ ứng dụng web và các tuyến API rewrite.

### 9.4 Nguồn Khóa Riêng RSA & Quản lý Bí mật
* **Tuyệt đối không commit:** Khóa riêng tư RSA (`JWT_PRIVATE_KEY` / `private_key.pem`) không bao giờ được lưu trong kho mã nguồn git hay Docker image.
* **Nạp từ biến môi trường/Secret Manager:** Khóa riêng được nạp qua biến môi trường `JWT_PRIVATE_KEY` (hoặc Kubernetes Secret / AWS Secrets Manager).
* **Hướng dẫn tạo cặp khóa RSA 2048-bit cho môi trường phát triển (Dev/Demo):**
  ```bash
  # Sinh khóa riêng RSA 2048-bit PKCS#8
  openssl genpkey -algorithm RSA -out rsa_private.pem -pkeyopt rsa_keygen_bits:2048
  # Trích xuất khóa công khai tương ứng chuẩn X.509
  openssl rsa -in rsa_private.pem -pubout -out rsa_public.pem
  ```

### 9.5 JWKS Caching và Cơ chế Xoay vòng Khóa (Key Rotation by `kid`)
* Identity service công khai bộ khóa công khai tại endpoint tiêu chuẩn `/.well-known/jwks.json`.
* Mỗi khóa trong JWKS chứa `kid` duy nhất (ví dụ `cozynest-key-2026-v1`).
* Các Resource Server (`listing-service`, `billing-service`, `booking-service`, `admin-service`) cache JWKS trong bộ nhớ với thời gian sống TTL 24 giờ. Khi gặp token mang `kid` mới chưa có trong cache, Resource Server tự động fetch lại JWKS để cập nhật khóa mới mà không cần khởi động lại dịch vụ (Zero-downtime key rotation).

---

## 10. Chiến lược Định danh Thực thể: Rà soát UUID v7 và BIGINT, Trade-offs & Lộ trình Di chuyển

### 10.1 Hiện trạng Rà soát Schema (`rooms`, `room_images`, `users`)
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

### 10.2 Lý do Áp dụng Mô hình Khóa Kép (Dual-ID Pattern) & Trade-offs
1. **Hiệu năng B-tree Clustered Index của InnoDB (MySQL):**
   * *Ưu điểm của BIGINT PK:* InnoDB lưu trữ bảng theo chỉ mục phân cụm (Clustered Index B+Tree). Mọi chỉ mục phụ (Secondary Index) đều phải lưu trữ giá trị của PK làm con trỏ hàng. Khóa chính `BIGINT` (8 bytes) giúp kích thước node chỉ mục nhỏ gọn hơn 50-75% so với chuỗi UUID (36 bytes), giảm phân mảnh trang (page split) và tối ưu bộ nhớ đệm InnoDB Buffer Pool.
   * *Nhược điểm của BIGINT đơn lẻ:* Dễ bị quét ID (ID Enumeration Attack), làm lộ tổng số lượng khách hàng hoặc số lượng phòng của nền tảng.
2. **Bảo mật và Phân tán với UUID v7:**
   * *Ưu điểm của UUID v7:* Có chứa 48-bit timestamp ở phần đầu (Time-ordered), giúp việc chèn mới không bị nhảy ngẫu nhiên khắp cây B-tree như UUID v4, giảm thiểu phân tán IOPS. Không thể đoán trước ID của người dùng hay bài đăng khác.
   * *Trade-off:* Tốn 36 bytes dạng chuỗi (hoặc 16 bytes dạng `BINARY(16)`), chi phí join giữa các bảng đa dịch vụ cao hơn so với số nguyên 64-bit.
3. **Đánh giá quyết định:** Mô hình kết hợp (`BIGINT AUTO_INCREMENT` làm PK nội bộ + `UUID v7` làm Public Resource ID ở URL và API) là giải pháp cân bằng tối ưu nhất cho hệ thống vừa đạt thông lượng xử lý cao, vừa bảo vệ tính riêng tư.

### 10.3 Lộ trình Di chuyển Chuẩn hóa (Migration Roadmap)
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

## 11. Kiến trúc Quản lý Lịch hẹn Xem phòng ([APPT-02])

> Ngày quyết định: 07/11/2026  
> Áp dụng cho: `booking-service` và `accommodation-web`

### 11.1 Máy trạng thái (State Machine)
Lịch hẹn xem phòng (`ViewingAppointment`) tuân thủ nghiêm ngặt máy trạng thái trong domain model (không dùng setter công khai):
* `PENDING` -> `CONFIRMED`: Chủ trọ chấp thuận lịch hẹn. Hệ thống kiểm tra trùng giờ; nếu trùng giờ với lịch đã `CONFIRMED` khác của cùng chủ trọ, trả về `ERR_APPT_TIME_CONFLICT` (HTTP 409). Chủ trọ có thể gửi kèm cờ `force: true` để chấp thuận đè.
* `PENDING` -> `DECLINED`: Chủ trọ từ chối lịch hẹn, bắt buộc có lý do (`reasonCode`).
* `CONFIRMED` -> `CANCELLED`: Hủy lịch hẹn đã xác nhận, bắt buộc có lý do (`reasonCode`).
* `CONFIRMED` -> `COMPLETED`: Đánh dấu đã gặp khách thành công. Chỉ cho phép sau thời điểm `scheduled_start`.
* `CONFIRMED` -> `NO_SHOW`: Đánh dấu khách không đến xem phòng. Chỉ cho phép sau thời điểm `scheduled_start`.
* `PENDING` -> `EXPIRED`: Hệ thống định kỳ quét và đánh dấu hết hạn đối với các lịch chờ chưa được xử lý khi đã qua giờ hẹn.
* Mọi chuyển đổi trạng thái trái phép trả về lỗi nghiệp vụ ổn định `ERR_APPT_INVALID_TRANSITION` (HTTP 409).

### 11.2 Xử lý Trùng giờ (Time Conflict Policy)
* Khi chủ trọ xác nhận lịch, `booking-service` quét các lịch `CONFIRMED` của cùng `landlord_id` trong khung giờ giao nhau `[scheduled_start, scheduled_end)`.
* Nếu phát hiện trùng, trả về mã lỗi `ERR_APPT_TIME_CONFLICT` kèm danh sách lịch trùng.
* Nếu client gửi cờ `force: true`, hệ thống cho phép xác nhận và ghi vết vào `appointment_status_history` với lý do "Xác nhận đè lịch trùng".

### 11.3 Transactional Outbox Pattern & Event Publishing
* Khi trạng thái thay đổi (`CONFIRMED`, `DECLINED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`, `EXPIRED`), một domain event tương ứng được ghi vào bảng `appointment_events_outbox` trong cùng giao dịch database (ACID transaction).
* *TODO*: `communication-service` hiện chưa có API gửi SMS/Email thời gian thực; khi `communication-service` hoàn thiện, outbox worker hoặc Debezium CDC sẽ gửi event lên message broker (Kafka/RabbitMQ) để bắn thông báo cho khách thuê và chủ trọ.

### 11.4 Bảo vệ Quyền riêng tư Số điện thoại (Phone Privacy Protection)
* Bảo mật dữ liệu người thuê: Thuộc tính `contactPhone` chỉ được trả về đầy đủ khi lịch hẹn ở trạng thái `CONFIRMED` hoặc `COMPLETED`.
* Với các trạng thái `PENDING`, `DECLINED`, `CANCELLED`, `EXPIRED`: số điện thoại được che một phần tại tầng Application/DTO (ví dụ `090••••567`). Không bao giờ gửi số điện thoại thô qua mạng khi chưa xác nhận.
* Nghiêm cấm ghi số điện thoại khách hàng vào log hệ thống.


---

## 12. Kiến trúc Hồ sơ Người dùng (USER-01) & Xác minh Danh tính Chủ trọ (KYC-01)

> Ngày quyết định: 07/11/2026  
> Áp dụng cho: `identity-service`, `listing-service`, `common`, `api-gateway`, và `accommodation-web`

### 12.1 Thư viện Xử lý Ảnh Dùng chung (Shared Image Processing)
* **Vấn đề**: Cả `listing-service` (ảnh phòng) và `identity-service` (avatar người dùng, ảnh CCCD KYC) đều cần kiểm tra an ninh hình ảnh nghiêm ngặt (magic bytes, chống file polyglot, chặn bom pixel $\le 8000$px, tẩy EXIF/GPS, nén sang WebP).
* **Quyết định**: Di chuyển và đưa các thư viện `com.twelvemonkeys.imageio:imageio-webp:3.12.0` và `net.coobird:thumbnailator:0.4.20` vào module `common`.
* Đóng gói tiện ích xử lý ảnh dùng chung trong `vn.cozynest.common.image.ImageProcessingHelper`:
  * Xác thực magic bytes: JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`), WebP (Byte 0-3 `RIFF`, byte 8-11 `WEBP`).
  * Tẩy sạch EXIF, dữ liệu định vị GPS và mã độc bằng cách vẽ lại hoàn toàn sang một `BufferedImage` kiểu `TYPE_INT_RGB` mới.
  * Cắt ảnh vuông từ tâm (center square crop) tự động cho avatar người dùng.
  * Xuất ảnh định dạng WebP tối ưu với các kích cỡ chuẩn: Avatar (64px, 256px), KYC CCCD ($\le 2000$px cạnh dài).

### 12.2 Bảo mật Dữ liệu Nhạy cảm KYC & Mã hóa Lưu trữ
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

### 12.3 Chính sách Lưu giữ và Hủy Dữ liệu KYC Bị Từ chối (Retention & Cleanup Policy)
* **Thời hạn lưu trữ và Cấp quyết định**:
  * Thời hạn lưu trữ hiện dùng là **30 ngày** (cấu hình qua `app.kyc.retention-days=30`).
  * Thời hạn này do **Bộ phận Pháp chế & Tuân thủ (Legal & Compliance) phối hợp với Cán bộ Bảo vệ Dữ liệu Cá nhân (Data Protection Officer - DPO)** quyết định, căn cứ theo Nghị định số 13/2023/NĐ-CP về Bảo vệ Dữ liệu Cá nhân và quy định khiếu nại dân sự của Việt Nam (cho phép người dùng có 30 ngày để khiếu nại quyết định từ chối trước khi dữ liệu nhạy cảm bị tiêu hủy hoàn toàn).
* **Cơ chế An toàn của Scheduled Job (`KycImageCleanupScheduler`)**:
  * Cờ kích hoạt `app.kyc.cleanup-enabled`: **MẶC ĐỊNH LÀ FALSE (TẮT)**. Chỉ khi quản trị viên chủ động bật qua biến môi trường hoặc cấu hình hệ thống, tác vụ mới chạy.
  * **Tuyệt đối KHÔNG xóa hồ sơ PENDING hoặc APPROVED**: Truy vấn cơ sở dữ liệu (`findRejectedBefore(cutoff)`) được lọc chặt chẽ chỉ lấy các bản ghi có trạng thái `REJECTED` và thời điểm bị từ chối cũ hơn 30 ngày. Các hồ sơ đang chờ duyệt (`PENDING`) và hồ sơ đã xác thực thành công (`APPROVED`) không bao giờ bị scheduler chạm tới hay xóa ảnh.
  * Khi quét và xóa file vật lý trên đĩa, đường dẫn ảnh được cập nhật thành giá trị đại diện `PURGED` để đánh dấu hồ sơ đã được thanh tẩy dữ liệu.

### 12.4 Cổng Chặn Đăng Tin (Listing KYC Gate) & Inter-Service Token
* **Quy định**: Chủ trọ chỉ được phép gửi duyệt phòng (`POST /api/v1/landlord/rooms/{id}/submit`) và xuất bản phòng khi KYC ở trạng thái `APPROVED`. Nếu chưa được duyệt, hệ thống từ chối với HTTP 403 và mã lỗi `ERR_KYC_REQUIRED` kèm trạng thái KYC hiện tại (`NONE`, `PENDING`, `REJECTED`). Thao tác lưu nháp (`DRAFT`) vẫn được cho phép.
* **Cơ chế Xác minh Liên Dịch vụ (Inter-Service KYC Verification)**:
  * `identity-service` cung cấp endpoint nội bộ `GET /internal/users/{id}/kyc-status`, không đi qua gateway công khai.
  * Endpoint được bảo vệ bằng Service Token / Secret nội bộ truyền qua header `X-Internal-Service-Key`.
  * `listing-service` gọi qua HTTP Client với thời gian chờ timeout 3 giây, bộ nhớ đệm ngắn $\le 60$ giây.
  * Áp dụng chính sách **FAIL-CLOSED**: Nếu không kết nối được `identity-service` hoặc phản hồi lỗi/hết thời gian, hệ thống tự động từ chối yêu cầu gửi duyệt để bảo vệ an toàn nền tảng.
* **Chính sách Phòng Đã Đăng Khi KYC Bị Thu hồi**:
  * Các phòng đã được `PUBLISHED` trước đó sẽ không bị tự động ẩn để tránh gián đoạn hợp đồng và người thuê đang tìm kiếm, tuy nhiên mọi hành động đăng mới, gửi duyệt lại hoặc mở lại phòng ẩn đều bị chặn cho đến khi KYC được phê duyệt lại.

### 12.5 Đổi Mật khẩu & Thu hồi Phiên Làm việc
* Đổi mật khẩu yêu cầu nhập đúng mật khẩu hiện tại. Mật khẩu mới bắt buộc phải khác mật khẩu cũ và đáp ứng độ phức tạp tối thiểu.
* Áp dụng Rate Limiting qua Redis: Nếu nhập sai mật khẩu cũ 5 lần liên tiếp trong 15 phút, tài khoản bị tạm khóa đổi mật khẩu để chống brute-force.
* Khi đổi mật khẩu thành công: Hệ thống tự động thu hồi tất cả các `refresh_tokens` của các thiết bị khác, chỉ duy trì phiên đăng nhập hiện tại để người dùng không bị văng ra ngoài đột ngột. Đồng thời gửi email cảnh báo bảo mật tới người dùng.


## 13. Quyết định Kiến trúc & Bảo mật Xác thực [AUTH-01, AUTH-03, AUTH-04]

### 13.1 Thuật toán Băm Mật khẩu & Chống Dò Tài khoản (Timing Attack)
* **Thuật toán thật đang sử dụng**: BCrypt với hệ số phức tạp 12 (`BCryptPasswordEncoder(12)`).
* **Lý do & Bảo đảm tiêu chuẩn**: BCrypt hệ số 12 tạo độ trễ tính toán ~80-100ms cho mỗi lần băm/so sánh trên CPU hiện đại, đáp ứng đầy đủ khuyến nghị của OWASP Authentication Cheat Sheet và NIST SP 800-63B.
* **Chống Timing Attack (Timing Enumeration)**: Khi người dùng đăng nhập với email không tồn tại trong hệ thống, `LoginUseCase` vẫn kích hoạt việc so sánh mật khẩu với hằng số `DUMMY_BCRYPT_HASH` (`$2a$12$7kPj5aXN62q.7XJjN8wGxeQJv9XW1Nq2g5U1rQ0M5J4L3K2H1G0F.`). Việc này đảm bảo thời gian phản hồi giữa trường hợp "email không tồn tại" và "mật khẩu không đúng" là hoàn toàn xấp xỉ nhau (~80-100ms), ngăn chặn kẻ tấn công đo thời gian để dò email hợp lệ.

### 13.2 Chính sách Khóa Tài khoản Chống DoS (Lockout Policy)
* **Quyết định**: Khóa theo cặp `(email, IP)` thay vì khóa toàn cầu theo `email`.
* **Lý do**: Nếu khóa toàn cục theo email, bất kỳ kẻ tấn công nào biết trước email nạn nhân đều có thể gửi 5 request sai mật khẩu để làm tê liệt tài khoản của nạn nhân (Denial-of-Service). Bằng cách đếm số lần sai theo `(email, ipAddress)`, kẻ tấn công chỉ bị khóa trên chính IP của họ trong 15 phút, trong khi người dùng thực từ địa chỉ IP hợp lệ vẫn có thể đăng nhập bình thường.

### 13.3 Đánh đổi Khi Đăng ký Email Trùng
* **Quyết định**: Khi đăng ký bằng email đã tồn tại, trả về `400 Bad Request` kèm mã lỗi `ERR_AUTH_EMAIL_EXISTS` ("Email đã tồn tại trên hệ thống").
* **Đánh đổi**: Thay vì gửi email ẩn thông báo trùng (gây chậm trễ và khó khăn lớn cho người dùng thực muốn đăng ký nhanh), hệ thống chấp nhận thông báo lỗi trực tiếp để tối ưu trải nghiệm người dùng (UX). Rủi ro brute-force quét email được triệt tiêu thông qua cơ chế Redis Rate Limiting nghiêm ngặt (tối đa 10 lần đăng ký / 1 phút / IP, quá ngưỡng trả về HTTP 429).


## 14. Quyết Định Di Chuyển Node.js Sang Microservices [SERVICE-01]

### 14.1 Phân Chia Microservices & Phạm Vi Nghiệp Vụ (DDD Bounded Contexts)
* Hệ thống phân tách thành 6 microservices độc lập nghiệp vụ và 1 API Gateway:
  1. `api-gateway` (Port 8080): Điểm đón tiếp duy nhất, quản lý CORS, chặn tuyệt đối `/internal/**`.
  2. `identity-service` (Port 8081): Xác thực, người dùng, phân quyền RBAC, hồ sơ KYC CCCD và Sinh trắc học khuôn mặt.
  3. `listing-service` (Port 8082): Tin đăng phòng trọ, danh mục tiện ích, tìm kiếm, lưu phòng (Bookmarks), đánh giá.
  4. `booking-service` (Port 8083): Lịch hẹn xem phòng, yêu cầu thuê, hợp đồng điện tử.
  5. `billing-service` (Port 8084): Chỉ số điện nước, hóa đơn, thanh toán.
  6. `communication-service` (Port 8085): Trò chuyện thời gian thực, thông báo in-app.
  7. `admin-service` (Port 8086): Kiểm duyệt, báo cáo vi phạm, thống kê hệ thống.

### 14.2 Cô Lập Cơ Sở Dữ Liệu Tuyệt Đối (Database Isolation & Zero Cross-Schema FK)
* Mỗi microservice sở hữu CSDL schema riêng biệt trên MySQL 9.1 (`cozynest_identity`, `cozynest_listing`, `cozynest_booking`, `cozynest_billing`, `cozynest_communication`, `cozynest_admin`).
* Cấm hoàn toàn Foreign Key liên schema (xác thực qua truy vấn `information_schema.KEY_COLUMN_USAGE` trả về 0 liên kết).
* Giao tiếp liên dịch vụ thực hiện qua REST API nội bộ hoặc Outbox Pattern, không chia sẻ database connection.

### 14.3 Bỏ Có Chủ Đích Địa Giới Cấp Quận/Huyện
* Áp dụng quy chuẩn địa giới hành chính 2 cấp (Tỉnh/Thành -> Phường/Xã).
* Loại bỏ route `GET /api/v1/location/districts` và bảng `districts` trong nghiệp vụ. Phường/Xã liên kết trực tiếp với mã Tỉnh/Thành qua trường `province_code`.

