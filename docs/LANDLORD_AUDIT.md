# Khảo Sát & Đối Chiếu Hiện Trạng: LANDLORD-04 & LANDLORD-02

> Ngày thực hiện: 07/11/2026  
> Phạm vi: `listing-service` (Backend) và `accommodation-web` (Frontend), nhánh `feat/toam-rebuild`

---

## 1. Hiện Trạng Cơ Sở Dữ Liệu (listing-service)

### 1.1. Bảng `rooms`
| Cột | Hiện tại (`V1` + `V3`) | Yêu cầu LANDLORD-04 | Đánh giá & Kế hoạch |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT AUTO_INCREMENT` | Giữ nguyên PK nội bộ | ĐÃ CÓ |
| `room_uuid` | `VARCHAR(36) UNIQUE` | Định danh UUID cross-service | ĐÃ CÓ |
| `landlord_id` | `BIGINT NOT NULL` | Liên kết chủ trọ | ĐÃ CÓ |
| `title`, `slug`, `description` | Đầy đủ | Tiêu đề, slug URL, mô tả | ĐÃ CÓ |
| `price`, `deposit`, chi phí tiện ích | Đầy đủ | Giá thuê, tiền cọc, điện nước | ĐÃ CÓ |
| `status` | `VARCHAR(30)` | Trạng thái phòng (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `HIDDEN`, `REJECTED`, `RENTED`) | ĐÃ CÓ, chuẩn hóa enum CHECK |
| `deleted_at` | `TIMESTAMP NULL` | Xóa mềm | ĐÃ CÓ ở V3, bổ sung index |
| `hidden_at` | **CHƯA CÓ** | Thời điểm ẩn tin | **CẦN BỔ SUNG** trong `V5` |
| `hidden_by` | **CHƯA CÓ** | `OWNER`, `MODERATOR`, `ADMIN` | **CẦN BỔ SUNG** trong `V5` |
| `hidden_reason` | **CHƯA CÓ** | Lý do bị ẩn (đặc biệt khi Mod ẩn) | **CẦN BỔ SUNG** trong `V5` |
| `rejected_reason` | **CHƯA CÓ** | Lý do kiểm duyệt từ chối duyệt | **CẦN BỔ SUNG** trong `V5` |
| `published_at` | **CHƯA CÓ** | Ngày tin được duyệt xuất bản lần đầu | **CẦN BỔ SUNG** trong `V5` |
| `view_count` | **CHƯA CÓ** | Bộ đếm lượt xem tích lũy | **CẦN BỔ SUNG** trong `V5` (INT DEFAULT 0) |
| `title_search` | **CHƯA CÓ** | Cột chuẩn hóa không dấu hỗ trợ tìm kiếm nhanh | **CẦN BỔ SUNG** trong `V5` |
| Index tìm kiếm & lọc | Chỉ có location, status, price | Cần index `(landlord_id, status, deleted_at, published_at DESC)` và `title_search` | **CẦN BỔ SUNG** trong `V5` |

### 1.2. Bảng `room_images`
| Cột | Hiện tại (`V1`) | Yêu cầu LANDLORD-02 | Đánh giá & Kế hoạch |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT AUTO_INCREMENT` | PK | ĐÃ CÓ |
| `room_id` | `BIGINT NOT NULL` | FK tới `rooms(id)` | ĐÃ CÓ |
| `image_url` | `VARCHAR(500)` | URL phục vụ ảnh | ĐÃ CÓ |
| `is_primary` / `is_cover` | `is_primary BOOLEAN` | Đổi tên/ánh xạ `is_cover` | Chuẩn hóa sang `is_cover` |
| `storage_key`, `thumb_key`, `medium_key`, `large_key` | **CHƯA CÓ** | Khóa lưu trữ 3 kích cỡ WebP | **CẦN BỔ SUNG** trong `V5` |
| `original_filename`, `content_type`, `size_bytes`, `width`, `height` | **CHƯA CÓ** | Metadata ảnh gốc | **CẦN BỔ SUNG** trong `V5` |
| `sort_order` | `display_order INT` | Thứ tự hiển thị liên tục | Đổi tên/bổ sung `sort_order` |
| `alt_text`, `uploaded_by`, `created_at` | **CHƯA CÓ** | Tiêu chuẩn trợ năng & kiểm toán | **CẦN BỔ SUNG** trong `V5` |
| **Ràng buộc đúng 1 ảnh bìa mỗi phòng ở DB** | **CHƯA CÓ** | Dùng cột sinh `cover_room_id` + UNIQUE Index | **CẦN BỔ SUNG** trong `V5` |

### 1.3. Bảng `room_status_history`
* **Hiện tại**: Chưa tồn tại.
* **Yêu cầu**: Cần tạo mới trong `V5` gồm các cột `id`, `room_id`, `from_status`, `to_status`, `actor_id`, `actor_role`, `reason`, `created_at` để ghi vết mọi biến động trạng thái (ẩn/hiện/duyệt/từ chối/thuê).

---

## 2. Hiện Trạng Backend Endpoints (listing-service)

