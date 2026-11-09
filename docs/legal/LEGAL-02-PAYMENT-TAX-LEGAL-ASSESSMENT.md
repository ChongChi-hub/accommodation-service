# [LEGAL-02] BÁO CÁO THẨM ĐỊNH PHÁP LÝ MÔ HÌNH DÒNG TIỀN, THUẾ VÀ HỢP ĐỒNG ĐIỆN TỬ
## Dự án: Nền tảng Thuê phòng trọ, Nhà nguyên căn & Căn hộ Cozynest (Tổ Ấm)
> **Mã công việc:** `[LEGAL-02]` | **Story Points:** 3 | **Độ ưu tiên:** `P0-GO-LIVE`  
> **Chế độ:** `LEGAL RESEARCH + BUSINESS FLOW AUDIT + DOCUMENTATION ONLY`  
> **Ngày lập báo cáo:** 09/11/2026  
> **Chủ nhiệm kỹ thuật:** Cường (Dev 1 / Tech Lead)  
> **Tình trạng:** `PENDING_EXTERNAL_LEGAL_REVIEW` (Chờ ý kiến tư vấn chính thức từ Luật sư và Kiểm toán viên/Kế toán trưởng)

---

## 1. TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

1. **Bối cảnh vận hành:** Tổ Ấm là nền tảng thương mại điện tử kết nối Người thuê (Tenant) và Chủ trọ (Landlord) đối với bất động sản cho thuê (phòng trọ, căn hộ, nhà nguyên căn, ở ghép). Định hướng sản phẩm là vận hành với người dùng thật, tiền cọc thật, tiền thuê thật và hợp đồng điện tử có giá trị thi hành.
2. **Cảnh báo cốt lõi:**  
   - Tổ Ấm **KHÔNG PHẢI TỔ CHỨC TÍN DỤNG** và **KHÔNG CÓ GIẤY PHÉP TRUNG GIAN THANH TOÁN** của Ngân hàng Nhà nước Việt Nam (NHNN).
   - Theo **Nghị định 52/2024/NĐ-CP** (hiệu lực từ 01/07/2024), việc một doanh nghiệp công nghệ tự ý mở tài khoản công ty để nhận tiền cọc của người thuê, giữ tiền và tự động giải ngân cho chủ trọ cấu thành hành vi **cung ứng dịch vụ trung gian thanh toán (thu hộ, chi hộ, giữ hộ tiền) không phép**, tiềm ẩn rủi ro xử phạt vi phạm hành chính (lên tới 500 triệu đồng) hoặc truy cứu trách nhiệm hình sự.
   - Do đó, **Mô hình D (Tổ Ấm tự thu, giữ cọc và giải ngân trên tài khoản công ty) bị BÁC BỎ HOÀN TOÀN**.
3. **Mô hình tiền đề xuất cho giai đoạn Go-Live:**
   - **Giai đoạn 1 (Ra mắt an toàn): Áp dụng Mô hình A (Chuyển khoản trực tiếp P2P có hỗ trợ mã định danh VietQR):** Người thuê quét mã VietQR chuyển tiền trực tiếp vào tài khoản ngân hàng của Chủ trọ. Tổ Ấm **không chạm vào tiền**, chỉ tạo mã tham chiếu giao dịch (Transaction Reference), ghi nhận trạng thái và hỗ trợ quản lý hợp đồng. Nguồn thu duy nhất của Tổ Ấm là Phí dịch vụ đăng tin / Gói VIP / Phí tiện ích thu từ Chủ trọ (xuất hóa đơn GTGT đầy đủ).
   - **Giai đoạn 2 (Tăng trưởng & Giữ cọc an toàn): Áp dụng Mô hình B hoặc Mô hình C kết hợp Đối tác được cấp phép:** Tích hợp với Tổ chức Trung gian thanh toán có giấy phép NHNN (ví dụ: Cổng thanh toán có tính năng Split Payment / Escrow / Thu hộ chi hộ) hoặc Ngân hàng thương mại cung cấp giải pháp Tài khoản đảm bảo thanh toán (Blocked / Escrow Account) có thỏa thuận 3 bên.
4. **Trách nhiệm thuế:**
   - Theo Luật sửa đổi Luật Quản lý thuế số 56/2024/QH15 và các văn bản hướng dẫn năm 2025-2026, sàn TMĐT có chức năng thanh toán có nghĩa vụ khấu trừ, kê khai nộp thuế thay cho cá nhân kinh doanh trên sàn. Tuy nhiên, đối với dịch vụ cho thuê bất động sản của cá nhân (ngưỡng 100 triệu đồng/năm theo Thông tư 40/2021/TT-BTC), việc khấu trừ thuế chỉ phát sinh nếu Tổ Ấm trực tiếp tham gia xử lý dòng tiền thanh toán tiền thuê. Tiền đặt cọc về bản chất là biện pháp bảo đảm nghĩa vụ dân sự, không cấu thành doanh thu chịu thuế cho đến khi được chuyển thành tiền thuê hoặc tiền bồi thường.
5. **Hợp đồng điện tử & Ký OTP:**
   - Theo Luật Nhà ở 2023 (hiệu lực 01/08/2024), hợp đồng thuê nhà **không bắt buộc công chứng, chứng thực**.
   - Theo Luật Giao dịch điện tử 2023 (hiệu lực 01/07/2024), mã OTP qua SMS/Email **không phải là Chữ ký số** (Digital Signature có chứng thư CA), mà là **Phương thức xác thực điện tử** thể hiện sự chấp thuận của các bên. Hệ thống bắt buộc phải lưu trữ toàn văn thông điệp dữ liệu, mã băm SHA-256 bất biến, nhật ký IP, thiết bị và dấu vết phiên để làm chứng cứ hợp lệ trước Tòa án.

