# Báo Cáo Kiểm Toán & Đối Chiếu Tính Năng Xác Thực (Auth Audit)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Thời điểm thực hiện:** 2026-11-07  
**Phạm vi:** `identity-service`, `api-gateway`, `accommodation-web`

---

## 1. Bảng đối chiếu [AUTH-01] Đăng ký & Đăng nhập (BCrypt + JWT)

| Yêu cầu kỹ thuật | Hiện trạng Backend (`identity-service`) | Hiện trạng Gateway & Frontend | Đánh giá & Hạng mục cần bổ sung |
| :--- | :--- | :--- | :--- |
| **POST `/register`** (validate, hash BCrypt cost 12, tạo user TENANT/LANDLORD, chặn MODERATOR/ADMIN) | Đã có `RegisterUseCase`, hash BCrypt cost 12. Chưa chặn role MODERATOR/ADMIN từ payload đăng ký. | Frontend form đã có trường chọn `TENANT`/`LANDLORD`. Chưa tích hợp React Hook Form + Zod đầy đủ. | **Backend:** Cần thêm logic validation ép buộc chỉ cho phép `TENANT` hoặc `LANDLORD`. **Frontend:** Cập nhật RHF + Zod schema validation. |
| **POST `/login`** (xác thực mật khẩu, sinh Access Token 15p, Refresh Token rotation) | Đã có `LoginUseCase`, token rotation, lưu hash. Tuy nhiên, access token hạn hiện đang là 24h (`86400000ms`), cần chỉnh về **15 phút** (`900000ms`). | Frontend form đã có, nhưng `authStore` đang lưu access token vào `localStorage` thay vì in-memory. | **Backend:** Cập nhật hạn access token thành 15 phút. **Frontend:** Chuyển access token vào Zustand in-memory state; lưu refresh token qua httpOnly cookie hoặc secure cookie; thêm cookie role phục vụ middleware. |
| **POST `/refresh-token`** (xoay vòng token, thu hồi cả chuỗi nếu bị tái sử dụng) | Đã có trong `RefreshTokenUseCase` (thu hồi qua `revokeAllByUserId`). | Chưa có axios/fetch interceptor tự động refresh khi gặp mã lỗi 401. | **Frontend:** Bổ sung interceptor tự động gọi refresh token một lần chống loop. |
| **POST `/logout`** (thu hồi token) | Đã có endpoint `/logout`. | `useAuthStore.logout` đã gọi API. | Đã hoàn thiện. |
| **GET `/me`** (thông tin người dùng hiện tại) | Đã có `/api/v1/auth/me` và `/api/v1/auth/profile`. | `UserProfileView` đã tái sử dụng. | Đã hoàn thiện. |

---

## 2. Bảng đối chiếu [AUTH-03] Phân quyền truy cập (RBAC)

| Yêu cầu kỹ thuật | Hiện trạng Backend (`identity-service` & các service) | Hiện trạng Gateway & Frontend | Đánh giá & Hạng mục cần bổ sung |
| :--- | :--- | :--- | :--- |
| **Resource Server / JWT Validation Filter** | `identity-service` có `JwtAuthFilter`. Các service khác chưa có JWT resource filter để tự xác thực độc lập. | `api-gateway` chỉ chuyển tiếp route mà chưa giải mã/kiểm tra token hoặc chuyển tiếp claim. | **Backend:** Cần triển khai filter xác thực JWT thống nhất (Resource Server) trên các service có endpoint nghiệp vụ cần bảo vệ; Method Security `@PreAuthorize("hasRole(...)")`. |
| **Chuẩn hóa mã lỗi 401 & 403** | `GlobalExceptionHandler` trả `ApiResponse.error()`, nhưng cần đảm bảo `HttpStatus.UNAUTHORIZED` và `HttpStatus.FORBIDDEN` với error code ổn định. | Đã có trang `/403` tĩnh. Chưa có cơ chế redirect tự động từ middleware khi chưa đăng nhập hoặc sai quyền. | **Backend:** Cấu hình AuthenticationEntryPoint & AccessDeniedHandler trả JSON `ApiResponse`. **Frontend:** Cập nhật Next.js `middleware.ts` kiểm tra cookie role/auth, chuyển hướng `/login?returnUrl=...` và `/403`. |
| **Kiểm tra quyền sở hữu (chống IDOR)** | Đã kiểm tra ở một số use case cơ bản. | Client có role switcher và command palette theo role. | Kiểm soát chặt chẽ ở tầng Application usecase khi thao tác với ID tài nguyên. |

