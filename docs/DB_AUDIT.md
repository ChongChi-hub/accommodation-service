# Báo Cáo Khảo Sát & Kiểm Toán Cơ Sở Dữ Liệu (Database Audit)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Thời điểm thực hiện:** 2026-11-07  
**Phiên bản:** 2.0 (ToAm Rebuild - Multi-Service DDD Architecture)

---

## 1. Hiện trạng triển khai theo từng Microservice

| Service | Tồn tại | Cổng | DB Schema | Số bảng hiện tại | Đánh giá hiện trạng |
| :--- | :---: | :---: | :--- | :---: | :--- |
| **`identity-service`** | Có | 8081 | `toam_identity` | 7 | Đã có Flyway V1, V2. Thiếu bảng phục vụ OTP/Forgot Password (`password_reset_tokens`), token email, tài khoản OAuth, và thiếu cột soft delete/version. |
| **`listing-service`** | Có | 8082 | `toam_listing` | 10 | Đã có Flyway V1, V2. Thiếu biểu phí dịch vụ chi tiết (`room_prices`), nội quy chi tiết (`room_rules`), ảnh đánh giá (`review_images`), tìm kiếm đã lưu (`saved_searches`), và nhật ký lượt xem (`room_view_logs`). |
| **`booking-service`** | **Chưa** | 8083 | `toam_booking` | 0 | Chưa có module codebase. Cần tạo mới khung DDD, Flyway V1 schema (hẹn xem, yêu cầu thuê, hợp đồng, chữ ký, bảo trì), V2 seed data. |
| **`billing-service`** | **Chưa** | 8084 | `toam_billing` | 0 | Chưa có module codebase. Cần tạo mới khung DDD, Flyway V1 schema (chỉ số điện nước, hóa đơn, chi tiết hóa đơn, thanh toán, cọc, chi phí, gói đăng tin, mã giảm giá), V2 seed data. |
| **`communication-service`** | **Chưa** | 8085 | `toam_communication` | 0 | Chưa có module codebase. Cần tạo mới khung DDD, Flyway V1 schema (thông báo, hội thoại, tin nhắn, ticket hỗ trợ), V2 seed data. |
| **`admin-service`** | **Chưa** | 8086 | `toam_admin` | 0 | Chưa có module codebase. Cần tạo mới khung DDD, Flyway V1 schema (báo cáo vi phạm, kiểm duyệt, audit log, cài đặt hệ thống, thống kê hàng ngày, tranh chấp, bài viết nội dung/FAQ/banner, mẫu email), V2 seed data. |

---

## 2. Chi tiết Bảng & Ràng buộc theo từng Service

### 2.1. `identity-service` (`toam_identity`)
* **Bảng hiện có (V1):** `roles`, `users`, `user_roles`, `user_profiles`, `user_verifications`, `refresh_tokens`, `login_attempts`.
* **Bảng còn thiếu:**
  1. `password_reset_tokens`: Bắt buộc cho task **[AUTH-04]** (lưu `otp_hash`, `expires_at`, `attempts`, `used_at`, `request_ip`).
  2. `email_verification_tokens`: Phục vụ kích hoạt tài khoản qua email.
  3. `oauth_accounts`: Hỗ trợ Google/Facebook OAuth sau này.
* **Cột & Ràng buộc thiếu:**
  - `users`: Cần bổ sung `deleted_at DATETIME(3) NULL`, `version BIGINT NOT NULL DEFAULT 0`.
  - `user_verifications`: Cần bổ sung `version BIGINT NOT NULL DEFAULT 0`, `deleted_at`.
* **Seed data hiện có (V2):** Chỉ có 4 tài khoản demo đơn giản (1 Tenant, 1 Landlord, 1 Admin, 1 Moderator).
* **Seed data còn thiếu (yêu cầu A3):**
  - Cần nâng quy mô lên tối thiểu: 8 Tenant, 6 Landlord (gồm các tài khoản có cả 2 vai trò), 3 Moderator, 2 Admin.
  - Đa dạng trạng thái: `ACTIVE`, `LOCKED`, `PENDING`.
  - KYC đa dạng: `PENDING`, `APPROVED`, `REJECTED`.

---

### 2.2. `listing-service` (`toam_listing`)
* **Bảng hiện có (V1):** `provinces`, `districts`, `wards`, `room_types`, `amenities`, `rooms`, `room_images`, `room_amenities`, `reviews`, `bookmarks`.
* **Bảng còn thiếu:**
  1. `room_prices`: Biểu phí đơn giá điện (kWh), nước (m3), internet, vệ sinh... tương ứng với từng phòng.
  2. `room_rules`: Danh mục nội quy phòng (cho nuôi pet, nấu ăn, giờ giới nghiêm, bạn bè ở lại...).
  3. `review_images`: Ảnh đính kèm khi người thuê gửi đánh giá.
  4. `saved_searches`: Bộ lọc tìm kiếm được người dùng lưu lại.
  5. `room_view_logs`: Nhật ký theo dõi lượt xem theo IP/User để phục vụ thống kê thị hiếu và trending.
