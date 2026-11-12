# SPRINT 3 COMPLETION & VERIFICATION TRACKER (`S3-COMPLETE`)

> Branch: `feat/sprint-3-complete` (from `feat/sprint-3-audit-remediation`)
> Target Scope: **28 cards / 128 story points** (Cường 32, Dev 2 26, Dev 3 24, Dev 4 23, Dev 5 23)
> Checklist Baseline: **55 items** from Section 4.1 (10 new cards) + **18 rows** (split by `;` into sub-items) from Section 4.2 (18 existing/updated cards) of `KE_HOACH_TRELLO_V2.md` (`d:\RIKKEI\THUC_TAP\PROJECT\index.md`).
> Status Vocabulary (strict): `DONE-VERIFIED`, `SANDBOX-ONLY`, `PARTIAL`, `HUMAN`, `BLOCKED-LEGAL`.

---

## PART A — TỔNG HỢP 28 THẺ SPRINT 3 (ĐÁNH GIÁ ĐỘC LẬP TẠI PHA 0)

> Lưu ý: Tại Pha 0, toàn bộ kết luận "27 VERIFIED_DONE" cũ được hạ cấp thận trọng để kiểm chứng lại từng dòng checklist từ nguồn độc lập (code, ArchUnit, Testcontainers, runtime Docker, security & FE E2E). Tất cả các thẻ đụng tiền đều gắn nhãn **"chờ review 2 người, có Cường"**.

| Thẻ | Chủ | Điểm | Trạng thái | Bằng chứng (Baseline Pha 0) | Còn thiếu (Cần xử lý Pha 1–4) | Commit |
|---|---|---|---|---|---|---|
| LEGAL-02 | Cường | 3 | `HUMAN` | ADR-008/010 kỹ thuật trong `docs/DECISIONS.md` (chờ review) | Tham vấn luật sư/kế toán, NĐ 52/2024, NĐ 117/2025, mẫu HĐ, ĐK TMĐT BCT; **chờ review 2 người, có Cường** | - |
| HARDEN-01 | Cường | 5 | `PARTIAL` | ArchUnit + JWT JWKS filter + Resilience4j; Pha 0 chuẩn hóa `ADR-007`, MySQL `8.4 LTS`, bỏ `JWT_SECRET` Compose, cập nhật `DECISIONS.md` | Còn kiểm chứng JWT fail-closed 4 kịch bản, `prod` Flyway không seed ở 6 service, ArchUnit 6 service, RBAC 5 vai trò, `verify-links` menu, lint text literal ERROR | Pha 0: TBD |
| SAGA-01 | Cường | 5 | `PARTIAL` | Sơ khởi orchestrator/outbox ở `booking-service` | Thiếu test hỗn loạn đầy đủ (kill service, trùng/đảo event), hiển thị trạng thái trung gian FE, đối chiếu saga treo định kỳ; **chờ review 2 người, có Cường** | - |
| LEDGER-01 | Cường | 8 | `PARTIAL` | `LedgerService`, chart of accounts, integer VND, ArchUnit cơ bản | Kiểm chứng độc lập không có đường ghi ngoài `LedgerService`, chụp số dư theo ngày, bút toán đảo có lý do + 2 người duyệt; **chờ review 2 người, có Cường** | - |
| LEDGER-02 | Dev 2 | 3 | `PARTIAL` | API điều chỉnh & xuất báo cáo cơ bản | Kiểm tra quyền DB user (`billing_user`) chặn UPDATE/DELETE bảng sổ cái, job cân đối đêm + alert, test tự duyệt/sửa số dư bị chặn; **chờ review 2 người, có Cường** | - |
| FIN-RISK-01 | Dev 2 | 5 | `PARTIAL` | Hạn mức & cấu hình `platform_settings` bước đầu | Kiểm chứng velocity rule, freeze ví/tranh chấp, blocklist STK/SĐT/thiết bị, hàng đợi cảnh báo SLA, chargeback; **chờ review 2 người, có Cường** | - |
| PAY-01 | Cường | 8 | `PARTIAL` | Webhook + idempotency cơ bản (`SANDBOX_ONLY`) | Xử lý trả thiếu/thừa/trễ/trùng/sai nội dung vào hàng khớp thủ công, replay timestamp, allowlist IP + xoay secret, nút tắt cổng; **chờ review 2 người, có Cường** | - |
| PAY-02 | Cường | 3 | `PARTIAL` | `PaymentReconciliationService` & `ReconciliationReport` | Báo cáo gửi kế toán, SLA xử lý MISMATCH, đối soát cả hoàn & chi; **chờ review 2 người, có Cường** | - |
| ESCROW-01 | Dev 2 | 5 | `PARTIAL` | `EscrowService`, `EscrowController`, cờ sandbox | Chốt Model A (ADR-008) vs cờ `money.custody.enabled=false`, 2 bên xác nhận move-in, khớp tên STK eKYC, duyệt 2 người trên ngưỡng; **chờ review 2 người, có Cường** | - |
| REFUND-01 | Dev 4 | 3 | `PARTIAL` | `RefundService`, `RefundController` | Hoàn từng phần/nhiều lần không vượt thu, retry idempotent `REQUESTED->PROCESSING->DONE|FAILED`, UI khách + admin, 2 người duyệt vượt ngưỡng; **chờ review 2 người, có Cường** | - |
| APPT-RENT-BE | Dev 2 | 5 | `PARTIAL` | Booking/appointment controller + entity | Bắt buộc SĐT đã xác thực, rate limit, khóa kiểm tra phòng trống lúc giữ chỗ, hết hạn tự hoàn cọc, `idempotency-key`, outbox; **chờ review 2 người, có Cường** | - |
| CONTRACT-BE | Dev 2 | 5 | `PARTIAL` | E-contract entity, OTP signing, PDF service | Snapshot bất biến + hash, audit log (IP, UA, OTP, timestamp) xuất bằng chứng, khóa sửa sau ký, PDF kho riêng có version, gate `LEGAL-02`; **chờ review 2 người, có Cường** | - |
| CONTRACT-02 | Dev 2 | 3 | `PARTIAL` | `ContractAmendmentsSection`, `ContractOccupantsSection` | Kiểm tra bảng `contract_amendments`, `contract_occupants`, ký OTP phụ lục, hóa đơn tháng sau áp giá mới đúng ngày hiệu lực, nhắc hạn 30/15/7 ngày | - |
| DASH-FIX-1 | Dev 3 | 5 | `PARTIAL` | BFF Next.js `/api/v1/[...path]`; Pha 0 nối `INTERNAL_API_URL` trong Compose | E2E baseline phát hiện 71/246 fail (nhiều test cũ dùng auth helper cũ/401 trên `/support`, `/admin-*`, `/landlord-*`); kiểm chứng CSRF Origin, khóa chống đua refresh, JS không đọc được token | - |
| APPT-RENT-FE | Dev 3 | 5 | `PARTIAL` | Trang đặt lịch & đặt thuê FE | Chống bấm đúp + `idempotency-key`, hiển thị số tiền + chính sách trước xác nhận, xử lý rớt mạng kiểm tra lại trạng thái, check responsive mobile; **chờ review 2 người, có Cường** | - |
| CONTRACT-FE | Dev 3 | 5 | `PARTIAL` | Giao diện hợp đồng & ký FE | Chống bấm đúp khi ký, hiển thị hash & thời điểm ký, xử lý OTP hết hạn/quá lần, bản in rõ ràng | - |
| ROOM-03 | Dev 3 | 3 | `PARTIAL` | Wizard/list phòng đã cập nhật một phần | Kiểm tra migration `available_from`, `min_lease_months`, `max_occupants`, quy tắc duyệt lại + lịch sử sửa tin, khóa sửa trường quan trọng khi `RENTED` | - |
| KYC-03 | Dev 3 | 3 | `PARTIAL` | KYC flow cơ bản | Giấy tờ sở hữu/cho thuê lại gắn tin/tòa nhà lưu kho riêng tư mã hóa, moderator audit log, huy hiệu & thu hồi hết hạn, che một phần & retention | - |
| STORAGE-01 | Dev 3 | 3 | `PARTIAL` | Local/private storage service bước đầu | Mã hóa server-side, bucket/kho riêng tư + signed URL ngắn hạn KYC/hợp đồng, lifecycle retention, cấu hình backup chéo vùng | - |
| METER-01 | Dev 4 | 5 | `PARTIAL` | `UtilityReadingController` / `Service` | Sửa/hủy chỉ số có lý do & lịch sử, khóa sửa sau `ISSUED`, cảnh báo tăng đột biến, ảnh công tơ bắt buộc theo cấu hình | - |
| INVOICE-01/02 | Dev 4 | 5 | `PARTIAL` | `InvoiceService`, `InvoicePdfService` | Bất biến sau `PAID`, hóa đơn điều chỉnh/ghi có, trả một phần & tiền thừa, idempotent không trùng kỳ, xem trước hàng loạt, đánh số tuần tự; **chờ review 2 người, có Cường** | - |
| PAY-VIP-FE | Dev 4 | 5 | `PARTIAL` | Giao diện thanh toán FE (`PAY-FE`) | Tách PRICING-CONV-01 sang Sprint 4 rõ ràng, hiển thị trạng thái chờ/xử lý/thất bại, xử lý thanh toán quá hạn; **chờ review 2 người, có Cường** | - |
| DASH-STATS-BE | Dev 4 | 5 | `PARTIAL` | `BillingDashboardService` / `Controller` | Số liệu lấy trực tiếp từ sổ cái, cache invalidate, chống IDOR, export có giới hạn + access log, tài khoản mới trả rỗng; **chờ review 2 người, có Cường** | - |
| DEVOPS-02 | Dev 5 | 5 | `PARTIAL` | Pha 0 vá fallback secret Compose/init SQL, thêm `.dockerignore`, đồng bộ `package-lock.json`, dựng sạch `--profile full` 12 container trên MySQL `8.4.13` | Kiểm tra secrets trong image/Dockerfile, chiến lược migration expand/contract trên bản sao dữ liệu, diễn tập rollback local, SPF/DKIM/DMARC/staging thuộc `HUMAN`, feature flag tiền | Pha 0: TBD |
| BROKER-01 | Dev 5 | 5 | `PARTIAL` | RabbitMQ 3.13.7 + `AdminDlqListener` + `OutboxPublisher` | Event catalog & schema version, ordering theo aggregate, queue lag metrics, outbox cleanup job, replay DLQ có ghi log | - |
| SEC-PROD-01 | Dev 5 | 5 | `PARTIAL` | JWT RS256 + `kid` + production hardening | Quy trình khôi phục 2FA mất máy (2 admin duyệt), giới hạn thử TOTP, xoay khóa `kid`, xác nhận không chứa secret trong Docker image | - |
| PHONE-OTP-01 | Dev 5 | 3 | `PARTIAL` | OTP service bước đầu | Đổi SĐT xác thực cả số cũ & mới, chặn số ảo/tạm, hạn mức chi phí SMS, không lưu mã OTP rõ, kênh dự phòng (Zalo->SMS) & sandbox stub | - |
| QA-FINTECH | Dev 5 | 5 | `PARTIAL` | Suite unit/Testcontainers/E2E hiện có | Chaos/failure test, parallel ledger test, refund & mismatch test, chạy trên staging sandbox (`HUMAN`), checklist Go/No-Go 2 chữ ký (`HUMAN`); **chờ review 2 người, có Cường** | - |

