# ĐẶC TẢ QUY TẮC KIỂM TRA DỮ LIỆU XÁC THỰC (AUTH VALIDATION SPECIFICATION)

**Tài liệu chuẩn hóa dùng chung cho Frontend (Zod) và Backend (Spring Boot Bean Validation / Domain Policies)**  
**Áp dụng cho:** `accommodation-web` và `accommodation-service`  
**Phiên bản:** 1.0.0 (AUTH-UI) - Ngày: 09/11/2026

---

## 1. MỤC TIÊU VÀ NGUYÊN TẮC THỐNG NHẤT

1. **Một nguồn chân lý (Single Source of Truth):** Toàn bộ quy tắc kiểm tra tính hợp lệ của dữ liệu xác thực (Auth) trên giao diện (Frontend) và máy chủ (Backend) phải hoàn toàn tương đồng về mặt nghiệp vụ. Không để xảy ra tình trạng Frontend cho phép gửi nhưng Backend từ chối với lỗi không giải thích được, hoặc ngược lại Frontend chặn oan các trường hợp hợp lệ.
2. **Khả năng tương thích kiểm thử:** Cả hai hệ thống đều phải vượt qua trọn vẹn bộ test vector chuẩn `docs/auth_test_vectors.json`.
3. **Thân thiện với người dùng:**
   - Không đặt ra các quy tắc mật khẩu tùy tiện gây ức chế (như bắt buộc ký tự hoa, ký tự đặc biệt khó nhớ).
   - Tuyệt đối không chặn thao tác dán mật khẩu (Paste) từ trình quản lý mật khẩu (1Password, Bitwarden, Apple Keychain,...).
   - Kiểm tra byte UTF-8 thực tế thay vì độ dài chuỗi ký tự (tránh việc chuỗi ký tự UTF-8 vượt quá 72 bytes bị BCrypt âm thầm cắt ngắn).
   - Đăng nhập chỉ kiểm tra không rỗng, không áp chính sách mật khẩu mới làm khóa tài khoản cũ.

---

## 2. CHI TIẾT BẢNG QUY TẮC KIỂM TRA DỮ LIỆU

### 2.1. Email
- **Mô tả:** Định danh chính của tài khoản người dùng trong hệ thống CozyNest.
- **Quy tắc:**
  - Bắt buộc (`required` / `@NotBlank`).
  - Tự động cắt khoảng trắng thừa ở hai đầu (`trim`).
  - Tự động chuyển thành chữ thường (`toLowerCase()`).
  - Độ dài tối đa: **254 ký tự** (theo tiêu chuẩn RFC 5321).
  - Định dạng: Chuỗi định dạng email hợp lệ `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`.
  - **Gợi ý lỗi gõ cục bộ (Frontend):** Kiểm tra tên miền phổ biến bị gõ sai (chạy trên client, không gọi mạng):
    - `gmial.com`, `gamil.com`, `gmai.com`, `gmail.con` $\rightarrow$ *"Ý bạn là @gmail.com?"*
    - `yaho.com`, `yahooo.com` $\rightarrow$ *"Ý bạn là @yahoo.com?"*
    - `hotmial.com` $\rightarrow$ *"Ý bạn là @hotmail.com?"*
    - `icoud.com` $\rightarrow$ *"Ý bạn là @icloud.com?"*
  - **Bảo mật:** Không kiểm tra "email đã tồn tại" theo thời gian thực (as-you-type) để chống dò quét danh sách người dùng (User Enumeration). Lỗi trùng email chỉ trả về khi submit form (`ERR_AUTH_EMAIL_EXISTS`).

