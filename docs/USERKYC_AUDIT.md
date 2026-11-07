# USER-01 & KYC-01 Audit Report

**Date**: 2026-11-07  
**Scope**: User Profile Management, Avatar Upload/Crop, Password Change, Landlord Identity Verification (KYC), KYC Moderation, and Listing KYC Gate.  
**Repositories**: `accommodation-service` (identity-service, listing-service, common, api-gateway) and `accommodation-web`.

---

## 1. Hiện trạng Database & Schema

| Bảng | Trạng thái hiện tại | So với yêu cầu USER-01 & KYC-01 | Hướng xử lý |
| :--- | :--- | :--- | :--- |
| `user_profiles` | Đã có trong V1 (`id`, `user_id`, `permanent_address`, `occupation`, `bio`, `language`, `timezone`) | Thiếu các trường: `full_name`, `phone` (chuẩn E.164, UNIQUE khi khác null), `gender`, `date_of_birth`, `address_detail`, `province_code`, `ward_id` (địa giới 2 cấp), `bio`, `locale`, `avatar_key`, `avatar_thumb_key`, `avatar_medium_key`, `version` (optimistic lock), `updated_at`. | Viết migration `V4__user_profile_and_kyc_schema.sql` bổ sung cột đầy đủ vào `user_profiles`. |
| `user_verifications` | Đã có trong V1 và V3 (`id`, `user_id`, `document_type`, `document_number`, `front_image_url`, `back_image_url`, `selfie_url`, `status`, `rejection_reason`, `version`, `deleted_at`) | Thiếu trường bảo mật cao: `full_name_on_id`, `date_of_birth`, `id_number_enc` (AES-GCM), `id_number_hash` (HMAC-SHA256 có pepper, UNIQUE trong số PENDING/APPROVED), `id_number_last4`, `issue_date`, `issue_place`, `front_image_key`, `back_image_key` (kho riêng tư), `consent_at`, `consent_policy_version`, `submitted_at`, `reviewed_at`, `reviewed_by`, `reject_reason_code`, `reject_reason_note`, `attempt_count`. | Bổ sung/cập nhật cấu trúc trong migration `V4`. |
| `kyc_access_logs` | Chưa có | Yêu cầu bảng nhật ký truy cập ảnh/chi tiết KYC (append-only) gồm: `verification_id`, `actor_id`, `actor_role`, `action`, `ip`, `created_at`. | Tạo mới trong migration `V4`. |
| `kyc_status_history` | Chưa có | Yêu cầu bảng theo dõi lịch sử chuyển trạng thái KYC: `verification_id`, `from_status`, `to_status`, `actor_id`, `reason`, `created_at`. | Tạo mới trong migration `V4`. |
| `kyc_events_outbox` | Chưa có | Yêu cầu bảng outbox lưu sự kiện `KycSubmitted`, `KycApproved`, `KycRejected` để phát tán tin cậy. | Tạo mới trong migration `V4`. |
| `refresh_tokens` | Đã có trong V1 | Có `token_hash`, `revoked_at`. Chưa có hàm thu hồi chọn lọc: thu hồi mọi phiên trừ phiên hiện tại. | Bổ sung query trong `RefreshTokenRepository`. |

---

## 2. Hiện trạng Endpoints & Backend Services