---

## PART B — CHECKLIST TỪNG DÒNG CỦA 28 THẺ (TRÍCH NGUYÊN VĂN TỪ `KE_HOACH_TRELLO_V2.md`)

### 4.1. Thẻ MỚI trong Sprint 3 (10 thẻ, 55 dòng checklist)

#### `[LEGAL-02] Thẩm định pháp lý mô hình tiền, thuế và hợp đồng điện tử`
> Member: Cường | Points: 3 | Priority: P0-GO-LIVE | Labels: Legal/Docs Fintech
> Mô tả: Trước khi nhận tiền thật, cần luật sư và kế toán xác nhận mô hình dòng tiền và nghĩa vụ liên quan. Kết quả quyết định thiết kế LEDGER, ESCROW, PAY và có thể giảm phạm vi.
> DoD: Có văn bản tư vấn hoặc biên bản làm việc; mô hình tiền được chốt và ghi vào DECISIONS.md; các thẻ tiền được cập nhật theo kết luận.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| LEGAL-02.1 | Hỏi: ví nội bộ, giữ cọc, nạp và rút có thuộc dịch vụ trung gian thanh toán cần giấy phép NHNN (NĐ 52/2024) không; phương án thay thế: dòng tiền đi qua đối tác thanh toán có phép hoặc tài khoản đảm bảo, sàn chỉ thu phí | `HUMAN` | Đã nêu giả thuyết Model A trong ADR-008 (`docs/DECISIONS.md`); chờ luật sư xác nhận |
| LEGAL-02.2 | Hỏi: nghĩa vụ khấu trừ VAT và TNCN theo NĐ 117/2025 với thu nhập cho thuê của cá nhân, có phải khấu trừ không, mức nào, khai báo ra sao | `HUMAN` | Cần tư vấn thuế chính thức trước khi bật thu hộ/chi hộ |
| LEGAL-02.3 | Hỏi: giá trị pháp lý của ký OTP và chữ ký điện tử cho hợp đồng thuê nhà; có cần công chứng, chứng thực trong trường hợp nào | `HUMAN` | Cần văn bản luật sư về Luật Giao dịch điện tử & Luật Nhà ở |
| LEGAL-02.4 | Hỏi: hóa đơn điện tử cho phí sàn và gói VIP; đăng ký sàn TMĐT với Bộ Công Thương | `HUMAN` | Thủ tục pháp nhân & Bộ Công Thương (`online.gov.vn`) |
| LEGAL-02.5 | Hợp đồng với đối tác thanh toán: phí, thời gian chuyển tiền, xử lý tranh chấp, trách nhiệm khi sai sót | `HUMAN` | Chờ ký kết với PSP/Ngân hàng |
| LEGAL-02.6 | Điều khoản sử dụng, chính sách hoàn cọc, mẫu hợp đồng thuê do luật sư duyệt | `HUMAN` | Chờ bản duyệt pháp lý trước go-live |
| LEGAL-02.7 | Ghi quyết định vào docs/DECISIONS.md và cập nhật thẻ LEDGER-01, ESCROW-01, PAY-01, TAX-EINV-01 | `PARTIAL` | Đã ghi đề xuất kỹ thuật vào ADR-008/010; giữ cờ `fintech.mode=SANDBOX_ONLY`, chờ kết luận pháp lý cuối |

