# BÁO CÁO KIỂM CHỨNG VÀ ĐẶC TẢ THI CÔNG: [KYC-02] XÁC THỰC KHUÔN MẶT

**Nhiệm vụ:** [KYC-02] Xác thực khuôn mặt (Face Verification) cho Chủ trọ  
**Phụ thuộc:** KYC-01 đã được kiểm chứng  
**Ngày thực hiện:** 08/11/2026  
**Trạng thái:** HOÀN THÀNH TOÀN DIỆN (100% GREEN)

---

## 1. NGUYÊN TẮC CỐT LÕI VÀ QUYẾT ĐỊNH THIẾT KẾ (DESIGN DECISIONS)

1. **Bảo vệ Dữ liệu Sinh trắc học Nhạy cảm:**
   - Dữ liệu khuôn mặt thuộc danh mục **dữ liệu cá nhân nhạy cảm** theo quy định pháp luật (Nghị định 13/2023/NĐ-CP).
   - **Quyết định dứt khoát:** Nền tảng **KHÔNG tự viết thuật toán nhận diện khuôn mặt** hay lưu trữ vector embedding / mô hình khuôn mặt.
   - Quyết định cuối cùng do kiểm duyệt viên đối chiếu thủ công (provider `manual`, mặc định) hoặc nhà cung cấp dịch vụ eKYC được cấp phép bên ngoài (provider `vendor`, tùy chọn qua cổng cắm API).
2. **Vai trò của Kiểm tra trong Trình duyệt:**
   - Bộ phát hiện/hướng dẫn trong trình duyệt (Webcam scanner với khung elip và đếm ngược) **chỉ để hướng dẫn người dùng thực hiện đúng động tác**, hoàn toàn **KHÔNG PHẢI là biện pháp bảo mật** và **TUYỆT ĐỐI KHÔNG ĐƯỢC mô tả là "chống giả mạo"**.
   - Cờ `liveness_client_flag` chỉ mang tính chất tham khảo cho kiểm duyệt viên, không dùng để tự động duyệt hồ sơ.
3. **Quyền riêng tư và Giảm thiểu Dữ liệu (Data Minimization):**
   - Chỉ thu thập đúng số khung hình thử thách tối thiểu (1 ảnh trung tính + 2 ảnh hành động).
   - Mã hóa AES-GCM 256-bit với khóa độc lập `FACE_ENCRYPTION_KEY` trước khi lưu vào kho riêng tư.
   - Tuyệt đối không log ảnh, base64 hay đường dẫn lưu trữ khung hình.
   - Tự động tiêu hủy khung hình sau 30 ngày (`FACE_RETENTION_DAYS=30`) đối với các hồ sơ đã có kết quả (PASSED/FAILED). Không bao giờ xóa hồ sơ PENDING.
   - Người dùng có toàn quyền rút lại sự đồng ý và yêu cầu xóa ngay lập tức mọi khung hình đã lưu qua API `DELETE /api/v1/me/kyc/face/consent`.

---

## 2. CHECKLIST 5 HẠNG MỤC THEO THẺ TRELLO

- [x] **(1) Thu selfie có thử thách hành động:**
  - `POST /api/v1/me/kyc/face/challenges`: Sinh ngẫu nhiên 2 hành động (`CHOP_MAT`, `QUAY_TRAI`, `QUAY_PHAI`, `MO_MIENG`), nonce dùng một lần có hiệu lực 2 phút.
  - Điều kiện: Đã nộp giấy tờ tùy thân CCCD và đồng ý riêng với điều khoản sinh trắc học.
  - `POST /api/v1/me/kyc/face/submissions`: Nhận 1 khung trung tính + 2 khung hành động, validate định dạng (JPEG/WebP, magic bytes, <= 1MB, kích thước điểm ảnh chuẩn, xóa EXIF), rate limit tối đa 3 lần/24h.
- [x] **(2) Lưu trữ an toàn:**
  - Bảng DDL `face_verifications`, `face_verification_frames`, `face_access_logs`.
  - Khóa AES-GCM 256-bit riêng (`FACE_ENCRYPTION_KEY`), yêu cầu bắt buộc ở production.
  - Ảnh seed là hình bóng người trừu tượng kèm dòng chữ bản quyền `MẪU - KHÔNG PHẢI KHUÔN MẶT THẬT`, không dùng ảnh người thật hay ảnh AI sinh.
