# KIỂM KÊ CHI TIẾT 28 THẺ SPRINT 3 — PHẦN 2 (THẺ 15 - 28)

> Dự án: Tổ Ấm (CozyNest) | Ngày: 13/11/2026
> Quy tắc: Mỗi khẳng định gắn nhãn nguồn [CHẠY LẠI HÔM NAY], [ĐỌC TỪ MÃ], hoặc [THEO BÁO CÁO].
> Trạng thái: DEMO-OK, SANDBOX, PARTIAL, NOT-VERIFIED, BLOCKED, NOT-DONE.

---

### 15. [INVOICE-01/02] Hóa Đơn Hằng Tháng, Biên Lai & Nhắc Nợ
- **a. Phụ trách:** Kế hoạch: Dev 4 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Tự động tổng hợp tiền phòng, tiền điện, tiền nước và phí dịch vụ thành hóa đơn định kỳ; hỗ trợ thanh toán từng phần, hóa đơn điều chỉnh và sinh PDF biên lai. Đối tượng: Chủ trọ, Người thuê.
- **c. Chức năng đã có:**
  - Sinh hóa đơn theo kỳ với tính toán chi tiết từng khoản mục tiền [PARTIAL]
  - Đảm bảo tính bất biến sau khi hóa đơn chuyển sang trạng thái `PAID` [PARTIAL]
  - Hỗ trợ hóa đơn điều chỉnh (Credit/Debit Adjustment Note) thay vì sửa hóa đơn gốc [PARTIAL]
  - Sinh mã số hóa đơn tuần tự định dạng `INV-YYYYMM-XXXXX` [PARTIAL]
  - Hỗ trợ xem trước (preview dry-run) trước khi phát hành chính thức [PARTIAL]
