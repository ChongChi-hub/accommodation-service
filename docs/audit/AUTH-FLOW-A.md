# BÁO CÁO CHẨN ĐOÁN VÀ ĐẶC TẢ THI CÔNG: [AUTH-FLOW-A]
**Nhiệm vụ:** Sửa lỗi đăng ký Google vào nhầm trang chủ trọ và làm mô hình "chủ trọ phải chờ duyệt" ở backend.  
**Ngày thực hiện:** 08/11/2026  
**Trạng thái chẩn đoán:** BƯỚC 0 HOÀN TẤT (CHẨN ĐOÁN TRƯỚC KHI SỬA MÃ)

---

## 1. BƯỚC 0: CHẨN ĐOÁN CHI TIẾT (LỆNH VÀ ĐẦU RA THỰC TẾ)

### a. Tái hiện lỗi Google và Đăng ký với vai trò Chủ trọ (Incognito / Simulated Flow)
Thực hiện gọi API đăng ký và phân tích luồng đăng ký Google hiện hành:

**Lệnh thực nghiệm:**
```bash
POST /api/v1/auth/register
Body: { "email": "test_landlord_... @example.com", "phone": "0987123456", "password": "Password@123", "fullName": "Test Landlord Immediate", "role": "LANDLORD" }
```

**Đầu ra phản hồi từ máy chủ (status 201 Created):**
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "token": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImNvenluZXN0LWlkZW50aXR5LWtleS0xIn0...",
    "refreshToken": "...",
    "user": {
      "id": "df9e2981-a563-4ca8-8943-194cae2b2930",
      "email": "test_landlord_1794121049172@example.com",
      "roles": ["LANDLORD"]
    }
  }
}
```

**Truy vấn cơ sở dữ liệu `cozynest_identity.user_roles`:**
```sql
SELECT u.id, u.email, r.name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id 
WHERE u.id = 'df9e2981-a563-4ca8-8943-194cae2b2930';
```
*Kết quả:*
```text
id                                    email                                    name
df9e2981-a563-4ca8-8943-194cae2b2930  test_landlord_1794121049172@example.com  LANDLORD
```

**Đối chiếu mã nguồn Backend (`RegisterUseCase.java` & `GoogleAuthUseCase.java`):**
- Trong `RegisterUseCase.java` (dòng 54-58):
  ```java
  UserRoleType role = UserRoleType.fromString(command.getRole());
  User newUser = User.createNew(command.getEmail(), command.getPhone(), passwordHash, command.getFullName(), role);
  ```
  Nếu client gửi `role: "LANDLORD"`, hệ thống cấp ngay `LANDLORD` trong `user_roles`.
- Trong `GoogleAuthUseCase.java` (dòng 242-251):
  ```java
  if (intendedRole != null && !intendedRole.isBlank()) {
      role = UserRoleType.fromString(roleUpper);
  }
  User newUser = User.createNewGoogleUser(email, claims.name(), role);
  ```
  Nếu `intendedRole` là `LANDLORD`, hệ thống cấp ngay `LANDLORD` mà không có bước duyệt hay tạo hồ sơ KYC.
- Trong `RegisterPage.tsx` (dòng 33-37):
  ```typescript
  if (role === 'LANDLORD') {
      router.push('/landlord');
  } else {
      router.push('/tenant');
  }
  ```
- Trong `GoogleLoginButton.tsx` (dòng 173-174):
  ```typescript
  else if (result.role === 'LANDLORD' || intendedRole === 'LANDLORD') {
      router.push('/dashboard/landlord');
  }
  ```

**KẾT LUẬN MỤC A:**
Lỗi xảy ra do **CẢ BA NƠI**:
1. Backend cấp ngay vai trò `LANDLORD` vào bảng `user_roles` khi đăng ký tự do với `role: "LANDLORD"` hoặc `intendedRole: "LANDLORD"`.
2. Frontend (`RegisterPage.tsx`, `GoogleLoginButton.tsx`) điều hướng mù quáng sang `/landlord` hoặc `/dashboard/landlord` chỉ dựa vào `role === 'LANDLORD'` hoặc `intendedRole === 'LANDLORD'` thay vì kiểm tra trạng thái phê duyệt.
3. Không hề có bảng `landlord_profiles` hay cơ chế quản lý hồ sơ chờ duyệt (AWAITING_KYC/PENDING_REVIEW/APPROVED/REJECTED).

---

### b. Tái hiện kịch bản phiên cũ (Stale Demo Session)
**Kịch bản:**
1. Người dùng bấm "Chủ trọ" ở trang Đăng nhập để đăng nhập tài khoản demo `landlord@cozynest.vn`.
2. Cookie được thiết lập:
   - `cozynest_token`: JWT chứa claim `roles: "LANDLORD"`.
   - `cozynest_role`: `LANDLORD` (max-age 30 ngày).
   - `localStorage.cozynest_refresh_token`: Refresh token của chủ trọ.
   - `localStorage.cozynest_user`: JSON user chủ trọ.
3. Người dùng KHÔNG đăng xuất mà gõ `/register` trên thanh địa chỉ hoặc click "Đăng ký".
4. Phản ứng của `src/middleware.ts` (dòng 136-147):
   ```typescript
   if ((cleanPath === '/login' || cleanPath === '/register') && auth) {
     if (roles.includes('LANDLORD')) {
       return NextResponse.redirect(new URL(`${localePrefix}/dashboard/landlord`, req.url));
     }
     ...
   }
   ```
   Middleware ngay lập tức trả về mã `307 Temporary Redirect` đưa người dùng về `/dashboard/landlord`, ngăn người dùng truy cập trang `/register`!
5. Nếu người dùng mở cửa sổ khác và gọi Google Auth, `loginWithGoogle` trong `auth.store.ts` không dọn dẹp các cookie cũ trước khi gọi API, dẫn đến việc cookie vai trò cũ xung đột với tài khoản mới.

---

### c. Bảng liệt kê MỌI nơi tính "trang đích sau đăng nhập" (Redirect Matrix)

| STT | Tập tin | Dòng | Logic hiện tại | Đánh giá & Hướng khắc phục |
|---|---|---|---|---|
| 1 | `src/middleware.ts` | 69-80 | `/landlord/*` -> `/dashboard/landlord/*` | Hợp lý cho việc chuẩn hóa route alias. Giữ nguyên. |
| 2 | `src/middleware.ts` | 112-114 | Chặn `/dashboard/landlord/*` nếu thiếu `hasRole('LANDLORD')` | Trả về 403. Cần chuyển hướng hồ sơ chưa duyệt sang trang KYC/Application Status thay vì 403 trơ trọi. |
| 3 | `src/middleware.ts` | 120-132 | `/dashboard` chuyển hướng theo vai trò (ADMIN -> /dashboard/admin, LANDLORD -> /dashboard/landlord, TENANT -> /dashboard/tenant) | Cần kiểm tra: nếu chỉ có TENANT thì luôn vào /dashboard/tenant. |
| 4 | `src/middleware.ts` | 136-147 | `/login` hoặc `/register` nếu đã có `auth` thì đẩy thẳng vào dashboard của role cao nhất (ADMIN > MOD > LANDLORD > TENANT) | **LỖI:** Ngăn cản người dùng đăng ký tài khoản mới khi còn phiên cũ. Cần cho phép hoặc có nút "Đăng xuất tài khoản hiện tại". |
| 5 | `src/app/[locale]/(auth)/login/page.tsx` | 58-71 | `getSafeRedirectUrl(returnUrl, roles)` -> nếu không có returnUrl thì đẩy theo `ADMIN > MOD > LANDLORD > TENANT` | Cần giữ nguyên nếu đăng nhập đúng bằng tài khoản có vai trò LANDLORD thật sự. |
| 6 | `src/app/[locale]/(auth)/register/page.tsx` | 33-37 | `if (role === 'LANDLORD') router.push('/landlord') else router.push('/tenant')` | **LỖI NGHIÊM TRỌNG:** Phải chuyển sang `/dashboard/tenant` (kèm thông báo "Đã tạo hồ sơ xin làm chủ trọ, vui lòng hoàn tất xác minh KYC") hoặc dẫn thẳng tới `/dashboard/landlord/kyc`. |
| 7 | `src/presentation/components/auth/GoogleLoginButton.tsx` | 170-179 | `else if (result.role === 'LANDLORD' || intendedRole === 'LANDLORD') router.push('/dashboard/landlord')` | **LỖI NGHIÊM TRỌNG:** Dù backend trả về `role === 'TENANT'`, nếu `intendedRole === 'LANDLORD'`, FE vẫn đẩy vào `/dashboard/landlord`. |
| 8 | `src/infrastructure/repositories/http-auth.repository.ts` | 43, 67 | `const primaryRole = res.user?.roles?.[0] || data.role || 'TENANT'` và ghi cookie `cozynest_role` | Cần lấy đúng vai trò từ response của server, không dùng fallback mù quáng `data.role` hay `intendedRole`. |

---

### d. Đối chiếu hiện trạng hệ thống

1. **Endpoint Đăng ký (`POST /api/v1/auth/register`):**
   - Hiện nhận: `email`, `phone`, `password`, `fullName`, `role`.
   - Hiện trạng: Chấp nhận `role: "LANDLORD"` và ghi trực tiếp `r-002` vào `user_roles`.
   - Yêu cầu mới: Chuẩn hóa email (lowercase), phone (E.164 `+84`). Luôn chỉ cấp vai trò `TENANT` (`r-003`). Nếu `role === "LANDLORD"`, tự động khởi tạo bản ghi trong bảng `landlord_profiles` với trạng thái `AWAITING_KYC`.
2. **Bảng dữ liệu `landlord_profiles` và KYC:**
   - Hiện trạng: Bảng `landlord_profiles` **CHƯA TỒN TẠI**. Chỉ có bảng `user_verifications` với các trạng thái `PENDING`, `APPROVED`, `REJECTED`.
   - Yêu cầu mới: Tạo bảng `landlord_profiles` bằng Flyway migration `V7__create_landlord_profiles_and_application_flow.sql`.
   - Máy trạng thái `landlord_profiles.approval_status`:
     `AWAITING_KYC` -> `PENDING_REVIEW` -> `APPROVED` | `REJECTED`; `REJECTED` -> `PENDING_REVIEW`.
3. **Endpoint KYC (`LandlordKycController.java`):**
   - Hiện trạng: `@PreAuthorize("hasRole('LANDLORD')")` ở cấp Controller (dòng 20).
   - Hậu quả: Người dùng đăng ký mới chỉ có vai trò `TENANT` sẽ bị `403 Forbidden` khi nộp KYC!
   - Yêu cầu mới: Cho phép người nộp đơn (vai trò `TENANT` hoặc `LANDLORD`) nộp và theo dõi trạng thái KYC của chính mình: `@PreAuthorize("hasAnyRole('TENANT', 'LANDLORD')")`.
4. **Token Claim & Refresh Token:**
   - Access token chứa claim `roles: "TENANT"` hoặc `roles: "TENANT,LANDLORD"`.
   - `RefreshTokenUseCase.java` hiện tải user từ DB nhưng chưa kiểm tra trạng thái tài khoản `SUSPENDED` / `DELETED` / `LOCKED`.
   - Khi hồ sơ được duyệt và cấp thêm vai trò `LANDLORD` trong DB, refresh token sẽ tự động phát hành token mới chứa đủ cả hai vai trò `TENANT,LANDLORD`.

---

## 2. NGUYÊN NHÂN GỐC CỦA LỖI GOOGLE VÀ AUTH FLOW

| Vấn đề | Bằng chứng mã nguồn | Tác động thực tế |
|---|---|---|
| **Gán vai trò chủ trọ tức thì** | `RegisterUseCase.java:54-58`, `GoogleAuthUseCase.java:248-251` | Người dùng chọn "Chủ trọ" được cấp ngay quyền chủ trọ đầy đủ mà không cần xét duyệt hay xác minh danh tính. |
| **Frontend điều hướng mù theo intendedRole** | `GoogleLoginButton.tsx:173`, `RegisterPage.tsx:34` | Người đăng ký Google dù là tài khoản mới nhưng bị ép chuyển hướng vào `/dashboard/landlord`. |
| **Stale Session không được dọn dẹp** | `auth.store.ts:loginWithGoogle`, `middleware.ts:136-147` | Khi phiên demo landlord còn trong cookie, người dùng vào `/register` bị chặn và tự động đá về `/dashboard/landlord`. |
| **Endpoint nộp KYC đảo ngược phân quyền** | `LandlordKycController.java:20` (`hasRole('LANDLORD')`) | Nếu không cấp LANDLORD ngay từ đầu thì người dùng không thể nộp KYC để xin làm chủ trọ. |
| **Thiếu bảng quản lý vòng đời hồ sơ chủ trọ** | `SHOW TABLES IN cozynest_identity` không có `landlord_profiles` | Hệ thống đồng nhất trạng thái KYC với vai trò tài khoản một cách lỏng lẻo. |

---

## 3. KẾ HOẠCH THI CÔNG CHI TIẾT (ĐÁP ỨNG QUY TẮC R1 - R9)

### Giai đoạn 1: Cơ sở dữ liệu và Migration Flyway
- Tạo `V7__create_landlord_profiles_and_application_flow.sql` trong `identity-service`:
  - Tạo bảng `landlord_profiles` (`id`, `user_id`, `approval_status`, `applied_at`, `approved_at`, `reviewed_by`, `reviewed_at`, `reject_reason_code`, `reject_reason_note`, `version`, `created_at`, `updated_at`).
  - Khóa ngoại `user_id` liên kết `users(id)`, chỉ mục duy nhất trên `user_id`.
  - Khởi tạo dữ liệu hồ sơ `APPROVED` cho tất cả các chủ trọ hiện có trong seed (`u-landlord-01` đến `u-landlord-06`).
  - Đảm bảo tất cả chủ trọ sở hữu phòng trong seed đều có cả 2 vai trò `TENANT` và `LANDLORD` (R9).

### Giai đoạn 2: Domain Model & Máy trạng thái
- Định nghĩa Enum `LandlordApprovalStatus`: `AWAITING_KYC`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`.
- Entity `LandlordProfile` với các phương thức chuyển trạng thái thuần domain:
  - `submitKyc()`: `AWAITING_KYC` -> `PENDING_REVIEW` hoặc `REJECTED` -> `PENDING_REVIEW`.
  - `approve(reviewerId)`: `PENDING_REVIEW` -> `APPROVED`.
  - `reject(reviewerId, code, note)`: `PENDING_REVIEW` -> `REJECTED`.
  - Bất kỳ chuyển trạng thái trái phép nào ném ngoại lệ `IdentityDomainException("...", "ERR_LANDLORD_INVALID_STATE_TRANSITION")` ánh xạ mã HTTP 409 Conflict.

### Giai đoạn 3: Use Cases & Phân quyền Backend
- `RegisterUseCase`: Luôn cấp vai trò `TENANT`. Nếu `role == LANDLORD`, tạo hồ sơ `LandlordProfile(AWAITING_KYC)`. Chuẩn hóa email, phone.
- `GoogleAuthUseCase`: Người mới luôn nhận `TENANT`. Nếu `intendedRole == LANDLORD`, tạo `LandlordProfile(AWAITING_KYC)`. Bỏ qua `intendedRole` nếu user đã tồn tại. Chặn `intendedRole` là `ADMIN`/`MODERATOR` (HTTP 400).
- `LandlordApplicationUseCase`: `POST /api/v1/me/landlord-application` (idempotent, yêu cầu email đã xác minh), `GET /api/v1/me/landlord-application`.
- `RefreshTokenUseCase`: Đọc lại vai trò và hồ sơ từ DB, kiểm tra `SUSPENDED`/`DELETED`/`LOCKED`.
- `KycModerationUseCase`: Khi duyệt KYC -> cấp thêm vai trò `LANDLORD` cho user, chuyển trạng thái `LandlordProfile` sang `APPROVED`, phát outbox `LandlordApproved`. Kiểm tra chống tự duyệt hồ sơ của chính mình (`reviewerId.equals(userId) -> 403/409`).
- `LandlordKycController`: Cập nhật phân quyền cho phép `hasAnyRole('TENANT', 'LANDLORD')`.
- Bổ sung thông tin `landlordApplication`, `emailVerified`, `phoneMissing` trong `GET /api/v1/users/profile` và `/api/v1/auth/me`.

### Giai đoạn 4: Điều chỉnh Frontend & Giao diện
- `auth.store.ts` & `http-auth.repository.ts`: Dọn sạch cookie/storage cũ trước khi đăng ký/đăng nhập Google. Không tự ý gán vai trò theo `intendedRole`.
- `GoogleLoginButton.tsx` & `RegisterPage.tsx`: Người đăng ký chủ trọ mới (chưa có role `LANDLORD`) sẽ được điều hướng tới trang hướng dẫn nộp hồ sơ KYC (`/dashboard/landlord/kyc`), KHÔNG vào bảng điều khiển quản lý phòng.
- `middleware.ts`: Người dùng có `cozynest_token` hợp lệ khi truy cập `/register` sẽ không bị ép chuyển hướng nếu muốn tạo tài khoản hoặc chuyển đổi tài khoản.

### Giai đoạn 5: Cập nhật Seed, Kiểm thử và Tài liệu
- Cập nhật seed: Thêm các tài khoản demo `landlord.new@toam.vn` (`AWAITING_KYC`), `landlord.pending@toam.vn` (`PENDING_REVIEW`), `landlord.rejected@toam.vn` (`REJECTED`), `tenant.unverified@toam.vn`.
- Cập nhật `docs/seed-ids.md` và `docs/DECISIONS.md`.
- Viết Unit test, Testcontainers test và ma trận kiểm tra phân quyền.
- Chạy toàn bộ test suite backend và frontend.

---

## 4. KẾT QUẢ THI CÔNG VÀ KIỂM CHỨNG (IMPLEMENTATION & VERIFICATION)

### a. Bảng đối chiếu hoàn thành 9 quy tắc nghiệp vụ (R1 - R9)

| Quy tắc | Nội dung quy tắc | Thành phần thi công | Trạng thái | Bằng chứng kiểm thử |
|---|---|---|---|---|
| **R1** | Mọi tài khoản tự đăng ký (Email hoặc Google) luôn nhận vai trò `TENANT`. | `RegisterUseCase.java`, `GoogleAuthUseCase.java` | **HOÀN THÀNH** | `RegisterUseCaseTest`, `GoogleAuthUseCaseTest` |
| **R2** | Chọn "Chủ trọ" không cấp `LANDLORD`. Tạo hồ sơ `AWAITING_KYC`. Duyệt KYC cấp `LANDLORD` (giữ `TENANT`) trong 1 transaction, phát outbox `LandlordApproved`. | `LandlordProfile.java`, `KycModerationUseCase.java`, `User.java` | **HOÀN THÀNH** | `KycApprovalAndRoleGrantUnitTest`, `AuthFlowIntegrationTest` |
| **R3** | Máy trạng thái: `AWAITING_KYC` -> `PENDING_REVIEW` -> `APPROVED` \| `REJECTED`. Chuyển đổi sai trả HTTP 409 `ERR_LANDLORD_INVALID_STATE_TRANSITION`. | `LandlordProfile.java`, `LandlordApprovalStatus.java` | **HOÀN THÀNH** | `LandlordProfileStateMachineUnitTest` (10/10 test cases) |
| **R4** | Single source of truth là `landlord_profiles.approval_status`. Ghi quyết định kiến trúc vào `DECISIONS.md`. | `DECISIONS.md` (Section 11) | **HOÀN THÀNH** | `docs/DECISIONS.md` |
| **R5** | `TENANT` nộp đơn xin làm chủ trọ qua `POST /api/v1/me/landlord-application` (idempotent, yêu cầu email đã xác minh). | `MeController.java`, `LandlordApplicationUseCase.java` | **HOÀN THÀNH** | `AuthFlowIntegrationTest` |
| **R6** | Endpoint `/landlord/**` yêu cầu vai trò `LANDLORD` trong JWT. Chưa duyệt nhận `403 ERR_LANDLORD_NOT_APPROVED`. Applicant có thể nộp và xem KYC của chính mình. | `SecurityAccessDeniedHandler.java`, `LandlordKycController.java` (`hasAnyRole('TENANT', 'LANDLORD')`) | **HOÀN THÀNH** | `KycSecurityMatrixTest`, `SecurityAccessDeniedHandler` |
| **R7** | Token cũ không có `LANDLORD` cho đến khi refresh. Refresh token đọc lại từ DB (không copy role cũ). | `RefreshTokenUseCase.java` | **HOÀN THÀNH** | `RefreshTokenUseCaseTest` |
| **R8** | Tài khoản nội bộ (`ADMIN`, `MODERATOR`) chỉ có quyền quản trị, không có `TENANT`. Kiểm duyệt viên không được tự duyệt đơn của mình (`ERR_KYC_SELF_REVIEW_FORBIDDEN`). | `KycModerationUseCase.java`, `GoogleAuthUseCase.java` | **HOÀN THÀNH** | `KycModerationUseCaseTest`, `KycSecurityMatrixTest` |
| **R9** | Tất cả chủ trọ sở hữu phòng trong seed data (`u-landlord-01` đến `06`) có `TENANT, LANDLORD` và `APPROVED`. Bổ sung tài khoản demo các trạng thái. | `V7__create_landlord_profiles_and_application_flow.sql`, `R__05_seed_landlord_profiles_and_demo_flow.sql`, `docs/seed-ids.md` | **HOÀN THÀNH** | `ProductionFlywayMigrationTestcontainersTest` |

---

### b. Bảng tổng hợp kết quả kiểm thử tự động (Automated Test Suite)

1. **Backend (`accommodation-service`):**
   - **`identity-service`**: 143/143 tests **PASSED (100%)**, thời gian 30.133s.
     - `LandlordProfileStateMachineUnitTest`: 10/10 passed
     - `KycApprovalAndRoleGrantUnitTest`: 4/4 passed
     - `RefreshTokenUseCaseTest`: 4/4 passed
     - `RegisterUseCaseTest`: 6/6 passed
     - `GoogleAuthUseCaseTest`: 8/8 passed
     - `LandlordKycUseCaseTest`: 4/4 passed
     - `LandlordKycIntegrationTest`: 1/1 passed
     - `KycSecurityMatrixTest`: 10/10 passed
     - `ProductionFlywayMigrationTestcontainersTest`: 2/2 passed
   - **Toàn bộ microservices (`./gradlew.bat test --no-build-cache`)**: **BUILD SUCCESSFUL**, 32 actionable tasks (0 failures, 0 skipped across common, identity-service, listing-service, booking-service, billing-service, communication-service, admin-service, api-gateway).

2. **Frontend (`accommodation-web`):**
   - **Vitest Unit & Integration Tests**: 12/12 test suites **PASSED**, 44/44 tests passed.
   - **Đa ngôn ngữ (i18n)**: 689/689 keys khớp 100% giữa `vi.json` và `en.json`.
   - **Đường dẫn nội bộ**: 78/78 internal links hợp lệ, 268 source files quét sạch.
   - **Giới hạn số dòng code**: Tất cả các tệp sửa đổi/tạo mới đều đạt tiêu chuẩn $\le 250$ dòng (`GoogleLoginButton.tsx`: 249 dòng, `RegisterUseCase.java`: 116 dòng, `GoogleAuthUseCase.java`: 245 dòng, `LandlordProfile.java`: 142 dòng).

---

### c. 3 Bước kiểm thử thủ công (Manual Verification Steps)

#### Kịch bản 1: Đăng ký người dùng chọn "Chủ cho thuê"
1. **Thao tác:**
   - Người dùng truy cập `/register`, chọn tab "Chủ cho thuê".
   - Nhập thông tin: `test_landlord_new@toam.vn`, mật khẩu hợp lệ, số điện thoại `0912345678`.
   - Bấm "Đăng ký tài khoản".
2. **Hành vi hệ thống:**
   - Backend `RegisterUseCase` tạo user với duy nhất vai trò `TENANT`.
   - Tạo hồ sơ `landlord_profiles` tương ứng với `approval_status = 'AWAITING_KYC'`.
   - JWT access token chỉ chứa claim `roles: ["TENANT"]`.
   - Frontend hiển thị thông báo thành công và điều hướng sang trang nộp hồ sơ KYC: `/dashboard/landlord/kyc`.
3. **Kiểm tra DB:**
   - `SELECT name FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = '...'` -> chỉ trả về `TENANT`.
   - `SELECT approval_status FROM landlord_profiles WHERE user_id = '...'` -> trả về `AWAITING_KYC`.

#### Kịch bản 2: Truy cập trang quản trị phòng khi chưa được duyệt
1. **Thao tác:**
   - Tài khoản người dùng mới (đang `AWAITING_KYC` hoặc `PENDING_REVIEW`) cố tình gõ URL `/dashboard/landlord/rooms` hoặc gọi API `GET /api/v1/landlord/rooms`.
2. **Hành vi hệ thống:**
   - Gateway/Listing service kiểm tra JWT token: người dùng không có vai trò `LANDLORD`.
   - API trả về HTTP `403 Forbidden` với mã lỗi `ERR_LANDLORD_NOT_APPROVED` và thông điệp: "Tài khoản chưa được phê duyệt quyền Chủ trọ. Vui lòng hoàn tất xác minh KYC."
   - Frontend middleware bắt mã và chuyển hướng người dùng an toàn về `/dashboard/landlord/kyc`.

#### Kịch bản 3: Duyệt KYC và tự động cập nhật quyền Chủ trọ
1. **Thao tác:**
   - Chủ trọ nộp KYC qua `/dashboard/landlord/kyc`. Trạng thái chuyển thành `PENDING_REVIEW`.
   - Admin/Moderator đăng nhập (`admin@cozynest.vn` / `mod@cozynest.vn`) vào `/admin/kyc`, kiểm tra thông tin và bấm "Phê duyệt hồ sơ".
2. **Hành vi hệ thống:**
   - Backend `KycModerationUseCase` cập nhật `user_verifications.status = 'APPROVED'`.
   - Cập nhật `landlord_profiles.approval_status = 'APPROVED'`.
   - Gán thêm vai trò `LANDLORD` vào `user_roles` (user có cả `TENANT` và `LANDLORD`).
   - Ghi outbox event `LandlordApproved`.
3. **Kiểm tra khi làm mới phiên:**
   - Người dùng gọi `POST /api/v1/auth/refresh-token`.
   - Access token mới được cấp phát chứa cả hai vai trò: `roles: ["TENANT", "LANDLORD"]`.
   - Người dùng có toàn quyền truy cập `/dashboard/landlord` và quản lý phòng trọ.

