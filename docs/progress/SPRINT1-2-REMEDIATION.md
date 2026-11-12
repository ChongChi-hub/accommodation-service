# SỔ THEO DÕI KHẮC PHỤC CẤP SUBTASK — SPRINT 1 & 2 (`SPRINT1-2-REMEDIATION`)

> **Nhánh thực thi:** `fix/s12-subtask-remediation` (tách từ `chore/s12-reconcile` trên cả `accommodation-service` và `accommodation-web`)
> **Tài liệu nguồn đối chiếu:**
> - `d:\RIKKEI\THUC_TAP\PROJECT\index.md` (`KẾ HOẠCH TRELLO V2`)
> - `C:\Users\LE THANH CUONG\.gemini\antigravity\brain\a58b5293-0c05-4142-af20-831350631c3d\trello_wbs_cozynest.md`
> - `docs/audit/SPRINT-1-2-SUBTASK-AUDIT.md` (11/11/2026)
> **Nhãn trạng thái chuẩn:** `VERIFIED_DONE` | `IMPLEMENTED_UNVERIFIED` | `PARTIAL` | `NOT_IMPLEMENTED` | `BLOCKED` | `SPEC_MISSING` | `SUPERSEDED`
> **Phân loại yêu cầu:** `OFFICIAL_REQUIREMENT` | `DERIVED_REQUIREMENT` | `SECURITY_GAP` | `TEST_GAP`

---

## 1. BẢNG THEO DÕI SPRINT 1 (15 THẺ / 78 ĐIỂM)