- **d. Giao diện:** `/dashboard/landlord/invoices`, `/dashboard/tenant/invoices`, `/dashboard/tenant/invoices/[id]` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/billing/invoices` (LANDLORD), `GET /api/v1/billing/invoices/{id}` (AUTH), `POST /api/v1/billing/invoices/{id}/pay` (TENANT), `GET /api/v1/billing/invoices/{id}/pdf` (AUTH).
- **f. Dữ liệu:** Bảng `invoices`, `invoice_items`, `invoice_adjustments`. Migration: `V1__init_billing_schema.sql`, `V5__sprint3_meter_invoice_refund.sql`.
- **g. Công nghệ:** Spring Data JPA, OpenPDF [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Ràng buộc UNIQUE `(contract_id, billing_period)` chống sinh hóa đơn trùng kỳ; tiền thừa tự động kết chuyển khấu trừ kỳ sau.
- **i. Kiểm thử:** `InvoiceDomainTest`, `MeterInvoiceRefundDashboardTest`.
- **j. Commit chính:** `e25dd41` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Chức năng gửi email đính kèm PDF hóa đơn đang ghi log môi trường dev.

---

### 16. [PAY-VIP-FE] Giao Diện Thanh Toán & Ví (Frontend)
- **a. Phụ trách:** Kế hoạch: Dev 4 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cung cấp giao diện thanh toán trực quan qua mã QR, hiển thị trạng thái giao dịch thời gian thực (chờ, thành công, thất bại, quá hạn). Đối tượng: Người thuê, Chủ trọ.
- **c. Chức năng đã có:**
  - Trang hiển thị mã VietQR động kèm thông tin chuyển khoản và thời gian đếm ngược [PARTIAL]
  - Tự động polling kiểm tra trạng thái thanh toán và thông báo khi nhận tiền [PARTIAL]
  - Xử lý trạng thái khi mã QR hết hạn hoặc rớt mạng giữa chừng [PARTIAL]
  - Tách bạch phần mua gói VIP sang Sprint 4 (`PRICING-CONV-01`) theo đúng kế hoạch V2 [PARTIAL]
- **d. Giao diện:** `/dashboard/tenant/payments`, `/dashboard/tenant/deposits`, `/dashboard/landlord/deposits` [ĐỌC TỪ MÃ]
- **e. API:** Gọi qua BFF proxy `/api/v1/billing/payments/*`.
- **f. Dữ liệu:** React Query polling + Zustand state management.
- **g. Công nghệ:** Next.js 16.2 App Router, TailwindCSS 4, Lucide React [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Hiển thị rõ số tiền, nội dung chuyển khoản bắt buộc và hướng dẫn không sửa nội dung.
- **i. Kiểm thử:** Vitest tests, build pass [CHẠY LẠI HÔM NAY].
- **j. Commit chính:** `03a1044` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Dùng endpoint giả lập thanh toán sandbox để demo.

---

### 17. [DASH-STATS-BE] API Thống Kê Dashboard & Doanh Thu (Backend)
- **a. Phụ trách:** Kế hoạch: Dev 4 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cung cấp số liệu tài chính chính xác (doanh thu thực tế, công nợ phải thu, tỷ lệ lấp đầy) trích xuất trực tiếp từ sổ cái kế toán. Đối tượng: Chủ trọ, Admin.
- **c. Chức năng đã có:**
  - API thống kê tài chính chủ trọ (tổng doanh thu tháng, số tiền đang giữ cọc, nợ đọng) [PARTIAL]
  - Số liệu tiền lấy trực tiếp từ bảng `ledger_entries` và `ledger_lines`, không tính toán rời rạc [PARTIAL]
  - Cơ chế cache thống kê kèm tự động invalidate khi có bút toán mới phát sinh [PARTIAL]
  - Kiểm tra quyền sở hữu chống tấn công IDOR [PARTIAL]
  - Tài khoản mới chưa có giao dịch trả về cấu trúc dữ liệu rỗng chuẩn mực (không lỗi null) [PARTIAL]
- **d. Giao diện:** Phục vụ `/dashboard/landlord/analytics`, `/dashboard/admin/analytics` [ĐỌC TỪ MÃ]
- **e. API:** `GET /api/v1/billing/dashboard/stats` (LANDLORD), `GET /api/v1/billing/dashboard/admin-overview` (ADMIN).
- **f. Dữ liệu:** Đọc từ `ledger_entries`, `invoices`, `contracts`. Migration: `V1`, `V2`, `V4`.
- **g. Công nghệ:** Spring Boot, Caffeine Cache in-memory [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Mọi truy vấn đều lọc cứng theo `landlord_id` lấy từ SecurityContext; xuất báo cáo có rate limit và audit log.
- **i. Kiểm thử:** `MeterInvoiceRefundDashboardTest`, `BillingSecurityMatrixTest`.
- **j. Commit chính:** `e25dd41` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Biểu đồ nâng cao theo quý/năm đang dùng mock dự báo.

---

### 18. [DASH-FIX-1] Dashboard Thật, BFF Cookie Proxy & Bảo Mật Phiên
- **a. Phụ trách:** Kế hoạch: Dev 3 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Triệt tiêu hoàn toàn dữ liệu giả trên toàn bộ các dashboard; chuyển đổi toàn bộ cơ chế xác thực sang BFF HttpOnly Cookie proxy an toàn. Đối tượng: Toàn bộ người dùng.
- **c. Chức năng đã có:**
  - BFF Route Handler `/api/v1/[...path]/route.ts` đóng vai trò proxy reverse an toàn [PARTIAL]
  - Lưu Access Token và Refresh Token hoàn toàn trong HttpOnly Cookie (`cozynest_token`, `cozynest_refresh_token`) [PARTIAL]
  - Kiểm tra header `Origin` và `Referer` chống tấn công CSRF trên các phương thức ghi [PARTIAL]
  - Khóa chống đua (single-flight refresh mutex) khi nhiều request đồng thời gặp 401 [PARTIAL]
  - Đảm bảo JS trình duyệt (`document.cookie`, `localStorage`) hoàn toàn KHÔNG đọc được token [PARTIAL]
- **d. Giao diện:** Toàn bộ 98 trang thuộc `/dashboard/*` [ĐỌC TỪ MÃ]
- **e. API:** Tuyến proxy Next.js `/api/v1/[...path]` chuyển tiếp tới `http://api-gateway:8080/api/v1`.
- **f. Dữ liệu:** Không lưu token ở client storage.
- **g. Công nghệ:** Next.js 16.2 App Router Route Handlers, TypeScript [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Cờ `HttpOnly`, `SameSite=Lax`, `Secure` (khi production); gỡ bỏ hoàn toàn token khỏi body JSON trả về.
- **i. Kiểm thử:** `auth-helper.ts`, `sprint3-repositories.test.ts`, Vitest suite pass [CHẠY LẠI HÔM NAY].
- **j. Commit chính:** `7ac700c` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Playwright E2E còn một số kịch bản test cũ bị lỗi 401 do chưa cập nhật hết auth helper mới.

---

### 19. [HARDEN-01] Rà Soát & Vá Nợ Kỹ Thuật Sprint 1-2
- **a. Phụ trách:** Kế hoạch: Cường (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Đóng toàn bộ các lỗ hổng kỹ thuật từ Sprint 1-2 (chuẩn hóa ID, xác thực JWT fail-closed qua JWKS, siết ArchUnit, MySQL 8.4 LTS, loại bỏ cảnh báo lint). Đối tượng: Toàn hệ thống.
- **c. Chức năng đã có:**
  - Chuẩn hóa kiến trúc ID (ADR-007): Dual-ID cho bảng lõi, UUID v7 cho bảng mới [PARTIAL]
  - Bộ lọc `JwtAuthFilter` xác thực chữ ký RS256 qua JWKS nội bộ, fail-closed [PARTIAL]
  - Siết ArchUnit: domain không phụ thuộc JPA hay Spring; entity ở infrastructure [PARTIAL]
  - Thống nhất MySQL 8.4 LTS cho toàn bộ Docker Compose và 8 class Testcontainers [PARTIAL]
  - Xử lý triệt để 53 cảnh báo `i18next/no-literal-string` đạt `eslint --max-warnings=0` [PARTIAL]
- **d. Giao diện:** Không trực tiếp có giao diện; nâng cao chất lượng toàn bộ mã nguồn.
- **e. API:** Toàn bộ các endpoint của 6 service.
- **f. Dữ liệu:** Đồng bộ cấu hình Flyway trên 6 database schema.
- **g. Công nghệ:** MySQL 8.4 LTS, ArchUnit 1.5.1, Spring Security OAuth2 JOSE, JJWT 0.12.7 [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Seed data chỉ chạy ở profile dev/test; profile production không có dữ liệu demo.
- **i. Kiểm thử:** 396 test BE không cache PASS [CHẠY LẠI HÔM NAY], `CleanDddArchitectureTest` PASS.
- **j. Commit chính:** `f90c8b9`, `7ac700c` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Đã hoàn tất các mục code, tiếp tục giám sát độ trễ mạng trong môi trường production.

---

### 20. [LEGAL-02] Thẩm Định Pháp Lý Mô Hình Tiền, Thuế & Hợp Đồng Điện Tử
- **a. Phụ trách:** Kế hoạch: Cường (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Thẩm định tính hợp pháp của mô hình ví/escrow theo NĐ 52/2024/NĐ-CP, trách nhiệm thuế theo NĐ 117/2025/NĐ-CP và giá trị chứng cứ hợp đồng điện tử theo Luật GDĐT 2023. Đối tượng: Ban Giám Đốc, Pháp chế.
- **c. Chức năng đã có:**
  - Soạn thảo bộ 7 câu hỏi pháp lý và thuế gửi luật sư chuyên ngành (`LEGAL-02-COUNSEL-QUESTIONS.md`) [HUMAN]
  - Báo cáo đánh giá rủi ro pháp lý chi tiết (`LEGAL-02-PAYMENT-TAX-LEGAL-ASSESSMENT.md`) [HUMAN]
  - Quyết định kiến trúc ADR-008: Đề xuất Mô hình A (chuyển khoản P2P trực tiếp) làm mặc định an toàn [PARTIAL]
  - Gated Sandbox: Khóa toàn bộ tính năng tự giữ tiền thật sau feature flag `money.custody.enabled=false` [PARTIAL]
- **d. Giao diện:** Các văn bản tài liệu trong thư mục `docs/legal/`.
- **e. API:** Cờ cấu hình backend `fintech.mode=SANDBOX_ONLY`.
- **f. Dữ liệu:** Không có bảng mới; điều hướng luồng dữ liệu tiền tệ.
- **g. Công nghệ:** Tài liệu pháp lý Markdown, Feature flag configuration [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Tuyệt đối không nhận tiền người thuê vào tài khoản công ty Tổ Ấm khi chưa có giấy phép trung gian thanh toán của NHNN.
- **i. Kiểm thử:** Đối soát tài liệu đối chiếu với các điều luật hiện hành.
- **j. Commit chính:** `26ccbe4`, `cb84914` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `HUMAN`. Giới hạn: Bắt buộc chờ ý kiến tư vấn chính thức bằng văn bản của luật sư trước khi Go-Live tiền thật.

---

### 21. [ROOM-03] Bổ Sung Dữ Liệu Tin Đăng & Quy Tắc Sửa Tin
- **a. Phụ trách:** Kế hoạch: Dev 3 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Bổ sung các trường dữ liệu quan trọng cho tin đăng (ngày có thể dọn vào, thời hạn thuê tối thiểu, số người ở tối đa); siết chặt quy tắc sửa tin đã duyệt. Đối tượng: Chủ trọ, Người thuê, Kiểm duyệt viên.
- **c. Chức năng đã có:**
  - Thêm trường `available_from`, `min_lease_months`, `max_occupants` vào bảng rooms [PARTIAL]
  - Bộ lọc tìm kiếm và trang chi tiết phòng hiển thị các trường dữ liệu mới [PARTIAL]
  - Quy tắc kiểm duyệt lại: Sửa giá, địa chỉ hoặc ảnh bìa của tin đã APPROVED buộc đưa về PENDING_REVIEW [PARTIAL]
  - Khóa không cho phép sửa đổi các trường cốt lõi khi phòng đang ở trạng thái `RENTED` [PARTIAL]
- **d. Giao diện:** `/rooms`, `/rooms/[slug]`, `/dashboard/landlord/rooms/new`, `/dashboard/landlord/rooms/[id]/edit` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/landlord/rooms` (LANDLORD), `PUT /api/v1/landlord/rooms/{id}` (LANDLORD).
- **f. Dữ liệu:** Bảng `rooms`, `room_audit_logs`. Migration: `V14__sprint3_room_fields_and_kyc.sql`.
- **g. Công nghệ:** Spring Boot, Flyway, Next.js React Hook Form [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Không cho phép dọn vào trong quá khứ; số người ở tối thiểu là 1.
- **i. Kiểm thử:** `ListingSprint3HardeningTest`, `RoomDetailAndHostTest`.
- **j. Commit chính:** `cb84914`, `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Lịch sử thay đổi tin đăng hiện lưu dạng log, chưa có UI riêng cho chủ trọ xem lại từng phiên bản.

---

### 22. [KYC-03] Xác Minh Quyền Cho Thuê & Huy Hiệu "Tin Đã Xác Thực"
- **a. Phụ trách:** Kế hoạch: Dev 3 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cho phép chủ trọ tải giấy tờ chứng minh quyền sở hữu hoặc quyền cho thuê lại (sổ đỏ, hợp đồng ủy quyền); hiển thị huy hiệu "Tin đã xác thực" sau khi duyệt. Đối tượng: Chủ trọ, Kiểm duyệt viên, Người thuê.
- **c. Chức năng đã có:**
  - Tải giấy tờ pháp lý sở hữu phòng trọ lưu trữ an toàn trong kho riêng tư [PARTIAL]
  - Màn hình kiểm duyệt giấy tờ dành riêng cho Moderator kèm ghi nhật ký truy cập bất biến [PARTIAL]
  - Hiển thị huy hiệu "Tin đã xác thực" (Verified Badge) trên thẻ phòng và trang chi tiết [PARTIAL]
  - Tự động thu hồi huy hiệu khi giấy tờ hết hạn (`KycExpiryScheduler`) [PARTIAL]
- **d. Giao diện:** `/dashboard/landlord/rooms/[id]/ownership`, `/dashboard/moderator/kyc`, thẻ phòng trên `/rooms` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/landlord/rooms/{id}/ownership-documents` (LANDLORD), `POST /api/v1/moderation/ownership/{id}/approve` (MODERATOR).
- **f. Dữ liệu:** Bảng `property_ownership_documents`, `property_ownership_audit_logs`. Migration: `V14`, `V16`, `V17`.
- **g. Công nghệ:** AES-256-GCM file encryption, Spring Security RBAC [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Số giấy tờ được che một phần khi hiển thị; nhân sự xem giấy tờ bị ghi log (IP, UA, timestamp).
- **i. Kiểm thử:** `KycSecurityAndPrivacyAuditTest`, `ListingSprint3HardeningTest`.
- **j. Commit chính:** `cb84914`, `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Giấy tờ scan được lưu trên ổ đĩa mã hóa cục bộ, chưa tích hợp OCR tự động đọc số sổ đỏ.

---

### 23. [STORAGE-01] Kho Lưu Trữ Riêng Tư & Đường Dẫn Ký (Private Object Storage)
- **a. Phụ trách:** Kế hoạch: Dev 3 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Cung cấp hạ tầng lưu trữ tài liệu nhạy cảm (ảnh CCCD, ảnh sinh trắc học, hợp đồng PDF, sổ đỏ) với mã hóa phía server và phát hành liên kết có chữ ký (signed URL) ngắn hạn. Đối tượng: Toàn hệ thống.
- **c. Chức năng đã có:**
  - Dịch vụ lưu trữ mã hóa `EncryptedLocalStorageService` với thuật toán AES-256-GCM [PARTIAL]
  - Phát hành token truy cập có thời hạn (HMAC signed URL, TTL mặc định 15 phút) [PARTIAL]
  - Ngăn chặn triệt để việc truy cập trực tiếp file qua web root hoặc URL công khai [PARTIAL]
  - Tác vụ định kỳ dọn dẹp các tệp tạm và dữ liệu bị từ chối quá 30 ngày [PARTIAL]
- **d. Giao diện:** Tích hợp trong các màn hình xem ảnh CCCD, xem PDF hợp đồng, xem giấy tờ phòng [ĐỌC TỪ MÃ]
- **e. API:** `GET /api/v1/private-storage/download/{token}` (AUTH/SIGNED).
- **f. Dữ liệu:** Bảng `private_storage_audit_logs`. Migration: `V15__private_storage_audit.sql`.
- **g. Công nghệ:** Java Cryptography (AES/GCM/NoPadding, HmacSHA256), Spring Boot [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Header phản hồi luôn có `Cache-Control: no-store, private` và `X-Content-Type-Options: nosniff`.
- **i. Kiểm thử:** `PrivateStorageSecurityUnitTest`, `PrivateStorageSecurityTest`.
- **j. Commit chính:** `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Đang chạy trên ổ đĩa cục bộ (Local Encrypted Storage), chưa cắm driver AWS S3 / MinIO cloud thật.

---

### 24. [DEVOPS-02] Môi Trường, Cấu Hình & Diễn Tập Rollback
- **a. Phụ trách:** Kế hoạch: Dev 5 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Chuẩn hóa cấu hình môi trường (dev/staging/prod), loại bỏ hoàn toàn mật khẩu cứng và bí mật khỏi git, xây dựng kế hoạch rollback ứng dụng và cơ sở dữ liệu. Đối tượng: DevOps, SRE, Developer.
- **c. Chức năng đã có:**
  - Cấu hình Docker Compose sạch với 12 container khởi chạy hoàn chỉnh qua 1 lệnh [PARTIAL]
  - Chuyển script khởi tạo người dùng DB sang `01-init-service-users.sh` đọc biến môi trường [PARTIAL]
  - Loại bỏ hoàn toàn fallback password và `JWT_SECRET` thừa khỏi mã nguồn [PARTIAL]
  - Thêm tài liệu hướng dẫn diễn tập rollback chi tiết (`DEPLOYMENT-ROLLBACK-RUNBOOK.md`) [PARTIAL]
  - Bổ sung `.dockerignore` cho cả hai repository tối ưu kích thước build [PARTIAL]
- **d. Giao diện:** Không có giao diện người dùng; các file cấu hình `docker-compose.yml`, `.env.example`.
- **e. API:** Các endpoint `/actuator/health` trên 7 dịch vụ (ports 8080-8086).
- **f. Dữ liệu:** Scripts khởi tạo MySQL 8.4 LTS với 6 database schema và 6 user riêng biệt.
- **g. Công nghệ:** Docker, Docker Compose v2, Bash scripts, Alpine Linux [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Nguyên tắc đặc quyền tối thiểu (Least Privilege) cho user database; cấm commit tệp `.env` thật.
- **i. Kiểm thử:** Dựng sạch từ volume trống (`docker compose --profile full up`) thành công 12/12 container [THEO BÁO CÁO].
- **j. Commit chính:** `f90c8b9`, `7ac700c` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Cấu hình DNS SPF/DKIM/DMARC và máy chủ staging cloud thật thuộc nhóm HUMAN (chờ hạ tầng).

---

### 25. [BROKER-01] RabbitMQ, Transactional Outbox & Hàng Đợi Thư Chết (DLQ)
- **a. Phụ trách:** Kế hoạch: Dev 5 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Đảm bảo truyền thông điệp bất đồng bộ tin cậy giữa các dịch vụ thông qua Outbox pattern; chuyển các thông điệp lỗi vào DLQ để kiểm tra và phát lại (redrive). Đối tượng: Toàn hệ thống, SRE, Admin.
- **c. Chức năng đã có:**
  - Topology RabbitMQ với Topic Exchange `cozynest.events`, hàng đợi chính và Dead Letter Exchange `cozynest.dlx` [PARTIAL]
  - Transactional Outbox ghi nhận sự kiện trong cùng transaction ACID của nghiệp vụ [PARTIAL]
  - Bộ lập lịch quét outbox (`OutboxPublisher`) phát sự kiện định kỳ kèm xóa bản ghi cũ [PARTIAL]
  - Bảng điều khiển quản trị xem và phát lại thông điệp từ DLQ (`DeadLetterAdminService`) [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/system-health` (tab Message Broker & DLQ) [ĐỌC TỪ MÃ]
- **e. API:** `GET /api/v1/admin/dead-letter-messages` (ADMIN), `POST /api/v1/admin/dead-letter-messages/{id}/replay` (ADMIN).
- **f. Dữ liệu:** Bảng `appointment_events_outbox`, `billing_events_outbox`, `admin_dead_letter_messages`. Migration: `V2__admin_dead_letter_messages.sql`.
- **g. Công nghệ:** RabbitMQ 3.13.7 (Spring AMQP), Jackson JSON serializer [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Thông điệp mang phiên bản schema (`schemaVersion: 1.0`); sự kiện được sắp thứ tự theo `aggregateId`.
- **i. Kiểm thử:** `CommunicationBrokerDlqTest`, `DeadLetterAdminServiceTest`, `AppointmentOutboxPublisherTest`.
- **j. Commit chính:** `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Phiên bản RabbitMQ 3.13.7 đã hết hỗ trợ cộng đồng, cần kế hoạch nâng cấp lên 4.x LTS ở Sprint 4.

---

### 26. [SEC-PROD-01] Xác Thực Hai Yếu Tố (2FA/TOTP) & Quét Bảo Mật CI
- **a. Phụ trách:** Kế hoạch: Dev 5 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Bắt buộc xác thực hai yếu tố (TOTP) cho tài khoản quản trị và các hành động tài chính nhạy cảm; tích hợp quét bí mật tự động trên GitHub Actions CI. Đối tượng: Admin, Moderator, Chủ trọ.
- **c. Chức năng đã có:**
  - Kích hoạt 2FA TOTP qua ứng dụng Authenticator (Google Authenticator / Authy) kèm mã QR [PARTIAL]
  - Giới hạn số lần thử nhập mã TOTP (tối đa 5 lần sai trong 15 phút, khóa chống brute-force) [PARTIAL]
  - Quy trình khôi phục 2FA khi người dùng mất điện thoại có cơ chế duyệt 2 Admin [PARTIAL]
  - Pipeline quét bảo mật GitHub Actions (`ci-security.yml`) quét lộ lọt private key và API key [PARTIAL]
- **d. Giao diện:** `/dashboard/admin/security`, `/dashboard/landlord/security` [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/auth/2fa/setup` (AUTH), `POST /api/v1/auth/2fa/verify` (AUTH), `POST /api/v1/admin/users/{id}/recover-2fa` (ADMIN).
- **f. Dữ liệu:** Bảng `user_two_factor_auth`, `two_factor_recovery_requests`. Migration: `V8`, `V9__two_factor_hardening_and_recovery.sql`.
- **g. Công nghệ:** com.warrenstrange:googleauth:1.5.0, GitHub Actions gitleaks/grep scanner [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Mã bí mật TOTP được mã hóa AES-256-GCM trước khi lưu DB; xóa mã khôi phục một lần sau khi sử dụng.
- **i. Kiểm thử:** `TwoFactorHardeningTest` (181 tests identity PASS [CHẠY LẠI HÔM NAY]).
- **j. Commit chính:** `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `PARTIAL`. Giới hạn: Chưa triển khai HashiCorp Vault chuyên dụng ở môi trường production.

---

### 27. [PHONE-OTP-01] Xác Thực Số Điện Thoại & Chống Lạm Dụng SMS
- **a. Phụ trách:** Kế hoạch: Dev 5 (3 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Xác thực số điện thoại di động chính chủ qua mã OTP, ngăn chặn việc đăng ký bằng số ảo/số tạm thời và kiểm soát chi phí gửi tin nhắn. Đối tượng: Người thuê, Chủ trọ.
- **c. Chức năng đã có:**
  - Gửi mã xác thực OTP 6 số tới số điện thoại di động Việt Nam (+84) [SANDBOX]
  - Chỉ lưu trữ mã băm SHA-256 của OTP kèm TTL 5 phút trong Redis/DB, tuyệt đối không lưu OTP thô [PARTIAL]
  - Bộ kiểm tra đầu số hợp lệ của các nhà mạng viễn thông Việt Nam (Viettel, VinaPhone, MobiFone, Vietnamobile), chặn đầu số ảo VoIP/disposable [PARTIAL]
  - Giới hạn tần suất (Rate Limit): tối đa 3 tin nhắn OTP / 1 số điện thoại / 1 giờ để chống cạn kiệt ngân sách SMS [PARTIAL]
- **d. Giao diện:** `/dashboard/tenant/profile`, `/dashboard/landlord/profile` (mục Xác thực SĐT) [ĐỌC TỪ MÃ]
- **e. API:** `POST /api/v1/auth/phone/send-otp` (AUTH), `POST /api/v1/auth/phone/verify-otp` (AUTH).
- **f. Dữ liệu:** Bảng `phone_verifications`, `phone_verification_rate_limits`. Migration: `V8__phone_otp_and_2fa_schema.sql`.
- **g. Công nghệ:** Spring Boot, Redis TTL, Java Regex validator [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Đổi số điện thoại bắt buộc phải xác thực mã OTP gửi về cả số cũ và số mới; chỉ tài khoản có SĐT xác thực mới được tạo yêu cầu thuê.
- **i. Kiểm thử:** `PhoneVerificationServiceTest`, `AuthValidationVectorTest`.
- **j. Commit chính:** `f90c8b9` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Cổng gửi tin nhắn thực tế đang chạy ở chế độ Sandbox Stub (in mã ra log/Mailpit), chưa ký hợp đồng với nhà mạng SMS Brandname hoặc Zalo ZNS.

---

### 28. [QA-FINTECH] Bộ Kiểm Thử E2E Chu Trình Tiền & Kiểm Soát Go/No-Go
- **a. Phụ trách:** Kế hoạch: Dev 5 (5 điểm) | Git: catim07 [ĐỌC TỪ MÃ]
- **b. Mục tiêu nghiệp vụ:** Xây dựng bộ kịch bản kiểm thử tích hợp tự động cho toàn bộ chu trình tiền tệ (đặt cọc, giữ cọc, hoàn tiền, thanh toán hóa đơn, giải ngân) và thiết lập checklist Go/No-Go tài chính. Đối tượng: QA Lead, Tech Lead, Tài chính.
- **c. Chức năng đã có:**
  - Bộ kiểm thử tích hợp giao dịch song song trên sổ cái kép chứng minh không âm tài khoản [PARTIAL]
  - Bộ kiểm thử kịch bản bù trừ tự động của Saga khi bước thanh toán gặp sự cố [PARTIAL]
  - Kiểm tra đối soát tự động phát hiện 100% các trường hợp sai lệch số tiền hoặc thiếu giao dịch [PARTIAL]
  - Mẫu danh mục kiểm tra Go/No-Go tài chính yêu cầu 2 chữ ký phê duyệt trước khi Go-Live [HUMAN]
- **d. Giao diện:** Bộ tài liệu kiểm thử và kết quả chạy test tự động trên CI/CD.
- **e. API:** Bao quát toàn bộ các API thuộc `billing-service` và `booking-service`.
- **f. Dữ liệu:** Fixtures dữ liệu test tự động sinh và tự động dọn dẹp sau mỗi lần chạy test.
- **g. Công nghệ:** JUnit 5, Testcontainers MySQL 8.4 LTS, Spring MockMvc, Awaitility [ĐỌC TỪ MÃ].
- **h. Quy tắc:** Mọi test tài chính phải chạy trên môi trường sạch, không phụ thuộc vào thứ tự thực thi; không cache kết quả test.
- **i. Kiểm thử:** `Wave2FintechSecurityAndAuditTest`, `LedgerImmutabilityTestcontainersTest`, `MeterInvoiceRefundDashboardTest`.
- **j. Commit chính:** `5dfc78a` [ĐỌC TỪ MÃ].
- **k. Trạng thái:** `SANDBOX`. Giới hạn: Kiểm thử diễn tập trên môi trường staging thật cần có hạ tầng cloud từ DevOps; checklist Go/No-Go đang chờ ký duyệt thực tế.

