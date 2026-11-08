# Báo Cáo Chẩn Đoán Lỗi Mã Hóa Tiếng Việt & Kế Hoạch Sửa Chữa (UI-FIX-1)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Thời điểm thực hiện:** 08/11/2026  
**Mục tiêu:** Chẩn đoán lỗi hiển thị `???` thay cho tiếng Việt và kế hoạch khắc phục triệt để.

---

## 1. Kết Quả Quét Mọi Cột Văn Bản Ở Mọi Schema (information_schema)

Lệnh thực hiện: Quét 440 cột văn bản (`VARCHAR`, `TEXT`, `MEDIUMTEXT`, `LONGTEXT`, `CHAR`) trên 6 schemas:
- `cozynest_identity`
- `cozynest_listing`
- `cozynest_booking`
- `cozynest_billing`
- `cozynest_communication`
- `cozynest_admin`

```sql
SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA LIKE 'cozynest_%'
  AND DATA_TYPE IN ('varchar', 'text', 'mediumtext', 'longtext', 'char')
  AND TABLE_NAME != 'flyway_schema_history';
```

### Bảng Thống Kê Các Cột Chứa Ký Tự `?` Hoặc `U+FFFD`

| Schema | Bảng | Cột | Bộ mã / Collation | Số dòng | Ví dụ giá trị | Đánh giá |
| :--- | :--- | :--- | :--- | :---: | :--- | :---: |
| `cozynest_listing` | `landlord_snapshots` | `name` | `utf8mb4` / `utf8mb4_unicode_ci` | **2** | `Tr???n Th??? Ch??? Tr???` | **HỎNG DỮ LIỆU (CORRUPTED)** |
| `cozynest_listing` | `rooms` | `curfew` | `utf8mb4` / `utf8mb4_unicode_ci` | **54** | `Gi??? gi???c t??? do, kh??a v??n tay ri??ng` | **HỎNG DỮ LIỆU (CORRUPTED)** |
| `cozynest_listing` | `banners` | `image_url` | `utf8mb4` / `utf8mb4_unicode_ci` | 3 | `https://images.unsplash.com/...?auto=format` | Hợp lệ (query param URL) |
| `cozynest_listing` | `landlord_snapshots` | `avatar_url`| `utf8mb4` / `utf8mb4_unicode_ci` | 2 | `https://images.unsplash.com/...?auto=format` | Hợp lệ (query param URL) |
| `cozynest_listing` | `provinces` | `image_url` | `utf8mb4` / `utf8mb4_unicode_ci` | 6 | `https://images.unsplash.com/...?auto=format` | Hợp lệ (query param URL) |
| `cozynest_listing` | `room_images` | `image_url` | `utf8mb4` / `utf8mb4_unicode_ci` | 10 | `https://images.unsplash.com/...?auto=format` | Hợp lệ (query param URL) |
| `cozynest_communication` | `messages` | `content` | `utf8mb4` / `utf8mb4_unicode_ci` | 1 | `...xe máy miễn phí không ạ?` | Hợp lệ (dấu hỏi câu hỏi) |
| `cozynest_communication` | `support_tickets` | `description`| `utf8mb4` / `utf8mb4_unicode_ci` | 1 | `...tải file PDF ở đâu?` | Hợp lệ (dấu hỏi câu hỏi) |
| `cozynest_communication` | `support_ticket_replies` | `message` | `utf8mb4` / `utf8mb4_unicode_ci` | 1 | `...chứng thư số không?` | Hợp lệ (dấu hỏi câu hỏi) |

---

## 2. Bằng Chứng Mã Thập Lục Phân (HEX Analysis)

### 2.1. Phân Tích Cột `landlord_snapshots.name`:
- **Giá trị:** `Tr???n Th??? Ch??? Tr???`
- **Mã HEX:** `54 72 3F 3F 3F 6E 20 54 68 3F 3F 3F 20 43 68 3F 3F 3F 20 54 72 3F 3F 3F`
- **Giải mã từng byte:**
  - `54 72` = "Tr"
  - `3F 3F 3F` = 3 ký tự ASCII `?` liên tiếp (mã ASCII `0x3F`). Ký tự gốc trong UTF-8 là chữ "ần" (`0xE1 0xBA 0xA7` - đúng 3 bytes).
  - `6E` = "n"
  - `54 68` = "Th"
  - `3F 3F 3F` = 3 ký tự ASCII `?` (ký tự gốc "ị" là `0xE1 0xBB 0x8B` - đúng 3 bytes).
  - `43 68` = "Ch"
  - `3F 3F 3F` = 3 ký tự ASCII `?` (ký tự gốc "ủ" là `0xE1 0xBB 0xA7` - đúng 3 bytes).
  - `54 72` = "Tr"
  - `3F 3F 3F` = 3 ký tự ASCII `?` (ký tự gốc "ọ" là `0xE1 0xBB 0x8D` - đúng 3 bytes).