| Card | Subtask ID | Phân loại | Status trước | Status sau | Mô tả vấn đề / hạng mục cần làm | Thay đổi đã làm | Bằng chứng test |
|---|---|---|---|---|---|---|---|
| INFRA-02 | INFRA-02.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | Đồng bộ JDK 23.0.2, `libs.versions.toml` Java 23, `.github/workflows/ci.yml` và Alpine Java 23 Dockerfiles | Cập nhật CI `java-version: '23'` và 5 `Dockerfile` dùng `eclipse-temurin:23-jre-alpine` | `.\gradlew.bat clean test --no-daemon --no-build-cache` BUILD SUCCESSFUL (320 tests, 0 failed) |
| INFRA-02 | INFRA-02.2 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | 31 file trong `listing-service/domain` import JPA/Spring | Tách `DomainPage`/`RoomSearchQuery` và `infrastructure/persistence/{entity,repository,mapper,adapter,specification}` cho `listing-service`; gỡ toàn bộ JPA/Hibernate khỏi `domain` & `application` | `ListingArchitectureTest` + `listing-service:test` (74/74 pass) |
| INFRA-02 | INFRA-02.3 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | `RouteLocator`, `CorsWebFilter` + `DedupeResponseHeader` trên `api-gateway` | Giữ nguyên (sẽ kiểm tra routing `/uploads/**` ở FE-03/FE-01) | `api-gateway:test` (7/7 pass) |
| INFRA-02 | INFRA-02.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `IMPLEMENTED_UNVERIFIED` | Thiếu `application-prod.yml`, `SWAGGER_ENABLED:true` công khai | Tạo `application-prod.yml` cho cả 7 service, đổi default `SWAGGER_ENABLED:false`, gỡ fallback `jwt.secret` | `ProductionProfileNoSeedSecurityTest` pass; chờ rebuild container & probe live `:8081` |
| INFRA-02 | INFRA-02.5 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `CleanDddArchitectureTest` có `allowEmptyShould(true)`; `Listing/Billing/AdminArchitectureTest` thiếu rule cấm JPA/Spring ở `domain` | Gỡ `allowEmptyShould(true)`, bổ sung rule cấm JPA/Spring/`application`/`interfaces` trên `domain` cho Listing/Billing/Admin & `:common` | Full 8-module ArchUnit `--no-build-cache` BUILD SUCCESSFUL |
| INFRA-02 | INFRA-02.6 | SECURITY_GAP | `PARTIAL` | `IMPLEMENTED_UNVERIFIED` | `listing-service` & `communication-service` `SecurityConfig` `permitAll` cho `/actuator/**` | Chỉ `permitAll` `/actuator/health/**`, khóa `/actuator/**` với `hasRole("ADMIN")` trên cả 5 service | Đã sửa code; chờ test Security Matrix & live probe sau rebuild |
| AUTH-01 | AUTH-01.1..4 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Email/SĐT `+84`, BCrypt(12), timing defense, 5-fail lock, rate limit | Giữ nguyên, kiểm tra regression | Sẽ chạy regression toàn suite |
| AUTH-02 | AUTH-02.1 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Cặp khóa RSA `RS256` & `/.well-known/jwks.json` | Giữ nguyên, kiểm tra regression | Sẽ chạy regression |
| AUTH-02 | AUTH-02.2 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `@Transactional(noRollbackFor = IdentityDomainException.class)` + `REQUIRES_NEW` giữ nguyên revoke khi phát hiện reuse | `RefreshTokenUseCase`, `RefreshTokenRepositoryAdapter`, `RefreshTokenReuseTestcontainersTest` | MySQL 9.0 Testcontainers test pass (`skipped=0`) |
| AUTH-02 | AUTH-02.3 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `LogoutUseCase` `@Transactional` thu hồi refresh token theo token/userId khi logout | `LogoutUseCase`, `AuthController`, `RefreshTokenReuseTestcontainersTest` | MySQL 9.0 Testcontainers test pass (`skipped=0`) |
| AUTH-02 | AUTH-02.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `JwksTokenValidator` fetch JWKS động (5s cooldown, fail-closed `kid`/`iss`/`aud`); wiring 5 service + `admin-service` `SecurityConfig`/`JwtAuthFilter` | `JwksTokenValidator`, 5 `JwtAuthFilter`, `admin/SecurityConfig` | `DownstreamJwksSecurityMatrixTest` (8/8), `AdminSecurityMatrixTest`, 4 downstream security suites pass |
| DB-01 | DB-01.1..2 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | ERD 6 miền + Flyway V1 6 schemas | Giữ nguyên | Sẽ kiểm tra lại runtime |
| DB-01 | DB-01.3 | OFFICIAL_REQUIREMENT | `NOT_IMPLEMENTED` | `VERIFIED_DONE` | MySQL chỉ có `root`; compose gắn `DB_USER: root` cho 6 service | Tạo `docker/mysql/01-init-service-users.sql` cấp quyền riêng cho 6 user, cập nhật `docker-compose.yml` & `.env.example` | Đã apply vào `cozynest_mysql` runtime: `identity_user` đọc `cozynest_identity.users` (25) OK, đọc `cozynest_listing.rooms` -> `ERROR 1142` |
| DB-01 | DB-01.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `IdentityApplication` & `BookingApplication` hardcode `Clock` `Asia/Ho_Chi_Minh`; MySQL compose `TZ: Asia/Ho_Chi_Minh` | Đổi `Clock.systemUTC()` tại `IdentityApplication`, `BookingApplication`, `TZ: UTC` trong `docker-compose.yml` & 5 Dockerfiles | `common:test` `TimezoneBoundaryTest` (part of 16/16 pass) |
| DB-01 | DB-01.5 | OFFICIAL_REQUIREMENT | `PARTIAL` | `VERIFIED_DONE` | `DatabaseIsolationTestcontainers` + vận hành user tách biệt thật | Xác minh `DatabaseIsolationTestcontainersTest` & live MySQL `cozynest_mysql` cross-schema `ERROR 1142` | `identity-service:test` (161/161 pass) + MySQL runtime check |
| DB-02 | DB-02.1..4 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | 34 tỉnh + xã/phường 2 cấp, UTF-8 sạch `??`/`U+FFFD` | Giữ nguyên, chạy lại `check:seed-text` | Sẽ chạy lại |
| AUTH-04 | AUTH-04.1..3 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | OTP `SecureRandom`, JavaMail `@Async`, anti-enumeration | Giữ nguyên | Sẽ chạy regression |
| AUTH-04 | AUTH-04.4 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Xác thực lại `mail-check.ps1` + E2E Mailpit live 8/8 | Đang xử lý | Chờ chạy live Mailpit E2E |
| FE-01 | FE-01.1, FE-01.5 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Clean Architecture 4 tầng + bộ công cụ test | Giữ nguyên | Sẽ chạy lại |
| FE-01 | FE-01.2 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `container.ts` còn wire 6 `Static*Repository` | Đang xử lý (cùng UI-STATIC-01) | Chờ audit & thay thế repo có BE |
| FE-01 | FE-01.3 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `e2e/auth-rbac-middleware.spec.ts` fail live (selector + luồng RBAC) | Đang xử lý | Chờ Playwright pass |
| FE-01 | FE-01.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `/uploads/avatars/default.png` 404 qua Gateway `:8080` | Đang xử lý | Chờ probe Gateway `:8080` |
| AUTH-03 | AUTH-03.1 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Flyway `V6` allow null phone/password | Giữ nguyên | Sẽ chạy regression |
| AUTH-03 | AUTH-03.2..4 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Google GIS + Redis nonce (live GIS bị chặn nếu thiếu Google token thật -> bổ sung Mock/WireMock JWKS integration test + tài liệu blocker nếu còn) | Đang xử lý | Chờ test bổ sung |
| AUTH-FLOW-A | AUTH-FLOW-A.1, A.3 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | `V7` `landlord_profiles`, mở quyền `TENANT` nộp KYC | Giữ nguyên | Sẽ chạy regression |
| AUTH-FLOW-A | AUTH-FLOW-A.2, A.4 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Kiểm chứng toàn luồng live `TENANT (AWAITING_KYC) -> KYC -> approve -> LANDLORD` & `/become-landlord` | Đang xử lý | Chờ live API/E2E test |
| FE-02 | FE-02.1..2, 2.4..5 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Tokens, Dark Mode, UI Kit, static pages | Giữ nguyên | Sẽ chạy regression |
| FE-02 | FE-02.3 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | 737 ESLint warnings `i18next/no-literal-string` trên 99 files | Đang xử lý | Chờ `eslint` 0 warnings |
| AUTH-UI | AUTH-UI.1, UI.3 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | 2-column AuthShell/BrandPanel + 72-byte UTF-8 validation | Giữ nguyên | Sẽ chạy lại Vitest |
| AUTH-UI | AUTH-UI.2 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | E2E live quên mật khẩu 2 bước `/forgot-password` + `OtpInput` | Đang xử lý | Chờ Playwright/E2E live |
| AUTH-UI | AUTH-UI.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `auth-rbac-middleware.spec.ts` fail selector email & `auth-ui-live.spec.ts` mutate DB thật | Đang xử lý | Chờ Playwright isolated pass |
| KYC-01-FE | KYC-01-FE.1..2 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Verify live `/dashboard/*/profile`, `/security`, `/dashboard/landlord/kyc` | Đang xử lý | Chờ Playwright E2E |
| KYC-01-FE | KYC-01-FE.3 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `KycGateBanner.tsx` còn chuỗi cứng tiếng Việt chưa i18n | Đang xử lý | Chờ ESLint 0 warnings & render test |
| INFRA-01 | INFRA-01.1..3 | OFFICIAL_REQUIREMENT | `PARTIAL` | `IMPLEMENTED_UNVERIFIED` | Healthchecks docker-compose; remove hardcode dev secrets fallback; align CI | Thêm Redis/Gateway healthchecks, user service DB & `HEALTHCHECK` trong 5 Dockerfiles, bỏ fallback `jwt.secret`, chuẩn hóa `.github/workflows/ci.yml` BE/FE | CI/compose/Dockerfile đã cập nhật; chờ rebuild Docker stack & `docker ps` |
| KYC-01-BE | KYC-01-BE.1, BE.3 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Flyway `V4` + chặn tự duyệt | Giữ nguyên | Sẽ chạy regression |
| KYC-01-BE | KYC-01-BE.2 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Kiểm chứng live 401/403/200 cho `/me/kyc/documents` & `/moderation/kyc/**` | Đang xử lý | Chờ Integration/Live Matrix |
| KYC-01-BE | KYC-01-BE.4 | SECURITY_GAP | `PARTIAL` | `PARTIAL` | Bật `@EnableScheduling` & cấu hình an toàn cho `KycImageCleanupScheduler` | Đang xử lý | Chờ Scheduler test |
| KYC-02 | KYC-02.1..2 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | `V5` + challenges/submissions AES-256-GCM + EXIF strip | Giữ nguyên | Sẽ chạy regression |
| KYC-02 | KYC-02.3 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Verify `FaceVerificationScanner.tsx` với mock MediaStream + consent | Đang xử lý | Chờ Vitest/Playwright |
| KYC-02 | KYC-02.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `FaceModerationUseCase.getFaceDetail()` ghi `face_access_logs` trong `@Transactional(readOnly = true)`; `KycModerationModal.tsx` còn literal string | Đang xử lý | Chờ MySQL test + ESLint 0 warnings |
| KYC-02 | KYC-02.5 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `IdentityApplication` thiếu `@EnableScheduling` + `face.cleanup.enabled: false`; `FaceSeedDataLoader` thiếu `@Profile` guard | Đang xử lý | Chờ Cleanup test + Profile test |

