# TIẾN ĐỘ THỰC HIỆN [PAYMENT-HARDEN-VIETQR]

- **Nhánh git:** `feat/payment-harden`
- **Mục tiêu:** Vá triệt để lỗ hổng tự xác nhận thanh toán (L1), chặn tạo mã VietQR cho khoản đã thanh toán (L2), chuẩn hóa chuyển khoản VietQR, quản lý trạng thái khoản cọc và thanh toán, đảm bảo tính bất biến của sổ cái và an toàn ký quỹ Escrow.
- **Quy tắc tuân thủ:** Không phá code, conventional commit theo từng pha, file tối đa 250 dòng, không emoji, i18n vi/en 100%, bảo mật nghiêm ngặt.

---

## 1. Baseline Kiểm Thử Trước Khi Sửa (Commit `3db7708` / `9d2f389`)

### Frontend (`accommodation-web`):
- `npx tsc --noEmit`: 0 lỗi.
- `npx eslint`: 0 lỗi (0 warnings).
- `npm test`: **22/22 test files passed, 101/101 tests passed**.
- i18n parity: 100% (2265 keys vi/en).
- Internal links verification: 96/96 links hợp lệ.
- Manifest/Asset check: 224/224 hình ảnh hợp lệ.
- Build production: **202 routes generated thành công**.

### Backend (`accommodation-service`):
- `:billing-service:test`: **40/40 tests passed** (`BUILD SUCCESSFUL in 18s`).
- `:communication-service:test`: **40/40 tests passed** (`BUILD SUCCESSFUL in 19s`).
- Docker containers: 12/12 container healthy (`cozynest_mysql`, `cozynest_billing`, `cozynest_communication`, ...).
- Database backup: Đã sao lưu toàn bộ cơ sở dữ liệu sang `d:\backup_cozynest_all_databases_pre_payment_harden.sql`.

---

## 2. Tiến Độ Từng Pha

| Pha | Nội Dung | Trạng Thái | Minh Chứng / Ghi Chú |
| :--- | :--- | :--- | :--- |
| **Pha 0** | Tái hiện & Tìm nguyên nhân gốc (L1, L2, cờ custody, kiểm kê endpoint) | **DONE-VERIFIED** | Xác định chính xác lỗ hổng client tự kích hoạt PAID và thiếu kiểm tra obligation status |
| **Pha 1** | Vá khẩn cấp: Đóng đường tự xác nhận, chặn tạo mã cho khoản đã xong (409), kiểm tra cờ | **DONE-VERIFIED** | `PaymentObligationValidator`, `ObligationAlreadyPaidException`, chặn `409` khi đã cọc, giới hạn webhook simulation role ADMIN |
| **Pha 2** | Máy trạng thái khoản & intent, TTL hết hạn, phục hồi QR khi rời trang | **DONE-VERIFIED** | Hỗ trợ trạng thái `SUBMITTED`, countdown timer 15:00 theo server, gọi `GET /billing/payments/pending` khôi phục QR |
| **Pha 3** | Nguồn xác nhận tin cậy (Provider, Webhook, Xác nhận thủ công Landlord/Admin, Hàng chờ) | **DONE-VERIFIED** | `confirmPaymentByLandlordOrAdmin` yêu cầu `bankTxCode` và `actualAmount >= deposit`, ghi sổ kép với `MANUAL-FT...` |
| **Pha 4** | Hoàn thiện Giao diện Thanh toán VietQR & Trang quản lý cọc của Chủ trọ | **DONE-VERIFIED** | Bảng đối soát `LandlordSubmittedDepositsTable` và `LandlordEscrowTable`, nút 'Chốt đã nhận tiền' & 'Báo chưa nhận' |
| **Pha 5** | Nghiệm thu bảo mật, hồi quy toàn diện, tài liệu DECISIONS.md | **DONE-VERIFIED** | E2E Playwright test passed 100%, 4 ảnh chụp màn hình thực tế, MySQL kiểm tra ghi sổ và cọc chính xác |

---

## 3. Kết Quả Kiểm Thử Sau Khi Hoàn Thành

### Frontend (`accommodation-web`):
- `npx tsc --noEmit`: 0 lỗi.
- `npx eslint --max-warnings=0`: 0 lỗi, 0 warnings.
- `npm test`: 22 test files passed, 101/101 unit tests passed, 100% i18n parity (2265 keys).
- Production build: Đã build Docker image `accommodation-service-web:latest` thành công, container chạy `Next.js 16.4.6` khỏe mạnh.

### Backend (`accommodation-service`):
- `:billing-service:test`: 41/41 tests passed (thêm `PaymentSecurityHardeningPhase1Test`).
- Flyway migrations: `V8__allow_submitted_in_payment_intent_status.sql` và `V11__allow_awaiting_deposit_in_contracts.sql` đã áp dụng.
- Tất cả các file code đều tuân thủ nghiêm ngặt định mức tối đa 250 dòng (từ 8 đến 250 dòng).