#### `[HARDEN-01] Rà soát và vá nợ kỹ thuật Sprint 1-2 trước khi mở giao dịch tiền`
> Member: Cường | Points: 5 | Priority: P0-GO-LIVE | Labels: BE-Java Security QA/Test
> Mô tả: Nhiều mục đã rà trước đó chưa có bằng chứng hoàn thành. Phải đóng hết trước khi tiền thật chạy qua hệ thống.
> DoD: Từng mục có kết quả lệnh thật (không dùng cache); không còn mục "đã đúng" mà thiếu bằng chứng.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| HARDEN-01.1 | Chốt kiểu ID (UUID v7 hay BIGINT có quy ước) và chuẩn hóa nếu lệch; ghi vào DECISIONS.md | `PARTIAL` | Chuẩn hóa ADR-007 trong `DECISIONS.md` (Pha 0) + kiểm tra schema mới ở Pha 1 |
| HARDEN-01.2 | Mọi service (listing, booking, billing, communication, admin) xác thực JWT bằng JWKS, fail-closed, test token sai chữ ký, hết hạn, alg=none, sai issuer | `PARTIAL` | Đã có `JwtAuthFilter`, cần kiểm chứng test đủ cả 4 kịch bản tấn công trên mọi service ở Pha 1 |
| HARDEN-01.3 | Seed chỉ ở db/seed; test profile production không còn dữ liệu demo ở mọi service | `PARTIAL` | Kiểm tra Flyway `prod` ở toàn bộ 6 service bằng Testcontainers ở Pha 1 |
| HARDEN-01.4 | Siết ArchUnit cho mọi service: domain không import jakarta.persistence hay Spring; JPA entity và repository Spring Data ở infrastructure | `PARTIAL` | Kiểm tra ArchUnit hiện có và bổ sung đủ cho 6 service ở Pha 1 |
| HARDEN-01.5 | Testcontainers MySQL cho mọi service ở profile dev và production; test cho api-gateway (định tuyến, xác thực, CORS) | `PARTIAL` | Đã thấy 8 class Testcontainers dùng `mysql:9.0` (chưa đồng bộ LTS & chưa đủ hết 6 service/2 profile); hoàn tất ở Pha 0–1 |
| HARDEN-01.6 | Ma trận 200/401/403/404 đủ 5 vai trò cho toàn bộ endpoint hiện có | `PARTIAL` | Kiểm tra & mở rộng test ma trận RBAC toàn endpoint ở Pha 1 |
| HARDEN-01.7 | Lint chữ ghi cứng ở mức ERROR (xử lý hơn 500 cảnh báo); quét class màu palette | `PARTIAL` | Kiểm tra cấu hình `eslint.config.*` (`i18next/no-literal-string`) & script quét màu ở Pha 3 |
| HARDEN-01.8 | Crawler route kiểm tra đúng đích redirect; verify-links đọc cả cấu hình menu | `PARTIAL` | Kiểm tra `scripts/verify-links.mjs` và E2E crawler ở Pha 3 |
| HARDEN-01.9 | Gọi đồng bộ giữa service có timeout, retry, circuit breaker (resilience4j); không có log chứa PII | `PARTIAL` | Kiểm tra adapter HTTP liên service & log sanitizer ở Pha 1 |

#### `[SAGA-01] Giao dịch xuyên service: đặt thuê → cọc → ký hợp đồng → phòng RENTED`
> Member: Cường | Points: 5 | Priority: P0-GO-LIVE | Labels: BE-Java Fintech
> Mô tả: Một giao dịch thuê chạm booking, billing, listing, communication. Phải có cơ chế điều phối và bù trừ khi một bước lỗi giữa chừng để không bao giờ có trạng thái "đã thu tiền nhưng phòng chưa khóa" hay ngược lại.
> DoD: Mô phỏng sập từng service ở từng bước vẫn ra trạng thái cuối nhất quán hoặc vào hàng xử lý thủ công có cảnh báo.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| SAGA-01.1 | Vẽ sơ đồ trạng thái và các bước bù trừ (cọc hết hạn → hoàn, ký lỗi → giải phóng phòng, v.v.) | `PARTIAL` | Kiểm chứng & bổ sung tài liệu state machine trong `docs/` ở Pha 2 |
| SAGA-01.2 | Saga theo outbox/event hoặc orchestrator; mọi bước idempotent | `PARTIAL` | Kiểm tra `LeaseSagaOrchestrator` / outbox & idempotency ở Pha 2; **chờ review 2 người, có Cường** |
| SAGA-01.3 | Trạng thái trung gian hiển thị rõ cho người dùng (đang xử lý, cần hành động) | `PARTIAL` | Kiểm tra API + FE booking/contract hiển thị bước trung gian ở Pha 2–3 |
| SAGA-01.4 | Job đối chiếu định kỳ phát hiện saga treo, gắn cờ cho Admin | `PARTIAL` | Kiểm tra scheduler quét saga quá hạn + UI Admin xử lý ở Pha 2 |
| SAGA-01.5 | Test hỗn loạn: tắt service giữa chừng, gửi trùng sự kiện, đảo thứ tự sự kiện | `PARTIAL` | Cần viết & chạy test mô phỏng chaos/out-of-order ở Pha 2 & Pha 4 |

#### `[LEDGER-02] Toàn vẹn sổ cái hằng ngày, duyệt 2 người và xuất báo cáo kế toán`
> Member: Dev 2 | Points: 3 | Priority: P0-GO-LIVE | Labels: Fintech BE-Java DB-Flyway
> Mô tả: Bảo đảm sổ cái luôn cân đối và mọi điều chỉnh thủ công có người duyệt thứ hai.
> DoD: Job cân đối chạy mỗi đêm, lệch sẽ cảnh báo Telegram/Slack; điều chỉnh thủ công cần 2 người khác nhau.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| LEDGER-02.1 | Job kiểm tra tổng Nợ = tổng Có theo ngày và theo ví; số dư ví khớp tổng bút toán | `PARTIAL` | Kiểm tra `LedgerIntegrityJob` & kết nối cảnh báo webhook cấu hình được ở Pha 2; **chờ review 2 người, có Cường** |
| LEDGER-02.2 | Bút toán điều chỉnh chỉ qua API có lý do, người tạo, người duyệt (khác người tạo), lưu bất biến | `PARTIAL` | Kiểm tra maker-checker API ở Pha 2; **chờ review 2 người, có Cường** |
| LEDGER-02.3 | Xuất sổ cái, bảng cân đối thử, sao kê theo kỳ (CSV/Excel UTF-8 BOM) cho kế toán | `PARTIAL` | Kiểm tra endpoint CSV UTF-8 BOM & escape công thức Excel (`=`, `+`, `-`, `@`) ở Pha 2 |
| LEDGER-02.4 | Quyền DB chặn UPDATE/DELETE trên bảng sổ cái cho user ứng dụng | `PARTIAL` | Kiểm tra `docker/mysql/` grants + Flyway triggers bất biến bảng sổ cái ở Pha 2 |
| LEDGER-02.5 | Test: cố sửa trực tiếp số dư, cố tự duyệt điều chỉnh của chính mình đều bị từ chối | `PARTIAL` | Xác nhận test tích hợp Testcontainers MySQL thật ở Pha 2 |

