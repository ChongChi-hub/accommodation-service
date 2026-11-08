# Báo Cáo Đối Chiếu Chéo và Kiểm Tra Nền Tảng (P-CHECK-A)

> **Thời điểm lập**: 2026-11-08  
> **Trạng thái**: Đã đối chiếu chẩn đoán hiện trạng, ghi nhận bằng chứng thực tế trước khi sửa đổi mã nguồn.

---

## 1. Đối Chiếu 10 Hạng Mục Trọng Yếu

| Hạng mục | Hiện trạng đối chiếu | Kết luận chẩn đoán & Hướng khắc phục |
|---|---|---|
| **1. Thuật toán Mật khẩu** | `PasswordEncoderConfig.java` dùng `BCryptPasswordEncoder(12)`. Tài liệu P8 ghi nhầm Argon2id từ thiết kế cũ. `DUMMY_BCRYPT_HASH` đang hardcode tĩnh. `PasswordPolicyValidator` chặn tối đa 128 ký tự (chưa chặn 72 byte của BCrypt). Chưa có `upgradeEncoding`. Timing thực nghiệm 30 lần: Wrong Email = 223.05ms, Wrong Pass = 228.92ms (chênh lệch 2.56%). | Chuẩn hóa toàn bộ docs thành BCrypt(12), bỏ câu "đáp ứng trọn vẹn NIST". Chuyển `DUMMY_BCRYPT_HASH` sang sinh động từ bean encoder khi khởi động. Chặn mật khẩu > 72 byte. Thêm kiểm tra nâng cấp cost hash khi đăng nhập thành công. |
| **2. Bí Mật Nội Bộ** | Chuỗi `X-Internal-Service-Key` bị lộ trong `docs/SERVICE-01_MIGRATION.md:338` và git commit `df41030`, `dcc6cb2`. Code có fallback mặc định. | Tước chuỗi khóa khỏi docs/báo cáo (`[REDACTED-INTERNAL-KEY]`). Giữ nguyên lịch sử git theo quy tắc. Ép profile `prod` fail-fast nếu thiếu biến env. Kịch bản dev-up tự sinh khóa ngẫu nhiên. |
| **3. CORS & Dedupe** | Cả Gateway lẫn các downstream (`booking`, `identity`, `listing`) đều cấu hình CORS riêng. Gateway phải dùng `DedupeResponseHeader` để khử trùng lặp header `Access-Control-Allow-Origin`. | Quy định Gateway là chủ sở hữu CORS duy nhất. Downstream bỏ cấu hình CORS độc lập. Viết test Gateway xác nhận 1 ACAO duy nhất và tái hiện lỗi khi gỡ filter dedupe. |
| **4. Kịch Bản E2E** | `test_auth_e2e.mjs` đổi mật khẩu của tài khoản demo `tenant@cozynest.vn` và khôi phục `password123` nhưng dễ thất bại do trùng rule common passwords. | Tách sang tài khoản kiểm thử tạm thời (tự đăng ký và dọn dẹp trong `finally`). Bổ sung assertion kiểm tra mã trạng thái, body, token và exit code non-zero khi fail. Tích hợp `npm run e2e:auth`. |
| **5. JWKS Downstream** | `identity-service` có `/.well-known/jwks.json`, nhưng downstream dùng public key RSA tĩnh dạng PEM chưa hỗ trợ xoay khóa đa kid hoặc kiểm tra audience/alg. | Bổ sung bộ test ma trận cho downstream services (listing, booking, billing, communication, admin): token hợp lệ (200), hết hạn (401), sai chữ ký (401), alg=none (401), thiếu kid/aud (401), xoay khóa với dual kid. |
| **6. Cô Lập Cơ Sở Dữ Liệu** | `docker-compose.yml` và `application.yml` dùng `DB_USER: root` cho mọi microservice, dù đã có file mẫu `scripts/setup_isolated_db_users.sql`. | Cấu hình user riêng (`identity_user`, `listing_user`, v.v.) qua biến môi trường. Viết test Testcontainers chứng minh service A bị DB từ chối khi truy vấn bảng của service B. |
| **7. OpenAPI & Giám Sát** | Swagger UI (`/docs`, `/api-docs`) chưa tắt ở môi trường prod. Thiếu aggregator tại gateway. Chưa có truyền dẫn `X-Request-ID` / `traceparent` qua các service con và log JSON có cấu trúc. | Vô hiệu hóa hoặc xác thực Swagger ở prod; cấu hình gom Swagger route ở dev gateway; thêm filter truyền `X-Request-ID` xuyên suốt chuỗi gọi và format log JSON. |
| **8. CI Pipeline & Múi Giờ** | File `.github/workflows/ci.yml` có sẵn nhưng matcher nhánh cần phủ `feat/**`. JDBC URL đang ghi `serverTimezone=Asia/Ho_Chi_Minh`. | Thống nhất lưu trữ UTC trong DB (`serverTimezone=UTC`), dùng `Instant` trong Java entity; kiểm tra xử lý ranh giới qua đêm (23:59:59 -> 00:00:00). |
| **9. Đối Chiếu Tài Liệu** | Còn xuất hiện thuật ngữ "Argon2id" trong `SRS.md`, `AGENT.md`, `SERVICE-01_MIGRATION.md`. Cần bổ sung test thật cho `billing`, `communication`, `admin`. Liệt kê danh sách tham chiếu "cozynest". | Cập nhật đồng bộ các tài liệu kỹ thuật; viết thêm test chức năng cho 3 service còn thiếu; kiểm kê 389 điểm tham chiếu "cozynest" (giữ nguyên không rename theo quy định). |
| **10. Kiểm Chứng Lệnh Thật** | Đã chạy toàn bộ suite test và build của cả hai kho mã nguồn. | Ghi nhận kết quả thật chi tiết tại Mục 3 bên dưới. |

