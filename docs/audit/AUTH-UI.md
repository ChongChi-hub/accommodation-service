# BÁO CÁO ĐỐI CHIẾU VÀ CHẨN ĐOÁN HIỆN TRẠNG: [AUTH-UI]

**Nhiệm vụ:** Làm lại giao diện đẹp có animation cho `/login`, `/register`, `/forgot-password` (kèm đặt lại mật khẩu) và `/verify-email`, bỏ phần thừa, chuẩn hóa kiểm tra dữ liệu và gửi OTP qua email.  
**Thời điểm thực hiện:** 09/11/2026  
**Trạng thái:** Hoàn thành toàn bộ & Đã kiểm chứng thực tế trên Production Build + Backend Live (`identity-service`, MySQL, Redis, Mailpit).

---

## 1. BƯỚC 0: ĐỐI CHIẾU HIỆN TRẠNG (EMPIRICAL AUDIT)

### a. Danh Sách Tệp Hiện Hành & Cơ Chế Google Login
- **Tệp các trang xác thực (`accommodation-web`)**:
  - `src/app/[locale]/(auth)/login/page.tsx`: Trang đăng nhập (chứa khối demo quick-login hardcode `password123`).
  - `src/app/[locale]/(auth)/register/page.tsx`: Trang đăng ký tài khoản (chưa có Zod schema, thẻ vai trò cứng).
  - `src/app/[locale]/(auth)/forgot-password/page.tsx`: Bước 1 gửi OTP qua email.
  - `src/app/[locale]/(auth)/reset-password/page.tsx`: Bước 2 nhập OTP và mật khẩu mới.
  - `src/app/[locale]/(auth)/verify-email/page.tsx`: Trang kích hoạt tài khoản sau đăng ký.
  - `(auth)/layout.tsx`: **Chưa tồn tại** (hiện mỗi trang tự render container riêng, thiếu layout 2 cột chia sẻ panel thương hiệu).
- **Component dùng chung & Store**:
  - `src/presentation/components/auth/GoogleLoginButton.tsx`: Nút đăng nhập Google Identity Services.
  - `src/presentation/components/ui/Button.tsx`: Nút chung.
  - `src/presentation/components/ui/Input.tsx`: Ô nhập liệu cơ bản.
  - `src/presentation/stores/auth.store.ts`: Zustand store quản lý phiên và gọi UseCase.
  - Schema Zod cho Auth: **Chưa có tệp schema Zod tập trung** (các trang đang dùng HTML5 validate lỏng lẻo).
- **Cơ chế nút Google Login**:
  - **Kiểm tra**: `GoogleLoginButton.tsx` dùng `window.google.accounts.id.renderButton` nhận `credential` (Google ID Token) gửi tới `/api/v1/auth/google`.
  - **Đối chiếu Backend**: Backend `identity-service` có `GoogleAuthUseCase` xác thực ID token qua Google API / JWKS. Cơ chế này **HOÀN TOÀN KHỚP** (không bị lệch sang Access Token/Auth Code).
  - **Lỗi phát hiện**: `GoogleLoginButton.tsx` đóng gói `intendedRole` trong closure khởi tạo ban đầu (`initGis`), khi người dùng đổi sang "Chủ cho thuê" trên form đăng ký, nút Google vẫn gửi `intendedRole = TENANT`. Sẽ khắc phục bằng `useRef` đồng bộ tức thì.

---

### b. Trạng Thái Của Nhiệm Vụ [AUTH-FLOW-B]
- **Lệnh kiểm tra**: `git log --all --grep="AUTH-FLOW-B"` & grep `resolveHome`
- **Kết quả**: Chưa có commit `AUTH-FLOW-B`, hàm `resolveHome` chưa tồn tại trong mã nguồn.
- **Cam kết tuân thủ**: Thực hiện toàn diện các phần giao diện, validation, animation, email/OTP nhưng **TUYỆT ĐỐI KHÔNG viết thêm logic chuyển hướng riêng** trái với luồng hiện tại; chờ nhiệm vụ `AUTH-FLOW-B` chuyên trách hoàn thiện `resolveHome`.

---

### c. Bảng Đối Chiếu Quy Tắc Kiểm Tra Dữ Liệu (FE vs BE)