| Endpoint / Chức năng | Hiện có | Đánh giá & Phần còn thiếu |
| :--- | :--- | :--- |
| `GET /api/v1/auth/me` & `GET /api/v1/auth/profile` | Đã có trong `AuthController` | Đang trả `UserDto`. Cần thêm route chuẩn `/api/v1/users/profile` trả thông tin profile mở rộng, avatar key/url và trạng thái KYC hiện tại của chủ trọ. |
| `PUT /api/v1/users/profile` | Chưa có | Chưa có endpoint updateProfile thật trên backend. Cần hỗ trợ: họ tên, SĐT VN chuẩn hoá, ngày sinh, giới tính, địa chỉ (2 cấp), bio, locale, version (409 conflict). Giới hạn tần suất và chặn đổi email/role. |
| `POST /api/v1/users/profile/avatar` | Chưa có | Cần endpoint upload multipart < 5MB, kiểm tra magic bytes, xoá EXIF, cắt vuông giữa, sinh 2 kích cỡ WebP (64px, 256px), xoá file cũ, lưu kho công khai `/api/v1/media`. |
| `DELETE /api/v1/users/profile/avatar` | Chưa có | Cần endpoint xoá ảnh đại diện và file lưu trữ, đưa về avatar mặc định. |
| `POST /api/v1/auth/change-password` | Đã có cơ bản | Thiếu: cấm trùng mật khẩu cũ, độ mạnh mật khẩu, khoá khi nhập sai mật khẩu cũ quá số lần (Redis), giữ phiên hiện tại và thu hồi các phiên khác, gửi email thông báo. |
| `GET /api/v1/landlord/kyc` | Chưa có | Cần trả trạng thái KYC, thông tin đã nộp (CCCD che), lý do từ chối nếu có, số lần nộp còn lại. Tuyệt đối không trả URL ảnh công khai. |
| `POST /api/v1/landlord/kyc` | Chưa có | Nộp CCCD (front/back), 12 số CCCD, mã hoá AES-GCM, băm HMAC-SHA256, kiểm tra trùng, lưu ảnh vào KHO RIÊNG TƯ, outbox và history. |
| `GET /api/v1/landlord/kyc/images/{type}` | Chưa có | Stream ảnh có xác thực (chính chủ), header `Cache-Control: no-store, private`, chống IDOR. |
| `GET & POST /api/v1/moderation/kyc/**` | Chưa có | Kiểm duyệt cho ADMIN/MODERATOR: danh sách, chi tiết, stream ảnh kèm ghi nhật ký `kyc_access_logs`, Approve/Reject với lý do bắt buộc và khoá lạc quan. |
| `GET /internal/users/{id}/kyc-status` | Chưa có | Endpoint nội bộ identity phục vụ listing-service kiểm tra KYC, bảo vệ bằng Service Token. |
| Cổng chặn đăng tin `listing-service` | Chưa có kiểm tra KYC | Endpoint `POST /landlord/rooms/{id}/submit` cần gọi sang `identity-service` kiểm tra KYC. Nếu khác APPROVED thì trả 403 `ERR_KYC_REQUIRED`. |
| `api-gateway` routes | Đã định tuyến một phần | Cần cấu hình ưu tiên route `/api/v1/landlord/kyc/**` và `/api/v1/moderation/kyc/**` về `identity-service` trước route chung `/api/v1/landlord/**`. |

---

## 3. Hiện trạng Xử lý Ảnh (Image Processing)

- **Listing Service**: Đang chứa logic kiểm tra magic bytes, WebP header bytes 8-11, giới hạn pixel $\le 8000$, tẩy EXIF bằng cách vẽ lại trên `BufferedImage` mới, resize sang WebP bằng Thumbnailator + `imageio-webp`.
- **Vấn đề**: Logic xử lý ảnh đang bị giới hạn trong `listing-service`, nếu `identity-service` viết lại sẽ vi phạm nguyên tắc DRY.
- **Giải pháp**:
  - Đưa dependency `imageio-webp` và `thumbnailator` vào module `common`.
  - Tách bộ xử lý ảnh `ImageProcessingHelper` / `ImageSanitizer` vào `vn.toam.common.image` với các hàm kiểm tra magic bytes, đọc kích thước an toàn, xoá EXIF, cắt vuông và nén WebP.
  - Cả `listing-service` và `identity-service` cùng sử dụng chung module này, đảm bảo kiểm thử hiện có của `listing-service` tiếp tục xanh 100%.

---

## 4. Hiện trạng Frontend (accommodation-web)

| Màn hình / Component | Hiện trạng | Yêu cầu cải tạo |
| :--- | :--- | :--- |
| `UserProfileView.tsx` | Đang dùng state cục bộ, gọi `updateProfile` lưu vào `localStorage` giả lập | Tích hợp React Hook Form + Zod, TanStack Query, HttpAuthRepository kết nối API thật. Hỗ trợ địa giới 2 cấp (Tỉnh/Thành -> Phường/Xã), xử lý 409 Conflict. |
| Avatar Management | Chưa có tính năng upload/crop/delete | Tích hợp chọn ảnh/kéo thả, validate client (< 5MB, format), xem trước cắt khung tròn, tiến trình upload, xoá ảnh có dialog xác nhận, cập nhật `authStore` ngay lập tức. |
| Đổi mật khẩu trong Profile | Chưa có form trong Profile | Thêm component `UserChangePasswordView` với mật khẩu cũ, mật khẩu mới, xác nhận, thanh độ mạnh, cảnh báo đăng xuất thiết bị khác. |
| `/dashboard/landlord/kyc` | Giao diện mockup cứng, `setTimeout` và `isVerified=true` | Viết lại hoàn chỉnh với 4 trạng thái (`NONE`, `PENDING`, `APPROVED`, `REJECTED`), upload 2 mặt CCCD, form 12 số CCCD, checkbox cam kết, xem lại hồ sơ có CCCD che và ảnh stream bảo mật. |
| Cổng chặn đăng tin trên UI | Chưa có | Thêm cảnh báo banner trên Landlord Dashboard và Rooms list khi KYC chưa APPROVED. Nút "Gửi duyệt" bị vô hiệu kèm tooltip giải thích và liên kết KYC. |
| Quản lý duyệt KYC Admin/Moderator | Dùng `StaticAdminRepository` | Nối API kiểm duyệt thật `/api/v1/moderation/kyc/**`, hiển thị ảnh đối chiếu hai mặt có zoom, cảnh báo ghi nhật ký xem, modal phê duyệt / từ chối với lý do chuẩn. |