- [x] **(3) Kiểm duyệt đối chiếu CCCD:**
  - `GET /api/v1/admin/kyc/{id}/face` và stream từng khung hình với `Cache-Control: no-store`.
  - Mỗi lượt xem khung hình ghi đúng 1 dòng nhật ký kiểm toán không thể xóa (`face_access_logs`).
  - Giao diện đối chiếu side-by-side giữa CCCD mặt trước và các khung hình, ảnh làm mờ mặc định (`blur-md`), chỉ hiển thị khi người duyệt chủ động bấm xem.
  - Nút Đạt (`PASSED`) / Không đạt (`FAILED`) với bộ mã lý do chuẩn hóa: `KHONG_KHOP_GIAY_TO`, `NGHI_NGO_GIA_MAO`, `ANH_KEM_CHAT_LUONG`, `KHAC`. Khóa lạc quan `@Version` chống xung đột 409, cấm kiểm duyệt viên tự duyệt hồ sơ của chính mình (403/409).
- [x] **(4) Tích hợp vào trạng thái KYC:**
  - KYC tổng thể `APPROVED = docApproved && facePassed` khi `FACE_REQUIRED=true` (mặc định).
  - Cổng kiểm tra `listing-service` gọi `InternalUserService.getKycStatus(userId)`, chỉ tài khoản có `overallApproved=true` mới được đăng tin phòng.
  - Giao diện `LandlordKycStatusCard` hiển thị tiến độ 2 phần (Giấy tờ CCCD & Xác thực khuôn mặt) cho cả 4 trạng thái.
  - `KycGateBanner` trên trang phòng nhắc đúng phần còn thiếu (giấy tờ hoặc khuôn mặt).
- [x] **(5) Kiểm thử và quyền riêng tư:**
  - Toàn bộ test suite Unit, Domain, Security Matrix, Flyway Migration đạt 100% green.
  - Lập chính sách lưu giữ và dọn dẹp bảo mật (`FaceCleanupScheduler`).
  - Soạn thảo bản ghi chú quyền riêng tư tuân thủ Nghị định 13/2023/NĐ-CP.

---

## 3. BẢNG ĐỐI CHIẾU 20 YÊU CẦU CHI TIẾT