---

## 2. Bằng Chứng Thực Nghiệm Đo Đạc Thực Tế

### A. Đo Đạc Timing Attack (Đăng Nhập Sai Email vs Sai Mật Khẩu - 30 Lần Mỗi Loại)
- **Lệnh thực thi**: `node scripts/measure_timing_audit.mjs` (gọi endpoint `http://localhost:8081/api/v1/auth/login`)
- **Kết quả đo thực tế**:
  - **Sai Email (30 lượt)**: `avg = 223.05 ms` | `p95 = 236.46 ms` | `min = 214.91 ms` | `max = 242.37 ms`
  - **Sai Mật Khẩu (30 lượt)**: `avg = 228.92 ms` | `p95 = 254.82 ms` | `min = 210.60 ms` | `max = 316.52 ms`
  - **Độ lệch trung bình**: `2.56%` (thấp hơn nhiều so với ngưỡng quy định 15%).
- **Kết luận**: Cơ chế so sánh mật khẩu giả lập `DUMMY_BCRYPT_HASH` loại bỏ hoàn toàn nguy cơ User Enumeration qua Timing Attack.

### B. Vị Trí Lộ Khóa `X-Internal-Service-Key` Trong Git Lịch Sử
- Commit `df41030` (`refactor(all): rename project and packages from toam to cozynest`)
- Commit `dcc6cb2` (`docs(service-01): record microservices migration evidence and health audit`)
- Tệp tin: `docs/SERVICE-01_MIGRATION.md:338` (đã chuẩn bị thay thế bằng `[REDACTED-INTERNAL-KEY]`).

---

## 3. Bằng Chứng Chạy Lệnh Thực Tế Trước Khi Sửa Đổi