#### `[FIN-RISK-01] Quản trị rủi ro giao dịch: hạn mức, đóng băng, cảnh báo bất thường`
> Member: Dev 2 | Points: 5 | Priority: P0-GO-LIVE | Labels: Fintech BE-Java Security
> Mô tả: Giảm thiệt hại nếu tài khoản bị chiếm hoặc có hành vi lừa đảo. Cấu hình được từ platform_settings.
> DoD: Rút vượt hạn mức, thay đổi bất thường hay giao dịch dồn dập bị chặn hoặc chờ duyệt; Admin đóng băng ví được ngay.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| FIN-RISK-01.1 | Hạn mức rút theo giao dịch, theo ngày; chủ trọ mới và vừa đổi STK có hạn mức thấp hơn | `PARTIAL` | Kiểm tra `platform_settings` + rule giới hạn ở Pha 2; **chờ review 2 người, có Cường** |
| FIN-RISK-01.2 | Quy tắc tốc độ (velocity): nhiều lần rút/nạp/hoàn trong thời gian ngắn, nhiều tài khoản chung thiết bị hoặc STK | `PARTIAL` | Kiểm tra kiểm soát velocity & liên kết thiết bị/STK ở Pha 2 |
| FIN-RISK-01.3 | Đóng băng ví/escrow theo người dùng hoặc theo hợp đồng; chặn rút khi có tranh chấp | `PARTIAL` | Kiểm tra freeze API + dispute lock ở Pha 2 |
| FIN-RISK-01.4 | Danh sách chặn (STK, SĐT, thiết bị); lý do và nhật ký thay đổi | `PARTIAL` | Kiểm tra blocklist DB/API/audit log ở Pha 2 |
| FIN-RISK-01.5 | Hàng đợi cảnh báo cho Admin với SLA; thông báo khi giao dịch bị giữ | `PARTIAL` | Kiểm tra risk alert queue + thông báo user ở Pha 2 |
| FIN-RISK-01.6 | Quy trình xử lý chargeback/tiền vào nhầm; test các kịch bản trên | `PARTIAL` | Kiểm tra quy trình & test kịch bản chargeback ở Pha 2 |

#### `[REFUND-01] Hoàn tiền ra ngân hàng cho người thuê (toàn phần, từng phần)`
> Member: Dev 4 | Points: 3 | Priority: P0-GO-LIVE | Labels: Fintech BE-Java FE-Next
> Mô tả: Hoàn cọc, hoàn hóa đơn thừa, hoàn khi hủy phải đi đến tay khách, xử lý được cả khi hoàn thất bại.
> DoD: Hoàn tiền tạo bút toán đúng; hoàn thất bại không mất tiền, có trạng thái và thử lại.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| REFUND-01.1 | Hoàn về nguồn (cùng cổng/ngân hàng) hoặc STK khách cung cấp có kiểm tra tên | `PARTIAL` | Kiểm tra `RefundService` ở Pha 2 (`SANDBOX_ONLY`); **chờ review 2 người, có Cường** |
| REFUND-01.2 | Hoàn từng phần, hoàn nhiều lần không vượt số tiền đã thu | `PARTIAL` | Kiểm tra ràng buộc DB/lock tổng tiền đã hoàn <= đã thu ở Pha 2 |
| REFUND-01.3 | Trạng thái REQUESTED → PROCESSING → DONE \| FAILED, thử lại an toàn (idempotent) | `PARTIAL` | Kiểm tra state machine & retry key ở Pha 2 |
| REFUND-01.4 | Giao diện khách theo dõi hoàn tiền; giao diện Admin xử lý hoàn thất bại | `PARTIAL` | Kiểm tra FE tenant refund tracker & admin retry UI ở Pha 3 |
| REFUND-01.5 | Hai người duyệt khi hoàn vượt ngưỡng cấu hình (dùng LEDGER-02) | `PARTIAL` | Kiểm tra Tích hợp maker-checker vào `RefundService` ở Pha 2; **chờ review 2 người, có Cường** |

#### `[KYC-03] Xác minh quyền cho thuê và huy hiệu "Tin đã xác thực"`
> Member: Dev 3 | Points: 3 | Priority: P1-REVENUE | Labels: Security BE-Java FE-Next
> Mô tả: Chặn lừa đảo cho thuê nhà không phải của mình, tăng niềm tin cho người thuê.
> DoD: Tin có huy hiệu chỉ khi Moderator duyệt giấy tờ; giấy tờ không bao giờ lộ công khai.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| KYC-03.1 | Chủ trọ tải giấy tờ (sổ đỏ, hợp đồng thuê lại, giấy ủy quyền) gắn với tin hoặc tòa nhà, lưu kho riêng tư, mã hóa | `PARTIAL` | Kiểm tra upload + AES-GCM + kho riêng tư ở Pha 3 |
| KYC-03.2 | Moderator xem qua endpoint có xác thực, ghi nhật ký truy cập, duyệt hoặc từ chối kèm lý do | `PARTIAL` | Kiểm tra access log + moderator UI/API ở Pha 3 |
| KYC-03.3 | Huy hiệu và ưu tiên hiển thị cho tin đã xác thực; thu hồi huy hiệu khi hết hạn giấy tờ | `PARTIAL` | Kiểm tra badge FE + ranking/scheduler thu hồi ở Pha 3 |
| KYC-03.4 | Quy tắc: số liệu giấy tờ che một phần, xóa theo chính sách lưu giữ | `PARTIAL` | Kiểm tra masking + retention cleanup job ở Pha 3 |

#### `[ROOM-03] Bổ sung dữ liệu tin đăng và quy tắc sửa tin`
> Member: Dev 3 | Points: 3 | Priority: P1-REVENUE | Labels: BE-Java FE-Next DB-Flyway
> Mô tả: Tin cần có ngày có thể vào ở, thời hạn thuê tối thiểu, đồng thuê/người ở cùng; và quy tắc duyệt lại khi sửa tin đã đăng.
> DoD: Tìm kiếm và trang chi tiết dùng các trường mới; sửa giá/địa chỉ/ảnh bìa của tin đã duyệt buộc duyệt lại.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| ROOM-03.1 | Migration: available_from, min_lease_months, max_occupants (nếu chưa), ghi chú nội quy | `PARTIAL` | Kiểm tra Flyway schema & JPA/domain ở `listing-service` ở Pha 3 |
| ROOM-03.2 | Wizard đăng tin và trang sửa, bộ lọc tìm kiếm, thẻ phòng, trang chi tiết | `PARTIAL` | Kiểm tra toàn bộ FE `accommodation-web` ở Pha 3 |
| ROOM-03.3 | Quy tắc duyệt lại và lịch sử thay đổi tin; không cho đổi trường quan trọng khi phòng đang RENTED | `PARTIAL` | Kiểm tra state rule + change history table ở Pha 3 |
| ROOM-03.4 | Cập nhật seed và test | `PARTIAL` | Kiểm tra seed + BE/FE test ở Pha 3 |