---

## 2. MÔ HÌNH KINH DOANH SẢN PHẨM (PRODUCT BUSINESS MODEL)

### 2.1. Các chủ thể tham gia
1. **Người thuê (Tenant):** Người tìm kiếm phòng trọ, đặt lịch hẹn xem phòng, gửi yêu cầu thuê, thanh toán tiền cọc, ký hợp đồng điện tử và đóng tiền phòng định kỳ.
2. **Chủ trọ (Landlord):** Người sở hữu hoặc có quyền khai thác hợp pháp bất động sản, đăng tải thông tin phòng, tiếp nhận yêu cầu thuê, ký hợp đồng điện tử, nhận tiền thuê và ghi nhận chỉ số điện nước.
3. **Nền tảng Tổ Ấm (Platform Operator):** Đơn vị vận hành sàn TMĐT, cung cấp hạ tầng công nghệ tìm kiếm, xác thực danh tính (eKYC), mẫu hợp đồng điện tử, công cụ quản lý vận hành phòng trọ và thu phí dịch vụ phần mềm.

### 2.2. Các dòng tiền phát sinh trên nền tảng
| STT | Dòng tiền | Người chuyển | Người nhận | Bản chất kinh tế |
|---|---|---|---|---|
| 1 | **Tiền cọc giữ chỗ / Đảm bảo hợp đồng** | Người thuê | Chủ trọ (hoặc Đơn vị giữ hộ có phép) | Khoản tiền bảo đảm thực hiện nghĩa vụ theo Điều 328 BLDS 2015. |
| 2 | **Tiền thuê phòng hàng tháng** | Người thuê | Chủ trọ | Tiền thanh toán sử dụng tài sản thuê theo hợp đồng thuê nhà. |
| 3 | **Tiền dịch vụ tiện ích (Điện, Nước, Rác, WiFi)** | Người thuê | Chủ trọ | Chi phí thanh toán theo chỉ số sử dụng thực tế hàng tháng. |
| 4 | **Phí gói dịch vụ / Tin đăng VIP** | Chủ trọ | Công ty Tổ Ấm | Doanh thu dịch vụ phần mềm / quảng bá của Tổ Ấm (chịu thuế GTGT 10%). |
| 5 | **Phí hoa hồng giao dịch thành công (Take-rate)** | Chủ trọ | Công ty Tổ Ấm | Phí môi giới / xúc tiến thương mại thu trên mỗi hợp đồng ký thành công. |
| 6 | **Tiền hoàn cọc (Refund)** | Chủ trọ / Đơn vị giữ cọc | Người thuê | Hoàn trả khoản tiền bảo đảm khi chấm dứt hợp đồng hợp lệ. |

---

## 3. KHẢO SÁT DÒNG TIỀN TRONG MÃ NGUỒN HIỆN TẠI

Kiểm tra đối chiếu thực tế trên 2 kho mã nguồn ngày 09/11/2026:

### 3.1. Phía Backend (`accommodation-service`)
1. **Module `billing-service`:**
   - Cơ sở dữ liệu Flyway [V1__init_billing_schema.sql](file:///d:/RIKKEI/THUC_TAP/PROJECT/accommodation-service/billing-service/src/main/resources/db/migration/V1__init_billing_schema.sql) đã khai báo cấu trúc bảng:
     - `deposits`: `tenant_id`, `landlord_id`, `original_amount`, `current_balance`, `status` (`HELD`, `DEDUCTED`, `REFUNDED`, `FORFEITED`).
     - `payments`: `invoice_id`, `payer_id`, `amount`, `method` (`VIETQR`, `BANK_TRANSFER`, `CASH`, `MOMO`, `VNPAY`), `status` (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`).
     - `invoices` & `invoice_items`: Hóa đơn tiền phòng hàng tháng kèm chi tiết điện, nước, dịch vụ.
     - `listing_plans`: Bảng giá các gói tin VIP.
   - **Thực tế mã nguồn Java:** Chỉ có `Invoice.java` và `HealthController.java`. **Chưa có bất kỳ dòng code Java nào thực hiện thanh toán, trừ tiền, tích hợp cổng hay giữ tiền**. Toàn bộ dữ liệu thanh toán hiện có trong DB là dữ liệu Seed giả lập ([R__01_seed_demo_billing.sql](file:///d:/RIKKEI/THUC_TAP/PROJECT/accommodation-service/billing-service/src/main/resources/db/seed/R__01_seed_demo_billing.sql)).
2. **Module `booking-service`:**
   - Cơ sở dữ liệu có bảng `contracts`, `contract_signatures`, `rental_requests`.
   - Seed data có mẫu chữ ký `contract_signatures` lưu `signature_type='OTP'`, `signature_hash`, `ip_address`.
   - Mã nguồn Java chỉ có nghiệp vụ `ViewingAppointment` (Lịch hẹn xem phòng). Chưa có nghiệp vụ chuyển trạng thái tiền cọc hay sinh giao dịch thanh toán tự động.

### 3.2. Phía Frontend (`accommodation-web`)
1. **Màn hình thanh toán & hóa đơn:**
   - `dashboard/tenant/invoices/page.tsx`, `dashboard/tenant/payments/page.tsx`, `dashboard/landlord/contracts/page.tsx`: Toàn bộ sử dụng mảng JavaScript tĩnh (Mock data).
   - [StaticTransactionRepository.ts](file:///d:/RIKKEI/THUC_TAP/PROJECT/accommodation-web/src/infrastructure/static/StaticTransactionRepository.ts): Đang cung cấp danh sách giao dịch giả định (ví dụ: `TX-20261014-089`).
   - Chưa tích hợp SDK thanh toán của bất kỳ bên thứ ba nào (VNPay, MoMo, PayOS, Napas).

**KẾT LUẬN HIỆN TRẠNG:**  
Hệ thống hiện tại **hoàn toàn chưa có dòng tiền thật chảy qua**. Toàn bộ cấu trúc tiền tệ chỉ nằm ở tầng thiết kế schema cơ sở dữ liệu và giao diện mock tĩnh. Đây là cơ hội vàng để chuẩn hóa pháp lý trước khi triển khai bất kỳ đoạn mã thanh toán nào.

---

## 4. HỆ THỐNG VĂN BẢN PHÁP LUẬT ÁP DỤNG & TÌNH TRẠNG HIỆU LỰC

| STT | Tên văn bản | Số hiệu | Ngày ban hành | Ngày hiệu lực | Tình trạng tại 09/11/2026 | Điều khoản liên quan & Áp dụng |
|:---:|---|---|:---:|:---:|:---:|---|
| 1 | **Nghị định quy định về thanh toán không dùng tiền mặt** | `52/2024/NĐ-CP` | 15/05/2024 | 01/07/2024 | **Đang có hiệu lực** (Thay thế NĐ 101/2012 và NĐ 80/2016) | Điều 3, Điều 22, Điều 26, Điều 27: Định nghĩa dịch vụ trung gian thanh toán, điều kiện cấp phép, nghiêm cấm giữ tiền và thu hộ chi hộ không phép. |
| 2 | **Thông tư hướng dẫn dịch vụ trung gian thanh toán** | `40/2024/TT-NHNN` | 28/06/2024 | 01/07/2024 | **Đang có hiệu lực** | Quy định về mở tài khoản đảm bảo thanh toán và nghĩa vụ đối soát kỹ thuật. |
| 3 | **Luật Giao dịch điện tử** | `20/2023/QH15` | 22/06/2023 | 01/07/2024 | **Đang có hiệu lực** (Thay thế Luật GDĐT 2005) | Điều 9, 12, 22, 23, 25: Giá trị thông điệp dữ liệu như văn bản gốc, phân loại chữ ký điện tử, điều kiện bảo đảm tính toàn vẹn. |
| 4 | **Luật Nhà ở** | `27/2023/QH15` | 27/11/2023 | 01/08/2024 | **Đang có hiệu lực** (Hiệu lực sớm theo Luật 43/2024/QH15) | Điều 163, 164: Hợp đồng thuê nhà ở phải lập thành văn bản; **Khoản 2 Điều 164: Không bắt buộc công chứng, chứng thực**. |
| 5 | **Bộ luật Dân sự** | `91/2015/QH13` | 24/11/2015 | 01/01/2017 | **Đang có hiệu lực** | Điều 119: Giao dịch điện tử có giá trị như văn bản; Điều 328: Đặt cọc; Điều 472: Hợp đồng thuê tài sản. |
| 6 | **Luật Quản lý thuế (sửa đổi)** | `56/2024/QH15` | 29/11/2024 | 01/01/2025 | **Đang có hiệu lực** | Quy định trách nhiệm của tổ chức quản lý sàn TMĐT có chức năng thanh toán phải khấu trừ, kê khai, nộp thuế thay cho người bán/chủ trọ. |
| 7 | **Dự thảo / Nghị định quản lý thuế TMĐT** | `NĐ 117/2025/NĐ-CP` (Dự thảo / Hoàn thiện) | Năm 2025 | Năm 2025/2026 | **CẦN KIỂM TRA LẠI VĂN BẢN CHÍNH THỨC BAN HÀNH** | Trách nhiệm, quy trình kê khai, khấu trừ thuế GTGT và TNCN của sàn TMĐT. |
| 8 | **Thông tư hướng dẫn thuế đối với hộ, cá nhân kinh doanh** | `40/2021/TT-BTC` | 01/06/2021 | 01/08/2021 | **Đang có hiệu lực** | Phụ lục I: Dịch vụ cho thuê tài sản chịu 5% GTGT + 5% TNCN nếu doanh thu trên 100 triệu đồng/năm. Tiền cọc không phải doanh thu tính thuế. |
| 9 | **Nghị định về thương mại điện tử** | `52/2013/NĐ-CP` & `85/2021/NĐ-CP` | 25/09/2021 | 01/01/2022 | **Đang có hiệu lực** | Điều 35, 36: Đăng ký sàn giao dịch TMĐT với Bộ Công Thương; công khai thông tin chủ trọ; quy chế giải quyết khiếu nại. |
| 10 | **Nghị định bảo vệ dữ liệu cá nhân** | `13/2023/NĐ-CP` | 17/04/2023 | 01/07/2023 | **Đang có hiệu lực** | Điều 9, 11, 23, 24: Bảo vệ dữ liệu cá nhân nhạy cảm (CCCD, sinh trắc khuôn mặt, số tài khoản); nộp hồ sơ DPIA cho Bộ Công an. |

---

## 5. PHÂN TÍCH SO SÁNH 4 MÔ HÌNH DÒNG TIỀN

### 5.1. Mô hình A — Chuyển khoản trực tiếp P2P (Direct Peer-to-Peer Transfer via Dynamic VietQR)
- **Cơ chế vận hành:**  
  Người thuê quét mã VietQR chuyển tiền thẳng từ App Ngân hàng của mình vào Tài khoản Ngân hàng chính chủ của Chủ trọ. Tổ Ấm sinh chuỗi nội dung chuyển khoản chuẩn (ví dụ: `TOAM-DEP-10293`), lắng nghe biến động số dư của Chủ trọ (thông qua ủy quyền API ngân hàng Open Banking / VietQR của chủ trọ) hoặc Chủ trọ bấm xác nhận "Đã nhận tiền".
- **Đánh giá chi tiết:**
  | Tiêu chí | Đánh giá |
  |---|---|
  | Khả năng phù hợp pháp luật | **CAO NHẤT (Hoàn toàn hợp pháp)**. Tổ Ấm không đóng vai trò trung gian thanh toán, không nhận tiền, không giữ tiền. |
  | Giấy phép cần thiết | Chỉ cần Giấy phép Đăng ký sàn TMĐT với Bộ Công Thương. **Không cần giấy phép NHNN**. |
  | Ai kiểm soát tiền | Người thuê và Chủ trọ kiểm soát 100%. Tiền đi thẳng từ tài khoản A sang tài khoản B. |
  | Chủ tài khoản nhận tiền | Chủ trọ (đã xác thực CCCD và STK qua eKYC). |
  | Trách nhiệm hoàn tiền | Thuộc về Chủ trọ theo hợp đồng dân sự giữa hai bên. |
  | Nghĩa vụ thuế của sàn | **Rất thấp / Không phải khấu trừ thuế tiền thuê**: Vì sàn không có chức năng thanh toán trực tiếp dòng tiền thuê nhà, chỉ cần báo cáo thông tin theo Nghị định 91/2022/NĐ-CP. Sàn chỉ xuất hóa đơn GTGT cho phí dịch vụ / gói VIP của sàn. |
  | Độ phức tạp kỹ thuật | **Thấp - Trung bình**. Dễ triển khai nhanh, an toàn cho Sprint 3. |
  | Rủi ro tài chính | Tổ Ấm không chịu rủi ro bồi thường tài chính trực tiếp, không lo rủi ro thanh khoản hay đóng băng tài khoản. |
  | Nhược điểm | Khó giữ cọc bảo đảm để can thiệp cưỡng chế khi có tranh chấp (chủ trọ bùng cọc thì người thuê phải khởi kiện dân sự). |

### 5.2. Mô hình B — Thanh toán qua Đối tác Trung gian được cấp phép (Licensed Payment Gateway / Split Payment)
- **Cơ chế vận hành:**  
  Tổ Ấm tích hợp cổng thanh toán được cấp phép của Ngân hàng Nhà nước (như VietinBank, VNPay, MoMo, PayOS / Napas). Tiền cọc hoặc tiền thuê của Người thuê được thanh toán qua cổng. Cổng thanh toán giữ tiền theo cơ chế Escrow / Giữ hộ của tổ chức trung gian, hoặc tự động Split (chia tiền): chuyển 95% cho Chủ trọ, 5% phí dịch vụ cho Tổ Ấm theo lệnh API.
- **Đánh giá chi tiết:**
  | Tiêu chí | Đánh giá |
  |---|---|
  | Khả năng phù hợp pháp luật | **HỢP PHÁP** nếu đối tác trung gian có giấy phép Dịch vụ hỗ trợ thu hộ, chi hộ và Cổng thanh toán điện tử theo Nghị định 52/2024/NĐ-CP. |
  | Giấy phép cần thiết | Đối tác phải có Giấy phép TGTT của NHNN. Tổ Ấm ký hợp đồng đại lý / đối tác công nghệ kết nối. |
  | Ai kiểm soát tiền | Đối tác trung gian thanh toán và Ngân hàng hợp tác kiểm soát. Tiền nằm trong Tài khoản đảm bảo thanh toán của đối tác. |
  | Chủ tài khoản | Tài khoản đảm bảo thanh toán của Tổ chức TGTT mở tại Ngân hàng. |
  | Trách nhiệm hoàn tiền | Đối tác trung gian thực hiện hoàn tiền theo lệnh giải tỏa hợp lệ từ hệ thống Tổ Ấm. |
  | Nghĩa vụ thuế của sàn | Phải làm rõ thỏa thuận: Nếu đối tác thu hộ toàn bộ dòng tiền, Tổ Ấm có thể bị coi là "nền tảng có chức năng thanh toán" và phải thực hiện khấu trừ thuế thay theo Luật 56/2024/QH15. |
  | Độ phức tạp kỹ thuật | **Trung bình - Cao**: Cần xử lý Webhook HMAC, Reconciliation đối soát hàng ngày, Idempotency, Quản lý trạng thái giao dịch bất đồng bộ. |
  | Rủi ro tài chính | Phí cổng thanh toán (1% - 2.5%), rủi ro chậm đối soát, chargeback. |

### 5.3. Mô hình C — Tài khoản Phong tỏa / Ký quỹ tại Ngân hàng Thương mại (Bank Escrow Account)
- **Cơ chế vận hành:**  
  Người thuê và Chủ trọ mở hoặc ủy quyền tiền cọc vào một Tài khoản Ký quỹ (Escrow Account) mở tại Ngân hàng thương mại theo hợp đồng 3 bên (Người thuê - Chủ trọ - Ngân hàng, hoặc Cozynest là bên quản lý điều kiện giải tỏa). Tiền chỉ được chuyển cho Chủ trọ khi có lệnh xác nhận chuyển vào ở (Move-in Check-in) hoặc hoàn trả cho người thuê khi hủy hợp đồng đúng luật.
- **Đánh giá chi tiết:**
  | Tiêu chí | Đánh giá |
  |---|---|
  | Khả năng phù hợp pháp luật | **CHUẨN MỰC PHÁP LÝ CAO NHẤT cho bất động sản lớn**. Theo Điều 330 Bộ luật Dân sự 2015 về Ký quỹ tại tổ chức tín dụng. |
  | Giấy phép cần thiết | Thuộc thẩm quyền nghiệp vụ của Ngân hàng thương mại được cấp phép thành lập và hoạt động. |
  | Ai kiểm soát tiền | Ngân hàng thương mại phong tỏa và giải ngân theo điều kiện hợp đồng ký quỹ. |
  | Trách nhiệm hoàn tiền | Ngân hàng giải tỏa theo biên bản thỏa thuận hoặc phán quyết trọng tài / tòa án. |
  | Độ phức tạp kỹ thuật & Vận hành | **CỰC KỲ CAO**: Hiện rất ít ngân hàng tại Việt Nam cung cấp sẵn Open API hoàn chỉnh cho sản phẩm ký quỹ phòng trọ giá trị nhỏ (thường chỉ áp dụng cho mua bán nhà dự án lớn). Quy trình thủ tục giấy tờ ký 3 bên nặng nề, khó scale tự động cho phòng trọ 3 - 5 triệu đồng. |
  | Kết luận | Chưa khả thi cho Sprint 3. Có thể nghiên cứu khi hợp tác chiến lược với ngân hàng lớn trong tương lai. |

### 5.4. Mô hình D — Nền tảng tự nhận và giữ tiền trên tài khoản công ty Cozynest (Platform Custody / In-house Wallet)
- **Cơ chế vận hành:**  
  Người thuê chuyển tiền cọc vào số tài khoản ngân hàng của Công ty Cổ phần / TNHH Tổ Ấm. Tổ Ấm giữ số tiền này trên tài khoản công ty, ghi nhận "Ví / Số dư tạm giữ" trên hệ thống, sau đó định kỳ hàng tuần/tháng tự chuyển khoản ngân hàng (Payout) cho Chủ trọ.
- **Đánh giá chi tiết:**
  | Tiêu chí | Đánh giá |
  |---|---|
  | Khả năng phù hợp pháp luật | **BẤT HỢP PHÁP — RỦI RO ĐẶC BIỆT NGHIÊM TRỌNG**. |
  | Căn cứ pháp lý vi phạm | **Vi phạm nghiêm trọng Nghị định 52/2024/NĐ-CP** (Điều 22, Điều 27): Cung ứng dịch vụ trung gian thanh toán (thu hộ, chi hộ, giữ ví điện tử) khi chưa được Ngân hàng Nhà nước cấp phép. |
  | Rủi ro phá sản & Trộn lẫn dòng tiền | Tiền của người thuê và chủ trọ bị trộn lẫn với tài sản của doanh nghiệp (Co-mingling of funds). Nếu công ty gặp tranh chấp dân sự, phá sản hoặc bị cưỡng chế thuế, toàn bộ tiền cọc của người dùng sẽ bị cơ quan nhà nước phong tỏa, dẫn đến mất khả năng thanh toán. |
  | Rủi ro thuế | Toàn bộ dòng tiền vào tài khoản công ty có nguy cơ bị Cơ quan Thuế xác định là Doanh thu chịu thuế của doanh nghiệp, bị truy thu thuế TNDN (20%) và GTGT (10%), phạt chậm nộp và phạt hành chính về hóa đơn. |
  | Trách nhiệm hình sự | Nguy cơ cấu thành tội phạm kinh doanh trái phép hoặc lạm dụng tín nhiệm chiếm đoạt tài sản nếu phát sinh mất thanh khoản. |
  | **KẾT LUẬN TUYỆT ĐỐI** | **NGHIÊM CẤM TRIỂN KHAI MÔ HÌNH NÀY DƯỚI MỌI HÌNH THỨC.** |

---

## 6. MA TRẬN PHÂN TÍCH THUẾ THEO TỪNG LOẠI TIỀN

| Loại tiền | Người trả | Người nhận | Bản chất theo pháp luật | Nghĩa vụ thuế | Trách nhiệm của Nền tảng Tổ Ấm | Cần Kế toán / Thuế xác nhận |
|---|---|---|---|---|---|---|
| **Tiền thuê phòng hàng tháng** | Người thuê | Chủ trọ | Thu nhập từ hoạt động cho thuê tài sản của cá nhân/hộ kinh doanh | Nếu Chủ trọ có doanh thu > 100 triệu đ/năm: Phải nộp 5% thuế GTGT + 5% thuế TNCN (Thông tư 40/2021/TT-BTC). Nếu $\le$ 100 triệu đ/năm: Miễn thuế. | • **Mô hình A (P2P):** Tổ Ấm KHÔNG khấu trừ thuế. Chỉ cung cấp thông tin định kỳ hàng quý cho Tổng cục Thuế theo NĐ 91/2022/NĐ-CP.<br/>• **Mô hình B (Có chức năng thanh toán):** Nếu tham gia dòng tiền, sàn phải khấu trừ 10% nộp thay theo Luật 56/2024/QH15. | Xác nhận ngưỡng 100 triệu có tính lũy kế trên toàn bộ các kênh của chủ trọ hay tính riêng trên sàn Cozynest. |
| **Tiền đặt cọc** | Người thuê | Chủ trọ / Giữ hộ | Biện pháp bảo đảm thực hiện hợp đồng (Điều 328 BLDS 2015). KHÔNG PHẢI doanh thu. | **KHÔNG PHÁT SINH THUẾ** tại thời điểm nhận cọc. | Không ghi nhận vào doanh thu công ty. Không xuất hóa đơn cho khoản tiền cọc. | Hướng dẫn chứng từ kế toán khi tiền cọc bị phạt cọc chuyển thành thu nhập bồi thường. |
| **Phí gói VIP / Đăng tin** | Chủ trọ | Tổ Ấm | Doanh thu cung cấp dịch vụ phần mềm / truyền thông thương mại điện tử | Chịu thuế GTGT 10% và thuế TNDN 20% trên lợi nhuận thuần. | **BẮT BUỘC:** Xuất hóa đơn điện tử GTGT (e-Invoice) cho Chủ trọ ngay khi giao dịch thành công. Kê khai thuế định kỳ. | Cấu hình tích hợp API phần mềm Hóa đơn điện tử (VNPT / Viettel / MISA). |
| **Phí hoa hồng dịch vụ (Take-rate)** | Chủ trọ | Tổ Ấm | Doanh thu môi giới / dịch vụ sàn giao dịch TMĐT | Chịu thuế GTGT 10% và thuế TNDN 20%. | **BẮT BUỘC:** Xuất hóa đơn GTGT cho khoản phí hoa hồng thực thu. | Thời điểm ghi nhận doanh thu: Ngày hợp đồng có hiệu lực hay ngày nhận tiền. |
| **Tiền phạt vi phạm / Giữ cọc do hủy hợp đồng** | Bên vi phạm | Bên bị thiệt hại | Khoản bồi thường thiệt hại và phạt vi phạm hợp đồng | Cá nhân nhận bồi thường không thuộc đối tượng chịu thuế GTGT, cần kê khai TNCN thu nhập khác tùy trường hợp. | Sàn lập biên bản hòa giải / ghi nhận sự việc. Không xuất hóa đơn nếu không phải khoản thu của sàn. | Xác nhận chứng từ khấu trừ tiền phạt khi hoàn trả một phần tiền cọc. |

---

## 7. PHÂN TÍCH HỢP ĐỒNG ĐIỆN TỬ VÀ KÝ XÁC THỰC OTP

### 7.1. Căn cứ pháp lý về giá trị của Hợp đồng thuê nhà điện tử
1. **Quy định về hình thức văn bản:**  
   - Theo Điều 163 Luật Nhà ở 2023 và Điều 119 Bộ luật Dân sự 2015: Hợp đồng thuê nhà ở phải được lập thành văn bản. Giao dịch dân sự thông qua phương tiện điện tử dưới hình thức thông điệp dữ liệu theo quy định của pháp luật về giao dịch điện tử được coi là giao dịch bằng văn bản.
2. **Không bắt buộc công chứng:**  
   - Khoản 2 Điều 164 Luật Nhà ở 2023 quy định rõ: *"Hợp đồng thuê nhà ở không bắt buộc phải công chứng, chứng thực, trừ trường hợp các bên có nhu cầu"*. Do đó, hợp đồng thuê nhà điện tử giao kết qua nền tảng Tổ Ấm hoàn toàn có giá trị pháp lý ràng buộc nếu đáp ứng các điều kiện có hiệu lực của hợp đồng.

### 7.2. Giá trị pháp lý và giới hạn của Chữ ký điện tử bằng OTP (SMS/Email)
- **Bản chất của OTP:** Theo Luật Giao dịch điện tử 2023 (Điều 22, 23):
  - Mã OTP (One-Time Password) gửi qua SMS hoặc Email **KHÔNG PHẢI LÀ CHỮ KÝ SỐ** (Digital Signature) có chứng thư điện tử do tổ chức CA công cộng cấp.
  - Mã OTP là **phương thức xác thực điện tử (Electronic Authentication)** dùng để xác nhận ý chí chấp thuận của chủ thể đối với nội dung thông điệp dữ liệu.
- **Để hợp đồng ký bằng OTP được Tòa án chấp nhận làm chứng cứ khi có tranh chấp:**  
  Cần thỏa mãn Điều 12 và Điều 22 Luật Giao dịch điện tử 2023 về Tính toàn vẹn và Định danh người ký:
  1. **Định danh chủ thể xác thực:** Số điện thoại nhận OTP phải trùng khớp với số điện thoại đã được xác thực danh tính cá nhân qua eKYC (kiểm tra đối soát CCCD với cơ sở dữ liệu quốc gia về dân cư hoặc tài khoản ngân hàng chính chủ).
  2. **Snapshot nội dung tài liệu bất biến:** Tại thời điểm bấm "Ký hợp đồng", hệ thống phải tạo một bản chụp (snapshot) toàn bộ văn bản hợp đồng dưới dạng PDF/A-1b hoặc JSON chuẩn hóa.
  3. **Mã băm SHA-256:** Tính toán mã băm SHA-256 của tài liệu snapshot để chứng minh văn bản không bị sửa đổi sau thời điểm ký:
     $$\text{Hash} = \text{SHA-256}(\text{ContractContent} + \text{SignerMetadata})$$
  4. **Dấu thời gian tin cậy (Timestamping):** Ghi nhận chính xác thời điểm ký theo giờ chuẩn quốc gia UTC+7. Để tăng tính thuyết phục pháp lý, khuyến nghị tích hợp Dịch vụ cấp dấu thời gian tin cậy (TSA - Time Stamping Authority) được Bộ Thông tin & Truyền thông cấp phép.
  5. **Nhật ký bằng chứng ký số (Audit Trail):** Lưu vết không thể sửa đổi các trường thông tin:
     - `contract_id`, `version`, `document_hash` (SHA-256).
     - `signer_id`, `signer_role` (`TENANT` / `LANDLORD`).
     - `auth_method` (`OTP_SMS` / `OTP_EMAIL`).
     - `phone_number_masked` (`098****321`).
     - `ip_address` và `user_agent` của thiết bị ký.
     - `otp_verified_at` (thời điểm xác thực thành công).
     - **TUYỆT ĐỐI KHÔNG LƯU MÃ OTP THÔ** trong cơ sở dữ liệu (chỉ lưu mã băm OTP hoặc trạng thái đã xác thực).

---

## 8. NGHĨA VỤ PHÁP LÝ CỦA SÀN THƯƠNG MẠI ĐIỆN TỬ (E-COMMERCE PLATFORM)

Để hoạt động hợp pháp tại Việt Nam, Tổ Ấm bắt buộc phải hoàn thành các thủ tục và nghĩa vụ sau:
1. **Đăng ký sàn giao dịch thương mại điện tử với Bộ Công Thương:**
   - Theo Nghị định 52/2013/NĐ-CP và Nghị định 85/2021/NĐ-CP, nền tảng cho phép người dùng đăng tin cho thuê và đặt thuê trực tuyến bắt buộc phải nộp hồ sơ đăng ký thiết lập website/ứng dụng cung cấp dịch vụ sàn giao dịch TMĐT tại Cổng thông tin Quản lý hoạt động TMĐT (`online.gov.vn`).
   - Thời điểm hoàn thành: **Trước khi chính thức khai thác thương mại và thu tiền**.
2. **Nghĩa vụ công khai thông tin trên sàn:**
   - Công khai thông tin của chủ quản sàn (Tên công ty, Mã số thuế, Địa chỉ, Số điện thoại đại diện).
   - Công khai Quy chế hoạt động sàn TMĐT, Điều khoản dịch vụ (Terms of Service) và Chính sách bảo mật (Privacy Policy).
   - Yêu cầu và công khai thông tin của người đăng tin (Chủ trọ): Họ tên, địa chỉ cư trú, số điện thoại liên hệ, thông tin bất động sản cho thuê.
3. **Cơ chế tiếp nhận khiếu nại và giải quyết tranh chấp:**
   - Thiết lập quy trình tiếp nhận và xử lý khiếu nại của người thuê và chủ trọ.
   - Khi có tranh chấp phát sinh giữa người thuê và chủ trọ, sàn có trách nhiệm cung cấp thông tin liên quan đến giao dịch cho các bên hoặc cơ quan nhà nước có thẩm quyền khi có yêu cầu bằng văn bản.
4. **Nghĩa vụ báo cáo định kỳ cho Cơ quan Thuế:**
   - Theo Nghị định 91/2022/NĐ-CP: Báo cáo định kỳ hàng quý thông tin của cá nhân kinh doanh trên sàn (Họ tên, CCCD, Mã số thuế, Địa chỉ phòng cho thuê, Doanh thu giao dịch phát sinh qua sàn) qua Cổng thông tin TMĐT của Tổng cục Thuế.

---

## 9. QUY ĐỊNH VỀ DỮ LIỆU CÁ NHÂN VÀ ĐIỀU KHOẢN VỚI BÊN THỨ BA

Theo **Nghị định 13/2023/NĐ-CP** về Bảo vệ dữ liệu cá nhân:
1. **Phân loại dữ liệu thu thập:**
   - *Dữ liệu cá nhân cơ bản:* Họ tên, ngày sinh, giới tính, số điện thoại, địa chỉ email, địa chỉ phòng trọ.
   - *Dữ liệu cá nhân nhạy cảm:* Số CCCD, hình ảnh mặt trước/mặt sau CCCD, dữ liệu sinh trắc học khuôn mặt eKYC, thông tin tài khoản ngân hàng.
2. **Quy tắc bắt buộc:**
   - Phải có sự đồng ý rõ ràng, tự nguyện của người dùng (Consent Checkbox) trước khi thu thập hình ảnh CCCD và chụp ảnh khuôn mặt.
   - Lập và lưu trữ **Hồ sơ đánh giá tác động xử lý dữ liệu cá nhân (DPIA)** gửi Cục An ninh mạng và phòng chống tội phạm sử dụng công nghệ cao (A05) - Bộ Công an trong thời hạn 60 ngày kể từ ngày bắt đầu xử lý dữ liệu.
3. **Ràng buộc điều khoản với bên thứ ba (Data Processing Agreements - DPA):**
   - **Nhà cung cấp Cloud / Lưu trữ (AWS / GCP / Cloudinary / MinIO):** Hợp đồng dịch vụ phải có điều khoản cam kết bảo mật, dữ liệu lưu trữ tại trung tâm dữ liệu đáp ứng tiêu chuẩn an ninh thông tin, mã hóa ở trạng thái nghỉ (At-rest) và trạng thái truyền (In-transit), nhà cung cấp không có quyền khai thác hoặc cung cấp dữ liệu người dùng cho bên thứ tư.
   - **Cổng thanh toán / Ngân hàng:** Cam kết tuân thủ chuẩn bảo mật thanh toán PCI-DSS, chỉ sử dụng thông tin giao dịch vào mục đích đối soát và xử lý thanh toán.
   - **Nhà mạng viễn thông / Cung cấp SMS OTP (eSMS / VietGuys / SpeedSMS / Zalo ZNS):** Nghiêm cấm lưu vết nội dung OTP và số điện thoại vào mục đích quảng cáo hoặc chia sẻ cho bên thứ ba.

---

## 10. MA TRẬN RỦI RO PHÁP LÝ & TÀI CHÍNH (RISK REGISTER)

| Mã rủi ro | Mô tả rủi ro | Căn cứ pháp lý | Mức độ nghiêm trọng | Khả năng xảy ra | Biện pháp giảm thiểu bắt buộc |
|:---:|---|---|:---:|:---:|---|
| **R-01** | Bị xử phạt hoặc đình chỉ vì cung ứng dịch vụ trung gian thanh toán trái phép | Nghị định 52/2024/NĐ-CP (Điều 22, 27) | **CỰC KỲ CAO (P0)** | Rất cao nếu làm Mô hình D | **Tuyệt đối không nhận và giữ tiền cọc trên tài khoản công ty**. Sử dụng Mô hình A (P2P VietQR) hoặc Mô hình B (Đối tác TGTT có phép). |
| **R-02** | Bị cơ quan Thuế truy thu và xử phạt vì không thực hiện nghĩa vụ khấu trừ / báo cáo | Luật Quản lý thuế số 56/2024/QH15, NĐ 91/2022/NĐ-CP | **CAO (P1)** | Cao | Tích hợp module kết xuất báo cáo định kỳ hàng quý chuẩn XML gửi Tổng cục Thuế. Tham vấn Cục Thuế địa phương về quy chế khấu trừ tiền thuê. |
| **R-03** | Hợp đồng điện tử bị Tòa án tuyên vô hiệu hoặc bác bỏ chứng cứ khi có tranh chấp | Luật Giao dịch điện tử 2023, Bộ luật Tố tụng Dân sự 2015 | **TRUNG BÌNH (P1)** | Trung bình | Lưu trữ snapshot PDF bất biến, băm SHA-256, lưu vết IP, thiết bị, thời gian và số điện thoại đã xác minh eKYC. |
| **R-04** | Bị xử phạt vi phạm quy định bảo vệ dữ liệu cá nhân (CCCD, eKYC) | Nghị định 13/2023/NĐ-CP (Điều 44) | **CAO (P1)** | Trung bình | Mã hóa dữ liệu CCCD bằng AES-256-GCM, lưu trữ kho riêng tư (Presigned URLs ngắn hạn), nộp hồ sơ DPIA cho Bộ Công an. |
| **R-05** | Bị xử phạt vì vận hành sàn thương mại điện tử chưa đăng ký với Bộ Công Thương | Nghị định 98/2020/NĐ-CP (phạt từ 20 - 40 triệu đồng và có thể bị đình chỉ hoạt động) | **CAO (P1)** | Chắc chắn nếu chạy thật | Nộp hồ sơ đăng ký sàn TMĐT tại `online.gov.vn` trước ngày mở beta thương mại. |

---

## 11. HÀNH ĐỘNG TIẾP THEO ĐỀ XUẤT CHO SPRINT 3 & SPRINT 4

1. **Khóa chặt kiến trúc Sprint 3 theo Mô hình A (P2P VietQR):**
   - Thiết kế `[PAY-01]` và `[PAY-FE]` theo hướng: Sinh mã VietQR động chứa STK của Chủ trọ kèm mã tham chiếu giao dịch chuẩn `TOAM-REF-xxxxxx`.
   - Bổ sung trường số tài khoản ngân hàng và tên chủ tài khoản vào hồ sơ Chủ trọ (`landlord_profiles`), đối soát tên chủ tài khoản khớp với tên trên CCCD qua eKYC.
2. **Điều chỉnh phạm vi các thẻ Sprint 3 liên quan:**
   - **`[LEDGER-01]`:** Thiết kế sổ cái kép để quản lý: Phí dịch vụ sàn, Gói VIP, và Theo dõi luồng tiền P2P (ghi nhận trạng thái công nợ đối soát, không giữ số dư tiền gửi của người dùng).
   - **`[ESCROW-01]`:** Tạm thời chuyển trạng thái thành `DEFERRED_TO_SPRINT_4` hoặc chỉ triển khai dưới dạng ghi nhận trạng thái xác nhận đặt cọc 2 bên (không trực tiếp giữ tiền).
   - **`[CONTRACT-BE]` & `[CONTRACT-FE]`:** Triển khai snapshot PDF, mã băm SHA-256, và nhật ký ký số OTP theo đúng yêu cầu pháp lý tại Mục 7.
3. **Thực hiện các thủ tục pháp lý hành chính song song:**
   - Gửi danh sách câu hỏi thẩm định cho Luật sư chuyên trách Fintech và Kế toán trưởng (theo file `LEGAL-02-COUNSEL-QUESTIONS.md`).
   - Chuẩn bị hồ sơ đăng ký sàn TMĐT với Bộ Công Thương.

---

## 12. TÀI LIỆU THAM KHẢO & CƠ SỞ DỮ LIỆU NGUỒN

1. Cổng Thông tin điện tử Chính phủ: *Nghị định số 52/2024/NĐ-CP ngày 15/05/2024 về thanh toán không dùng tiền mặt*.
2. Cổng Thông tin điện tử Ngân hàng Nhà nước: *Thông tư số 40/2024/TT-NHNN ngày 28/06/2024 về dịch vụ trung gian thanh toán*.
3. Cơ sở dữ liệu Quốc gia về Văn bản Pháp luật: *Luật Giao dịch điện tử số 20/2023/QH15* & *Luật Nhà ở số 27/2023/QH15*.
4. Cổng Thông tin Tổng cục Thuế: *Thông tư số 40/2021/TT-BTC* và *Nghị định số 91/2022/NĐ-CP về quản lý thuế TMĐT*.
5. Cổng Quản lý hoạt động Thương mại điện tử - Bộ Công Thương: `http://online.gov.vn`.

