# BÁO CÁO KIỂM CHỨNG & GIA CỐ TÍNH NĂNG [AUTH-02-FINISH]
## Hoàn Tất Đăng Nhập & Đăng Ký Bằng Google Identity Services (GIS)

* **Ngày thực hiện:** 08/11/2026
* **Nhánh làm việc:** `feat/toam-rebuild`
* **Người thực hiện:** Đội ngũ Kỹ sư Dự án "Tổ Ấm"
* **Trạng thái:** HOÀN TẤT & ĐẠT 100% TIÊU CHUẨN KIỂM ĐỊNH

---

## I. BIÊN BẢN RÀ SOÁT BẢO MẬT & BÍ MẬT (SECRET AUDIT)

Tuân thủ nghiêm ngặt nguyên tắc bảo mật thông tin dự án:
1. Không đọc, không in, không log, không lưu vào commit giá trị khóa bí mật hay Client ID thật.
2. Kiểm tra sự tồn tại của khóa qua script kiểm định: in ra CÓ/KHÔNG.
3. Luồng xác thực dùng Google Identity Services (GIS) ID Token: KHÔNG dùng `GOOGLE_CLIENT_SECRET`, không thêm biến này vào môi trường.
4. Tệp `.env` và `.env.local` đã nằm trong `.gitignore` của cả hai repository và không bị Git theo dõi.
5. Không có chuỗi bắt đầu bằng `GOCSPX-` hay Client ID thật trong toàn bộ lịch sử git được theo dõi.

### Kết Quả Kiểm Tra Script `google-login-check.ps1`
```powershell
=== KIEM TRA CAU HINH GOOGLE OAUTH [AUTH-02-FINISH] ===
[1] Tệp .env tồn tại trong backend: CO
[2] GOOGLE_CLIENT_ID có cấu hình trong backend: CO (khong phai gia tri giu cho)
[3] GOOGLE_LOGIN_ENABLED = true trong backend: CO
[4] Backend TUYET DOI khong chua GOOGLE_CLIENT_SECRET: CO
[5] Tệp .env.local tồn tại trong frontend: CO
[6] NEXT_PUBLIC_GOOGLE_CLIENT_ID có cấu hình trong frontend: CO (khong phai gia tri giu cho)
[7] NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED = true trong frontend: CO
[8] NEXT_PUBLIC_ TUYET DOI khong chua chuoi SECRET: CO
[9] .env va .env.local duoc gitignore: CO
Ket qua kiem tra: 9/9 dat yeu cau!
```

---

## II. BẢNG ĐỐI CHIẾU 20 HẠNG MỤC CHECKLIST KỸ THUẬT