#### `[CONTRACT-02] Phụ lục hợp đồng, gia hạn, đồng thuê và khai báo người ở`
> Member: Dev 2 | Points: 3 | Priority: P1-REVENUE | Labels: BE-Java DB-Flyway
> Mô tả: Hợp đồng thực tế có đổi giá, gia hạn và nhiều người cùng thuê.
> DoD: Phụ lục ký OTP như hợp đồng gốc; hóa đơn tháng tiếp theo dùng giá mới đúng ngày hiệu lực.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| CONTRACT-02.1 | Bảng contract_amendments, contract_occupants; trạng thái và ký OTP cho phụ lục | `PARTIAL` | Kiểm tra Flyway + OTP signing cho phụ lục ở Pha 2–3 |
| CONTRACT-02.2 | Gia hạn, đổi giá có hiệu lực từ ngày; không sửa hợp đồng gốc đã ký | `PARTIAL` | Kiểm tra `InvoiceService` đọc giá phụ lục có hiệu lực đúng ngày ở Pha 2 |
| CONTRACT-02.3 | Người thuê chính và người ở cùng; mỗi người có thông tin khai báo tạm trú | `PARTIAL` | Kiểm tra schema + validation khai báo tạm trú ở Pha 2–3 |
| CONTRACT-02.4 | Thông báo trước hết hạn 30/15/7 ngày | `PARTIAL` | Kiểm tra scheduler thông báo hết hạn hợp đồng ở Pha 2 |

#### `[DEVOPS-02] Môi trường dev/staging/prod, cấu hình, migration an toàn, email deliverability`
> Member: Dev 5 | Points: 5 | Priority: P0-GO-LIVE | Labels: SRE/DevOps Security
> Mô tả: Cần staging gần giống production để thử sandbox thanh toán và đợt phát hành mà không đụng vào dữ liệu thật.
> DoD: Ba môi trường tách biệt; một lệnh triển khai staging; có kế hoạch rollback; email OTP vào hộp thư chính.

| # | Dòng checklist (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| DEVOPS-02.1 | Môi trường dev, staging, prod: DB, Redis, secrets, tên miền, khóa cổng thanh toán sandbox/live riêng | `PARTIAL` | Hồ sơ Spring `dev`/`staging`/`prod` có sẵn; tên miền/staging server ngoài hạ tầng thuộc `HUMAN` |
| DEVOPS-02.2 | Cấu hình và secrets ngoài mã nguồn, không trong image hay file .env commit | `PARTIAL` | Đang vá fallback secret trong `docker-compose.yml` ở Pha 0; xác nhận không copy `.env`/key vào Dockerfile |
| DEVOPS-02.3 | Chiến lược migration expand/contract, kiểm tra migration trên bản sao dữ liệu trước khi chạy | `PARTIAL` | Kiểm tra tài liệu & script kiểm thử Flyway trên bản sao dữ liệu ở Pha 1/4 |
| DEVOPS-02.4 | Quy trình rollback ứng dụng và dữ liệu; thử diễn tập một lần | `PARTIAL` | Soạn & diễn tập rollback trên Docker local ở Pha 4 |
| DEVOPS-02.5 | Email giao dịch: SPF, DKIM, DMARC, tên miền gửi riêng, theo dõi tỷ lệ gửi thành công | `HUMAN` | Cấu hình DNS/SPF/DKIM/DMARC tên miền thật do Admin hạ tầng thực hiện |
| DEVOPS-02.6 | Feature flag cho tính năng tiền (bật tắt không cần deploy) | `PARTIAL` | Kiểm tra `platform_settings` runtime flag (`fintech.mode`, `money.custody.enabled`, gateway kill-switch) ở Pha 2 |

---

### 4.2. Thẻ CŨ trong Sprint 3 (18 thẻ — tách theo dấu `;` thành từng ý nhỏ nguyên văn)

#### `[LEDGER-01]` (Cường | 8 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| LEDGER-01.1 | Bảng tài khoản (chart of accounts) cố định có tài liệu | `PARTIAL` | Kiểm chứng ở Pha 2; **chờ review 2 người, có Cường** |
| LEDGER-01.2 | tiền kiểu số nguyên đồng, quy tắc làm tròn thống nhất | `PARTIAL` | Kiểm chứng ở Pha 2; **chờ review 2 người, có Cường** |
| LEDGER-01.3 | mọi nghiệp vụ tiền đi qua duy nhất một LedgerService (ArchUnit cấm ghi ngoài) | `PARTIAL` | Kiểm chứng ArchUnit rule + code ở Pha 2 |
| LEDGER-01.4 | bút toán đảo có lý do và người duyệt | `PARTIAL` | Kiểm chứng API & 2-person approval ở Pha 2 |
| LEDGER-01.5 | chụp số dư theo ngày | `PARTIAL` | Kiểm chứng snapshot job & DB ở Pha 2 |

#### `[PAY-01]` (Cường | 5 → 8 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| PAY-01.1 | Xử lý trả thiếu/thừa/trả trễ sau khi QR hết hạn/trả 2 lần/nội dung chuyển khoản sai (đưa vào hàng khớp thủ công) | `PARTIAL` | Kiểm chứng state + manual matching queue ở Pha 2 (`SANDBOX_ONLY`); **chờ review 2 người, có Cường** |
| PAY-01.2 | intent có thời hạn và hủy được | `PARTIAL` | Kiểm chứng expiresAt & cancel API ở Pha 2 |
| PAY-01.3 | chống phát lại webhook bằng timestamp | `PARTIAL` | Kiểm chứng replay window + nonce store ở Pha 2 |
| PAY-01.4 | allowlist IP, xoay secret | `PARTIAL` | Kiểm chứng filter IP + dual-secret rotation ở Pha 2 |
| PAY-01.5 | test hợp đồng với sandbox | `PARTIAL` | Kiểm chứng sandbox contract tests ở Pha 2/4 |
| PAY-01.6 | nút tắt cổng khi sự cố | `PARTIAL` | Kiểm chứng runtime kill switch ở Pha 2 |

#### `[PAY-02]` (Cường | 2 → 3 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| PAY-02.1 | Báo cáo đối soát gửi kế toán | `PARTIAL` | Kiểm chứng xuất/gửi báo cáo đối soát ở Pha 2; **chờ review 2 người, có Cường** |
| PAY-02.2 | quy trình xử lý MISMATCH có SLA | `PARTIAL` | Kiểm chứng workflow MISMATCH + SLA tracking ở Pha 2 |
| PAY-02.3 | đối soát cả giao dịch hoàn và chi | `PARTIAL` | Kiểm chứng đối soát bao gồm refund & payout ở Pha 2 |

#### `[APPT-RENT-BE]` (Dev 2 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| APPT-RENT-BE.1 | Yêu cầu SĐT đã xác thực | `PARTIAL` | Kiểm chứng guard phoneVerified trên BE ở Pha 2 |
| APPT-RENT-BE.2 | rate limit chống spam | `PARTIAL` | Kiểm chứng Redis/Bucket rate limit ở Pha 2 |
| APPT-RENT-BE.3 | khóa kiểm tra phòng còn trống tại thời điểm giữ chỗ | `PARTIAL` | Kiểm chứng pessimistic/unique lock DB ở Pha 2 |
| APPT-RENT-BE.4 | hết hạn giữ chỗ tự hoàn cọc | `PARTIAL` | Kiểm chứng hold expiry scheduler + refund trigger ở Pha 2; **chờ review 2 người, có Cường** |
| APPT-RENT-BE.5 | idempotency-key khi tạo yêu cầu | `PARTIAL` | Kiểm chứng idempotency filter/store ở Pha 2 |
| APPT-RENT-BE.6 | phát sự kiện outbox | `PARTIAL` | Kiểm chứng outbox event trong cùng transaction ở Pha 2 |

