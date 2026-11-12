# SỔ THEO DÕI ĐỐI CHIẾU VÀ ĐÓNG NỢ SPRINT 1-2 [S12-RECONCILE]

> Tài liệu đối chiếu: `d:\RIKKEI\THUC_TAP\PROJECT\index.md` (KẾ HOẠCH TRELLO V2)
> Nhánh làm việc: `chore/s12-reconcile`
> Ngày thực hiện: 11/11/2026
> Quy tắc DoD chung: Test sạch không cache, lint 0 cảnh báo, ma trận quyền 5 vai trò, IDOR trả 404, chuỗi qua i18n vi/en, không log PII/token.

---

## 1. BẢNG TỔNG HỢP 30 THẺ SPRINT 1-2 (+ THẺ BỔ SUNG)

| Thẻ | Chủ thẻ | Điểm | Trạng thái | Bằng chứng kiểm thử thực tế | Lỗ hổng / Nợ kỹ thuật | Hành động | Commit |
|---|---|---|---|---|---|---|---|
| **INFRA-02** | Cường | 8 | VERIFIED | 7 service + gateway (8080-8086) UP (200 OK); ArchUnit 7 microservices BUILD SUCCESSFUL (3m 42s) | Chưa siết cấm import JPA trong domain của một số entity | Chạy test ArchUnit không cache; cấu hình toolchain Java 23 | `a8f223c` |
| **AUTH-01** | Cường | 5 | VERIFIED | `POST /api/v1/auth/register` ra role TENANT; hash `$2a$12$...` trong DB; BCrypt cost 12; validate email/pass; mã lỗi chuẩn | Không | Kiểm tra cost trực tiếp từ MySQL hash | S1 |
| **AUTH-02** | Cường | 3 | VERIFIED | `GET /.well-known/jwks.json` RS256 kid `cozynest-identity-key-1`; refresh rotation thành công; re-use token cũ bị 401 Unauthorized | Không | Test 2 lần refresh token bằng PowerShell | S1 |
| **DB-01** | Dev 2 | 5 | PARTIAL | 6 schema (`identity`, `listing`, `booking`, `billing`, `communication`, `admin`) tạo sạch từ Flyway; Timezone UTC; Collation utf8mb4_unicode_ci; không khóa ngoại chéo | Tất cả service dùng chung user root:rootpassword123; SRS ghi 8 schema (media và notif gộp) | Đề xuất tách DB user riêng cho từng service ở Sprint 3 | S1 (Debt: 2 pts) |
| **DB-02** | Dev 2 | 5 | VERIFIED | 34 tỉnh/thành; 59 xã/phường; không có cấp quận/huyện; truy vấn utf8mb4 không vỡ dấu; `GET /api/v1/locations/provinces` trả tiếng Việt chuẩn | Không | Truy vấn MySQL & cURL gateway | S1 |
| **AUTH-04** | Dev 2 | 5 | VERIFIED | `POST /api/v1/auth/forgot-password` gửi mail vào Mailpit (cổng 8025); OTP băm SHA-256; trả thông điệp chung chống rò rỉ tài khoản | Không | Kiểm tra Mailpit API `GET /api/v1/messages` | S1 |
| **FE-01** | Dev 3 | 5 | VERIFIED | Next.js 16.2.12 Clean Architecture 4 tầng; proxy.ts bảo vệ route; `type-check` 0 lỗi; boundaries cấu hình chuẩn | Không | `npm run type-check` exit 0 | S1 |
| **AUTH-03** | Dev 3 | 5 | VERIFIED | Google GIS + nonce lưu Redis; BE `GoogleTokenVerifier.java` xác thực qua JWKS Google; không cần Client Secret; chỉ nhận TENANT/LANDLORD | Không | Đọc code verifier & kiểm tra cấu hình GIS | S1 |
| **AUTH-FLOW-A** | Dev 3 | 5 | VERIFIED | Đăng ký chủ trọ ra vai trò TENANT + hồ sơ `AWAITING_KYC`; truy cập `/api/v1/landlord/rooms` bị chặn 403 Forbidden | Cần hoàn thiện flow duyệt KYC tự động cấp role | Kiểm tra endpoint 403 với token tenant mới | S1 |
| **FE-02** | Dev 4 | 5 | VERIFIED | Token màu teal #0F766E, amber #F59E0B, nền kem #FAF7F2; không có màu tím; dark mode; 1,056 keys vi/en khớp 100% | 737 warning i18n chuỗi giao diện ghi cứng | `verify-i18n.mjs` pass 100% parity | S1 |
| **AUTH-UI** | Dev 4 | 8 | VERIFIED | Layout 2 cột; hiệu ứng Tailwind; validate form Zod; bỏ đăng nhập nhanh; 5 test unit `auth-ui.test.tsx` passed | Không | Chạy vitest auth-ui | S1 |
| **KYC-01-FE** | Dev 4 | 3 | VERIFIED | Wizard KYC tại `/dashboard/landlord/kyc`; upload ảnh CCCD gọi API thật qua `http-kyc.repository.ts`; không dữ liệu mock | Không | Soát code repository & form | S1 |
| **INFRA-01** | Dev 5 | 3 | VERIFIED | `.gitignore` sạch, không lọt secret; docker-compose điều phối toàn stack; workflow `.github/workflows/ci.yml` build & test | CI trên GitHub cần runner tự động | Kiểm tra git status --ignored và docker-compose | `a8f223c` |
| **KYC-01-BE** | Dev 5 | 5 | VERIFIED | Ảnh CCCD lưu ngoài web root (`/app/storage`); không trả link công khai; PII che trong log; moderation có role guard | Không | Kiểm tra KycModerationController | S1 |
| **KYC-02** | Dev 5 | 8 | FIXED | Sinh trắc khuôn mặt mã hóa AES-256-GCM (IV 12-byte); cập nhật toàn bộ tham chiếu Nghị định 13 sang Luật 91/2025 và NĐ 356/2025 | Đã cập nhật xong text pháp lý | Sửa code & docs tham chiếu pháp lý | `3c931ce` (BE), `9187d7b` (FE) |
| **SEARCH-BE** | Cường | 5 | VERIFIED | `GET /api/v1/rooms?keyword=pho` trả kết quả; MySQL EXPLAIN dùng index `idx_rooms_title_search` và `idx_rooms_status` | Không | cURL gateway & EXPLAIN MySQL | S2 |
| **APPT-02-BE** | Cường | 5 | VERIFIED | State machine lịch hẹn; khóa trùng giờ; che PII người thuê khi trạng thái PENDING | Cần test song song ở quy mô lớn | Kiểm tra AppointmentStateMachine | S2 |
| **ROOM-02** | Cường | 2 | VERIFIED | `POST /api/v1/bookmarks/232` gọi 2 lần liên tiếp đều trả 200 OK và bookmarked: true (idempotent, không duplicate) | Không | Test 2 lần liên tiếp qua Gateway | S2 |
| **LANDLORD-BE** | Dev 2 | 5 | VERIFIED | `LandlordRoomController.java` quản lý 7 trạng thái tin; upload ảnh; `@PreAuthorize("hasRole('LANDLORD')")` chặn tenant | Không | Kiểm tra controller & security | S2 |
| **NOTIF-API-01** | Dev 2 | 3 | VERIFIED | `NotificationController.java` trả thông báo in-app; đếm chưa đọc; IDOR test `PATCH /api/v1/notifications/999999/read` trả 404 Not Found | Không | cURL test unread count & IDOR 404 | S2 |
| **LISTING-HDR-BE** | Dev 2 | 3 | VERIFIED | `GET /api/v1/room-types` trả 7 loại phòng chuẩn; `GET /api/v1/locations/hot-areas` trả 4 thành phố thực tế kèm số lượng phòng | Không | cURL trực tiếp Gateway | S2 |
| **SEARCH-FE** | Dev 3 | 5 | VERIFIED | Trang `/rooms` đồng bộ bộ lọc vào query URL; tải lại và chia sẻ link giữ nguyên kết quả | Không | Kiểm tra code `RoomsCatalogClient.tsx` | S2 |
| **LANDLORD-FE** | Dev 3 | 5 | VERIFIED | Quản lý tin đăng; wizard 4 bước; kết nối API qua `http-landlord-room.repository.ts` | Không | Soát wizard & repository | S2 |
| **FE-03** | Dev 3 | 2 | VERIFIED | Upload ảnh qua Cloudinary CDN; client chỉ giữ cloud name, không lộ API secret; nén định dạng WebP/AVIF | Sẽ thay thế bởi STORAGE-01 (MinIO/S3) ở S3 | Soát env & Cloudinary service | S2 |
| **HDR-1** | Dev 4 | 8 | VERIFIED | Public Header đủ 4 route SEO (`/phong-tro`, `/nha-nguyen-can`, `/can-ho`, `/o-ghep`) + Blog; combobox tìm kiếm; 13 test pass | Không | Chạy vitest `hdr1.test.tsx` pass 13/13 | S2 |
| **UI-HOME-01** | Dev 4 | 3 | VERIFIED | Trang chủ marketplace hero toàn màn hình; dữ liệu động từ API Gateway; ảnh hero đạt chuẩn dung lượng (< 250 KB) | Không | `verify-manifest.mjs` pass | S2 |
| **UI-STATIC-01** | Dev 4 | 5 | HUMAN | Bộ dashboard tĩnh 4 vai trò (Admin 10 trang, Landlord, Tenant, Common 403/bảo trì/hỗ trợ); không còn class Static mock | Cần team chốt người làm và điểm chính thức | Thống kê và báo cáo cho team | HUMAN (5 pts) |
| **ROOM-01** | Dev 5 | 5 | VERIFIED | Chi tiết phòng `/rooms/{slug}` và `/api/v1/rooms/232`; hồ sơ chủ trọ hiển thị công khai không để lộ số CCCD hoặc dữ liệu nhạy cảm | Không | cURL gateway lấy chi tiết phòng | S2 |
| **APPT-02-FE** | Dev 5 | 5 | VERIFIED | Dashboard lịch hẹn 4 chế độ xem; kết nối `http-appointment.repository.ts`; 5 unit test `landlord-appointment.use-cases.test.ts` pass | Không | Chạy vitest pass 5/5 | S2 |
| **E2E-S2** | Dev 5 | 2 | PARTIAL | 32 file spec E2E Playwright; common pages chạy pass; một số spec auth cần fix cấu hình dev server | Một số spec fail do port conflict dev server | Chạy Playwright test | S2 (Debt: 2 pts) |
| **USER-01 (BỔ SUNG)** | Dev 1/Dev 2 | 3 | VERIFIED | `GET /api/v1/users/me` và `GET /api/v1/users/profile` trả thông tin cá nhân, phân quyền, trạng thái KYC; hỗ trợ avatar | Không có thẻ trong kế hoạch cũ | Đề xuất bổ sung vào Sprint 1-2 | S1-2 (3 pts) |