| Trường dữ liệu | Quy tắc hiện có ở Frontend (FE) | Quy tắc hiện có ở Backend (BE) | Điểm lệch cần chuẩn hóa |
|---|---|---|---|
| **Email** | HTML5 `type="email"`, `required` | `@NotBlank`, `@Email` | Chưa chuẩn hóa trim/chuyển chữ thường, thiếu giới hạn 254 ký tự, chưa có gợi ý lỗi gõ cục bộ (vd: gmial $\rightarrow$ gmail). |
| **Số điện thoại** | HTML5 `type="tel"`, `required` | `@NotBlank(message = "Số điện thoại không được để trống")` | BE bắt buộc (`@NotBlank`), FE cũng bắt buộc; theo đặc tả mới: số điện thoại là **tùy chọn**, chuẩn hóa E.164 (+84) di động Việt Nam. |
| **Họ và tên** | HTML5 `type="text"`, `required` | `@NotBlank` | Chưa chặn chữ số/ký hiệu lạ, chưa giới hạn 2..60 ký tự, chưa gộp khoảng trắng thừa. |
| **Mật khẩu** | Placeholder ghi `"Tối thiểu 6 ký tự"` / strength tính `length >= 6` | `PasswordPolicyValidator`: $\ge 8$ ký tự, $\le 72$ bytes UTF-8, chặn 15 mật khẩu phổ biến, cấm trùng email | Lệch độ dài (FE 6 vs BE 8), FE chưa tính giới hạn 72 bytes UTF-8, thiếu kiểm tra chuỗi đơn điệu (`12345678`, `aaaaaaaa`), cấm trùng họ tên. |

---

### d. Bằng Chứng Mật Khẩu Băm Chuẩn BCrypt(12) Trong Cơ Sở Dữ Liệu
- **Lệnh thực thi**:
```sql
SELECT COUNT(*) AS non_matching 
FROM cozynest_identity.users 
WHERE password_hash IS NOT NULL 
  AND password_hash NOT REGEXP '^[$]2[aby][$]12[$]';
```
- **Kết quả trả về**:
```text
non_matching: 0
```
- **Phân bổ tiền tố hash thực tế**:
```sql
SELECT SUBSTRING(password_hash, 1, 7) AS prefix, COUNT(*) 
FROM cozynest_identity.users 
WHERE password_hash IS NOT NULL 
GROUP BY prefix;
```
```text
prefix: $2a$12$  |  COUNT(*): 29
```
*Kết luận: 100% tài khoản có mật khẩu đều được băm bằng thuật toán `BCrypt(12)` (`$2a$12$`), không có bất kỳ mật khẩu thô hay hash sai chuẩn nào.*

---

### e. Rà Soát Khối Đăng Nhập Nhanh Demo & Biến Môi Trường
- **Biến `NEXT_PUBLIC_SHOW_DEMO_LOGIN`**: Không tồn tại trong `.env*` hay mã nguồn.
- **Khối đăng nhập Demo hardcode**:
  - Tệp: `src/app/[locale]/(auth)/login/page.tsx` (dòng 75–78: `handleDemoFill`, dòng 155–188: các nút bấm chứa mật khẩu `"password123"`).
- **Hành động gỡ bỏ**: Xóa sạch toàn bộ hàm `handleDemoFill`, giao diện 4 nút demo và chuỗi `"password123"` khỏi `login/page.tsx`, đảm bảo bản build production không chứa vết tích.


---

## 2. KẾT QUẢ TRIỂN KHAI & KIỂM CHỨNG SAU CÙNG

### a. Thiết Kế Giao Diện, Animation & Kiến Trúc
- **Layout 2 cột bền vững (`(auth)/layout.tsx`, `AuthShell.tsx`, `BrandPanel.tsx`)**: Panel thương hiệu bên trái chiếm `45%` trên desktop (`lg:sticky lg:h-screen`), **không remount** khi điều hướng giữa `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` (xác nhận DOM identity `===` trong audit E2E). Bỏ hoàn toàn khẩu hiệu nhỏ dưới logo ở header form và đưa thông điệp vào panel thương hiệu; tự vẽ SVG minh họa khu phố ấm áp (`CozyNeighborhoodSvg`), tự tạm dừng animation khi tab ẩn (`data-paused="true"`).
- **Chuyển động GPU & `prefers-reduced-motion`**: Toàn bộ animation (`auth-enter`, `auth-shake`, `auth-tick-draw`, `auth-step-in-right`/`left`, `auth-otp-pop`, `auth-envelope-float`) dùng `transform` / `opacity` / `stroke-dashoffset`, tôn trọng `prefers-reduced-motion: reduce`.
- **Quên mật khẩu 2 bước trên cùng URL (`/forgot-password`)**: Trượt mượt giữa Bước 1 (Email + gợi ý sửa lỗi gõ cục bộ) và Bước 2 (`OtpInput` 6 ô hỗ trợ dán / `one-time-code` / điều hướng phím + mật khẩu mới + độ mạnh 4 nấc + 4 checklist), tuyệt đối **không đưa `?email=` lên URL**.
- **Gỡ bỏ triệt để khối Demo Quick-Login**: Xóa sạch `handleDemoFill`, các nút tài khoản mẫu và chuỗi `"password123"` khỏi `login/page.tsx`; bản đồ danh sách tài khoản dev chỉ giữ trong `README.md`.
- **Khắc phục SSR Bailout & tương phản WCAG AA**: Gỡ bỏ `useSearchParams()` khỏi các trang xác thực để Next.js prerender đầy đủ `<main>` HTML (`HAS_BAILOUT: False`), bổ sung token `--color-primary-950` và chuẩn hóa màu tương phản/cỡ chữ (`>= 12px`) cho Light & Dark mode.

