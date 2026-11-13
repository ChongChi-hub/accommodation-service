# KIỂM KÊ CHI TIẾT 28 THẺ SPRINT 3 — PHẦN 1 (THẺ 1 - 14)

> Dự án: Tổ Ấm (CozyNest) | Ngày: 13/11/2026
> Quy tắc: Mỗi khẳng định gắn nhãn nguồn [CHẠY LẠI HÔM NAY], [ĐỌC TỪ MÃ], hoặc [THEO BÁO CÁO].
> Trạng thái: DEMO-OK, SANDBOX, PARTIAL, NOT-VERIFIED, BLOCKED, NOT-DONE.

---

### 1. [LEDGER-01] Sổ Cái Kế Toán Kép (Double-Entry Ledger)
- **a. Phụ trách:** Kế hoạch: Cường (8 điểm) | Git: catim07 [ĐỌC TỪ MÃ: git log]
- **b. Mục tiêu nghiệp vụ:** Cung cấp lõi kế toán kép cho mọi nghiệp vụ tiền tệ trong nền tảng; bảo đảm cân đối bất biến tổng Nợ = tổng Có. Đối tượng: Kế toán, Admin, Hệ thống tự động.
- **c. Chức năng đã có:**
  - Hạch toán bút toán kép đa tài khoản (DEBIT = CREDIT) [PARTIAL]
  - Khóa bi quan có thứ tự (deterministic SELECT ... FOR UPDATE) chống deadlock [PARTIAL]
  - Trigger MySQL 8.4 LTS chặn tuyệt đối UPDATE và DELETE trên `ledger_entries` và `ledger_lines` [PARTIAL]
  - Quy tắc tiền số nguyên VND (BIGINT), không dùng số thực [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/transactions` (tab Sổ cái) [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/ledger/entries` (ADMIN), `GET /api/v1/billing/ledger/accounts/{id}/balance` (ADMIN, SYSTEM), service: `billing-service`.
- **f. Dữ liệu:** Bảng `ledger_accounts`, `ledger_entries`, `ledger_lines`, `ledger_daily_balances`. Migration: `V1__init_billing_schema.sql`, `V3__ledger_immutability_triggers.sql`, `V6__sprint3_billing_audit_hardening.sql`. Ràng buộc: `FOREIGN KEY`, `CHECK (amount > 0)`.
- **g. Công nghệ:** Spring Boot 4.1.2, MySQL 8.4 LTS InnoDB, ArchUnit 1.5.1 [ĐỌC TỪ MÃ: build.gradle].
- **h. Quy tắc:** ArchUnit cấm ghi sổ cái ngoài `LedgerService`; bút toán đảo có lý do; số dư chụp theo ngày.
- **i. Kiểm thử:** `LedgerServiceTest`, `LedgerHardeningTest`, `LedgerImmutabilityTestcontainersTest` (32 tests billing PASS [CHẠY LẠI HÔM NAY]).
- **j. Commit chính:** `f90c8b9`, `5dfc78a` [ĐỌC TỪ MÃ: git log].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Chờ review 2 người có Cường trước khi kích hoạt tiền thật.

---

### 2. [LEDGER-02] Toàn Vẹn Sổ Cái Hằng Ngày & Xuất Báo Cáo
- **a. Phụ trách:** Kế hoạch: Dev 2 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tự động đối soát tổng Nợ = tổng Có mỗi ngày và cung cấp cơ chế duyệt 2 người (maker-checker) cho bút toán điều chỉnh. Đối tượng: Kế toán, Admin.
- **c. Chức năng đã có:**
  - Job kiểm tra cân đối số dư sổ cái định kỳ [PARTIAL]
  - API đề xuất và duyệt bút toán điều chỉnh (Maker-Checker khác người tạo) [PARTIAL]
  - Xuất sao kê CSV UTF-8 BOM, trung hòa công thức Excel (`=`, `+`, `-`, `@`) [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/transactions` (tab Báo cáo & Điều chỉnh) [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/maker-checker/proposals` (MAKER), `POST /api/v1/billing/maker-checker/proposals/{id}/approve` (CHECKER), `GET /api/v1/billing/ledger/export/csv` (ADMIN).
- **f. Dữ liệu:** Bảng `maker_checker_proposals`, `ledger_daily_balances`. Migration: `V4__sprint3_fintech_hardening.sql`.
- **g. Công nghệ:** Spring Batch / Scheduled, OpenCSV UTF-8 BOM [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Cấm tự duyệt yêu cầu do chính mình tạo; giới hạn xuất <= 5000 dòng / <= 366 ngày; chặn formula injection.
- **i. Kiểm thử:** `MakerCheckerControllerTest`, `Wave2FintechSecurityAndAuditTest` (tích hợp).
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Kênh cảnh báo Telegram/Slack thực tế đang là logger stub.

---

### 3. [PAY-01] Cổng Thanh Toán & Webhook HMAC
- **a. Phụ trách:** Kế hoạch: Cường (8 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tiếp nhận thanh toán chuyển khoản ngân hàng qua VietQR, thẩm thực webhook HMAC-SHA256, chống replay attack. Đối tượng: Người thuê, Hệ thống đối tác.
- **c. Chức năng đã có:**
  - Khởi tạo Intent thanh toán VietQR động kèm mã tham chiếu duy nhất [SANDBOX]
  - Webhook xác thực chữ ký HMAC-SHA256 qua header `X-Webhook-Signature` [SANDBOX]
  - Chống replay attack bằng sliding window timestamp + nonce [SANDBOX]
  - Kill-switch tắt cổng tức thời khi gặp sự cố (`fintech.gateway.enabled`) [PARTIAL]
- **d. Giao diện:** `/dashboard/tenant/payments`, modal thanh toán cọc/hóa đơn [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/payments/intents` (TENANT), `POST /api/v1/billing/payments/webhook` (PUBLIC - IP allowlist + HMAC), `POST /api/v1/billing/payments/sandbox/simulate-webhook` (DEV).
- **f. Dữ liệu:** Bảng `payment_intents`, `payment_webhook_logs`. Migration: `V2__sprint3_billing_fintech.sql`.
- **g. Công nghệ:** Java Cryptography (HmacSHA256), Spring WebMVC [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Intent có TTL 15 phút; thanh toán trễ/sai nội dung đưa vào hàng khớp thủ công `MANUAL_RECON_QUEUE`.
- **i. Kiểm thử:** `PaymentHardeningTest`, `PaymentControllerTest`.
- **j. Commit chính:** `f90c8b9`, `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Cờ `fintech.mode=SANDBOX_ONLY`, chưa kết nối cổng trung gian thương mại thật.

---

### 4. [PAY-02] Đối Soát Hằng Đêm (Reconciliation)
- **a. Phụ trách:** Kế hoạch: Cường (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tự động đối chiếu giao dịch giữa cổng thanh toán và sổ cái nội bộ, phát hiện giao dịch lệch (MISMATCH). Đối tượng: Kế toán, Admin.
- **c. Chức năng đã có:**
  - Job đối soát tự động quét bản ghi trong ngày [SANDBOX]
  - Phân loại trạng thái đối soát: `MATCHED`, `MISMATCH_AMOUNT`, `MISSING_INTERNAL`, `MISSING_EXTERNAL` [SANDBOX]
  - Báo cáo đối soát lưu vết audit log [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/transactions` (tab Đối soát) [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/reconciliations/run` (ADMIN), `GET /api/v1/billing/reconciliations/reports` (ADMIN).
- **f. Dữ liệu:** Bảng `reconciliation_runs`, `reconciliation_discrepancies`. Migration: `V2__sprint3_billing_fintech.sql`.
- **g. Công nghệ:** Spring Scheduling, JPA [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Giao dịch lệch tạo ticket xử lý có SLA; đối soát cả chiều nạp (deposit), hoàn (refund), và chi (payout).
- **i. Kiểm thử:** `Wave2FintechSecurityAndAuditTest` [ĐỌC TỪ MÃ].
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Dữ liệu đối soát đang đối chiếu với mock statement của môi trường test.

---

### 5. [ESCROW-01] Giữ Cọc (Escrow Hold), Hoàn Hủy & Rút Tiền
- **a. Phụ trách:** Kế hoạch: Dev 2 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Giam giữ tiền đặt cọc trong thời gian chờ nhận phòng; giải ngân cho chủ trọ khi cả hai bên xác nhận dọn vào; hỗ trợ rút tiền (Payout) có 2FA. Đối tượng: Người thuê, Chủ trọ, Admin.
- **c. Chức năng đã có:**
  - Tạo khoản giữ cọc (Escrow Hold) gắn với hợp đồng thuê [SANDBOX]
  - Xác nhận hai phía nhận phòng (`confirmMoveIn`) trước khi giải ngân [SANDBOX]
  - Chặn rút tiền / giải ngân khi hợp đồng có trạng thái tranh chấp (`DISPUTED`) [PARTIAL]
  - Yêu cầu xác thực OTP/2FA khi tạo lệnh rút tiền (Payout) [PARTIAL]
- **d. Giao diện:** `/dashboard/tenant/deposits`, `/dashboard/landlord/deposits`, `/dashboard/landlord/payouts` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/escrow/{id}/move-in` (TENANT/LANDLORD), `POST /api/v1/billing/payouts` (LANDLORD), `POST /api/v1/billing/payouts/{id}/approve` (ADMIN).
- **f. Dữ liệu:** Bảng `escrow_holds`, `payout_requests`. Migration: `V2__sprint3_billing_fintech.sql`, `V4__sprint3_fintech_hardening.sql`.
- **g. Công nghệ:** Spring Boot 4.1.2, Spring Security [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Tên chủ tài khoản ngân hàng rút tiền phải khớp 100% với tên trên eKYC CCCD; cờ `money.custody.enabled=false` mặc định theo ADR-008 Model A.
- **i. Kiểm thử:** `EscrowAndRiskHardeningTest`, `Wave2FintechSecurityAndAuditTest`.
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Bị khóa sau cờ `money.custody.enabled=false` để tuân thủ pháp lý NĐ 52/2024.

---

### 6. [FIN-RISK-01] Quản Trị Rủi Ro Giao Dịch
- **a. Phụ trách:** Kế hoạch: Dev 2 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Thiết lập hạn mức giao dịch, phát hiện hành vi rút tiền dồn dập, hỗ trợ đóng băng tài khoản/ví khi nghi vấn gian lận. Đối tượng: Admin, Risk Officer.
- **c. Chức năng đã có:**
  - Cấu hình hạn mức giao dịch đơn lẻ và hạn mức ngày từ `platform_settings` [PARTIAL]
  - Kiểm tra quy tắc tốc độ (velocity rule): phát hiện nhiều giao dịch trong cửa sổ ngắn [PARTIAL]
  - Đóng băng tức thời ví hoặc khoản giữ cọc qua API Admin [PARTIAL]
  - Danh sách đen (blocklist) số tài khoản ngân hàng và thiết bị [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/settings` (tab Rủi ro), `/dashboard/admin/transactions` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/risk/freeze-account` (ADMIN), `GET /api/v1/billing/risk/alerts` (ADMIN), `POST /api/v1/billing/risk/blocklist` (ADMIN).
- **f. Dữ liệu:** Bảng `risk_rules`, `risk_alerts`, `payment_blocklist`. Migration: `V4__sprint3_fintech_hardening.sql`.
- **g. Công nghệ:** Spring Boot, Redis sliding window counter [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Chủ trọ mới có hạn mức rút thấp hơn; tài khoản đổi STK bị áp thời gian chờ (cooling period 24h).
- **i. Kiểm thử:** `EscrowAndRiskHardeningTest` [ĐỌC TỪ MÃ].
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Cảnh báo SLA cho Admin hiện lưu DB, chưa tích hợp push notification thời gian thực.

---

### 7. [REFUND-01] Hoàn Tiền Ra Ngân Hàng (Refund Engine)
- **a. Phụ trách:** Kế hoạch: Dev 4 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cho phép hoàn cọc khi hủy hợp đồng hoặc hủy yêu cầu thuê; hỗ trợ hoàn toàn phần hoặc từng phần. Đối tượng: Người thuê, Admin.
- **c. Chức năng đã có:**
  - Tạo yêu cầu hoàn tiền với trạng thái `REQUESTED -> PROCESSING -> DONE | FAILED` [SANDBOX]
  - Kiểm tra ràng buộc tổng số tiền hoàn không vượt quá số tiền đã thu thực tế [PARTIAL]
  - Thử lại an toàn với `idempotency-key` [PARTIAL]
  - Duyệt 2 người khi số tiền hoàn vượt ngưỡng cấu hình [PARTIAL]
- **d. Giao diện:** `/dashboard/tenant/deposits` (nút Yêu cầu hoàn), `/dashboard/admin/transactions` (tab Hoàn tiền) [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/refunds` (TENANT/ADMIN), `POST /api/v1/billing/refunds/{id}/retry` (ADMIN).
- **f. Dữ liệu:** Bảng `refund_requests`, `refund_audit_logs`. Migration: `V5__sprint3_meter_invoice_refund.sql`.
- **g. Công nghệ:** Spring Boot 4.1.2, JPA [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Bắt buộc xác thực OTP người nhận; hoàn về nguồn hoặc tài khoản đã đối soát tên eKYC.
- **i. Kiểm thử:** `MeterInvoiceRefundDashboardTest` [ĐỌC TỪ MÃ].
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Lệnh chi hoàn tiền thực tế đang mô phỏng trong sandbox.

---

### 8. [SAGA-01] Giao Dịch Xuyên Service (Rental Lifecycle Saga)
- **a. Phụ trách:** Kế hoạch: Cường (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Điều phối chu trình thuê phòng qua 4 dịch vụ (booking, listing, billing, communication) với cơ chế bù trừ tự động (compensation). Đối tượng: Toàn hệ thống.
- **c. Chức năng đã có:**
  - Saga Orchestrator: `RentalSagaOrchestrator` phối hợp các bước [PARTIAL]
  - Giữ phòng tạm thời (`/internal/rooms/{id}/hold`) và tự động giải phóng khi hủy/hết hạn (`/internal/rooms/{id}/release`) [PARTIAL]
  - Chuyển phòng sang `RENTED` khi hợp đồng được ký hoàn tất và cọc thành công [PARTIAL]
  - Bù trừ hoàn cọc hoặc mở khóa phòng khi phát sinh lỗi giữa chừng [PARTIAL]
- **d. Giao diện:** Hiển thị trạng thái tiến trình từng bước trên `/dashboard/tenant/rental-requests` và `/dashboard/landlord/contracts` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/booking/rental-requests` (TENANT), `POST /internal/rooms/{id}/hold` (INTERNAL), `POST /internal/rooms/{id}/release` (INTERNAL).
- **f. Dữ liệu:** Bảng `saga_instances`, `saga_steps`, `rental_requests`. Migration: `V3__sprint3_contracts_amendments_and_saga.sql`.
- **g. Công nghệ:** Spring Boot, Transactional Outbox, RabbitMQ 3.13.7 [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Mọi bước gọi liên service đều có timeout 3s, retry và idempotency key; saga treo quá hạn chuyển sang hàng can thiệp thủ công.
- **i. Kiểm thử:** `RentalSagaOrchestratorTest` (43 tests booking PASS [CHẠY LẠI HÔM NAY]).
- **j. Commit chính:** `2d4f28a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Chaos testing đầy đủ trên môi trường staging chưa thực hiện.

---

### 9. [APPT-RENT-BE] API Đặt Lịch Hẹn & Đặt Thuê (Backend)
- **a. Phụ trách:** Kế hoạch: Dev 2 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tiếp nhận lịch xem phòng và yêu cầu thuê trọ, kiểm tra số người ở tối đa và trạng thái phòng trống. Đối tượng: Người thuê, Chủ trọ.
- **c. Chức năng đã có:**
  - API đặt lịch xem phòng với kiểm tra trùng giờ [PARTIAL]
  - API gửi yêu cầu thuê phòng kèm kiểm tra `maxOccupants` và `availableFrom` [PARTIAL]
  - Ghi nhận sự kiện vào `appointment_events_outbox` và `booking_events_outbox` [PARTIAL]
  - Yêu cầu số điện thoại đã xác minh trước khi gửi yêu cầu thuê [PARTIAL]
- **d. Giao diện:** Tương tác với frontend route `/rooms/[slug]`, `/dashboard/tenant/appointments` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/booking/appointments` (TENANT), `POST /api/v1/booking/rental-requests` (TENANT), `GET /api/v1/landlord/appointments` (LANDLORD).
- **f. Dữ liệu:** Bảng `viewing_appointments`, `rental_requests`, `appointment_events_outbox`. Migration: `V2__viewing_appointments_schema.sql`, `V4__sprint3_booking_hardening.sql`.
- **g. Công nghệ:** Spring Data JPA, Hibernate, MySQL 8.4 LTS [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Khóa bi quan phòng trống khi tạo yêu cầu; che số điện thoại người thuê khi lịch hẹn chưa CONFIRMED.
- **i. Kiểm thử:** `RentalRequestServiceTest`, `TenantAppointmentServiceTest`, `ViewingAppointmentDomainTest`.
- **j. Commit chính:** `6dc895c` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: SMS thông báo đang qua Mailpit stub.

---

### 10. [APPT-RENT-FE] Giao Diện Đặt Lịch, Cọc & Yêu Cầu Thuê (Frontend)
- **a. Phụ trách:** Kế hoạch: Dev 3 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cung cấp trải nghiệm đặt lịch xem và tạo yêu cầu thuê phòng mượt mà, trực quan trên cả desktop và di động. Đối tượng: Người thuê.
- **c. Chức năng đã có:**
  - Modal đặt lịch hẹn chọn ngày giờ với validation [PARTIAL]
  - Modal gửi yêu cầu thuê phòng hiển thị số tiền cọc và chính sách hoàn hủy [PARTIAL]
  - Chống bấm đúp (disable nút submit + client UUID idempotency key) [PARTIAL]
  - Xuất file lịch hẹn định dạng iCalendar (.ics) [PARTIAL]
- **d. Giao diện:** `/rooms/[slug]`, `/dashboard/tenant/appointments`, `/dashboard/tenant/rental-requests` [ĐỌC TỪ MÃ]
- **e. API:** Gọi qua Next.js BFF proxy `/api/v1/booking/appointments` và `/api/v1/booking/rental-requests`.
- **f. Dữ liệu:** Client State (Zustand), Server State (React Query).
- **g. Công nghệ:** Next.js 16.2 App Router, TailwindCSS 4, Lucide React, i18n Next-Intl [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Không hardcode dữ liệu giả; thông điệp đa ngữ vi/en đầy đủ; token lưu HttpOnly cookie.
- **i. Kiểm thử:** Vitest component tests (`auth-ui.test.tsx`, `hdr1.test.tsx`), TypeScript type check PASS [CHẠY LẠI HÔM NAY].
- **j. Commit chính:** `71e8726` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Kiểm thử thực tế trên WebView Zalo và iOS Safari chưa thực hiện thủ công trên máy thật.

---

### 11. [CONTRACT-BE] API Hợp Đồng Điện Tử, Ký OTP & PDF (Backend)
- **a. Phụ trách:** Kế hoạch: Dev 2 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tự động phát sinh hợp đồng thuê nhà điện tử, hỗ trợ ký OTP cho cả hai bên, xuất bản chụp bất biến kèm mã băm SHA-256 và sinh file PDF. Đối tượng: Chủ trọ, Người thuê, Admin.
- **c. Chức năng đã có:**
  - Sinh bản hợp đồng điện tử tự động từ dữ liệu phòng và đơn thuê [PARTIAL]
  - Ký OTP 6 chữ số cho chủ trọ và người thuê kèm kiểm tra hết hạn [SANDBOX]
  - Sinh mã băm SHA-256 trên nội dung toàn văn hợp đồng [PARTIAL]
  - Lưu vết nhật ký ký số (IP, User-Agent, mã băm OTP, thời điểm UTC) [PARTIAL]
  - Khóa sửa đổi hợp đồng sau khi cả hai bên đã ký [PARTIAL]
- **d. Giao diện:** Phục vụ `/dashboard/landlord/contracts/[id]`, `/dashboard/tenant/contracts/[id]` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/booking/contracts` (LANDLORD), `POST /api/v1/booking/contracts/{id}/sign-landlord` (LANDLORD), `POST /api/v1/booking/contracts/{id}/sign-tenant` (TENANT), `GET /api/v1/booking/contracts/{id}/pdf` (AUTH).
- **f. Dữ liệu:** Bảng `contracts`, `contract_signatures`, `contract_snapshots`. Migration: `V3__sprint3_contracts_amendments_and_saga.sql`, `V5__sprint3_booking_audit_hardening.sql`.
- **g. Công nghệ:** OpenPDF / iText, Spring Security, SHA-256 MessageDigest [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Chỉ lưu hash mã OTP, tuyệt đối không lưu OTP thô; tài liệu sau khi ký là bất biến (immutable).
- **i. Kiểm thử:** `ContractAndAmendmentHardeningTest` (43 tests booking PASS [CHẠY LẠI HÔM NAY]).
- **j. Commit chính:** `d1b376f` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Mẫu hợp đồng chuẩn đang chờ văn bản duyệt của luật sư theo LEGAL-02.

---

### 12. [CONTRACT-02] Phụ Lục Hợp Đồng, Gia Hạn & Đồng Thuê
- **a. Phụ trách:** Kế hoạch: Dev 2 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cho phép ký phụ lục điều chỉnh giá thuê, gia hạn thời hạn hợp đồng và khai báo danh sách người ở cùng (đồng thuê). Đối tượng: Chủ trọ, Người thuê.
- **c. Chức năng đã có:**
  - Thêm phụ lục hợp đồng (`contract_amendments`) với quy trình ký OTP độc lập [PARTIAL]
  - Cập nhật giá thuê có hiệu lực từ kỳ tiếp theo mà không làm sai lệch hợp đồng gốc [PARTIAL]
  - Quản lý danh sách người ở cùng (`contract_occupants`) phục vụ khai báo tạm trú [PARTIAL]
  - Tự động nhắc nhở trước khi hợp đồng hết hạn 30, 15, 7 ngày [PARTIAL]
- **d. Giao diện:** Component `ContractAmendmentsSection`, `ContractOccupantsSection` trên trang chi tiết hợp đồng [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/booking/contracts/{id}/amendments` (LANDLORD), `POST /api/v1/booking/contracts/{id}/occupants` (TENANT/LANDLORD).
- **f. Dữ liệu:** Bảng `contract_amendments`, `contract_occupants`. Migration: `V3__sprint3_contracts_amendments_and_saga.sql`.
- **g. Công nghệ:** Spring Data JPA, Hibernate Envers / Audit [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Phụ lục phải được cả 2 bên ký OTP mới phát sinh hiệu lực; thông tin người ở cùng lưu số định danh được che.
- **i. Kiểm thử:** `ContractAndAmendmentHardeningTest` [ĐỌC TỪ MÃ].
- **j. Commit chính:** `d1b376f` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Tính năng tự động gửi tin nhắn nhắc hết hạn đang chạy mô phỏng log.

---

### 13. [CONTRACT-FE] Giao Diện Hợp Đồng, Ký OTP & Bàn Giao (Frontend)
- **a. Phụ trách:** Kế hoạch: Dev 3 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cung cấp giao diện đọc toàn văn hợp đồng, nhập mã OTP xác nhận ký, hiển thị mã băm chứng chỉ số và hỗ trợ in/tải PDF. Đối tượng: Chủ trọ, Người thuê.
- **c. Chức năng đã có:**
  - Xem toàn văn nội dung hợp đồng chia theo các điều khoản chuẩn hóa [PARTIAL]
  - Modal nhập mã OTP 6 số với bộ đếm ngược thời gian và nút gửi lại mã [PARTIAL]
  - Hiển thị badge bằng chứng số: SHA-256 Hash, thời gian ký, IP xác thực [PARTIAL]
  - Hỗ trợ in trực tiếp với CSS `@media print` được tối ưu [PARTIAL]
- **d. Giao diện:** `/dashboard/landlord/contracts`, `/dashboard/landlord/contracts/[id]`, `/dashboard/tenant/contracts`, `/dashboard/tenant/contracts/[id]` [ĐỌC TỪ MÃ]
- **e. API:** Gọi qua BFF proxy `/api/v1/booking/contracts/*`.
- **f. Dữ liệu:** React Hook Form + Zod schema validation.
- **g. Công nghệ:** Next.js 16.2 App Router, TailwindCSS 4, CSS Print styling [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Khóa nút ký khi đang gửi request; hiển thị thông báo lỗi rõ ràng khi nhập sai hoặc hết hạn OTP.
- **i. Kiểm thử:** Vitest tests, Next.js build PASS 98 pages [CHẠY LẠI HÔM NAY].
- **j. Commit chính:** `a95c9cf` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Cần kiểm tra độ tương thích in ấn trên nhiều loại trình duyệt khác nhau.

---

### 14. [METER-01] Chỉ Số Điện Nước & Ảnh Công Tơ
- **a. Phụ trách:** Kế hoạch: Dev 4 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cho phép chủ trọ chốt số điện, số nước định kỳ theo từng phòng; phát hiện chỉ số bất thường và lưu trữ ảnh chụp công tơ. Đối tượng: Chủ trọ, Người thuê.
- **c. Chức năng đã có:**
  - Ghi nhận chỉ số điện (kWh), nước ($m^3$) cũ và mới kèm ngày chốt [PARTIAL]
  - Tự động tính sản lượng tiêu thụ và cảnh báo khi chỉ số tăng đột biến (> 200% so với kỳ trước) [PARTIAL]
  - Khóa không cho phép sửa đổi chỉ số sau khi hóa đơn tương ứng đã được phát hành (`ISSUED`) [PARTIAL]
  - Lưu lịch sử và lý do điều chỉnh chỉ số (`reading_amendment_history`) [PARTIAL]
- **d. Giao diện:** `/dashboard/landlord/invoices` (tab Ghi chỉ số), `/dashboard/tenant/invoices` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/utility-readings` (LANDLORD), `GET /api/v1/billing/utility-readings/room/{roomId}` (LANDLORD/TENANT).
- **f. Dữ liệu:** Bảng `utility_readings`, `utility_reading_amendments`. Migration: `V5__sprint3_meter_invoice_refund.sql`.
- **g. Công nghệ:** Spring Boot, JPA, Image upload storage [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Chỉ số mới bắt buộc >= chỉ số cũ; mọi sửa đổi sau khi lưu phải có lý do tối thiểu 10 ký tự.
- **i. Kiểm thử:** `MeterInvoiceRefundDashboardTest` (32 tests billing PASS [CHẠY LẠI HÔM NAY]).
- **j. Commit chính:** `e25dd41` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Tính năng bắt buộc tải ảnh công tơ hiện có thể bật/tắt qua cờ cấu hình.