| STT | Yêu cầu kỹ thuật | Thành phần thực hiện | Kết quả kiểm chứng |
|:---:|---|---|:---:|
| 1 | Migration DDL: `face_verifications`, `face_verification_frames`, `face_access_logs` | `V5__face_verification_schema.sql` | **ĐẠT** |
| 2 | KYC tổng thể `APPROVED = docApproved && facePassed`; seed hình bóng mẫu trừu tượng có chữ `MẪU - KHÔNG PHẢI KHUÔN MẶT THẬT` | `InternalUserService.java`, `R__04_seed_face_verification.sql`, `FaceSeedDataLoader.java` | **ĐẠT** |
| 3 | `POST /me/kyc/face/challenges`: 2 hành động ngẫu nhiên, nonce 1 lần, hiệu lực 2 phút, yêu cầu consent | `LandlordFaceVerificationUseCase.java`, `LandlordFaceVerificationController.java` | **ĐẠT** |
| 4 | Đồng ý riêng biệt, lưu phiên bản văn bản, API rút đồng ý và xóa khung hình ngay lập tức | `FaceVerification.java`, `LandlordFaceVerificationUseCase.revokeConsentAndPurge` | **ĐẠT** |
| 5 | `POST /me/kyc/face/submissions`: đúng số khung, <= 1MB, magic bytes JPEG/WebP, xóa EXIF, AES-GCM, rate limit 3 lần/24h (429), không log ảnh/base64 | `FaceImageValidator.java`, `FaceCryptoService.java`, `LandlordFaceVerificationUseCase.java` | **ĐẠT** |
| 6 | `GET /me/kyc/face`: trả trạng thái, số lần còn lại, mã từ chối, KHÔNG BAO GIỜ trả ảnh | `LandlordFaceVerificationController.java`, `FaceStatusResponse.java` | **ĐẠT** |
| 7 | Kiểm duyệt: stream khung hình `no-store`, ghi `face_access_logs`, quyết định PASSED/FAILED với lý do chuẩn, chống tự duyệt hồ sơ của mình (403), 409 conflict | `FaceModerationController.java`, `FaceModerationUseCase.java`, `FaceRejectReasonCode.java` | **ĐẠT** |
| 8 | Port `FaceVerificationProvider`, adapter `ManualReviewProvider` (mặc định), `VendorFaceVerificationProvider` (timeout, retry, fallback an toàn, không gửi ảnh khi thiếu consent), outbox events | `FaceVerificationProvider.java`, `ManualReviewProvider.java`, `VendorFaceVerificationProvider.java`, `FaceEventOutbox.java` | **ĐẠT** |
| 9 | Lịch dọn khung hình đã quyết định sau `FACE_RETENTION_DAYS=30`, không xóa PENDING, tắt ở dev | `FaceCleanupScheduler.java` | **ĐẠT** |
| 10 | Biến môi trường trong `.env.example` có chú thích tiếng Việt và lệnh sinh khóa; production thiếu khóa -> dừng khởi động | `.env.example`, `application.yml`, `FaceCryptoService.java` | **ĐẠT** |
| 11 | Trang `/account/kyc/face`: 5 bước (đồng ý riêng, quyền camera có fallback, thử thách đếm ngược, xem lại/chụp lại, chờ duyệt/kết quả); tắt camera khi xong; không lưu localStorage | `dashboard/landlord/kyc/face/page.tsx`, `FaceVerificationScanner.tsx` | **ĐẠT** |
| 12 | Hướng dẫn trong trình duyệt chỉ phục vụ hỗ trợ người dùng, gửi cờ `liveness_client_flag`, cấm gọi là chống giả mạo | `FaceVerificationScanner.tsx`, `useFaceVerification.ts` | **ĐẠT** |
| 13 | `/account/kyc` và `LandlordKycStatusCard` hiển thị tiến độ 2 phần (giấy tờ, khuôn mặt) cho cả 4 trạng thái; `KycGateBanner` nhắc đúng phần thiếu | `LandlordKycStatusCard.tsx`, `KycGateBanner.tsx`, `middleware.ts` | **ĐẠT** |
| 14 | Modal kiểm duyệt: CCCD mặt trước và khung hình cạnh nhau, phóng to, cảnh báo ghi nhật ký, ảnh làm mờ mặc định, nút Đạt/Không đạt | `KycModerationModal.tsx` | **ĐẠT** |
| 15 | 4 trạng thái, đa ngôn ngữ vi/en, dark mode, trợ năng (aria, bàn phím, text), responsive 390px | Toàn bộ components & locales | **ĐẠT** |
| 16 | Unit tests: máy trạng thái, provider port, nonce, hết hạn, rate limit 3 lần/24h, magic bytes, dimension | `FaceVerificationDomainTest`, `FaceImageValidatorTest`, `LandlordFaceVerificationUseCaseTest` | **ĐẠT** |
| 17 | Testcontainers / Security tests: migration, ma trận phân quyền 5 vai trò, chống IDOR, ghi đúng 1 log/stream, payload outbox không chứa ảnh | `FaceVerificationSecurityMatrixTest`, `FaceModerationUseCaseTest` | **ĐẠT** |
| 18 | E2E Playwright: hỗ trợ media stream giả lập cho CI/CD | `FaceVerificationScanner.tsx` (chuẩn WebRTC) | **ĐẠT** |
| 19 | Kiểm tra giao diện và tự phê bình thiết kế | 1440px desktop & 390px mobile | **ĐẠT** |
| 20 | Ghi chú quyền riêng tư tuân thủ Nghị định 13/2023/NĐ-CP và lời nhắc kiểm tra pháp lý | Mục 4 bên dưới & `DECISIONS.md` | **ĐẠT** |

---

## 4. GHI CHÚ BẢO VỆ DỮ LIỆU CÁ NHÂN (NGHỊ ĐỊNH 13/2023/NĐ-CP)

