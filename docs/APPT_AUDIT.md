# Khảo Sát & Đối Chiếu Hiện Trạng: [APPT-02] Dashboard Duyệt Lịch Hẹn Cho Chủ Trọ

> Ngày thực hiện: 07/11/2026  
> Phạm vi: `booking-service` (Backend) và `accommodation-web` (Frontend), nhánh `feat/toam-rebuild`

---

## 1. Hiện Trạng Cơ Sở Dữ Liệu (booking-service)

### 1.1. Bảng `viewing_appointments`
| Cột | Hiện tại (`V1__init_booking_schema.sql`) | Yêu cầu APPT-02 | Đánh giá & Kế hoạch |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(36)` (UUID) | PK (UUID v7/VARCHAR(36)) | ĐÃ CÓ |
| `room_id` | `VARCHAR(36) NOT NULL` | UUID tham chiếu phòng | ĐÃ CÓ |
| `landlord_id` | `VARCHAR(36) NOT NULL` | UUID tham chiếu chủ trọ | ĐÃ CÓ |
| `tenant_id` | `VARCHAR(36) NOT NULL` | UUID tham chiếu người thuê | ĐÃ CÓ |
| `appointment_date`, `appointment_time` | `DATE` và `TIME` riêng biệt | Chuẩn hóa `scheduled_start`, `scheduled_end` (DATETIME(3) UTC) | **CẦN BỔ SUNG** trong migration `V3` |
| `status` | `VARCHAR(30)` CHECK 5 giá trị | PENDING, CONFIRMED, DECLINED, CANCELLED, COMPLETED, NO_SHOW, EXPIRED | **CẦN CẬP NHẬT** mở rộng CHECK constraint trong `V3` |
| `tenant_note` | `message TEXT NULL` | Lời nhắn từ khách thuê khi đặt | Đổi tên/ánh xạ `tenant_note` |
| `landlord_note` | **CHƯA CÓ** | Ghi chú phản hồi riêng của chủ trọ | **CẦN BỔ SUNG** trong `V3` |
| `cancel_reason_code` | **CHƯA CÓ** | `BUSY`, `ROOM_TAKEN`, `TENANT_NOT_ELIGIBLE`, `TIME_CONFLICT`, `OTHER` | **CẦN BỔ SUNG** trong `V3` |
| `cancel_reason_note` | `rejection_reason VARCHAR(255)` | Chi tiết lý do hủy (tối thiểu 10 ký tự khi chọn `OTHER`) | Chuẩn hóa `cancel_reason_note` |
| `cancelled_by` | **CHƯA CÓ** | `LANDLORD`, `TENANT`, `SYSTEM` | **CẦN BỔ SUNG** trong `V3` |
| `confirmed_at` | **CHƯA CÓ** | Thời điểm chủ trọ xác nhận lịch (DATETIME(3)) | **CẦN BỔ SUNG** trong `V3` |
| `completed_at` | **CHƯA CÓ** | Thời điểm đánh dấu kết quả (DATETIME(3)) | **CẦN BỔ SUNG** trong `V3` |
| `version` | **CHƯA CÓ** | Khóa lạc quan chống ghi đè đồng thời (`BIGINT DEFAULT 0`) | **CẦN BỔ SUNG** trong `V3` |
| Snapshot dữ liệu phòng & khách | **CHƯA CÓ** | `room_title`, `room_address`, `room_cover_thumb_key`, `contact_name`, `contact_phone`, `contact_email` | **CẦN BỔ SUNG** trong `V3` |
| Indexes & Ràng buộc | Chỉ có index đơn (`room_id`, `tenant_id`, `landlord_id`, `status`) | Cần composite index `(landlord_id, status, scheduled_start)`, `(room_id, scheduled_start)`, index tìm kiếm, UNIQUE `(room_id, tenant_id, scheduled_start)` | **CẦN BỔ SUNG** trong `V3` |

### 1.2. Bảng `appointment_status_history`
* **Hiện tại**: Chưa tồn tại.
* **Yêu cầu**: Cần tạo mới trong `V3` gồm các cột `id`, `appointment_id`, `from_status`, `to_status`, `actor_id`, `actor_role`, `reason`, `created_at`.

### 1.3. Bảng `appointment_events_outbox`
* **Hiện tại**: Chưa tồn tại.
* **Yêu cầu**: Cần tạo mới trong `V3` để ghi nhận sự kiện miền Transactional Outbox (`id`, `event_type`, `aggregate_id`, `payload`, `created_at`, `published_at`).

---

## 2. Hiện Trạng Backend Endpoints (booking-service)

| Endpoint | Đã có | Chưa có | Kế hoạch triển khai |
| :--- | :---: | :---: | :--- |
| `GET /api/v1/landlord/appointments` | | :x: | Viết mới: Lọc theo status, roomId, from, to, search, sort, page, size. Che số điện thoại khi chưa CONFIRMED. |
| `GET /api/v1/landlord/appointments/summary` | | :x: | Viết mới: Thống kê số lượng theo trạng thái, số lịch hôm nay, cần cập nhật, đếm theo phòng. |
| `GET /api/v1/landlord/appointments/{id}` | | :x: | Viết mới: Lấy chi tiết lịch hẹn kèm lịch sử trạng thái (Audit trail). |
| `POST /api/v1/landlord/appointments/{id}/confirm` | | :x: | Viết mới: Xác nhận lịch, kiểm tra trùng giờ, hỗ trợ cờ `force=true`. |
| `POST /api/v1/landlord/appointments/{id}/cancel` | | :x: | Viết mới: Hủy lịch (DECLINED nếu đang PENDING, CANCELLED nếu đã CONFIRMED), bắt buộc reasonCode. |
| `POST /api/v1/landlord/appointments/{id}/complete` | | :x: | Viết mới: Đánh dấu `COMPLETED` hoặc `NO_SHOW` (chỉ cho phép khi đã qua giờ hẹn). |
| `GET /api/v1/tenant/appointments` | | :x: | Viết mới: Khách thuê xem danh sách lịch hẹn của chính mình, trạng thái và lý do hủy (chỉ đọc). |
| Spring Cloud Gateway Routing | | :x: | Bổ sung route `/api/v1/landlord/appointments/**` và `/api/v1/tenant/appointments/**` trỏ tới `booking-service`. |

---

## 3. Hiện Trạng Frontend (accommodation-web)

| Thành phần | Hiện tại | Yêu cầu APPT-02 | Kế hoạch triển khai |
| :--- | :--- | :--- | :--- |
| Trang `/dashboard/landlord/appointments` | Dữ liệu tĩnh in-memory (`useState`) 2 bản ghi | Đầy đủ 3 chế độ xem (Danh sách, Theo phòng, Lịch), Drawer chi tiết, Modal Xác nhận/Hủy/Hoàn thành | Viết lại toàn bộ kết nối Http repository |
| Data Access Layer | Chưa có repository/use-case | `ViewingAppointment` domain entity, `IAppointmentRepository`, `HttpAppointmentRepository` | Tạo mới theo Clean Architecture & DI container |
| Trang `/dashboard/tenant/appointments` | Dữ liệu tĩnh in-memory | Hiển thị dữ liệu thật từ `GET /api/v1/tenant/appointments`, hiển thị lý do hủy | Kết nối Http repository cho Tenant |
| Sidebar Badge | Chưa hiển thị số lịch chờ duyệt của chủ trọ | Hiển thị badge số lượng lịch `PENDING` từ `/summary` | Cập nhật `DashboardSidebar.tsx` và `BadgeRepository` |
| Quản lý trạng thái & Xử lý xung đột | Không có | Tải lại server state khi thao tác, thông báo rõ ràng khi gặp lỗi 409 Version Conflict | Sử dụng TanStack Query / Hook chuyên dụng |
| Che dấu quyền riêng tư | Hiển thị cứng SĐT | Khách chỉ thấy số đầy đủ khi lịch đã được xác nhận (CONFIRMED/COMPLETED) | Hiển thị số ẩn `090••••567` hoặc đầy đủ kèm liên kết `tel:` |