* **Cột & Ràng buộc thiếu:**
  - `rooms`: Cần `slug VARCHAR(300) UNIQUE`, `deleted_at`, `version`, chỉ số đánh giá `rating_avg`, `review_count`.
  - Ràng buộc CHECK: `price > 0`, `area > 0`, `rating BETWEEN 1 AND 5`.
* **Seed data hiện có (V2):** 3 tỉnh/thành (Hà Nội, TP.HCM, Đà Nẵng) với 8 phòng trọ.
* **Seed data còn thiếu (yêu cầu A3):**
  - Địa giới: Toàn bộ 63 tỉnh/thành Việt Nam; đầy đủ quận/huyện và phường/xã cho 5 thành phố trực thuộc trung ương: Hà Nội, TP.HCM, Đà Nẵng, Cần Thơ, Hải Phòng.
  - Phòng trọ: Nâng lên 60 - 80 phòng đa dạng khu vực, loại phòng, khoảng giá và đủ các trạng thái (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `RENTED`, `HIDDEN`, `REJECTED`).

---

### 2.3. `booking-service` (`toam_booking`) - Bổ sung mới
* **Danh sách bảng cần thiết:**
  1. `viewing_appointments`: Lịch hẹn xem phòng giữa Tenant và Landlord.
  2. `rental_requests`: Yêu cầu thuê phòng chính thức gửi từ Tenant.
  3. `contracts`: Hợp đồng thuê phòng điện tử (mã hợp đồng, ngày bắt đầu, ngày kết thúc, giá thuê, tiền cọc, điều khoản, trạng thái, version khoá lạc quan).
  4. `contract_signatures`: Nhật ký ký số / OTP xác thực hợp đồng từ 2 bên.
  5. `contract_terminations`: Yêu cầu chấm dứt/thanh lý hợp đồng và hoàn trả cọc.
  6. `room_snapshots`: Bản lưu trữ cố định thông tin phòng tại thời điểm ký kết hợp đồng.
  7. `contract_templates`: Mẫu hợp đồng chuẩn được ban hành bởi hệ thống.
  8. `maintenance_requests`: Yêu cầu bảo trì, sửa chữa thiết bị từ người thuê.
  9. `maintenance_comments`: Trao đổi, ghi chú và hình ảnh hiện trường bảo trì.

---

### 2.4. `billing-service` (`toam_billing`) - Bổ sung mới
* **Danh sách bảng cần thiết:**
  1. `utility_readings`: Chỉ số điện/nước đầu kỳ - cuối kỳ hàng tháng kèm ảnh đồng hồ.
  2. `invoices`: Hóa đơn định kỳ hàng tháng (mã hóa đơn, kỳ hóa đơn, tiền phòng, tiền điện, tiền nước, dịch vụ, tổng tiền, hạn nộp, trạng thái, version).
  3. `invoice_items`: Chi tiết các khoản cấu thành nên hóa đơn.
  4. `payments`: Giao dịch thanh toán hóa đơn (VietQR, chuyển khoản, thẻ...).
  5. `deposits`: Khoản giữ tiền cọc của hợp đồng (giữ cọc, khấu trừ, hoàn trả, version).
  6. `deposit_transactions`: Lịch sử giao dịch biến động tiền cọc.
  7. `expenses`: Khoản chi phí phát sinh của chủ trọ trong quản lý vận hành.
  8. `listing_plans`: Bảng giá gói đăng tin VIP / dịch vụ quảng bá.
  9. `promo_codes`: Mã khuyến mãi giảm giá dịch vụ.
  10. `payment_gateway_logs`: Nhật ký webhook/giao tiếp cổng thanh toán.

---

### 2.5. `communication-service` (`toam_communication`) - Bổ sung mới
* **Danh sách bảng cần thiết:**
  1. `notifications`: Thông báo in-app gửi tới người dùng (hợp đồng, hóa đơn, lịch hẹn, hệ thống...).
  2. `notification_preferences`: Cài đặt nhận thông báo của người dùng (email, push, sms).
  3. `conversations`: Cuộc hội thoại trực tiếp giữa Người thuê và Chủ trọ gắn với phòng.
  4. `conversation_participants`: Thành viên trong cuộc hội thoại.
  5. `messages`: Tin nhắn trong cuộc hội thoại.
  6. `message_attachments`: Tệp đính kèm và hình ảnh gửi qua tin nhắn.
  7. `support_tickets`: Phiếu yêu cầu hỗ trợ khách hàng.
  8. `support_ticket_replies`: Phản hồi và trao đổi trong phiếu hỗ trợ.