| STT | Hạng mục kiểm tra | Giải pháp kỹ thuật | Trạng thái | Bằng chứng |
|:---:|:---|:---|:---:|:---|
| 1 | Xác thực ID Token trên Backend | `GoogleTokenVerifier.java` dùng `NimbusJwtDecoder` xác thực chữ ký RS256 qua JWKS Google | ĐẠT | 8/8 tests `GoogleTokenVerifierTest` passed |
| 2 | Nonce chống Replay Attack | `GET /api/v1/auth/google/nonce` sinh chuỗi ngẫu nhiên lưu Redis TTL 5 phút, tiêu thụ 1 lần | ĐẠT | Unit test `authenticate_ReplayAttack_ThrowsException` passed |
| 3 | Trạng thái & Fingerprint | `GET /api/v1/auth/google/status` trả về `enabled` và 8 ký tự SHA-256 fingerprint | ĐẠT | Endpoint trả về `{"enabled":true,"clientIdFingerprint":"e334eb49"}` |
| 4 | Tìm kiếm theo sub trước | `GoogleAuthUseCase` kiểm tra `oauth_accounts(provider='GOOGLE', provider_user_id=sub)` | ĐẠT | Unit test `authenticate_ExistingLinkedAccount_LogsIn` passed |
| 5 | Liên kết tài khoản theo email | Kiểm tra `users.email`. Nếu chưa liên kết -> tự động tạo bản ghi `oauth_accounts` | ĐẠT | Unit test `authenticate_ExistingVerifiedUser_LinksAccount` passed |
| 6 | Chống chiếm đoạt email chưa xác thực | Nếu email tồn tại nhưng `email_verified=false`, thu hồi session cũ, liên kết an toàn | ĐẠT | Unit test `authenticate_ExistingUnverifiedUser_PreventsTakeover` passed |
| 7 | Cấp phát JWT & Lưu phiên | Cấp accessToken + refreshToken qua cookie `cozynest_token`, ghi bảng `login_history` | ĐẠT | Bảng `login_history` lưu `oauth_provider='GOOGLE'` |
| 8 | Giới hạn tần suất (Rate Limit) | Redis Rate Limiter: tối đa 10 request/phút/IP đối với endpoint `/api/v1/auth/google` | ĐẠT | Test `testGoogleAuth_RateLimitExceeded_Returns429` passed |
| 9 | Migration CSDL cho phép Null | Migration Flyway `V6`: `phone` và `password_hash` cho phép `NULL`, thêm `login_history` | ĐẠT | Migration V6 executed successfully trên MySQL |
| 10 | Hướng dẫn tạo mật khẩu | Trang `/profile/change-password` hướng dẫn người dùng Google tạo mật khẩu qua `/forgot-password` | ĐẠT | Giao diện cảnh báo kèm liên kết trực tiếp |
| 11 | Nút GoogleLoginButton GIS | Dùng `next/script` tải SDK `https://accounts.google.com/gsi/client`, render nút chuẩn GIS | ĐẠT | Component `GoogleLoginButton.tsx` hoàn thiện |
| 12 | Hỗ trợ Login & Register | Tích hợp vào `/login` và `/register`, truyền cờ `isRegister` và `intendedRole` | ĐẠT | Chụp ảnh kiểm thử giao diện Desktop & Mobile |
| 13 | Kiểm tra Fingerprint lệch | Frontend so khớp 8 ký tự SHA-256 Client ID giữa client và server trong dev mode | ĐẠT | Ghi nhận thông báo cảnh báo nếu lệch cấu hình |
| 14 | Chống nhấp đúp & State | Biến trạng thái `isSubmitting` vô hiệu hóa nút khi đang xử lý xác thực | ĐẠT | Toast thông báo phản hồi thân thiện i18n |
| 15 | Tiêu đề COOP & CSP | `next.config.ts` cấu hình `Cross-Origin-Opener-Policy: same-origin-allow-popups` và CSP | ĐẠT | Cho phép `accounts.google.com` và `*.googleapis.com` |
| 16 | Tệp mẫu `.env.example` | Cập nhật hướng dẫn tiếng Việt rõ ràng, chỉ dùng giá trị giữ chỗ, không chứa secret | ĐẠT | Tệp `.env.example` ở cả hai repo đã cập nhật |
| 17 | Bộ kiểm thử Backend | 24 unit & integration test chuyên biệt cho Google Auth | ĐẠT | 100% passed (8 verifier + 9 use case + 7 integration) |
| 18 | Bộ kiểm thử Frontend | Vitest 41 tests kiểm tra toàn bộ use cases và component | ĐẠT | 11/11 test files, 41/41 unit tests passed |
| 19 | Kiểm tra đa ngôn ngữ & liên kết | Đồng bộ song ngữ Vi/En (679/679 keys) và 78 liên kết nội bộ hợp lệ | ĐẠT | `verify-i18n.mjs` & `verify-links.mjs` 100% |
| 20 | Kiểm thử hiển thị thực tế (Visual) | Chụp 6 màn hình thực tế trên Desktop (1440px) và Mobile (390px) cho Vi/En và Light/Dark | ĐẠT | 6 ảnh chụp thực tế được kiểm duyệt kỹ lưỡng |

---

## III. KẾT QUẢ KIỂM THỬ BACKEND & FRONTEND

### 1. Backend Gradle Tests (identity-service)
* `GoogleTokenVerifierTest`: 8/8 tests PASSED (Valid, Wrong Audience, Wrong Issuer, Expired, Bad Signature, Unverified Email, Missing Sub, Mismatched Nonce).
* `GoogleAuthUseCaseTest`: 9/9 tests PASSED (Status Fingerprint, Tenant Registration, Replay Attack Nonce Reject, Linked Account Login, Verified Email Linking, Anti-Takeover Revocation, Suspended Account 403, Intended Role Admin Rejection, Intended Role Landlord).
* `GoogleAuthControllerIntegrationTest`: 7/7 tests PASSED (Success 200 + Cookie, Bad Nonce 400, Invalid Token 401, Suspended 403, Rate Limit 429, Not Configured 503, Concurrency Handling).

