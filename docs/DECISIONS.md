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

### 9.1 Định danh và Bản chất Cookie `toam_token`
* **Loại Token:** JWT ký mật mã bằng thuật toán bất đối xứng **RS256** (RSA Signature with SHA-256).
* **Payload quy chuẩn:**
  * `sub`: UUID v7 của người dùng (khóa chính `users.id` / `users.user_uuid`).
  * `email`: Địa chỉ email xác thực.
  * `roles`: Danh sách vai trò (`ROLE_TENANT`, `ROLE_LANDLORD`, `ROLE_MODERATOR`, `ROLE_ADMIN`).
  * `iss`: `toam-identity` (được thẩm định bởi Resource Server).
  * `exp`: Thời hạn sống của Access Token là **24 giờ** (86,400 giây).
  * `kid`: Key ID tương ứng trong JWKS Keyset để phục vụ xác thực và xoay vòng khóa.
* **Thời hạn sống của Refresh Token:** Token ngẫu nhiên bảo mật cao được lưu mã hóa SHA-256 trong bảng `refresh_tokens` có hạn **30 ngày** (2,592,000 giây) kèm cơ chế phát hiện tái sử dụng (Reuse Detection Revocation).

### 9.2 Cơ chế Làm mới Token (Silent Refresh Flow)
* **Kênh truyền:** Khi Access Token hết hạn (nhận mã HTTP 401 Unauthorized từ gateway hoặc backend), HTTP client tại frontend (`ApiClient`) chặn request và tự động gọi endpoint `/api/v1/auth/refresh-token` với `toam_refresh_token` từ `localStorage`.
* **Cập nhật liền mạch:** Khi cấp thành công cặp token mới, `toam_token` trong cookie và `toam_refresh_token` trong `localStorage` được cập nhật đồng thời mà không bắt người dùng phải đăng nhập lại hoặc gián đoạn trải nghiệm người dùng.
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
* Mỗi khóa trong JWKS chứa `kid` duy nhất (ví dụ `toam-key-2026-v1`).
* Các Resource Server (`listing-service`, `billing-service`, `booking-service`, `admin-service`) cache JWKS trong bộ nhớ với thời gian sống TTL 24 giờ. Khi gặp token mang `kid` mới chưa có trong cache, Resource Server tự động fetch lại JWKS để cập nhật khóa mới mà không cần khởi động lại dịch vụ (Zero-downtime key rotation).