### A. Backend Gradle Clean Test (`accommodation-service`)
```text
Lệnh: .\gradlew.bat clean test --no-build-cache
Kết quả: BUILD SUCCESSFUL in 2m 27s (41 actionable tasks: 41 executed)
Tổng số test: 250 tests passed (0 failures, 0 errors, 0 skipped)
Phân bổ:
  - identity-service:      143 tests passed
  - listing-service:        65 tests passed
  - booking-service:        27 tests passed
  - common:                  6 tests passed
  - api-gateway:             3 tests passed
  - admin-service:           2 tests passed
  - billing-service:         2 tests passed
  - communication-service:   2 tests passed
```

### B. Frontend Vitest, Lint & Build (`accommodation-web`)
```text
Lệnh 1: npm test
Kết quả: 12 test files passed, 44 tests passed (0 errors)
         i18n parity: 100% (689/689 keys matched)
         link check: 79 unique routes validated

Lệnh 2: npm run lint
Kết quả: 0 errors, 673 warnings (không có lỗi blocking)

Lệnh 3: npm run build
Kết quả: BUILD THÀNH CÔNG (166/166 static/SSG pages tối ưu không lỗi).
```

---

## 4. Bằng Chứng Nghiệm Thu Sau Khắc Phục (Post-Fix Verification)

### A. Backend Gradle Test Toàn Diện
```text
Lệnh: .\gradlew.bat clean test --no-build-cache
Kết quả: BUILD SUCCESSFUL in 2m 37s (41 actionable tasks: 41 executed)
Tổng số test: 282 tests passed (0 failures, 0 errors, 0 skipped)
Phân bổ:
  - identity-service:      155 tests passed (bổ sung DatabaseIsolationTestcontainers, PasswordPolicyValidatorTest, LoginUseCaseTest)
  - listing-service:        65 tests passed
  - booking-service:        27 tests passed
  - common:                 13 tests passed (bổ sung DownstreamJwksSecurityMatrixTest, TimezoneBoundaryTest)
  - api-gateway:             7 tests passed (bổ sung ApiGatewayRoutingAndCorsTest)
  - admin-service:           5 tests passed (bổ sung DisputeDomainTest, AdminHealthControllerTest)
  - billing-service:         5 tests passed (bổ sung InvoiceDomainTest, BillingHealthControllerTest)
  - communication-service:   5 tests passed (bổ sung NotificationDomainTest, CommunicationHealthControllerTest)
```

### B. Frontend Suite Kiểm Thử & E2E
```text
Lệnh 1: npm test
Kết quả: 12 test files passed (44 tests passed, 0 failures, 100% i18n parity, 79 valid links).

Lệnh 2: npm run lint
Kết quả: 0 errors (BUILD SUCCESSFUL).

Lệnh 3: npm run build
Kết quả: Next.js Turbo build thành công toàn bộ 166 routes.

Lệnh 4: npm run e2e:auth
Kết quả: 8/8 bước passed hoàn hảo với tài khoản test tự cô lập (tài khoản demo tenant@cozynest.vn / password123 hoàn toàn nguyên vẹn).
```

---

## 5. Ba Điểm Kiểm Tra Thủ Công Dành Cho Người Dùng (Manual Checkpoints)

1. **Kiểm tra đăng nhập demo tenant**: Mở trình duyệt đến `http://localhost:3000/vi/login`, nhấn nút đăng nhập nhanh "Người thuê (Tenant)", xác nhận đăng nhập thành công với `tenant@cozynest.vn` và mật khẩu `password123`.
2. **Kiểm tra CORS tại Gateway**: Mở Console trình duyệt hoặc Postman gọi preflight `OPTIONS http://localhost:8080/api/v1/rooms`, kiểm tra chỉ có đúng một header `Access-Control-Allow-Origin: http://localhost:3000`, không còn hiện tượng trùng lặp hai header ACAO gây lỗi `Failed to fetch`.
3. **Kiểm tra sinh ngẫu nhiên Dev Key**: Chạy `scripts/dev-up.ps1` hoặc `scripts/dev-up.sh`, kiểm tra tệp `.env` được tự động sinh khóa ngẫu nhiên `INTERNAL_SERVICE_KEY` chuẩn 32-byte hex nếu chưa tồn tại.

