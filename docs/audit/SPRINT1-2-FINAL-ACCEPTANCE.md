# BÁO CÁO NGHIỆM THU CUỐI CÙNG — SPRINT 1 & 2 (TỔ ẤM / COZYNEST)

> **Ngày nghiệm thu:** 12/11/2026  
> **Nhánh nghiệm thu:** `fix/s12-subtask-remediation` (`accommodation-service` & `accommodation-web`)  
> **Đối tượng:** Toàn bộ 30 thẻ chính thức Sprint 1–2 (`index.md`, `141 pts`) + 01 thẻ bổ sung `USER-01` = **31 thẻ**.  
> **Báo cáo nguồn:** [SPRINT-1-2-SUBTASK-AUDIT.md](./SPRINT-1-2-SUBTASK-AUDIT.md) · **Tracker chi tiết:** [SPRINT1-2-REMEDIATION.md](../progress/SPRINT1-2-REMEDIATION.md)  
> **Nguyên tắc nghiệm thu:** Mọi con số, file, commit SHA, kiểm thử và HTTP status dưới đây đều được trích xuất trực tiếp từ kết quả chạy lệnh thực tế sau các lần chỉnh sửa cuối cùng.

---

## 1. TỔNG HỢP KẾT QUẢ NGHIỆM THU CẤP THẺ (31 THẺ)

| Nhóm trạng thái | Sprint 1 (15 thẻ) | Sprint 2 (15 thẻ) | Bổ sung `USER-01` (1 thẻ) | Tổng cộng (31 thẻ) | Chi tiết thẻ |
|---|---:|---:|---:|---:|---|
| **`VERIFIED_DONE`** | 11 | 13 | 0 | **24 (77.4%)** | S1: `INFRA-02`, `AUTH-01`, `AUTH-02`, `DB-01`, `DB-02`, `AUTH-04`, `FE-01`, `FE-02`, `AUTH-UI`, `INFRA-01`, `KYC-01-BE`<br>S2: `SEARCH-BE`, `APPT-02-BE`, `ROOM-02`, `LANDLORD-BE`, `NOTIF-API-01`, `LISTING-HDR-BE`, `SEARCH-FE`, `LANDLORD-FE`, `FE-03`, `HDR-1`, `UI-HOME-01`, `ROOM-01`, `E2E-S2` |
| **`IMPLEMENTED_UNVERIFIED`** | 3 | 1 | 0 | **4 (12.9%)** | S1: `AUTH-FLOW-A` *(chưa có E2E trình duyệt xuyên suốt cấp role LANDLORD)*, `KYC-01-FE` *(chưa có E2E upload CCCD 2 mặt)*, `KYC-02` *(chỉ riêng `KYC-02.3` WebRTC scanner chưa có E2E mock MediaStream; phần BE + Modal + Scheduler đã `VERIFIED_DONE`)*<br>S2: `APPT-02-FE` *(Vitest 5/5 + smoke load pass; chưa có Playwright live ranh giới nửa đêm GMT+7)* |
| **`PARTIAL`** *(External `BLOCKED`)* | 1 | 0 | 0 | **1 (3.2%)** | S1: `AUTH-03` — Vitest (3/3), BE unit/integration tests, và E2E từ chối token giả pass; live `GET /api/v1/auth/google/status` trả `enabled=false` và `POST /api/v1/auth/google` trả `503 ERR_GOOGLE_NOT_CONFIGURED` do môi trường local không có `GOOGLE_CLIENT_ID` thật. |
| **`SPEC_MISSING`** *(Derived verified/partial)* | 0 | 1 | 1 | **2 (6.5%)** | `UI-STATIC-01` (5 fake repos đã dọn `[]`, `StaticBadgeRepository` gọi live API, `verify:links` `86/86`, `smoke` `53/53`; thiếu checklist Trello chính thức), `USER-01` (derived code & tx Testcontainers pass; không thuộc bảng 30 thẻ). |

