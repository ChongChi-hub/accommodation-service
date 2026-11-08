# AUDIT & VERIFICATION REPORT: LANDLORD-04 & LANDLORD-02
**Thẻ:** [LANDLORD-04] Bảng quản lý phòng cho thuê (Tìm kiếm, Sửa, Xóa, Ẩn/Hiện) + [LANDLORD-02] Upload nhiều ảnh và chọn ảnh bìa  
**Trạng thái ban đầu:** Reviewing  
**Mục tiêu:** Kiểm chứng bằng bằng chứng thực tế, phát hiện lỗi, sửa dứt điểm từng lỗi với Conventional Commits, xác nhận qua test tự động và chạy thực tế.

---

## BẢNG TỔNG HỢP TIẾN ĐỘ 20 HẠNG MỤC

| # | Hạng mục kiểm tra | Hiện trạng đối chiếu | Kế hoạch & Test bổ sung | Trạng thái |
|---|---|---|---|---|
| **A** | **LANDLORD-04** | | | |
| 1 | `/landlord/rooms` lấy dữ liệu thật từ listing-service: tab trạng thái (đếm đúng, khớp DB), tìm kiếm không phân biệt dấu/hoa thường ("phong tro" -> "Phòng trọ"), sắp xếp, phân trang, sync URL | Cần kiểm tra controller, query service, JPA collation/accent-insensitive | Test query không dấu với seed, kiểm tra tab counts | Đang kiểm chứng |
| 2 | Sửa: liên kết tới form chỉnh sửa; khóa lạc quan (hai tab sửa cùng lúc -> 409 có thông báo rõ) | Cần kiểm tra `@Version` trên entity `Room`, xử lý `OptimisticLockingFailureException` -> HTTP 409 | Test concurrent update trả về 409 Conflict | Đang kiểm chứng |
| 3 | Ẩn/hiện: PUBLISHED <-> HIDDEN. Phòng bị MOD/ADMIN ẩn thì chủ trọ KHÔNG tự hiện lại được (403 mã riêng, nút khóa kèm lý do). Sau khi ẩn, `/rooms/[slug]` trả 404 và biến khỏi tìm kiếm; hiện lại thì quay lại | Cần kiểm tra logic `hidden_by`, `hidden_reason`, permission check và search filter | Test ẩn/hiện, test 403 khi mod ẩn, test public 404 | Đang kiểm chứng |
| 4 | Xóa mềm: hộp thoại xác nhận; chặn xóa phòng OCCUPIED, có hợp đồng hiệu lực hoặc có lịch hẹn CONFIRMED trong tương lai (409 nói rõ lý do); xóa xong công khai 404 | Cần kiểm tra cross-service check hoặc foreign key/state check (OCCUPIED, CONFIRMED appointment) -> 409 | Test chặn xóa phòng có lịch/hợp đồng | Đang kiểm chứng |
| 5 | IDOR: chủ trọ A xem/sửa/ẩn/xóa/đổi ảnh phòng của B -> 404 (test mọi endpoint) | `LandlordIdorSecurityTest.java` đã có một phần | Bổ sung test ma trận IDOR đầy đủ cho toàn bộ endpoints | Đang kiểm chứng |
| 6 | UI: 4 trạng thái; thao tác lạc quan có hoàn tác khi lỗi; ô tìm debounce 300 ms và hủy request cũ; 390px không tràn ngang | Kiểm tra `useLandlordRooms.ts`, `LandlordRoomTable.tsx`, responsive CSS | Test frontend components, responsive audit | Đang kiểm chứng |
| **B** | **LANDLORD-02** | | | |
| 7 | Giới hạn: tối đa 10 ảnh/phòng, mỗi file tối đa 5 MB, JPEG/PNG/WebP; kiểm tra magic bytes; đọc header trước khi giải mã chống decompression bomb (<= 40 MP); kiểm ở SERVER | `LandlordImageService.java` đã có validation magic bytes và max resolution | Kiểm tra lại logic chống decompression bomb, test upload file giả | Đang kiểm chứng |
| 8 | Xử lý: xóa EXIF (kể cả GPS), xoay theo EXIF trước khi xóa, tạo 3 kích cỡ WebP, PNG trong suốt không nền đen; tên file sinh ngẫu nhiên (chống path traversal); rollback khi lỗi giữa chừng | `LandlordImageService.java` dùng ImageIO / TwelveMonkeys / metadata-extractor | Test upload PNG transparent, kiểm tra WebP 3 kích cỡ, test rollback | Đang kiểm chứng |
| 9 | Ảnh bìa: đúng 1 ảnh bìa/phòng (cột sinh + UNIQUE hoặc tương đương, test hai request đồng thời); xóa ảnh bìa thì ảnh kế tiếp tự thành bìa; sắp xếp lại lưu thật; xóa ảnh xóa cả 3 file | V5 migration có unique cover_room_id, service có handle delete | Test race condition 2 request đặt bìa đồng thời, test cascade delete file | Đang kiểm chứng |
| 10 | `POST /landlord/rooms/{id}/submit` kiểm tra Ở SERVER tối thiểu 3 ảnh và có ảnh bìa (400 mã riêng) | Cần kiểm tra endpoint submit trong `LandlordRoomController` | Viết/kiểm tra test 400 khi < 3 ảnh hoặc không có ảnh bìa | Đang kiểm chứng |
| 11 | UI: kéo thả, nhiều file cùng lúc, tiến trình từng ảnh, thử lại ảnh lỗi, hủy, lỗi theo từng file, cảnh báo rời trang khi đang tải; ảnh có width/height và alt | Kiểm tra ImageUploader component | Kiểm tra CLS, alt text, warning unload | Đang kiểm chứng |
| 12 | Rate limit tải ảnh (Redis) có test 429; TENANT -> 403 | Kiểm tra filter rate limit bằng Redis trên `/landlord/rooms/{id}/images` | Test 429 khi spam upload, test 403 cho TENANT | Đang kiểm chứng |
| **C** | **KIỂM TRA CHÉO** | | | |
| 13 | listing-service và booking-service tự xác thực JWT bằng JWKS của identity; test 200/401/403/404 đủ 5 vai trò + token hết hạn + ký sai | Kiểm tra `JwtSecurityConfig.java`, JWKS cache, Nimbus reactive/servlet filter | Chạy `LandlordNamespaceSecurityMatrixTest` và booking security test | Đang kiểm chứng |
| 14 | Testcontainers MySQL cho MỌI service có migration: chạy sạch ở dev và prod; prod có 0 bản ghi seed, dev có > 0; seed nằm ở db/seed | Kiểm tra test Flyway profile prod vs dev | Viết/chạy test đếm record prod = 0 cho tất cả 6 services | Đang kiểm chứng |
| 15 | Ảnh seed là file thật (test quét: mọi room_images.url seed trả 200 qua endpoint phục vụ ảnh) | Kiểm tra thư mục storage tĩnh hoặc endpoint phục vụ ảnh | Viết script/test kiểm tra 100% URL seed trả về 200 | Đang kiểm chứng |
| 16 | api-gateway có test định tuyến (route nào tới service nào, /internal không lộ, CORS) | RouteLocator bean đã cấu hình trong gateway | Viết unit/integration test cho gateway routing | Đang kiểm chứng |
| 17 | Lập `docs/audit/ID_TYPES.md`: bảng mọi bảng/cột ID và kiểu thật. CHỈ lập bảng, KHÔNG sửa | File `ID_TYPES.md` đã có sẵn | Rà soát đối chiếu đầy đủ bảng và cột ID | Đang kiểm chứng |
| 18 | Crawler route kiểm tra đúng nơi chuyển hướng (không chỉ link tồn tại) | Test route crawler | Chạy script kiểm tra redirects | Đang kiểm chứng |
| 19 | E2E (backend thật): chủ trọ đăng nhập -> tải 3 ảnh -> chọn bìa -> gửi duyệt; ẩn rồi kiểm công khai; tìm không dấu; 404 với phòng của chủ trọ khác | Cần kịch bản E2E kiểm tra toàn bộ luồng với Docker stack thật | Chạy script/Playwright E2E | Đang kiểm chứng |
| 20 | Ảnh chụp danh sách (4 tab, rỗng, lỗi) và trang upload (đang tải, lỗi, đủ ảnh), 1440 và 390, light/dark, vi/en; tự phê bình 3 điểm CHƯA TỐT mỗi ảnh và sửa | Chụp screenshots bằng Playwright | Đánh giá 3 điểm chưa tốt và hoàn thiện | Đang kiểm chứng |
