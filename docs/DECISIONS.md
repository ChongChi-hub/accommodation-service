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