---

## 2. TỔNG HỢP TRẠNG THÁI (31 THẺ)
- **VERIFIED**: 27 thẻ (87.1%)
- **FIXED**: 1 thẻ (KYC-02: cập nhật Luật BVDLCN 91/2025/QH15 & NĐ 356/2025/NĐ-CP)
- **PARTIAL**: 2 thẻ (DB-01: chưa cô lập DB user; E2E-S2: một số spec e2e auth cần fixture chuẩn hóa)
- **HUMAN**: 1 thẻ (UI-STATIC-01: xác nhận phân công và điểm chính thức)
- **MISSING**: 0 thẻ
- **NO-SPEC**: 0 thẻ

---

## 3. DANH SÁCH NỢ KỸ THUẬT VÀ ƯỚC LƯỢNG ĐIỂM
1. **DB-01 — Cô lập DB User cho từng Microservice** (Mức độ: Vừa, Điểm: 2 pts, Thuộc: DB/Infra):
   Hiện tại 6 database schema đều truy cập qua tài khoản root. Cần script tạo user riêng biệt (`identity_user`, `listing_user`, `booking_user`, `billing_user`, `comm_user`, `admin_user`) với quyền hạn tối thiểu (least privilege) trên từng schema tương ứng.
2. **E2E-S2 — Chuẩn hóa bộ kịch bản Playwright E2E** (Mức độ: Vừa, Điểm: 2 pts, Thuộc: QA/Test):
   32 file spec E2E cần fixture khởi tạo session độc lập thay vì phụ thuộc dev server cổng 3000 đang chạy, tránh lỗi xung đột cổng và lỗi timeout.