### Tóm tắt 5 khoản nợ P0 từ Audit ban đầu
Tất cả **5 khoản nợ P0 (`15 pts`)** đã được khắc phục hoàn toàn và có kiểm thử xác minh:
1. **`DEBT-JWKS-ADMIN` -> ĐÃ ĐÓNG (`VERIFIED_DONE`):** Sửa `RefreshTokenUseCase` (`noRollbackFor = IdentityDomainException.class`) + `RefreshTokenReuseTestcontainersTest`, sửa `@Transactional` ở logout, áp dụng `JwksTokenValidator` động, bổ sung `SecurityConfig` + `JwtAuthFilter` cho `admin-service`, và sửa `GlobalExceptionHandler` ở `identity-service` trả `403 ERR_FORBIDDEN` thay vì `500` cho `AccessDeniedException`.
2. **`DEBT-APPT-LOCK-OUTBOX` -> ĐÃ ĐÓNG (`VERIFIED_DONE`):** `LandlordAppointmentService` chuyển sang `findOverlappingConfirmedForUpdate` (`PESSIMISTIC_WRITE`) được kiểm chứng bởi `AppointmentConcurrencyTestcontainersTest`, kèm `AppointmentOutboxPublisherScheduler` -> `HttpNotificationDispatchAdapter`.
3. **`DEBT-KYC-SCHEDULER-TX` -> ĐÃ ĐÓNG (`VERIFIED_DONE`):** Bật `@EnableScheduling` trên `IdentityApplication`, gỡ `@Transactional(readOnly = true)` ở `FaceModerationUseCase.getFaceDetail` & `UserProfileUseCase`, kiểm chứng bằng `KycAndProfileTxTestcontainersTest` & `FaceCleanupSchedulerTest`.
4. **`DEBT-PROD-SEED-SWAGGER` -> ĐÃ ĐÓNG (`VERIFIED_DONE`):** Tạo `application-prod.yml` trên 7 services, giới hạn `@Profile` non-prod cho seed loaders, khóa `/actuator/env` (`401` service / `404` gateway), và xử lý `NoResourceFoundException` trả `404 ERR_NOT_FOUND` tại `:8081/swagger-ui/index.html`.
5. **`DEBT-DB-USER` -> ĐÃ ĐÓNG (`VERIFIED_DONE`):** Xác nhận trực tiếp 6 containers kết nối MySQL bằng user riêng biệt (`identity_user`, `listing_user`, `booking_user`, `billing_user`, `communication_user`, `admin_user`) + `DatabaseIsolationTestcontainersTest`.

---

## 2. BẰNG CHỨNG KIỂM THỬ THỰC TẾ SAU CHỈNH SỬA CUỐI

### A. Backend (`accommodation-service`, nhánh `fix/s12-subtask-remediation`)
Lệnh sạch toàn cục: `.\gradlew.bat clean test --no-daemon --no-build-cache --console=plain` (cùng tái chạy `:identity-service:test` sau sửa `AccessDeniedException` ở commit `a1a8a4b`):

| Module | Test Suites (XML) | Test Cases | Failures | Errors | Skipped | Ghi chú Testcontainers thật |
|---|---:|---:|---:|---:|---:|---|
| `common` | 4 | 17 | 0 | 0 | 0 | `CleanDddArchitectureTest` (không còn `allowEmptyShould`) |
| `api-gateway` | 1 | 8 | 0 | 0 | 0 | CORS & Routing assertions |
| `identity-service` | 43 | 173 | 0 | 0 | 0 | 4 Testcontainers MySQL suites (`64.9s` tổng cộng, `0` skip) |
| `listing-service` | 19 | 79 | 0 | 0 | 0 | `RoomSearchTestcontainersTest` MySQL (`31.975s`, `0` skip) |
| `booking-service` | 7 | 29 | 0 | 0 | 0 | `AppointmentConcurrencyTestcontainersTest` MySQL (`34.316s`, `0` skip) |
| `billing-service` | 4 | 15 | 0 | 0 | 0 | Strict `BillingArchitectureTest` |
| `communication-service` | 4 | 13 | 0 | 0 | 0 | JWKS security & dispatch service |
| `admin-service` | 4 | 8 | 0 | 0 | 0 | `SecurityConfig` + Strict `AdminArchitectureTest` |
| **TỔNG CỘNG BE** | **86** | **342** | **0** | **0** | **0** | **7/7 Testcontainers MySQL tests đều chạy thật & pass** |

### B. Frontend (`accommodation-web`, nhánh `fix/s12-subtask-remediation`)
1. **Static & Unit Checks (sau sửa code cuối):**
   - `npx eslint . --max-warnings=0`: **PASS** (`0` errors, `0` warnings).
   - `npx tsc --noEmit`: **PASS** (`exit 0`).
   - `npm test` (`vitest run` + scripts):
     - Vitest: **16/16 test files**, **73/73 tests passed**.
     - `verify:i18n`: **`1725` VI keys / `1725` EN keys (100% parity)**.
     - `verify:links`: **`86` unique internal links verified across `357` source files**.
     - `verify:manifest`: **`224` raster images**, Hero #1 **`252298 bytes (246.4 KB)`** (trong ngân sách `<= 250 KB`).
     - `npm run check:seed-text`: **100% valid UTF-8, zero `U+FFFD`, zero `??`**.
   - `npm run build`: **PASS** (`exit 0`, `182` routes built) & đang phục vụ qua `next start` (`http://localhost:3000`).