---

## 3. Bảng đối chiếu [AUTH-04] Quên mật khẩu & OTP khôi phục qua email

| Yêu cầu kỹ thuật | Hiện trạng Backend (`identity-service`) | Hiện trạng Frontend | Đánh giá & Hạng mục cần bổ sung |
| :--- | :--- | :--- | :--- |
| **Hạ tầng gửi Email (JavaMailSender / Mailpit)** | Chưa có `spring-boot-starter-mail` trong `identity-service`. | N/A | **Backend:** Thêm starter mail, cấu hình Mailpit (cổng 1025 SMTP, 8025 Web UI) trong `docker-compose.yml`. |
| **Bảng `password_reset_tokens`** | Đã tạo trong Flyway V3 (`id`, `user_id`, `otp_hash`, `expires_at`, `attempts`, `used_at`, `request_ip`). | N/A | Schema đã sẵn sàng trong DB. |
| **POST `/forgot-password`** (sinh OTP 6 số ngẫu nhiên, hash lưu DB, gửi email qua template đa ngôn ngữ) | Chưa có use case và controller endpoint. | Đã có giao diện `/forgot-password`, nhưng đang dùng `setTimeout` giả lập 600ms. | **Backend:** Tạo `ForgotPasswordUseCase`, `EmailService` với template HTML teal/amber đẹp mắt, lưu otp băm BCrypt. **Frontend:** Nối API thật, thêm đếm ngược 60 giây gửi lại OTP. |
| **POST `/reset-password`** (xác thực OTP, hạn 15p, thử tối đa 5 lần, đổi pass, thu hồi toàn bộ refresh tokens) | Chưa có use case và controller endpoint. | Đã có giao diện `/reset-password`, nhưng đang dùng `setTimeout` giả lập. | **Backend:** Tạo `ResetPasswordUseCase`, cập nhật pass hash BCrypt, revoke refresh token. **Frontend:** Nối API thật, xử lý lỗi chi tiết qua i18n, thanh đo độ mạnh mật khẩu. |
| **Kiểm thử tự động e2e** | Chưa có test kiểm tra OTP và Mailpit. | Chưa có spec Playwright cho quên mật khẩu. | **Test:** Viết test integration backend và Playwright e2e kiểm tra toàn bộ luồng quên mật khẩu. |

---

## 4. Kế hoạch Thực hiện Cụ thể
1. **[AUTH-01] Backend & Frontend:**
   - Cập nhật thời hạn JWT Access Token về 15 phút (900,000 ms) trong `application.yml`.
   - Bổ sung validation chặn đăng ký MODERATOR/ADMIN từ endpoint công khai.
   - Nâng cấp `auth.store.ts` lưu access token trong bộ nhớ, lưu role cookie cho middleware.
2. **[AUTH-03] Phân quyền:**
   - Viết `CustomAuthenticationEntryPoint` (401) và `CustomAccessDeniedHandler` (403) trả `ApiResponse` chuẩn.
   - Nâng cấp Next.js `middleware.ts` bảo vệ các route `/tenant/**`, `/landlord/**`, `/moderator/**`, `/admin/**`.
3. **[AUTH-04] Quên mật khẩu:**
   - Thêm `spring-boot-starter-mail`, cấu hình Mailpit trong `docker-compose.yml` (port 1025 SMTP, 8025 Web).
   - Viết `ForgotPasswordUseCase` và `ResetPasswordUseCase` trong `identity-service`.
   - Nối API thật tại `/forgot-password` và `/reset-password` ở frontend, dịch lỗi qua i18n.
   - Viết bộ test kiểm tra tự động.