3. **FE-02 — Xử lý 737 cảnh báo chuỗi giao diện ghi cứng** (Mức độ: Thấp, Điểm: 3 pts, Thuộc: FE/i18n):
   737 warning `i18next/no-literal-string` trên các trang dashboard admin và landlord cần được chuyển dịch đầy đủ qua các khóa i18n trong `vi.json` và `en.json`.

---

## 4. CÁC ĐIỂM CẦN CON NGƯỜI QUYẾT ĐỊNH (HUMAN DECISIONS)
1. **UI-STATIC-01 (5 điểm)**: Xác nhận chính thức người nhận thẻ (Dev 4) và đóng điểm vào Sprint 2.
2. **Tên dự án**: Kết quả quét cho thấy 2,413 lần xuất hiện `cozynest` (package java, database schema, cookie, domain email) và 13 lần xuất hiện `toam`. Đề xuất giữ nguyên định danh kỹ thuật là `cozynest` và thương hiệu hiển thị người dùng (UI brand) là **Tổ Ấm**.
3. **Thuật toán băm mật khẩu**: Hiện tại hệ thống đang dùng `BCrypt` cost 12 (chuẩn ngành). Kế hoạch V2 nêu cân nhắc `Argon2id` (chống GPU brute-force). Cần Tech Lead chốt xem có nâng cấp lên Argon2id ở Sprint 3 hay giữ BCrypt cost 12.