### 2.2. Phân Tích Cột `rooms.curfew`:
- **Giá trị:** `Gi??? gi???c t??? do, kh??a v??n tay ri??ng`
- **Mã HEX:** `47 69 3F 3F 3F 20 67 69 3F 3F 3F 63 ...`
- Mỗi ký tự tiếng Việt 3 byte (`ờ`, `ấ`, `ự`, `ó`, `â`, `ê`) đều bị thay thế bằng 2 hoặc 3 ký tự `0x3F` (ASCII `?`).

---

## 3. Kiểm Tra Cấu Hình Hạ Tầng, Mã Nguồn & Đường Truyền

1. **Bộ mã CSDL MySQL:**
   - Server: `utf8mb4`, Collation `utf8mb4_unicode_ci` (Cấu hình chuẩn trong `docker-compose.yml`: `--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`).
   - Các bảng & cột: 100% là `utf8mb4`.
2. **File SQL trên đĩa (`listing-service/.../V10__room_detail_and_home_data.sql`):**
   - File trên đĩa là **UTF-8 chuẩn, KHÔNG có BOM** (`hasBOM = false`).
   - Nội dung trong file là tiếng Việt có dấu hoàn chỉnh: `'Trần Thị Chủ Trọ'`, `'Giờ giấc tự do, khóa vân tay riêng'`.
3. **Cách thức nạp seed/migration:**
   - Khi chạy qua câu lệnh shell PowerShell: `Get-Content V10__room_detail_and_home_data.sql | docker exec -i cozynest_mysql mysql ...`
   - Trong Windows PowerShell 5.1, lệnh `Get-Content` mặc định đọc tệp không có BOM bằng bảng mã hệ thống ANSI (Windows-1252).
   - Đường ống (pipe `|`) đẩy luồng bytes này qua stdout sang tiến trình `docker exec`, biến toàn bộ các byte ngoài dải ASCII thành ký tự ASCII `?` (`0x3F`).

---

## 4. Kết Luận Nguyên Nhân Gốc (Root Cause Conclusion)

> [!IMPORTANT]
> **NGUYÊN NHÂN GỐC ĐÃ ĐƯỢC XÁC MINH CÓ BẰNG CHỨNG (100% CHÍNH XÁC):**  
> Bản thân file SQL trên đĩa hoàn toàn đúng định dạng UTF-8. Lỗi xảy ra do **thao tác nạp SQL thủ công thông qua pipe của PowerShell (`Get-Content ... | docker exec ...`)**.  
> PowerShell 5.1 đã ép kiểu đọc ANSI (Windows-1252) trên file UTF-8 không có BOM, chuyển đổi mỗi byte của ký tự tiếng Việt UTF-8 thành mã byte ASCII `0x3F` (`?`) trước khi gửi vào MySQL.  
> CSDL MySQL lưu đúng những gì nhận được qua pipe (các byte `0x3F`), dẫn đến dữ liệu lưu trữ bị mất dấu vĩnh viễn trong CSDL lúc runtime.

---

## 5. Giải Pháp Sửa Tại Gốc & Quy Trình Ngăn Chặn

1. **Không nạp migration/seed qua pipe PowerShell:**
   - Mọi migration và seed data bắt buộc phải được nạp thông qua **Flyway tự động của Spring Boot** khi khởi động container hoặc thông qua Docker exec trực tiếp nạp file bên trong container với cờ `--default-character-set=utf8mb4`.
2. **Bổ sung tham số JDBC URL & Flyway:**
   - Thêm `characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci` vào chuỗi kết nối JDBC của toàn bộ 6 microservice.
   - Bổ sung `flyway.encoding: UTF-8` trong `application.yml`.
3. **Kịch bản làm sạch & thiết lập lại CSDL an toàn (`dev-reset-db`):**
   - Tạo `scripts/dev-reset-db.ps1` và `scripts/dev-reset-db.sh`.
   - Cơ chế bảo vệ: **Kiểm tra profile môi trường. Nếu phát hiện `prod` hoặc `production`, lập tức dừng khẩn cấp và từ chối chạy.**
   - Kịch bản đọc file trực tiếp bằng stream UTF-8 chuẩn xác, tái tạo các schema và nạp Flyway sạch sẽ.