### 2. Frontend Vitest & Verification Scripts (accommodation-web)
* Unit tests: 41/41 passed across 11 test suites.
* Parity kiểm định i18n: 100% đồng bộ (679 keys tiếng Việt, 679 keys tiếng Anh).
* Kiểm định liên kết nội bộ: 78/78 internal links hợp lệ.
* ESLint: 0 errors.

---

## IV. BÌNH LUẬN KIỂM DUYỆT GIAO DIỆN (VISUAL CRITIQUE)

Dựa trên 6 bức ảnh chụp giao diện thực tế thu được từ Playwright (`scripts/login_desktop_*.png`, `scripts/register_desktop_*.png`):

1. **Độ tương phản và phân cấp thị giác (Visual Hierarchy)**:
   * Nút Đăng nhập bằng Google chuẩn thương hiệu của Google với viền nhẹ và nền trung tính tách biệt rõ ràng với nút Submit chính (màu Teal thương hiệu Tổ Ấm).
   * Phân cách "Hoặc tiếp tục với" với đường kẻ mờ tạo cảm giác thoáng đãng, không gây rối mắt cho người dùng.

2. **Khả năng thích ứng giao diện tối (Dark Mode Accessibility)**:
   * Trên nền Dark Theme (`stone-900`), Google Login Button vẫn giữ được nhận diện thương hiệu rõ nét, chữ và biểu tượng tương phản tốt, không bị chìm hoặc lóa.
   * Thông báo lưu ý tại trang Đổi mật khẩu (`UserChangePasswordView`) sử dụng tông màu Amber nhẹ nhàng trên nền tối, tạo cảnh báo vừa đủ mà không gây cảm giác khó chịu.

3. **Trải nghiệm trên thiết bị di động (Mobile 390px)**:
   * Nút Google tự động co dãn 100% chiều rộng theo lưới container, vùng bấm cảm ứng (touch target) đạt chuẩn 44px của Apple Human Interface Guidelines.
   * Khoảng cách giữa các nút và trường nhập liệu không bị dính sát, đảm bảo thao tác ngón tay chính xác trên màn hình nhỏ.

---

## V. HƯỚNG DẪN 3 BƯỚC KIỂM THỬ BẰNG TÀI KHOẢN GOOGLE THỰC

Để người dùng tự kiểm chứng luồng hoạt động với tài khoản Google thực của mình:

1. **Bước 1: Chuẩn bị thông tin Client ID**:
   * Đảm bảo trong `accommodation-web/.env.local` đã có `NEXT_PUBLIC_GOOGLE_CLIENT_ID=<Client_ID_Thực_Của_Bạn>` và `NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED=true`.
   * Đảm bảo trong `accommodation-service/.env` đã có `GOOGLE_CLIENT_ID=<Client_ID_Thực_Của_Bạn>` và `GOOGLE_LOGIN_ENABLED=true`.
   * Trong Google Cloud Console (Credentials), thêm `http://localhost:3000` vào mục **Authorized JavaScript origins**.

2. **Bước 2: Kiểm thử Đăng ký người dùng mới**:
   * Truy cập `http://localhost:3000/register`.
   * Chọn vai trò mong muốn ("Người thuê" hoặc "Chủ trọ").
   * Bấm vào nút **Đăng ký với Google** và đăng nhập tài khoản Google của bạn.
   * Hệ thống sẽ tự động xác thực token, lưu hồ sơ người dùng mới (vai trò đã chọn, mật khẩu null, phone null, email tự động đánh dấu đã xác thực), thiết lập cookie `cozynest_token` và chuyển hướng mượt mà vào trang chủ hoặc dashboard.

3. **Bước 3: Kiểm thử Đăng nhập & Đổi mật khẩu**:
   * Đăng xuất khỏi hệ thống, sau đó truy cập `http://localhost:3000/login`.
   * Bấm nút **Đăng nhập với Google**. Hệ thống nhận diện tài khoản đã tồn tại qua `sub`, ghi nhận lịch sử đăng nhập vào bảng `login_history` và cho phép truy cập ngay lập tức.
   * Sau khi đăng nhập, truy cập trang Hồ sơ cá nhân -> Đổi mật khẩu: quan sát khung thông báo hướng dẫn dành cho tài khoản Google và thử tính năng tạo mật khẩu qua luồng Quên mật khẩu.