### 2.2. Số điện thoại (Phone)
- **Mô tả:** Kênh liên lạc và hỗ trợ khẩn cấp, có thể dùng để đăng nhập.
- **Quy tắc:**
  - **Không bắt buộc (Optional):** Người dùng có thể để trống khi đăng ký.
  - Nếu có nhập:
    - Bỏ khoảng trắng và dấu gạch nối thừa.
    - Chuẩn hóa định dạng di động Việt Nam: Bắt đầu bằng `+84` hoặc `0`, theo sau là các đầu số nhà mạng di động Việt Nam (Viettel, VinaPhone, MobiFone, Vietnamobile, Itelecom, Wintel):
      - Đầu số: `3[2-9]`, `5[689]`, `7[06-9]`, `8[1-9]`, `9[0-9]`
      - Tiếp theo là đúng 7 chữ số.
    - Biểu thức chính quy (Regex): `^((\+84)|0)(3[2-9]|5[689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$`.
    - Chuẩn hóa lưu trữ trong CSDL: Định dạng E.164 chuẩn quốc tế (`+84xxxxxxxxx`).

### 2.3. Họ và tên (Full Name)
- **Mô tả:** Tên hiển thị của người dùng trên hợp đồng, hóa đơn và hồ sơ cá nhân.
- **Quy tắc:**
  - Bắt buộc (`required` / `@NotBlank`).
  - Gộp nhiều khoảng trắng liên tiếp thành 1 khoảng trắng duy nhất, cắt khoảng trắng đầu/cuối (`trim`).
  - Độ dài: Từ **2 đến 60 ký tự**.
  - Ký tự cho phép: Chỉ bao gồm chữ cái Unicode (hỗ trợ đầy đủ tiếng Việt có dấu, chữ cái Latin), khoảng trắng, và các dấu phân tách tên danh dự/họ ghép: dấu nháy đơn `'`, gạch nối `-`, dấu chấm `.`.
  - Cấm: Tuyệt đối không chứa chữ số (`0-9`) hoặc các ký hiệu đặc biệt, mã độc HTML/script (`< > @ ! $ % ^ & *`).
  - Biểu thức chính quy: `^[\p{L}\s'.-]{2,60}$` (Java) / `^[\p{L}\s'.-]{2,60}$` với cờ `u` (JavaScript).

### 2.4. Mật khẩu (Password) - Áp dụng cho Đăng ký và Đặt lại mật khẩu
- **Mô tả:** Khóa xác thực bí mật của người dùng.
- **Quy tắc bảo mật chuẩn NIST SP 800-63B:**
  1. **Độ dài ký tự tối thiểu:** Tối thiểu **8 ký tự**.
  2. **Giới hạn độ dài UTF-8 tối đa:** Tối đa **72 bytes UTF-8** (đo bằng byte thực tế; tiếng Việt có dấu chiếm 2-3 bytes/ký tự; thông báo lỗi ghi rõ: *"Mật khẩu không được vượt quá 72 bytes UTF-8"*).
  3. **Không thuộc danh sách mật khẩu phổ biến / yếu:** Cấm các mật khẩu như `password`, `password123`, `admin123`, `12345678`, `cozynest2026`,... (tải lười danh sách phổ biến).
  4. **Không chứa thông tin định danh:**
     - Không chứa phần tên người dùng của email (phần trước dấu `@`).
     - Không chứa họ tên của người dùng (`fullName`).
  5. **Không chuỗi lặp / đơn điệu:**
     - Cấm chuỗi ký tự giống hệt nhau liên tiếp: `aaaaaaaa`, `11111111`.
     - Cấm chuỗi số thứ tự tăng/giảm liên tiếp: `12345678`, `87654321`.
  6. **Không quy tắc tùy tiện:** Tuyệt đối KHÔNG ép buộc phải có ký tự hoa hay ký tự đặc biệt nếu mật khẩu đã đủ dài và an toàn.
  7. **Khả năng sử dụng:** KHÔNG chặn thao tác sao chép / dán (Paste).

### 2.5. Đăng nhập (Login)
- **Quy tắc:**
  - Danh tính (`identifier`): Email hoặc Số điện thoại, không được để trống.
  - Mật khẩu (`password`): Không được để trống.
  - **Đặc biệt:** KHÔNG áp dụng chính sách mật khẩu mới (như kiểm tra phổ biến, độ dài 8 ký tự, 72 bytes) khi kiểm tra tính hợp lệ của mật khẩu lúc đăng nhập. Điều này đảm bảo người dùng cũ có mật khẩu tạo từ trước vẫn có thể đăng nhập bình thường và được kích hoạt băm lại (`upgradeEncoding`).

---