---

## 2. BẢNG THEO DÕI SPRINT 2 (15 THẺ / 63 ĐIỂM) + `USER-01`

| Card | Subtask ID | Phân loại | Status trước | Status sau | Mô tả vấn đề / hạng mục cần làm | Thay đổi đã làm | Bằng chứng test |
|---|---|---|---|---|---|---|---|
| SEARCH-BE | SEARCH-BE.1..2 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | `V9` + `RoomController`/`RoomSpecificationBuilder` không dấu, `size <= 50` | Giữ nguyên | Sẽ chạy regression |
| SEARCH-BE | SEARCH-BE.3 | OFFICIAL_REQUIREMENT | `NOT_IMPLEMENTED` | `NOT_IMPLEMENTED` | Chưa có Testcontainers MySQL thật test từng filter đơn, AND amenities, phân trang ổn định | Đang xử lý | Chờ MySQL Testcontainers test |
| SEARCH-BE | SEARCH-BE.4 | SECURITY_GAP | `PARTIAL` | `IMPLEMENTED_UNVERIFIED` | `SeedAssetLoader` thiếu `@Profile("!prod")` | Đã gắn `@Profile("!prod & !production")` lên `SeedAssetLoader` | Chờ xác minh trong `listing-service` test |
| APPT-02-BE | APPT-02-BE.1..2 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Domain thuần + state machine + PhoneMasker | Giữ nguyên | Sẽ chạy regression |
| APPT-02-BE | APPT-02-BE.3 | OFFICIAL_REQUIREMENT | `NOT_IMPLEMENTED` | `NOT_IMPLEMENTED` | `LandlordAppointmentService:119` gọi query không khóa thay vì `findOverlappingConfirmedForUpdate`; chưa có Testcontainers 2-thread test | Đang xử lý | Chờ MySQL Testcontainers concurrency test |
| APPT-02-BE | APPT-02-BE.4 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | Chưa có Outbox worker đọc `findUnprocessed()` gửi sang `communication-service` | Đang xử lý | Chờ Outbox integration test |
| ROOM-02 | ROOM-02.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | Bổ sung `@PutMapping("/api/v1/bookmarks/{roomId}")`, giới hạn 200 phòng/user, bỏ N+1 trong `getMyBookmarks()` | Đang xử lý | Chờ unit + integration test |
| ROOM-02 | ROOM-02.2 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Verify live Bookmark UI ở Card/Detail/Saved Rooms | Đang xử lý | Chờ Playwright/Vitest |
| LANDLORD-BE | LANDLORD-BE.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `RoomUsageAdapter` còn TODO gọi `booking-service` check hợp đồng/lịch hẹn hiệu lực trước khi xóa phòng | Đang xử lý | Chờ adapter + contract test |
| NOTIF-API-01 | NOTIF-API-01.1 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Đã chuyển `communication-service` sang JWKS validator + khóa `/actuator/**` | `communication/.../JwtAuthFilter`, `SecurityConfig` | `CommunicationSecurityMatrixTest` (4/4) pass; chờ Docker rebuild + live HTTP probe |
| NOTIF-API-01 | NOTIF-API-01.2 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | `HttpNotificationRepository` + UI chuông & trang notifications | Giữ nguyên | Sẽ chạy regression |
| LISTING-HDR-BE | LISTING-HDR-BE.1 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Verify live `/api/v1/room-types` & `/api/v1/locations/hot-areas?limit=8` | Đang xử lý | Chờ live HTTP probe |
| SEARCH-FE | SEARCH-FE.1 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | URL sync + Error/Empty states | Giữ nguyên | Sẽ chạy regression |
| SEARCH-FE | SEARCH-FE.2 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | Thiếu `300ms` debounce trên `RoomFilterBar`/`useRooms` & thiếu UI phân trang ở `RoomsCatalogClient` | Đang xử lý | Chờ Vitest + Playwright |
| LANDLORD-FE | LANDLORD-FE.1 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Gỡ literal strings ở `KycGateBanner` & verify live `/dashboard/landlord/rooms`, `/new`, `/[id]/edit` | Đang xử lý | Chờ ESLint + Playwright |
| FE-03 | FE-03.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `/uploads/avatars/default.png` 404 qua Gateway `:8080` | Đang xử lý | Chờ live Gateway `:8080` 200 OK |
| HDR-1 | HDR-1.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `RoomCard.tsx:131` overlay `z-10` chặn click `"Ảnh sau"` (làm `list1-verification` fail); `AreaDropdown.tsx:33-36` lệch base URL trên trang danh mục | Đang xử lý | Chờ `list1-verification` 7/7 pass |
| UI-HOME-01 | UI-HOME-01.1 | OFFICIAL_REQUIREMENT | `VERIFIED_DONE` | `VERIFIED_DONE` | Hero, 8 khu vực, `HomeBanners` `aspect-21/9`, Hero < 250KB, 6/6 E2E | Giữ nguyên | Sẽ chạy regression `home1` |
| UI-STATIC-01 | UI-STATIC-01.1 | `SPEC_MISSING` | `SPEC_MISSING` | `SPEC_MISSING` | Không có checklist chính thức trong `trello_wbs_cozynest.md` (chỉ có thẻ 5 điểm ở `index.md`) | Giữ nguyên `SPEC_MISSING` cho spec gốc | N/A |
| UI-STATIC-01 | UI-STATIC-01.2 | DERIVED_REQUIREMENT | `PARTIAL` | `PARTIAL` | Kiểm tra 6 `Static*Repository` trong `container.ts`; thay repo có BE thật (`Badge`/Notification/...), chặn dữ liệu giả im lặng ở các module chưa có BE (chờ Sprint 3/4) | Đang xử lý | Chờ static audit + tests |
| ROOM-01 | ROOM-01.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | `RoomDetailService:67,74` gán `provinceName` = `provinceCode`, `roomTypeName` = `roomTypeCode` | Đang xử lý | Chờ unit + live JSON probe |
| ROOM-01 | ROOM-01.2 | OFFICIAL_REQUIREMENT | `NOT_IMPLEMENTED` | `NOT_IMPLEMENTED` | `/hosts/[id]/page.tsx` dùng dữ liệu giả tĩnh (`Trần Minh Tuấn`, `4.9`, `useRooms().slice(0,4)`) | Đang xử lý (tạo API host công khai an toàn ở BE + nối FE thật) | Chờ BE + FE + live probe |
| APPT-02-FE | APPT-02-FE.1 | OFFICIAL_REQUIREMENT | `IMPLEMENTED_UNVERIFIED` | `IMPLEMENTED_UNVERIFIED` | Verify live 4 chế độ xem, 3 modal, múi giờ `Asia/Ho_Chi_Minh` ranh giới nửa đêm | Đang xử lý | Chờ Vitest/Playwright |
| E2E-S2 | E2E-S2.1 | OFFICIAL_REQUIREMENT | `PARTIAL` | `PARTIAL` | Fix `playwright.config.ts` ổn định, `storageState`/fixtures độc lập không mutate dev DB, toàn bộ Playwright E2E + Vitest + Gradle xanh | Đang xử lý | Chờ full suite pass |
| USER-01 | USER-01.1 | `SPEC_MISSING` | `SPEC_MISSING` | `SPEC_MISSING` | Không nằm trong bảng 30 thẻ `index.md` và không có trong `trello_wbs_cozynest.md` | Giữ nguyên `SPEC_MISSING` | N/A |
| USER-01 | USER-01.2 | DERIVED_REQUIREMENT | `PARTIAL` | `PARTIAL` | `UserProfileUseCase:54-60` ghi DB trong `@Transactional(readOnly = true)` + avatar fallback 404 qua Gateway `:8080` | Đang xử lý | Chờ MySQL test + Gateway 200 OK |

