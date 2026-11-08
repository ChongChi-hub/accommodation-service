# Bảng Kiểm Kê Kiểu ID Trên Tất Cả Các Service (ID Types Audit)

> **Mục tiêu**: Lập danh mục chi tiết kiểu định danh (Primary Key & Foreign Key) trên toàn bộ cơ sở dữ liệu của hệ thống Accommodation Services, đối chiếu sự đồng nhất giữa các microservices và kiểm tra các bảng mới tạo từ migration V4. Không sửa đổi mã nguồn.

---

## 1. Danh sách chi tiết kiểu ID theo từng Service

### 1.1. `identity-service`
Tất cả các bảng trong `identity-service` sử dụng `VARCHAR(36)` (chuỗi UUID hoặc chuỗi định danh tiền tố chuẩn như `u-landlord-01`).

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Cột Khóa ngoại / Tham chiếu | Kiểu Khóa ngoại | Định dạng thực tế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `roles` | `id` | `VARCHAR(36)` | - | - | UUID / mã role (e.g. `ROLE_LANDLORD`) |
| `users` | `id` | `VARCHAR(36)` | - | - | UUID v4 / chuỗi (e.g. `u-landlord-01`, `u-tenant-01`) |
| `user_roles` | (`user_id`, `role_id`) | Composite | `user_id`, `role_id` | `VARCHAR(36)` | UUID |
| `user_profiles` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` UNIQUE | UUID |
| `user_verifications` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` UNIQUE | UUID |
| `refresh_tokens` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` | UUID |
| `login_attempts` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` | UUID / IP string |
| `password_reset_tokens` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` | UUID |
| `email_verification_tokens` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` | UUID |
| `oauth_accounts` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` | UUID |
| `kyc_access_logs` *(V4)* | `id` | `VARCHAR(36)` | `verification_id`, `accessor_id` | `VARCHAR(36)` | UUID |
| `kyc_status_history` *(V4)* | `id` | `VARCHAR(36)` | `verification_id`, `changed_by` | `VARCHAR(36)` | UUID |
| `kyc_events_outbox` *(V4)* | `id` | `VARCHAR(36)` | `aggregate_id` | `VARCHAR(36)` | UUID |

---

### 1.2. `listing-service`
Hầu hết các bảng trong `listing-service` sử dụng `BIGINT AUTO_INCREMENT` làm khóa chính nội bộ.

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Cột Khóa ngoại / Tham chiếu | Kiểu Khóa ngoại | Định dạng thực tế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `provinces` | `id` | `BIGINT AUTO_INCREMENT` | - | - | Mã số nguyên tự tăng |
| `districts` | `id` | `BIGINT AUTO_INCREMENT` | `province_id` | `BIGINT` | Mã số nguyên tự tăng |
| `wards` | `id` | `BIGINT AUTO_INCREMENT` | `district_id` | `BIGINT` | Mã số nguyên tự tăng |
| `room_types` | `id` | `BIGINT AUTO_INCREMENT` | - | - | Mã số nguyên tự tăng |
| `amenities` | `id` | `BIGINT AUTO_INCREMENT` | - | - | Mã số nguyên tự tăng |
| `rooms` | `id` | `BIGINT AUTO_INCREMENT` | `landlord_id` *(lệch)* | `BIGINT` | ID số nguyên (101, 102...). `landlord_id` số nguyên 1, 2 |
| `room_images` | `id` | `BIGINT AUTO_INCREMENT` | `room_id` | `BIGINT` | Mã số nguyên tự tăng |
| `room_amenities` | (`room_id`, `amenity_id`) | Composite | `room_id`, `amenity_id` | `BIGINT` | Mã số nguyên |
| `reviews` | `id` | `BIGINT AUTO_INCREMENT` | `room_id`, `user_id` | `BIGINT` (`user_id` `VARCHAR(36)` ở V3) | Khóa số nguyên / UUID user |
| `bookmarks` | `id` | `BIGINT AUTO_INCREMENT` | `room_id`, `user_id` | `BIGINT` / `VARCHAR(36)` | - |
| `room_prices` | `id` | `BIGINT AUTO_INCREMENT` | `room_id` | `BIGINT` | Mã số nguyên |
| `room_rules` | `id` | `BIGINT AUTO_INCREMENT` | `room_id` | `BIGINT` | Mã số nguyên |
| `review_images` | `id` | `BIGINT AUTO_INCREMENT` | `review_id` | `BIGINT` | Mã số nguyên |
| `room_attributes` | `id` | `BIGINT AUTO_INCREMENT` | `room_id` | `BIGINT` | Mã số nguyên |

---

### 1.3. `booking-service`
`booking-service` có cấu trúc kết hợp: các bảng liên quan đến lịch hẹn (`viewing_appointments`) dùng `BIGINT`, trong khi các bảng hợp đồng dùng `VARCHAR(36)`.

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Cột Khóa ngoại / Tham chiếu | Kiểu Khóa ngoại | Định dạng thực tế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `rental_requests` | `id` | `VARCHAR(36)` | `tenant_id`, `room_id` | `VARCHAR(36)`, `BIGINT` | UUID |
| `rental_contracts` | `id` | `VARCHAR(36)` | `tenant_id`, `landlord_id`, `room_id` | `VARCHAR(36)`, `BIGINT`, `BIGINT` | UUID |
| `viewing_appointments` | `id` | `BIGINT AUTO_INCREMENT` | `room_id`, `landlord_id`, `tenant_id` | `BIGINT`, `BIGINT`, `VARCHAR(36)` | ID lịch hẹn số nguyên, tenant UUID |
| `appointment_status_history` | `id` | `BIGINT AUTO_INCREMENT` | `appointment_id`, `changed_by` | `BIGINT`, `VARCHAR(36)` | Số nguyên tự tăng |
| `appointment_events_outbox` | `id` | `BIGINT AUTO_INCREMENT` | `aggregate_id` | `VARCHAR(36)` | Số nguyên tự tăng, outbox aggregate UUID |

---

### 1.4. `billing-service`
Toàn bộ sử dụng `VARCHAR(36)` (chuỗi UUID).

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Tham chiếu User / Đối tượng | Kiểu Khóa ngoại |
| :--- | :--- | :--- | :--- | :--- |
| `invoices` | `id` | `VARCHAR(36)` | `contract_id`, `tenant_id`, `landlord_id` | `VARCHAR(36)` |
| `invoice_items` | `id` | `VARCHAR(36)` | `invoice_id` | `VARCHAR(36)` |
| `payments` | `id` | `VARCHAR(36)` | `invoice_id`, `payer_id` | `VARCHAR(36)` |
| `deposits` | `id` | `VARCHAR(36)` | `contract_id`, `tenant_id` | `VARCHAR(36)` |
| `deposit_transactions` | `id` | `VARCHAR(36)` | `deposit_id` | `VARCHAR(36)` |
| `expenses` | `id` | `VARCHAR(36)` | `landlord_id`, `room_id` | `VARCHAR(36)` |
| `listing_plans` | `id` | `VARCHAR(36)` | `landlord_id` | `VARCHAR(36)` |
| `promo_codes` | `id` | `VARCHAR(36)` | - | `VARCHAR(36)` |
| `payment_gateway_logs` | `id` | `VARCHAR(36)` | `payment_id` | `VARCHAR(36)` |
| `utility_readings` | `id` | `VARCHAR(36)` | `room_id`, `contract_id` | `VARCHAR(36)` |

---

### 1.5. `communication-service`
Toàn bộ sử dụng `VARCHAR(36)` (chuỗi UUID).

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Tham chiếu User / Đối tượng | Kiểu Khóa ngoại |
| :--- | :--- | :--- | :--- | :--- |
| `notifications` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` |
| `notification_preferences` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` |
| `conversations` | `id` | `VARCHAR(36)` | `room_id` (String) | `VARCHAR(36)` |
| `conversation_participants` | `id` | `VARCHAR(36)` | `conversation_id`, `user_id` | `VARCHAR(36)` |
| `messages` | `id` | `VARCHAR(36)` | `conversation_id`, `sender_id` | `VARCHAR(36)` |
| `message_attachments` | `id` | `VARCHAR(36)` | `message_id` | `VARCHAR(36)` |
| `support_tickets` | `id` | `VARCHAR(36)` | `user_id`, `assigned_to` | `VARCHAR(36)` |
| `support_ticket_replies` | `id` | `VARCHAR(36)` | `ticket_id`, `user_id` | `VARCHAR(36)` |

---

### 1.6. `admin-service`
Toàn bộ sử dụng `VARCHAR(36)` (chuỗi UUID).

| Bảng | Cột Khóa chính (PK) | Kiểu PK | Tham chiếu User / Admin | Kiểu Khóa ngoại |
| :--- | :--- | :--- | :--- | :--- |
| `reports` | `id` | `VARCHAR(36)` | `reporter_id`, `target_id` | `VARCHAR(36)` |
| `moderation_actions` | `id` | `VARCHAR(36)` | `moderator_id`, `target_id` | `VARCHAR(36)` |
| `audit_logs` | `id` | `VARCHAR(36)` | `user_id` | `VARCHAR(36)` |
| `system_settings` | `id` | `VARCHAR(36)` | `updated_by` | `VARCHAR(36)` |
| `daily_statistics` | `id` | `VARCHAR(36)` | - | `VARCHAR(36)` |
| `disputes` | `id` | `VARCHAR(36)` | `complainant_id`, `respondent_id` | `VARCHAR(36)` |
| `dispute_events` | `id` | `VARCHAR(36)` | `dispute_id`, `actor_id` | `VARCHAR(36)` |
| `content_posts` | `id` | `VARCHAR(36)` | `author_id` | `VARCHAR(36)` |
| `email_templates` | `id` | `VARCHAR(36)` | - | `VARCHAR(36)` |

---

## 2. Các bảng mới của Migration V4 (`identity-service`)

Migration `V4__user_profile_and_kyc_schema.sql` thêm các bảng và cột sau:
1. `kyc_access_logs`:
   - `id`: `VARCHAR(36)` PRIMARY KEY (UUID).
   - `verification_id`: `VARCHAR(36)` NOT NULL.
   - `accessor_id`: `VARCHAR(36)` NOT NULL.
   - `accessor_role`: `VARCHAR(32)` NOT NULL.
   - `action`: `VARCHAR(32)` NOT NULL.
2. `kyc_status_history`:
   - `id`: `VARCHAR(36)` PRIMARY KEY (UUID).
   - `verification_id`: `VARCHAR(36)` NOT NULL.
   - `changed_by`: `VARCHAR(36)` NOT NULL.
3. `kyc_events_outbox`:
   - `id`: `VARCHAR(36)` PRIMARY KEY (UUID).
   - `aggregate_id`: `VARCHAR(36)` NOT NULL.
4. Mở rộng `user_verifications`:
   - `id`: `VARCHAR(36)` PRIMARY KEY (UUID).
   - `user_id`: `VARCHAR(36)` NOT NULL UNIQUE.
   - `id_card_number_hash`: `VARCHAR(64)` UNIQUE.
   - `id_card_number_encrypted`: `TEXT`.
   - `id_card_number_masked`: `VARCHAR(16)`.
5. Mở rộng `user_profiles`:
   - `id`: `VARCHAR(36)` PRIMARY KEY (UUID).
   - `user_id`: `VARCHAR(36)` NOT NULL UNIQUE.
   - `version`: `BIGINT NOT NULL DEFAULT 0` (cho Khóa Lạc Quan - Optimistic Locking).

**Kết luận V4**: 100% các bảng mới và cột định danh trong V4 tuân thủ chuẩn `VARCHAR(36)` đồng nhất với toàn bộ `identity-service`.