## 3. MÃ LỖI VÀ KHÓA DỊCH QUỐC TẾ HÓA (I18N MAPPING)

| Mã lỗi hệ thống | Khóa dịch (`messages.json`) | Nội dung hiển thị tiếng Việt |
|---|---|---|
| `VALIDATION_EMAIL_REQUIRED` | `auth.errors.emailRequired` | Vui lòng nhập địa chỉ email |
| `VALIDATION_EMAIL_INVALID` | `auth.errors.emailInvalid` | Địa chỉ email không đúng định dạng |
| `VALIDATION_EMAIL_MAX` | `auth.errors.emailMax` | Email không được vượt quá 254 ký tự |
| `VALIDATION_PHONE_INVALID` | `auth.errors.phoneInvalid` | Số điện thoại di động Việt Nam không hợp lệ |
| `VALIDATION_NAME_REQUIRED` | `auth.errors.nameRequired` | Vui lòng nhập họ và tên |
| `VALIDATION_NAME_LENGTH` | `auth.errors.nameLength` | Họ và tên phải từ 2 đến 60 ký tự |
| `VALIDATION_NAME_CHARS` | `auth.errors.nameChars` | Họ tên chỉ được chứa chữ cái, khoảng trắng và dấu gạch nối |
| `VALIDATION_PASS_REQUIRED` | `auth.errors.passwordRequired` | Vui lòng nhập mật khẩu |
| `VALIDATION_PASS_MIN` | `auth.errors.passwordMin` | Mật khẩu phải có ít nhất 8 ký tự |
| `VALIDATION_PASS_MAX_BYTES` | `auth.errors.passwordMaxBytes` | Mật khẩu không được vượt quá 72 bytes |
| `VALIDATION_PASS_COMMON` | `auth.errors.passwordCommon` | Mật khẩu này quá phổ biến, vui lòng chọn mật khẩu khác an toàn hơn |
| `VALIDATION_PASS_NAME_EMAIL` | `auth.errors.passwordNameEmail` | Mật khẩu không được chứa họ tên hoặc email của bạn |
| `VALIDATION_PASS_REPETITIVE` | `auth.errors.passwordRepetitive` | Mật khẩu không được là chuỗi lặp lại hoặc thứ tự đơn điệu |
| `VALIDATION_PASS_MISMATCH` | `auth.errors.passwordMismatch` | Mật khẩu xác nhận không khớp |
| `ERR_AUTH_BAD_CREDENTIALS` | `auth.errors.badCredentials` | Email, số điện thoại hoặc mật khẩu không chính xác. |
| `ERR_AUTH_LOCKED` | `auth.errors.accountLocked` | Tài khoản bị tạm khóa do nhập sai nhiều lần. Thử lại sau {seconds} giây |
| `ERR_AUTH_TOO_MANY_REQUESTS` | `auth.errors.tooManyRequests` | Quá nhiều yêu cầu. Vui lòng chờ {seconds} giây trước khi thử lại |
| `ERR_AUTH_EMAIL_EXISTS` | `auth.errors.emailExists` | Địa chỉ email này đã được đăng ký tài khoản |
| `ERR_AUTH_OTP_INVALID` | `auth.errors.otpInvalid` | Mã xác thực không chính xác. Vui lòng kiểm tra lại |
| `ERR_AUTH_OTP_EXPIRED` | `auth.errors.otpExpired` | Mã xác thực đã hết hạn. Vui lòng nhấn gửi lại mã |
| `ERR_AUTH_SERVICE_UNAVAILABLE` | `auth.errors.serviceUnavailable` | Dịch vụ gửi email hiện không khả dụng. Vui lòng liên hệ bộ phận hỗ trợ |

---

## 4. BỘ TEST VECTOR THỰC TẾ
Chi tiết bộ test vector được lưu trữ tại `docs/auth_test_vectors.json`. Cả unit test Frontend (`src/presentation/validation/__tests__/auth.validation.test.ts`) và Backend test (`PasswordPolicyValidatorTest.java`, `RegisterCommandValidationTest.java`) đều đọc trực tiếp tệp này để xác thực tính đồng nhất 100%.