| Endpoint | Đã có | Chưa có | Kế hoạch triển khai |
| :--- | :---: | :---: | :--- |
| `GET /api/v1/rooms` (Public search) | :white_check_mark: | | Bổ sung điều kiện loại bỏ `deleted_at`, `hidden_at` và `status != 'PUBLISHED'` |
| `GET /api/v1/rooms/{id}` (Public detail) | :white_check_mark: | | Chặn truy cập nếu phòng chưa được duyệt hoặc bị ẩn |
| `GET /api/v1/landlord/rooms` | | :x: | Viết mới: lọc theo chủ trọ hiện tại (JWT), phân trang, sắp xếp, tìm kiếm không dấu, join ảnh bìa thumb |
| `GET /api/v1/landlord/rooms/summary` | | :x: | Viết mới: đếm số lượng phòng theo từng tab trạng thái |
| `GET /api/v1/landlord/rooms/{id}` | | :x: | Viết mới: lấy chi tiết phòng của chủ trọ phục vụ màn hình chỉnh sửa |
| `PATCH /api/v1/landlord/rooms/{id}/visibility` | | :x: | Viết mới: ẩn/hiện phòng, chặn chủ trọ tự hiện tin do Mod ẩn |
| `DELETE /api/v1/landlord/rooms/{id}` | | :x: | Viết mới: xóa mềm, chặn xóa phòng đang RENTED |
| `PUT /api/v1/landlord/rooms/{id}` | | :x: | Viết mới: cập nhật thông tin phòng |
| `POST /api/v1/landlord/rooms/{id}/images` | | :x: | Viết mới: upload multipart tối đa 10 ảnh, kiểm tra magic bytes, xóa EXIF, sinh 3 cỡ webp |
| `PATCH /api/v1/landlord/rooms/{id}/images/{imageId}/cover` | | :x: | Viết mới: đặt ảnh bìa nguyên tử trong 1 transaction |
| `DELETE /api/v1/landlord/rooms/{id}/images/{imageId}` | | :x: | Viết mới: xóa ảnh và xóa file đĩa, tự động chọn ảnh tiếp theo làm bìa |
| `PUT /api/v1/landlord/rooms/{id}/images/order` | | :x: | Viết mới: sắp xếp lại thứ tự ảnh liên tục |
| `GET /api/v1/media/{key}` | | :x: | Viết mới: stream ảnh tĩnh từ thư mục lưu trữ với Cache-Control dài |

---

## 3. Hiện Trạng Giao Diện Frontend (accommodation-web)

| Màn hình | Hiện tại | Yêu cầu LANDLORD-04 & LANDLORD-02 |
| :--- | :--- | :--- |
| `/dashboard/landlord/rooms` | Ghi cứng mảng `useState` 3 phần tử trong component, không phân trang, không tìm kiếm, không API | Chuyển sang TanStack Query + `HttpLandlordRoomRepository`. Bảng chuẩn responsive (card trên mobile), tab đếm số lượng, debounce tìm kiếm 300ms, nút ẩn/hiện lạc quan, modal xác nhận xóa mềm, link xem trước công khai. |
| `/dashboard/landlord/rooms/new` | Bước 4 dùng `FileUpload` giả lập toast, không chọn ảnh bìa, không có cơ chế tải lên thật | Tích hợp component dùng chung `ImageManager`: kéo thả, xem trước object URL (revoke khi hủy), đổi ảnh bìa, kéo thả sắp xếp, chỉ tải ảnh lên server khi nhấn Đăng tin, thanh tiến trình per-file. |
| `/dashboard/landlord/rooms/[id]/edit` | Dữ liệu khởi tạo tĩnh, thiếu khu vực quản lý ảnh thực tế | Nạp dữ liệu thật từ `GET /api/v1/landlord/rooms/{id}`, tích hợp `ImageManager` để tải thêm ảnh, xóa ảnh, đổi bìa, sắp xếp trực tiếp qua API. |

---

## 4. Quyết Định Kỹ Thuật (Java 25 & Image Processing)

1. **Xử lý ảnh WebP trên Java 25**:
   * Chuẩn `ImageIO` gốc của JDK không hỗ trợ ghi định dạng `.webp`.
   * Sử dụng thư viện mã nguồn mở `com.twelvemonkeys.imageio:imageio-webp:3.12.0` (tương thích hoàn toàn Java 21-25) kết hợp `net.coobird:thumbnailator:0.4.20` để resize ảnh chất lượng cao và loại bỏ toàn bộ metadata EXIF/GPS.
2. **Kho lưu trữ ảnh**:
   * Khởi tạo cổng trừu tượng `ImageStoragePort`.
   * Adapter `FileSystemImageStorageAdapter` lưu file vào thư mục ngoài web root (ví dụ `./storage/media/`) được mount qua Docker volume.
3. **Bảo vệ phòng RENTED**:
   * Tạo cổng `RoomUsagePort`. Adapter tạm thời kiểm tra trạng thái phòng, nếu `status == 'RENTED'` thì từ chối xóa kèm mã lỗi `ROOM_CURRENTLY_RENTED`. Ghi chú sẵn sàng kết nối Feign Client tới `booking-service` khi có API hợp đồng.