---

### 2.6. `admin-service` (`toam_admin`) - Bổ sung mới
* **Danh sách bảng cần thiết:**
  1. `reports`: Báo cáo vi phạm từ người dùng (báo cáo phòng giả mạo, người dùng lừa đảo, review xấu).
  2. `moderation_actions`: Nhật ký hành động xử lý của Kiểm duyệt viên / Quản trị viên.
  3. `audit_logs`: Nhật ký kiểm toán bảo mật và thay đổi dữ liệu nhạy cảm.
  4. `system_settings`: Cấu hình toàn hệ thống (phí nền tảng, thời gian timeout...).
  5. `daily_statistics`: Bảng tổng hợp số liệu thống kê vận hành theo ngày trong 90 ngày.
  6. `disputes`: Hồ sơ khiếu nại và tranh chấp giữa các bên (hoàn cọc, bồi thường hư hại...).
  7. `dispute_events`: Nhật ký sự kiện và phán quyết trọng tài của vụ tranh chấp.
  8. `content_posts`: Bài viết tin tức, bài cẩm nang, FAQ, banner chiến dịch.
  9. `email_templates`: Mẫu email hệ thống đa ngôn ngữ (chào mừng, OTP, thông báo hóa đơn...).

---

## 3. Đối chiếu giao diện Frontend với Schema

| Thực thể Frontend (`Static...Repository`) | Route / Trang UI sử dụng | Bảng tương ứng trong DB | Tình trạng hỗ trợ trong DB |
| :--- | :--- | :--- | :--- |
| **`MaintenanceRequest`** (`StaticMaintenanceRepository`) | `/dashboard/tenant/maintenance`, `/dashboard/landlord/maintenance` | `toam_booking.maintenance_requests`, `maintenance_comments` | Cần bổ sung trong `booking-service` V1. |
| **`SupportTicket`** (`StaticSupportRepository`) | `/support`, `/support/new`, `/support/[id]`, `/dashboard/admin/support` | `toam_communication.support_tickets`, `support_ticket_replies` | Cần bổ sung trong `communication-service` V1. |
| **`Dispute`** (`StaticDisputeRepository`) | `/dashboard/admin/disputes`, `/dashboard/admin/disputes/[id]` | `toam_admin.disputes`, `dispute_events` | Cần bổ sung trong `admin-service` V1. |
| **`Transaction`** (`StaticTransactionRepository`) | `/dashboard/admin/transactions` | `toam_billing.payments`, `deposits`, `deposit_transactions` | Cần bổ sung trong `billing-service` V1. |
| **`Notification`** (`StaticNotificationRepository`) | Header Bell dropdown, `/notifications` | `toam_communication.notifications` | Cần bổ sung trong `communication-service` V1. |
| **`AdminStats` & Pending Items** (`StaticAdminRepository`) | `/dashboard/admin`, `/dashboard/moderator` | `toam_admin.daily_statistics`, `toam_listing.rooms`, `toam_identity.user_verifications` | Cần liên kết số liệu nhất quán. |

---

## 4. Kế hoạch Hành động Kế tiếp (Action Plan)
1. **Thiết kế Định danh Seed UUID Nhất Quán (`docs/seed-ids.md`):** Khai báo toàn bộ hằng số ID cố định để bảo đảm quan hệ liên service nhất quán 100%.
2. **Nâng cấp `identity-service`:** Thêm migration `V3__auth_tokens_schema.sql` (bảng `password_reset_tokens`, `email_verification_tokens`, `oauth_accounts`, bổ sung `version` và `deleted_at`), cập nhật seed mở rộng.
3. **Nâng cấp `listing-service`:** Thêm migration `V3__listing_schema_enhancements.sql` (bảng `room_prices`, `room_rules`, `review_images`, `saved_searches`, `room_view_logs`), bổ sung dữ liệu địa giới 63 tỉnh thành và 75 phòng trọ mẫu.
4. **Khởi tạo 4 Microservice mới (`booking-service`, `billing-service`, `communication-service`, `admin-service`):**
   - Thiết lập cấu trúc DDD (domain, application, infrastructure, interfaces).
   - Viết migration `V1__init_..._schema.sql` và `V2__seed_demo_...sql`.
   - Cấu hình health endpoint, ArchUnit tests, và Testcontainers kiểm tra tính nhất quán logic nghiệp vụ.
   - Cập nhật `settings.gradle`, `api-gateway/src/main/resources/application.yml`, và `docker-compose.yml`.