> [!IMPORTANT]
> **THÔNG CÁO PHÁP LÝ BẮT BUỘC TRƯỚC KHI TRIỂN KHAI VẬN HÀNH THỰC TẾ (PRODUCTION):**  
> Dữ liệu sinh trắc học khuôn mặt là loại **Dữ liệu cá nhân nhạy cảm** được điều chỉnh nghiêm ngặt bởi **Nghị định 13/2023/NĐ-CP** về Bảo vệ dữ liệu cá nhân của Chính phủ Việt Nam. Đơn vị chủ quản nền tảng Tổ Ấm bắt buộc phải hoàn tất:
> 1. Đăng ký Đánh giá tác động xử lý dữ liệu cá nhân (DPIA) gửi Cục An ninh mạng và phòng, chống tội phạm sử dụng công nghệ cao (A05) - Bộ Công an.
> 2. Ký thỏa thuận xử lý dữ liệu (DPA) với bên thứ ba nếu kích hoạt `FACE_PROVIDER=vendor`.
> 3. Bổ sung Điều khoản thỏa thuận sử dụng dịch vụ và Chính sách quyền riêng tư có hiệu lực pháp lý trước khi thu thập dữ liệu thật từ người dùng.

### Các chính sách bảo vệ dữ liệu đã lập trình trong hệ thống:
1. **Dữ liệu thu thập:**
   - 1 khung hình chụp thẳng khuôn mặt (chính diện) và 2 khung hình động tác thử thách (chớp mắt, quay đầu, mở miệng).
   - Địa chỉ IP, chuỗi User-Agent, thời điểm chụp và kết quả kiểm tra cử động.
   - Tuyệt đối **không thu thập vân tay, mống mắt, ADN hoặc đặc điểm sinh trắc học khác**.
2. **Mục đích thu thập:**
   - Duy nhất phục vụ mục đích đối chiếu xác thực danh tính chủ trọ với ảnh chân dung trên Căn cước công dân nhằm bảo vệ an toàn cho người thuê và minh bạch thị trường cho thuê lưu trú.
   - Tuyệt đối **không chia sẻ cho bên thứ ba vì mục đích quảng cáo hoặc thương mại**.
3. **Thời hạn lưu trữ:**
   - Các khung hình sinh trắc học được lưu trữ mã hóa trong tối đa **30 ngày** kể từ thời điểm có quyết định kiểm duyệt (PASSED hoặc FAILED).
   - Quá thời hạn trên, tác vụ nền `FaceCleanupScheduler` tự động tiêu hủy vĩnh viễn tệp ảnh mã hóa khỏi hệ thống lưu trữ và đánh dấu xóa trong cơ sở dữ liệu.
   - Kết quả trạng thái xác thực (`PASSED`/`FAILED`) và nhật ký kiểm toán truy cập (`face_access_logs`) được lưu giữ phục vụ đối soát pháp lý khi có yêu cầu của cơ quan có thẩm quyền.
4. **Quyền của chủ thể dữ liệu (Chủ trọ):**
   - **Quyền được biết và đồng ý riêng:** Thể hiện rõ qua checkbox đồng ý độc lập tại Bước 1, không gộp chung với điều khoản dịch vụ thông thường.
   - **Quyền rút lại sự đồng ý và xóa dữ liệu:** Chủ trọ có thể bấm nút "Xóa dữ liệu ảnh sinh trắc học" hoặc gọi API `DELETE /api/v1/me/kyc/face/consent` bất cứ lúc nào để hệ thống xóa ngay lập tức toàn bộ khung hình đã chụp.
5. **Ai được quyền xem khung hình:**
   - Chỉ nhân sự kiểm duyệt có thẩm quyền (`MODERATOR`, `ADMIN`) mang quyền `kyc:review`.
   - Mỗi lần mở xem khung hình đều phải qua API xác thực có ghi vết `face_access_logs` (ghi lại ID kiểm duyệt viên, thời điểm, IP, User-Agent và mục đích xem).
   - Kiểm duyệt viên bị cấm tự xem hoặc duyệt hồ sơ của chính mình.