#### `[CONTRACT-BE]` (Dev 2 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| CONTRACT-BE.1 | Chụp bất biến + hash tài liệu | `PARTIAL` | Kiểm chứng SHA-256 hash & snapshot JSON/text ở Pha 2 |
| CONTRACT-BE.2 | nhật ký ký (IP, User-Agent, mã OTP, thời điểm) xuất được làm bằng chứng | `PARTIAL` | Kiểm chứng audit table + export proof endpoint ở Pha 2 |
| CONTRACT-BE.3 | khóa sửa sau khi ký | `PARTIAL` | Kiểm chứng immutable guard sau SIGNED ở Pha 2 |
| CONTRACT-BE.4 | PDF lưu kho riêng có phiên bản | `PARTIAL` | Kiểm chứng private storage + version metadata ở Pha 2 |
| CONTRACT-BE.5 | bước kiểm tra pháp lý theo LEGAL-02 | `HUMAN` | Phụ thuộc `LEGAL-02` duyệt mẫu hợp đồng |

#### `[ESCROW-01]` (Dev 2 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| ESCROW-01.1 | Chính sách hoàn/phạt cấu hình được | `PARTIAL` | Gắn cờ `money.custody.enabled=false` theo ADR-008 Model A; kiểm chứng logic sandbox ở Pha 2; **chờ review 2 người, có Cường** |
| ESCROW-01.2 | tự giải ngân dừng khi có tranh chấp | `PARTIAL` | Kiểm chứng dispute pause lock ở Pha 2 |
| ESCROW-01.3 | hai bên xác nhận move-in | `PARTIAL` | Kiểm chứng double confirmation tenant + landlord ở Pha 2 |
| ESCROW-01.4 | tên STK khớp eKYC | `PARTIAL` | Kiểm chứng so khớp tên chủ tài khoản eKYC ở Pha 2 |
| ESCROW-01.5 | hạn mức theo FIN-RISK-01 | `PARTIAL` | Kiểm chứng tích hợp `FinRiskService` ở Pha 2 |
| ESCROW-01.6 | duyệt 2 người khi chi trên ngưỡng | `PARTIAL` | Kiểm chứng maker-checker chi vượt ngưỡng ở Pha 2; **chờ review 2 người, có Cường** |

#### `[DASH-FIX-1]` (Dev 3 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| DASH-FIX-1.1 | CSRF cho route BFF (SameSite + kiểm tra Origin) | `PARTIAL` | Kiểm chứng `/api/v1/[...path]` + `/api/auth/*` Origin/SameSite guard ở Pha 3 |
| DASH-FIX-1.2 | không để token xuất hiện trong phản hồi hay log | `PARTIAL` | Kiểm chứng không trả `accessToken`/`refreshToken` về body client & log ở Pha 3 |
| DASH-FIX-1.3 | làm mới token có khóa chống đua | `PARTIAL` | Kiểm chứng single-flight refresh mutex ở BFF/FE ở Pha 3 |
| DASH-FIX-1.4 | e2e xác nhận JS không đọc được token | `PARTIAL` | Kiểm chứng `document.cookie` & `localStorage` không có JWT trong Playwright E2E ở Pha 3–4 |

#### `[APPT-RENT-FE]` (Dev 3 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| APPT-RENT-FE.1 | Chống bấm đúp (khóa client + idempotency-key) | `PARTIAL` | Kiểm chứng loading lock + UUID idempotency header ở Pha 3 |
| APPT-RENT-FE.2 | hiển thị số tiền và chính sách trước khi xác nhận | `PARTIAL` | Kiểm chứng UI preview trước nút submit ở Pha 3 |
| APPT-RENT-FE.3 | xử lý rớt mạng giữa lúc thanh toán (kiểm tra lại trạng thái) | `PARTIAL` | Kiểm chứng re-query status khi timeout/network error ở Pha 3 |
| APPT-RENT-FE.4 | thử trên Zalo, Safari iOS | `HUMAN` | Yêu cầu kiểm thử thủ công trên thiết bị thật iOS Safari / Zalo WebView (WebKit Playwright dùng để kiểm chứng sơ bộ ở Pha 4) |

#### `[CONTRACT-FE]` (Dev 3 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| CONTRACT-FE.1 | Chống bấm đúp khi ký | `PARTIAL` | Kiểm chứng button disable + submit guard ở Pha 3 |
| CONTRACT-FE.2 | hiển thị hash và thời điểm ký | `PARTIAL` | Kiểm chứng UI hiển thị SHA-256 hash & signedAt ở Pha 3 |
| CONTRACT-FE.3 | xử lý OTP hết hạn/quá lần | `PARTIAL` | Kiểm chứng UI đếm ngược, lỗi hết hạn/khóa OTP & nút gửi lại ở Pha 3 |
| CONTRACT-FE.4 | bản in rõ ràng | `PARTIAL` | Kiểm chứng `@media print` / print preview ở Pha 3 |

#### `[STORAGE-01]` (Dev 3 | 3 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| STORAGE-01.1 | Mã hóa phía server | `PARTIAL` | Kiểm chứng server-side encryption (AES-GCM / SSE) ở Pha 1/3 |
| STORAGE-01.2 | bucket riêng tư với liên kết ký ngắn hạn cho KYC/hợp đồng | `PARTIAL` | Kiểm chứng HMAC signed URL TTL ngắn & auth check ở Pha 3 |
| STORAGE-01.3 | chính sách lưu giữ (lifecycle) | `PARTIAL` | Kiểm chứng retention cleanup scheduler ở Pha 3 |
| STORAGE-01.4 | sao lưu chéo vùng | `PARTIAL` | Kiểm chứng tài liệu/script cấu hình backup cross-region ở Pha 3–4 (bucket cloud thực thuộc `HUMAN`) |

#### `[METER-01]` (Dev 4 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| METER-01.1 | Sửa/hủy chỉ số có lý do và lịch sử | `PARTIAL` | Kiểm chứng amendment history & reason ở Pha 2 |
| METER-01.2 | khóa sửa sau khi hóa đơn ISSUED | `PARTIAL` | Kiểm chứng lock guard khi hóa đơn ISSUED ở Pha 2 |
| METER-01.3 | cảnh báo chỉ số tăng đột biến | `PARTIAL` | Kiểm chứng threshold anomaly detection ở Pha 2 |
| METER-01.4 | ảnh công tơ bắt buộc theo cấu hình | `PARTIAL` | Kiểm chứng validation setting bắt buộc ảnh ở Pha 2 |

#### `[INVOICE-01/02]` (Dev 4 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| INVOICE-01/02.1 | Bất biến sau PAID | `PARTIAL` | Kiểm chứng lock sửa/xóa hóa đơn PAID ở Pha 2 |
| INVOICE-01/02.2 | dùng hóa đơn điều chỉnh/ghi có thay vì sửa | `PARTIAL` | Kiểm chứng credit/adjustment invoice API ở Pha 2 |
| INVOICE-01/02.3 | thanh toán một phần và tiền thừa (khấu trừ kỳ sau) | `PARTIAL` | Kiểm chứng partial pay & overpayment credit balance ở Pha 2; **chờ review 2 người, có Cường** |
| INVOICE-01/02.4 | sinh hóa đơn idempotent (không trùng kỳ) | `PARTIAL` | Kiểm chứng unique key `(contract_id, period)` ở Pha 2 |
| INVOICE-01/02.5 | xem trước trước khi sinh hàng loạt | `PARTIAL` | Kiểm chứng dry-run preview API/UI ở Pha 2–3 |
| INVOICE-01/02.6 | số hóa đơn tuần tự | `PARTIAL` | Kiểm chứng sequence generator không trùng ở Pha 2 |

