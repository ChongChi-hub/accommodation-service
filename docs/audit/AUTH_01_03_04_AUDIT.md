# AUDIT CHECKLIST: [AUTH-01] [AUTH-03] [AUTH-04]

## THUẬT TOÁN BĂM MẬT KHẨU
- [x] Ghi nhận thuật toán băm đang dùng thật: **BCrypt với hệ số tải 12 (`BCryptPasswordEncoder(12)`)**.
  - **Minh chứng**: `PasswordEncoderConfig.java` cấu hình `@Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }`.
  - **Ghi nhận**: Đã ghi nhận quyết định kiến trúc tại `docs/DECISIONS.md` Mục 13.1. BCrypt với strength 12 đáp ứng khuyến nghị của OWASP Password Storage Cheat Sheet, tạo độ trễ ~80-100ms chống tấn công vét cạn.

---

## AUTH-01: ĐĂNG KÝ / ĐĂNG NHẬP
- [x] 1. **Chính sách mật khẩu ở server**:
  - Tối thiểu $\ge 8$ ký tự (`@Size(min = 8)` trong `RegisterCommand`, `ResetPasswordCommand`, `ChangePasswordCommand`).
  - Lớp `PasswordPolicyValidator.java` kiểm tra danh sách mật khẩu phổ biến dễ đoán (blacklist: `password123`, `12345678`, `admin123`...) và cấm mật khẩu chứa/trùng địa chỉ email.
  - Sử dụng `@ToString.Exclude` trên mọi trường mật khẩu và OTP trong các DTO để bảo đảm không bao giờ bị lộ ra log.
- [x] 2. **Chống dò tài khoản (Timing Attack & Anti-Enumeration)**:
  - Khi đăng nhập sai email, `LoginUseCase` kích hoạt so sánh với `DUMMY_BCRYPT_HASH` (`$2a$12$7kPj5aXN62q.7XJjN8wGxeQJv9XW1Nq2g5U1rQ0M5J4L3K2H1G0F.`), đưa thời gian phản hồi của trường hợp "email không tồn tại" tương đương "sai mật khẩu" (~80ms).
  - Cả hai trường hợp đều trả về cùng mã lỗi `ERR_AUTH_INVALID_CREDENTIALS` và cùng thông báo `"Email hoặc mật khẩu không chính xác"`.
  - Đánh đổi khi đăng ký trùng email đã được ghi nhận tại `docs/DECISIONS.md` Mục 13.3 (trả về lỗi trực tiếp để tối ưu UX, bảo vệ bằng Rate Limiter 10 req/phút/IP).
- [x] 3. **Khóa tài khoản chống DoS (Account Lockout)**:
  - Khóa theo cặp `(email, IP)`: sai 5 lần liên tiếp trong 15 phút sẽ tạm khóa phiên đăng nhập của cặp `(email, IP)` đó trong 15 phút (`ERR_AUTH_TEMPORARY_LOCKED`).
  - Quyết định này ngăn chặn hoàn toàn kẻ tấn công DoS tài khoản người dùng thực.
  - Injected `java.time.Clock` bean trong `IdentityApplication.java` và `LoginAttemptAdapter.java` hỗ trợ mock clock cho kiểm thử.
- [x] 4. **Refresh Token Xoay Vòng & Thu Hồi**:
  - `RefreshTokenUseCase`: phát hiện token tái sử dụng sẽ lập tức thu hồi toàn bộ họ token (`familyId`) và trả về HTTP 401 `ERR_AUTH_INVALID_TOKEN`.
  - `AuthController.logout`: thu hồi token trong kho lưu trữ và xóa cookie `cozynest_token`.
  - `ResetPasswordUseCase`: thu hồi tất cả các token active của người dùng (`refreshTokenRepository.revokeAllByUserId(user.getId())`).
- [x] 5. **JWT RS256 & Key Rotation**:
  - Chữ ký RS256 xác thực `iss = "cozynest-identity"`, `aud = "cozynest-web"`, `exp`, và header `kid`.
  - `RsaKeyProvider`: kiểm tra môi trường production, ném `IllegalStateException` khi thiếu khóa riêng cấu hình.
  - Hỗ trợ xoay khóa qua `registerRotatedKey` và endpoint `/.well-known/jwks.json` trả về danh sách public keys của mọi `kid`.
- [x] 6. **Cookie & CSRF Defense**:
  - Cookie `cozynest_token` cấu hình: `HttpOnly`, `SameSite=Lax`, `Secure` (tự động bật trên production), `Path=/`.
  - Bộ lọc `CookieCsrfProtectionFilter` kiểm tra header `Origin` và `Sec-Fetch-Site` cho mọi state-changing request dùng cookie, chặn đứng CSRF.
- [x] 7. **Rate Limit Đăng ký / Đăng nhập theo IP (Redis)**:
  - `RedisRateLimiterAdapter`: áp dụng sliding window rate limit (Đăng nhập: 15 req/phút/IP, Đăng ký: 10 req/phút/IP, Quên mật khẩu: 5 req/giờ).
  - Khi Redis gặp sự cố trên môi trường production, cơ chế tự động chuyển sang **Fail-Closed** (`ERR_RATE_LIMIT_UNAVAILABLE`) để bảo vệ an toàn cho hệ thống. Môi trường dev/test có in-memory fallback.

---