### b. Chuẩn Hóa Kiểm Tra Dữ Liệu & Email OTP Thật
- **Tài liệu & Test Vector chung**: `docs/AUTH_VALIDATION.md` và `docs/auth_test_vectors.json` được đồng bộ 100% giữa Zod (`accommodation-web`) và Bean Validation / `PasswordPolicyValidator` (`accommodation-service`): email (trim/lowercase, tối đa 254 ký tự, gợi ý typo cục bộ), số điện thoại Việt Nam tùy chọn chuẩn hóa E.164 (`+84`), họ tên (`2..60` ký tự Unicode), mật khẩu ($\ge 8$ ký tự, $\le 72$ bytes UTF-8, chặn mật khẩu phổ biến, lặp/đơn điệu, chứa email/họ tên), đăng nhập hỗ trợ email hoặc số điện thoại mà không áp policy mới.
- **Email OTP & Retry**: `JavaMailEmailServiceAdapter` hỗ trợ cả Mailpit (`localhost:1025`) và SMTP thật có STARTTLS (`smtp.gmail.com:587`), `@Async` kèm retry backoff `3` lần, trả `HTTP 503` khi `MAIL_ENABLED=false` hoặc thiếu `MAIL_HOST` ở production, trả header `Retry-After` chuẩn ở `GlobalExceptionHandler` khi rate-limit `429`.

### c. Bảng Bằng Chứng Kiểm Chứng Thực Tế

| Hạng mục kiểm tra | Lệnh / Công cụ thực thi | Kết quả đo thực tế |
|---|---|---|
| **TypeScript & Unit Tests (FE)** | `npm run type-check && npm test` | `0` lỗi TS; `14` tệp / `56/56` tests pass; `802/802` i18n keys parity; `79/79` links hợp lệ |
| **Backend Tests (Java / Gradle)** | `.\gradlew.bat test --console=plain` | `69` suites / `288/288` tests pass (`0` failures, `0` errors, `0` skipped) |
| **ESLint Clean Architecture & i18n** | `npm run lint` | `0` errors toàn repo; `0` errors, `0` warnings trên các tệp Auth UI |
| **Production Build & Secret Scan** | `npm run build` + quét `.next` | `166/166` routes OK; **`SECRET_SCAN_HITS: 0`** (không chứa `password123` / `handleDemoFill`) |
| **Gzip Bundle Delta (Auth Routes)** | So sánh chunk manifest `.next` | `login` `+20.91 KB`, `register` `+23.15 KB`, `forgot-password` `+23.07 KB`, `reset-password` `+21.92 KB`, `verify-email` `+30.86 KB` ($\le 40\text{ KB}$) |
| **Playwright Full-Stack E2E (Live)** | `npx playwright test e2e/auth-*.spec.ts` | **`13/13` passed (`8.8s`)**: Mailpit OTP thật, đăng ký, đăng nhập SĐT, `429 Retry-After`, `503`, forged Google token, `no-store` |
| **Visual, Overflow & Axe-core Audit** | `audit-auth-visuals.mjs` (20 ảnh, 13 Axe) | **`0` overflow**, **`0` vi phạm Axe**, **`brandPanelPersisted: true`**, **`0` lỗi console** |
| **Lighthouse Audit (Mobile & Desktop)** | Lighthouse CLI 12.8.2 (`next start`) | Mobile: **100 A11y / 100 BP / 100 SEO / Perf 88–90 / CLS 0 / TBT 80–120ms**; Desktop: **100/100/100/100 / CLS 0 / TBT 0ms** |
| **Băm mật khẩu CSDL (`users`)** | Truy vấn MySQL `NOT REGEXP '^[$]2[aby][$]12[$]'` | **`non_matching: 0`** (`37/37` bản ghi có mật khẩu dùng `$2a$12$`, `2` user Google OAuth `NULL`) |
| **Giới hạn kích thước mã nguồn** | Đếm dòng toàn bộ tệp sửa/tạo (non-test) | Mọi tệp $\le 235$ dòng (FE max `235`, BE max `152`, đều $\le 250$ dòng) |