#### `[PAY-VIP-FE]` (Dev 4 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| PAY-VIP-FE.1 | Đổi tên PAY-FE. Mục "Trang subscription mua gói VIP" chuyển sang PRICING-CONV-01 (Sprint 4) cùng SUB-01-BE | `PARTIAL` | Kiểm chứng FE route `/payments` & tách VIP sang Sprint 4 ở Pha 3 |
| PAY-VIP-FE.2 | giữ điểm | `PARTIAL` | Giữ 5 điểm cho phạm vi `PAY-FE` |
| PAY-VIP-FE.3 | Thêm trạng thái chờ/xử lý/thất bại rõ ràng, xử lý thanh toán quá hạn | `PARTIAL` | Kiểm chứng UI states & expired recovery ở Pha 3; **chờ review 2 người, có Cường** |

#### `[DASH-STATS-BE]` (Dev 4 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| DASH-STATS-BE.1 | Số liệu tiền lấy từ sổ cái (không tính lại nhiều nơi) | `PARTIAL` | Kiểm chứng query nguồn sổ cái ở Pha 2; **chờ review 2 người, có Cường** |
| DASH-STATS-BE.2 | cache có invalidate | `PARTIAL` | Kiểm chứng evict/TTL khi có bút toán mới ở Pha 2 |
| DASH-STATS-BE.3 | chống IDOR | `PARTIAL` | Kiểm chứng ownership filter theo userId/role ở Pha 2 |
| DASH-STATS-BE.4 | export có giới hạn và ghi log truy cập | `PARTIAL` | Kiểm chứng rate/row limit & audit log ở Pha 2 |
| DASH-STATS-BE.5 | tài khoản mới trả dữ liệu rỗng | `PARTIAL` | Kiểm chứng empty-state contract cho tài khoản mới ở Pha 2 |

#### `[BROKER-01]` (Dev 5 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| BROKER-01.1 | Event catalog và phiên bản schema | `PARTIAL` | Kiểm chứng `schemaVersion` + docs catalog ở Pha 2 |
| BROKER-01.2 | thứ tự theo aggregate | `PARTIAL` | Kiểm chứng routing/sequence guard theo `aggregateId` ở Pha 2 |
| BROKER-01.3 | theo dõi độ trễ hàng đợi | `PARTIAL` | Kiểm chứng actuator/metrics queue lag & outbox lag ở Pha 2 |
| BROKER-01.4 | dọn outbox | `PARTIAL` | Kiểm chứng retention cleanup job cho outbox `SENT` ở Pha 2 |
| BROKER-01.5 | chạy lại DLQ có ghi log | `PARTIAL` | Kiểm chứng `DeadLetterAdminService` replay + audit log ở Pha 2 |

#### `[SEC-PROD-01]` (Dev 5 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| SEC-PROD-01.1 | Quy trình khôi phục 2FA khi mất điện thoại (2 admin duyệt) | `PARTIAL` | Kiểm chứng API & UI maker-checker khôi phục 2FA ở Pha 1–2 |
| SEC-PROD-01.2 | giới hạn thử TOTP | `PARTIAL` | Kiểm chứng rate limit / lockout TOTP attempts ở Pha 1 |
| SEC-PROD-01.3 | xoay khóa theo kid | `PARTIAL` | Kiểm chứng `RsaKeyProvider` hỗ trợ đa `kid` + JWKS ở Pha 1 |
| SEC-PROD-01.4 | secrets không nằm trong image | `PARTIAL` | Kiểm chứng Dockerfile + Compose env injection ở Pha 0–1 |

#### `[PHONE-OTP-01]` (Dev 5 | 3 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| PHONE-OTP-01.1 | Đổi SĐT phải xác thực cả số cũ và số mới | `PARTIAL` | Kiểm chứng two-step old+new OTP verification ở Pha 1/3 |
| PHONE-OTP-01.2 | chặn số ảo/tạm | `PARTIAL` | Kiểm chứng validator chặn đầu số ảo/disposable ở Pha 1 |
| PHONE-OTP-01.3 | hạn mức chi phí SMS | `PARTIAL` | Kiểm chứng daily/monthly SMS quota counter ở Pha 1 |
| PHONE-OTP-01.4 | không lưu mã OTP | `PARTIAL` | Kiểm chứng chỉ lưu SHA-256 hash + TTL Redis, không lưu plaintext ở Pha 1 |
| PHONE-OTP-01.5 | kênh dự phòng (Zalo → SMS) | `PARTIAL` | Kiểm chứng provider abstraction Zalo ZNS -> SMS sandbox stub ở Pha 1 |

#### `[QA-FINTECH]` (Dev 5 | 5 pts)
| # | Ý nhỏ (nguyên văn) | Trạng thái | Bằng chứng / Ghi chú |
|---|---|---|---|
| QA-FINTECH.1 | Thêm kịch bản thất bại (cổng timeout, mất mạng giữa chừng, service sập giữa saga), test song song ledger, test hoàn tiền, đối soát lệch | `PARTIAL` | Kiểm chứng & bổ sung suite integration/chaos ở Pha 4; **chờ review 2 người, có Cường** |
| QA-FINTECH.2 | chạy trên staging với sandbox | `HUMAN` | Cần triển khai môi trường staging có sandbox thật từ DevOps/Infra |
| QA-FINTECH.3 | checklist go/no-go tài chính có chữ ký 2 người | `HUMAN` | Checklist go/no-go cuối sprint cần ký tay/xác nhận 2 người (Cường + Reviewer) |

---

## PART C — DANH SÁCH `HUMAN` (NGOÀI TẦM AGENT — CẦN CON NGƯỜI XỬ LÝ)

1. **`LEGAL-02` (Toàn bộ câu hỏi pháp lý & thuế):** Thuê/tham vấn luật sư & kế toán chốt giấy phép trung gian thanh toán (NĐ 52/2024), nghĩa vụ khấu trừ thuế (NĐ 117/2025, Luật 56/2024/QH15, TT 40/2021), hiệu lực hợp đồng điện tử/ký OTP, đăng ký Bộ Công Thương, hợp đồng PSP, điều khoản sử dụng và mẫu hợp đồng chuẩn.
2. **Xoay khóa (Key/Secret Rotation từ kết quả `gitleaks`):** Thu hồi, tạo mới hoàn toàn và không tái sử dụng bất kỳ bí mật/mẫu mật khẩu nào đã từng xuất hiện trong lịch sử Git của cả 2 repo (không in giá trị bí mật):
   - **`accommodation-service` (9 findings / 100 commits):**
     - `16f6b22` | `.env:9` | `generic-api-key` (`DB_PASSWORD` từng bị commit trong lịch sử)
     - `16f6b22` | `.env.example:9` | `generic-api-key` (`DB_PASSWORD` mẫu, đã thay bằng placeholder ở Pha 0)
     - `16f6b22` | `DEPLOY_GUIDE.md:75,117` | `generic-api-key` (`DB_PASSWORD`, `token` ví dụ)
     - `1c8918b` | `identity-service/.../FaceCryptoService.java:30` | `generic-api-key` (`DEV_FALLBACK_KEY_BASE64` — xoay `FACE_ENCRYPTION_KEY` ở mọi môi trường thật)
     - `85978e2` | `docs/audit/AUTH-FLOW-A.md:25` | `generic-api-key` (`token`)
     - `108c847` | `identity-service/.../RsaKeyProvider.java:39` | `private-key` (chuỗi PEM header/footer — đảm bảo xoay cặp khóa RSA `kid` trên staging/prod)
     - `6905232` | `booking-service/.../LandlordAppointmentSecurityMatrixTest.java:183` & `listing-service/.../LandlordNamespaceSecurityMatrixTest.java:187` | `jwt` (token giả lập `alg=none` trong test)
   - **`accommodation-web` (4 findings / 106 commits):**
     - `ffce5ac` | `e2e/auth-ui-live.spec.ts:262` | `jwt` (test fixture)
     - `54f1ae3` | `docs/audit/AUTH-FLOW-A.md:25` | `generic-api-key` (`token`)
     - `15900eb` | `scripts/crawl-routes.mjs:4` | `private-key` (khóa ký test giả lập)
     - `1e8a42b` | `e2e/helpers/auth-helper.ts:4` | `private-key` (khóa ký test giả lập)