## AUTH-03: PHÂN QUYỀN TRUY CẬP (RBAC)
- [x] 8. **Phân quyền Permission-based ở Backend**:
  - `JwtAuthFilter` trích xuất `roles` và ánh xạ cả `ROLE_<role>` lẫn các quyền hạt nhân (`kyc:review`, `room:moderate`, `user:manage`, `room:create`...).
  - `@PreAuthorize` và `SecurityConfig` thực thi ma trận phân quyền 200/401/403.
- [x] 9. **FE `src/middleware.ts` & Bảo vệ Safe `returnUrl`**:
  - `src/middleware.ts` xác minh chữ ký JWT bằng thư viện `jose` (`RS256`), đối chiếu thời hạn `exp` và mảng quyền `roles`.
  - `src/app/[locale]/(auth)/login/page.tsx`: hàm `getSafeRedirectUrl` thẩm định `returnUrl` phải bắt đầu bằng `/`, chặn đứng giao thức độc hại `//` hoặc domain ngoài (chống Open Redirect).
  - Kiểm tra vai trò của người dùng đối với route đích; nếu người thuê cố tình chuyển hướng tới trang quản trị/chủ trọ, hệ thống tự động đưa về `/403`.
- [x] 10. **Chống Mass-Assignment Vai Trò Khi Đăng Ký**:
  - `RegisterUseCase` chỉ chấp nhận `TENANT` hoặc `LANDLORD`. Mọi nỗ lực truyền `ADMIN`, `MODERATOR` hoặc mảng vai trò đều bị từ chối với HTTP 400 `ERR_AUTH_FORBIDDEN_ROLE`.
- [x] 11. **Granular Permissions & Moderator vs Admin**:
  - MODERATOR chỉ được truy cập các route kiểm duyệt được cấp phép (`/dashboard/admin/rooms`, `kyc`, `reports`, `reviews`, `disputes`, `support`), không thể truy cập các route quản trị hệ thống của ADMIN (`/dashboard/admin/system`, `settings`, `users`).

---

## AUTH-04: QUÊN MẬT KHẨU & OTP
- [x] 12. **Cơ chế OTP An Toàn**:
  - Sinh bằng `SecureRandom`, 6 chữ số (`100000..999999`).
  - Băm bằng BCrypt trước khi lưu vào DB (`otpHash`).
  - Thời hạn hiệu lực: **chính xác 10 phút** (`plus(10, ChronoUnit.MINUTES)`).
  - Tối đa 5 lần thử sai trước khi bị vô hiệu (`attempts >= 5`).
  - Giãn cách gửi lại tối thiểu 60 giây và tối đa 5 lần/giờ qua Redis Rate Limiter.
  - So sánh bằng `passwordEncoder.matches` (hằng thời gian).
  - Yêu cầu quên mật khẩu trả về cùng một thông điệp 200 OK bất kể email có tồn tại hay không (chống dò email).
- [x] 13. **Xử lý Đặt lại Mật khẩu Thành công**:
  - Thu hồi toàn bộ phiên đăng nhập của người dùng (`refreshTokenRepository.revokeAllByUserId`).
  - Mở khóa tài khoản nếu đang bị khóa (`user.unlock()`).
  - Vô hiệu hóa OTP đã sử dụng (`token.setUsed(true)`).
  - Từ chối đặt lại nếu mật khẩu mới trùng mật khẩu cũ (`ERR_AUTH_PASSWORD_REUSE`).
  - Gửi email thông báo "Mật khẩu tài khoản đã được thay đổi thành công" song ngữ tới người dùng.
- [x] 14. **Kiểm chứng E2E với Backend & Mailpit Thật**:
  - Kịch bản tự động `docs/audit/test_auth_e2e.mjs` chạy qua API Gateway (8080) và Mailpit (8025):
    1. Yêu cầu OTP cho `tenant@cozynest.vn` -> HTTP 200.
    2. Đọc OTP thật từ Mailpit API (`/api/v1/messages`).
    3. Nhập OTP sai `999999` -> HTTP 400 `ERR_AUTH_INVALID_OTP`.
    4. Nhập OTP đúng với mật khẩu mới -> HTTP 200 thành công.
    5. Đăng nhập mật khẩu cũ `password123` -> HTTP 401 thất bại.
    6. Đăng nhập mật khẩu mới -> HTTP 200 thành công, nhận JWT token hợp lệ.
    7. Nhận email thông báo đổi mật khẩu thành công từ Mailpit.
- [x] 15. **Email Song ngữ & URL Chuẩn**:
  - Cả HTML và Text templates đều có đầy đủ nội dung song ngữ tiếng Việt và tiếng Anh.
  - Thông báo thời hạn hiệu lực chính xác: "10 phút / 10 minutes".
  - Mọi liên kết dùng cấu hình base URL `${app.frontend-url}`, tuyệt đối không sử dụng header `Host` của request (chống Host Header Injection).
- [x] 16. **Giao diện & Trải nghiệm Người dùng**:
  - Mọi luồng Quên mật khẩu, Nhập OTP, và Đặt lại mật khẩu đều có giao diện hoàn chỉnh, hiển thị lỗi rõ ràng (mã sai, hết hạn, quá số lần).
  - Hỗ trợ đầy đủ song ngữ vi/en, 100% parity giữa `vi.json` và `en.json`.