2. **Playwright E2E Suite (full 13 specs trên `next start` `:3000` + Docker BE `:8080` + Mailpit `:8025`):**
   - **Tổng kết:** **104/104 PASSED (`0` failed, `0` skipped) trong 2.6 phút**:
     - `admin-kyc.spec.ts`: `2/2`
     - `admin-rbac.spec.ts`: `2/2`
     - `auth-forgot-mailpit-full-flow.spec.ts`: `1/1`
     - `auth-rbac-middleware.spec.ts`: `6/6`
     - `auth-token-refresh.spec.ts`: `1/1`
     - `auth-ui-live.spec.ts`: `6/6`
     - `hdr1-live-verification.spec.ts`: `6/6` (Hero contrast >= 9.15:1, CLS 0.0008, Axe A11y pass)
     - `home1-live-verification.spec.ts`: `6/6`
     - `landlord-gateway-api-flow.spec.ts`: `5/5`
     - `landlord-rooms.spec.ts`: `6/6`
     - `list1-verification.spec.ts`: `7/7` (bao gồm click nút carousel `"Ảnh sau"` trên `RoomCard`)
     - `s12-remediation-fixes.spec.ts`: `3/3` (HDR-1.1 carousel, SEARCH-FE.2 phân trang `"Đến trang 2"`, ROOM-01.2 `/vi/hosts/4` -> `"Phan Thanh Bình"`)
     - `smoke.spec.ts`: `53/53` (public + auth + admin + landlord + tenant routes)

### C. Docker Runtime & Live HTTP Probes
- **Docker Health (`docker ps`):** **10/10** container `cozynest_*` (`gateway`, `identity`, `listing`, `booking`, `billing`, `communication`, `admin`, `mysql`, `redis`, `mailpit`) ở trạng thái **`(healthy)`**.
- **Actuator & Swagger Hardening:**
  - `GET :8080..:8086/actuator/health` -> `200` (`7/7` targets).
  - `GET :8080/actuator/env` -> `404`; `GET :8081..:8086/actuator/env` -> `401`; `Tenant GET :8086/actuator/env` -> `403`.
  - `GET :8081/swagger-ui/index.html` -> `404` (`ERR_NOT_FOUND`).
- **RBAC & Security Probes via Gateway (`:8080`):**
  - `/api/v1/landlord/kyc`: Unauth=`401`, Tenant=`200`, Landlord=`200`, Admin=`403`.
  - `/api/v1/moderation/kyc?status=PENDING&page=0&size=1`: Unauth=`401`, Tenant=`403`, Landlord=`403`, Admin=`200`.
  - `Tenant GET /api/v1/landlord/rooms` -> `403`; `Tenant GET /api/v1/landlord/appointments` -> `403`.
  - `GET :8083/api/v1/internal/bookings/room-usage/232` -> `401` (yêu cầu service key); `GET :8080/api/v1/internal/bookings/room-usage/232` -> `404` (ẩn khỏi Gateway).
  - `GET :8080/api/v1/notifications`: Unauth=`401`, User mới đăng ký=`200` (`count=0`), IDOR `PATCH /api/v1/notifications/notif-1/read` -> `404`.
- **Business Data & Flow Probes (`:8080` & `:8025`):**
  - `GET :8080/uploads/avatars/default.png` -> `200` (`1768 bytes`).
  - `GET :8080/api/v1/room-types` -> `200` (`1045 bytes`); `GET :8080/api/v1/locations/hot-areas?limit=8` -> `200` (`1198 bytes`).
  - `GET :8080/api/v1/rooms?page=0&size=1` -> `200` (`roomTypeName="Căn hộ Studio"`, `provinceName="Đà Nẵng"`, `total=69`).
  - `GET :8080/api/v1/hosts/4` -> `200` (`name="Phan Thanh Bình"`, `roomCount=9`, host wrapper không lộ email/PII; lưu ý `rooms[]` bên trong dùng `RoomResponse` có trường `landlordPhone` phục vụ liên hệ tin đăng).
  - User thử nghiệm -> `PUT /api/v1/bookmarks/232` -> `GET /api/v1/bookmarks` (`savedCount=1`) -> `DELETE /api/v1/bookmarks/232`.
  - `POST /api/v1/auth/forgot-password` -> OTP 6 số qua Mailpit `:8025` -> `POST /api/v1/auth/reset-password` -> `POST /api/v1/auth/login` mật khẩu mới thành công.
  - `GET /api/v1/auth/google/status` -> `enabled=false` (`fp=""`); `POST /api/v1/auth/google` (token giả) -> `503 ERR_GOOGLE_NOT_CONFIGURED`.

