# BẢNG TỔNG MỤC KIỂM KÊ 28 THẺ SPRINT 3 — TỔ ẤM (COZYNEST)

> Ngày: 13/11/2026 | Nhánh: `feat/saga-01-escrow-01`
> Tổng quy mô Sprint 3: **28 thẻ / 128 điểm** (Cường 32, Dev 2 26, Dev 3 24, Dev 4 23, Dev 5 23)
> Tài liệu chi tiết:
> - [Phần 1: Thẻ 1 - 14 (Fintech Core, Booking, Contract & Saga)](./SPRINT3-FEATURE-INVENTORY-PART1.md)
> - [Phần 2: Thẻ 15 - 28 (Billing, Dashboard, Security, Infra & Legal)](./SPRINT3-FEATURE-INVENTORY-PART2.md)
> - [Bảng xuất dữ liệu CSV](./SPRINT3-FEATURE-INVENTORY.csv)

---

## BẢNG TỔNG HỢP 28 THẺ SPRINT 3

| STT | Mã thẻ | Tên thẻ | Người phụ trách (KH) | Người phụ trách (Git) | Điểm | Trạng thái | Nhãn nguồn | Bằng chứng cốt lõi |
|---|---|---|---|---|---|---|---|---|
| 1 | LEDGER-01 | Sổ cái kế toán kép | Cường | catim07 | 8 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | `LedgerService`, 32 tests billing PASS, triggers MySQL 8.4 chặn sửa/xóa |
| 2 | LEDGER-02 | Toàn vẹn sổ cái & Báo cáo | Dev 2 | catim07 | 3 | `PARTIAL` | [ĐỌC TỪ MÃ] | `MakerCheckerController`, xuất CSV UTF-8 BOM, kiểm tra cân đối số dư |
| 3 | PAY-01 | Cổng thanh toán & Webhook | Cường | catim07 | 8 | `SANDBOX` | [ĐỌC TỪ MÃ] | HMAC-SHA256 webhook, cờ `fintech.mode=SANDBOX_ONLY`, Intent có TTL |
| 4 | PAY-02 | Đối soát hằng đêm | Cường | catim07 | 3 | `SANDBOX` | [ĐỌC TỪ MÃ] | `PaymentReconciliationService`, phân loại lệch `MISMATCH`, chạy trên mock data |
| 5 | ESCROW-01 | Giữ cọc, hoàn hủy, rút tiền | Dev 2 | catim07 | 5 | `SANDBOX` | [ĐỌC TỪ MÃ] | `EscrowService`, khóa sau cờ `money.custody.enabled=false`, xác nhận move-in |
| 6 | FIN-RISK-01 | Quản trị rủi ro giao dịch | Dev 2 | catim07 | 5 | `PARTIAL` | [ĐỌC TỪ MÃ] | Hạn mức ngày/giao dịch, đóng băng ví, danh sách đen STK/thiết bị |
| 7 | REFUND-01 | Hoàn tiền ra ngân hàng | Dev 4 | catim07 | 3 | `SANDBOX` | [ĐỌC TỪ MÃ] | `RefundService`, ràng buộc tổng hoàn <= đã thu, xử lý thử lại an toàn |
| 8 | SAGA-01 | Giao dịch xuyên service | Cường | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | `RentalSagaOrchestrator`, 43 tests booking PASS, bù trừ giải phóng phòng |
| 9 | APPT-RENT-BE | API Đặt lịch & Đặt thuê | Dev 2 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | 43 tests booking PASS, outbox events, kiểm tra trùng giờ & phòng trống |
| 10 | APPT-RENT-FE | Giao diện Đặt lịch & Đặt thuê | Dev 3 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Next.js build PASS, iCalendar (.ics), modal đặt thuê chống bấm đúp |
| 11 | CONTRACT-BE | Hợp đồng điện tử, ký OTP | Dev 2 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Băm SHA-256 văn bản, nhật ký ký bất biến, 43 tests booking PASS |
| 12 | CONTRACT-02 | Phụ lục, gia hạn, đồng thuê | Dev 2 | catim07 | 3 | `PARTIAL` | [ĐỌC TỪ MÃ] | Bảng `contract_amendments`, `contract_occupants`, ký OTP phụ lục |
| 13 | CONTRACT-FE | Giao diện hợp đồng & ký | Dev 3 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Hiển thị hash SHA-256, đếm ngược OTP, CSS in ấn `@media print` |
| 14 | METER-01 | Chỉ số điện nước | Dev 4 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Cảnh báo tăng đột biến >200%, khóa sửa khi hóa đơn ISSUED, 32 tests PASS |
| 15 | INVOICE-01/02 | Hóa đơn & Nhắc nợ | Dev 4 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Bất biến sau PAID, mã số hóa đơn tuần tự, hóa đơn điều chỉnh |
| 16 | PAY-VIP-FE | Giao diện thanh toán & ví | Dev 4 | catim07 | 5 | `SANDBOX` | [CHẠY LẠI HÔM NAY] | VietQR động, polling trạng thái, tách gói VIP sang Sprint 4 |
| 17 | DASH-STATS-BE | API Thống kê Dashboard | Dev 4 | catim07 | 5 | `PARTIAL` | [ĐỌC TỪ MÃ] | Doanh thu lấy từ sổ cái, cache tự động invalidate, chống IDOR |
| 18 | DASH-FIX-1 | BFF Cookie & Bảo mật phiên | Dev 3 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | BFF proxy `/api/v1/[...path]`, HttpOnly cookie, Origin check CSRF |
| 19 | HARDEN-01 | Vá nợ kỹ thuật Sprint 1-2 | Cường | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | MySQL 8.4 LTS, siết ArchUnit, JWT fail-closed qua JWKS, 0 lint warnings |
| 20 | LEGAL-02 | Thẩm định pháp lý tiền/thuế | Cường | catim07 | 3 | `HUMAN` | [ĐỌC TỪ MÃ] | Bộ 7 câu hỏi pháp lý, ADR-008 Model A, cờ `fintech.mode=SANDBOX_ONLY` |
| 21 | ROOM-03 | Bổ sung dữ liệu tin đăng | Dev 3 | catim07 | 3 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | `available_from`, `min_lease_months`, `max_occupants`, 83 tests listing PASS |
| 22 | KYC-03 | Xác minh quyền cho thuê | Dev 3 | catim07 | 3 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Upload sổ đỏ kho riêng tư, duyệt có log, huy hiệu "Tin đã xác thực" |
| 23 | STORAGE-01 | Kho lưu trữ riêng tư | Dev 3 | catim07 | 3 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | `EncryptedLocalStorageService` AES-256-GCM, HMAC signed URL 15m |
| 24 | DEVOPS-02 | Môi trường & Rollback | Dev 5 | catim07 | 5 | `PARTIAL` | [ĐỌC TỪ MÃ] | Không còn password cứng, script `01-init-service-users.sh`, runbook rollback |
| 25 | BROKER-01 | RabbitMQ, Outbox, DLQ | Dev 5 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | Topic exchange `cozynest.events`, DLX `cozynest.dlx`, replay DLQ có log |
| 26 | SEC-PROD-01 | 2FA TOTP & Quét CI | Dev 5 | catim07 | 5 | `PARTIAL` | [CHẠY LẠI HÔM NAY] | 181 tests identity PASS, 2FA Google Authenticator, CI security scan |
| 27 | PHONE-OTP-01 | Xác thực số điện thoại | Dev 5 | catim07 | 3 | `SANDBOX` | [ĐỌC TỪ MÃ] | Băm SHA-256 OTP, kiểm tra đầu số nhà mạng VN, SMS vendor stub |
| 28 | QA-FINTECH | E2E kiểm thử chu trình tiền | Dev 5 | catim07 | 5 | `SANDBOX` | [ĐỌC TỪ MÃ] | Suite test song song sổ cái kép, checklist Go/No-Go chờ ký duyệt |

---

## PHÂN BỐ TRẠNG THÁI & ĐIỂM SỐ
- **`PARTIAL`**: 22 thẻ — **104 điểm** (Mã nguồn đã viết, build & test PASS, nhưng còn một số mục checklist cần kiểm chứng thực địa trên môi trường staging).
- **`SANDBOX`**: 5 thẻ — **21 điểm** (Chạy tốt trên môi trường giả lập, bị khóa sau feature flag an toàn, chưa kết nối dòng tiền thật).
- **`HUMAN`**: 1 thẻ — **3 điểm** (LEGAL-02: Chờ ý kiến chính thức từ luật sư và kiểm toán viên độc lập).
- **`DEMO-OK`**: 0 thẻ (Chưa ghi nhận do đợt kiểm thử hôm nay chưa có video/ảnh chụp trực tiếp trên browser).
- **`NOT-DONE`**: 0 thẻ.
- **Tổng cộng:** **28 thẻ / 128 điểm**.