3. **Cổng thanh toán & SMS Vendor thật:** Ký hợp đồng thương mại với VietQR/Sepay/PayOS/VNPay và nhà cung cấp Zalo ZNS / SMS Brandname, cấp sandbox/production merchant credentials.
4. **Hạ tầng Staging/Production & Email Deliverability (`DEVOPS-02.1`, `DEVOPS-02.5`):** Cung cấp VPS/Cloud Kubernetes, tên miền thật, cấu hình DNS SPF/DKIM/DMARC, chứng chỉ TLS, bucket object storage cloud bật cross-region replication, và Secret Manager/Vault production.
5. **Kiểm thử thiết bị thật iOS Safari & Zalo WebView (`APPT-RENT-FE.4`):** QA/Product kiểm thử thực tế trên thiết bị iPhone Safari và app Zalo.
6. **Review 2 người & Ký Go/No-Go Tài chính (`QA-FINTECH.3`, mọi thẻ tiền):** Hai kỹ sư (bắt buộc có Cường) duyệt chéo code tài chính, đối soát trên staging và ký checklist Go/No-Go trước khi mở tiền thật.

---

## PART D — NHẬT KÝ BẰNG CHỨNG PHA 0 (2026-11-12)

- **Kiểm chứng chéo nguồn tài liệu:**
  - `index.md` (`KE_HOACH_TRELLO_V2.md`): 28 thẻ / 128 điểm (Cường 32, Dev 2 26, Dev 3 24, Dev 4 23, Dev 5 23); Mục 4.1 gồm 10 thẻ mới với đúng 55 dòng checklist (`LEGAL-02` 7, `HARDEN-01` 9, `SAGA-01` 5, `LEDGER-02` 5, `FIN-RISK-01` 6, `REFUND-01` 5, `KYC-03` 4, `ROOM-03` 4, `CONTRACT-02` 4, `DEVOPS-02` 6); Bảng 4.2 gồm 18 dòng thẻ cũ.
- **Kết quả kiểm thử nền sạch hoàn toàn (Backend — MySQL `8.4` LTS, `--no-build-cache --no-daemon`):**
  - `common`: 25 tests, 0 failures, 0 errors, 0 skipped
  - `identity-service`: 181 tests, 0 failures, 0 errors, 0 skipped
  - `listing-service`: 83 tests, 0 failures, 0 errors, 0 skipped
  - `booking-service`: 43 tests, 0 failures, 0 errors, 0 skipped
  - `billing-service`: 32 tests, 0 failures, 0 errors, 0 skipped
  - `communication-service`: 14 tests, 0 failures, 0 errors, 0 skipped
  - `admin-service`: 10 tests, 0 failures, 0 errors, 0 skipped
  - `api-gateway`: 8 tests, 0 failures, 0 errors, 0 skipped
  - **Tổng Backend:** `396 tests, 0 failures, 0 errors, 0 skipped` (cả 8 suite Testcontainers đều chạy qua `mysql:8.4` LTS).
- **Kết quả kiểm thử nền Frontend (`accommodation-web`):**
  - `npx tsc --noEmit`: PASS (`exit 0`)
  - `npm run lint -- --max-warnings=0`: PASS (`exit 0`, 0 warnings/errors; chỉ có cảnh báo deprecation config plugin `eslint-plugin-boundaries`)
  - `npx vitest run`: 17 test files, 77/77 tests PASS (`exit 0`)
  - `npm run build`: PASS (`exit 0`, 188 static/dynamic routes)
  - `verify-i18n.mjs`: PASS (`1936` keys VI/EN, 100% parity)
  - `verify-links.mjs`: PASS (`86` unique internal links across `384` source files)
  - `verify-manifest.mjs`: PASS (`224` raster images, Hero #1 `246.4 KB`)
  - `npx playwright test` (toàn bộ 246 tests): **170 passed, 71 failed, 5 did not run** (`exit 1` — phần lớn fail HTTP 401 hoặc timeout 30s ở các suite `admin-*`, `landlord-*`, `common-matrix-audit` `/support`, `auth-ui-live`, `capture-landlord-visuals`, `command-palette`, `hdr1-live-verification`). Được ghi nhận để xử lý triệt để ở Pha 3–4.
  - Sau khi sửa `next.config.ts` (loại bỏ rewrite `/api/v1/:path*` đè BFF Route Handler và bổ sung `INTERNAL_API_URL`) và đồng bộ `package-lock.json`, đã chạy lại `npx tsc --noEmit`, `npm run lint -- --max-warnings=0`, `npx vitest run` (17 files / 77 tests), `npm run build` (188 routes) và 3 script `verify-i18n/links/manifest`: tất cả PASS (`exit 0`).
- **Hạ tầng Docker & Bí mật (P0):**
  - Loại bỏ fallback mật khẩu cứng và `JWT_SECRET` thừa trong `docker-compose.yml`, truyền `INTERNAL_API_URL=http://api-gateway:8080/api/v1` cho `web`, chuyển script khởi tạo DB user từ SQL cứng sang `docker/mysql/01-init-service-users.sh` đọc biến môi trường, tạo `.env` cục bộ (gitignored) sinh khóa ngẫu nhiên, thay `DB_PASSWORD` trong `.env.example` bằng placeholder, bổ sung `.dockerignore` cho cả 2 repo và đồng bộ `package-lock.json` ở `accommodation-web` (fix lỗi `npm ci` thiếu `@swc/helpers@0.5.24`).
  - Dựng sạch hoàn toàn từ volume trống (`docker compose --profile full down -v --remove-orphans`, build lại image `web`, sau đó `docker compose --profile full up -d`): 11/11 service có healthcheck đều `healthy` + `cozynest_web` `Up` trên MySQL `8.4.13` LTS (6 schema + 6 tài khoản `*_user` độc lập) & RabbitMQ `3.13.7`, HTTP `/actuator/health` trả `200 {"status":"UP"}` trên đủ 7 cổng `8080..8086`, Web `http://localhost:3000/vi` trả HTTP `200` (`len=164132`), BFF `http://localhost:3000/api/v1/rooms?limit=3` trả HTTP `200` (`len=24261`).