---

## 3. LỊCH SỬ COMMIT TRÊN NHÁNH `fix/s12-subtask-remediation`

### Backend (`accommodation-service`)
- `86c9f70` — `refactor(listing): separate domain models from JPA and enforce strict ArchUnit rules [INFRA-02]`
- `7304b17` — `chore(infra,db): enforce least-privilege DB users, UTC clock, prod profiles, non-root Dockerfiles and CI checks [INFRA-01][DB-01]`
- `99c177a` — `fix(auth,security): enforce transactional logout, reuse revocation, dynamic JWKS validation and admin security [AUTH-02][NOTIF-API-01]`
- `fb5325c` — `fix(identity,gateway): fix read-write MySQL tx for KYC/profile, serve avatar media via gateway, and add MySQL flow tests [KYC-01][KYC-02][USER-01][AUTH-FLOW-A]`
- `075fda8` — `test(auth): add unit tests for VerifyEmailUseCase OTP expiry, cooldown, and anti-enumeration [AUTH-04]`
- `f9a1445` — `fix(listing,booking,comm): add MySQL search TC, pessimistic lock + outbox worker, internal room-usage check, batch bookmarks, and public host API [SEARCH-BE][APPT-02-BE][ROOM-02][LANDLORD-BE][ROOM-01]`
- `9fc5877` — `chore(docker): wire BOOKING_SERVICE_URL for listing-service internal room-usage check`
- `0003a1a` — `fix(identity,listing): return 404 on missing static resource and populate roomTypeName in RoomDto [INFRA-01][ROOM-01]`
- `a1a8a4b` — `fix(identity): map AccessDeniedException to HTTP 403 ERR_FORBIDDEN instead of 500 [KYC-01-BE][AUTH-02]`

### Frontend (`accommodation-web`)
- `87102af` — `fix(web): fix carousel click stacking, search debounce/pagination, real host profile, and remove fake static data [HDR-1.1][SEARCH-FE][ROOM-01][UI-STATIC-01]`
- `d29d5b5` — `chore(i18n): externalize remaining JSX literal strings and pass eslint --max-warnings=0 [FE-02.3][KYC-01-FE][KYC-02]`
- `44644f6` — `fix(auth): restrict /dashboard/landlord KYC redirect to TENANT role only [AUTH-03][KYC-01-FE]`
- `4c7aa23` — `test(e2e): stabilize Playwright auth helper, RS256 mock routes, and add S1-2 remediation E2E checks [HDR-1.1][SEARCH-FE][ROOM-01]`

---

## 4. KẾT LUẬN & KHUYẾN NGHỊ MỞ KHÓA SPRINT 3

1. **Toàn bộ 5 khoản nợ P0 và các lỗ hổng P1/P2 từ báo cáo audit ban đầu đã được xử lý và kiểm chứng thực tế** trên Docker runtime + MySQL Testcontainers + Playwright E2E (`104/104` passed) + Gradle (`342/342` passed) + Vitest/lint/type-check (`0` errors, `0` warnings).
2. **Các mục giữ trạng thái mở trung thực (không chặn Sprint 3 kỹ thuật nếu biết phạm vi):**
   - `AUTH-03` (`PARTIAL` / External `BLOCKED`): cần cấu hình `GOOGLE_CLIENT_ID` hợp lệ để test đăng nhập Google GIS thật trên môi trường staging/production.
   - `KYC-02.3` (`FaceVerificationScanner`), `KYC-01-FE.2` (upload CCCD 2 mặt), `AUTH-FLOW-A` (full multi-user workflow E2E) & `APPT-02-FE` (E2E nửa đêm GMT+7): đã có Vitest, BE Testcontainers và route smoke tests; nên bổ sung 2–3 kịch bản Playwright tương tác đầy đủ trong giai đoạn hardening/regression.
   - `UI-STATIC-01` & `USER-01`: giữ trạng thái `SPEC_MISSING` đối với checklist Trello chính thức vì không có trong `trello_wbs_cozynest.md`.
3. **Khuyến nghị:** Hệ thống đủ điều kiện nền tảng kiến trúc, bảo mật RBAC/JWKS, DB isolation và kiểm thử tự động để tiến hành triển khai tiếp các nghiệp vụ Sprint 3 (trước khi gộp nhánh nhớ không push tự động khi chưa có xác nhận và cấp cấu hình `GOOGLE_CLIENT_ID` cho staging).
