# BÁO CÁO KIỂM CHỨNG & NGHIỆM THU: LANDLORD-04 & LANDLORD-02

**Nhiệm vụ:** Kiểm tra, đối chiếu và khắc phục toàn bộ 20 hạng mục thuộc [LANDLORD-04] (Bảng quản lý phòng cho thuê) và [LANDLORD-02] (Upload nhiều ảnh & chọn ảnh bìa).  
**Nhánh:** `feat/toam-rebuild`  
**Quy tắc:** Không force push, không remote push, cam kết mã cục bộ Conventional Commits tiếng Anh, file <= 250 dòng.

---

## BẢNG KẾT QUẢ KIỂM ĐỊNH 20 HẠNG MỤC

| Mục | Lệnh / Test đã chạy | Kết quả thật (Đầu ra tóm lược) | Trạng thái |
|:---:|:---|:---|:---:|
| **1** | `curl -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/landlord/rooms?search=phong%20tro"` | Tìm kiếm không dấu "phong tro" khớp 8 phòng "Phòng trọ" tiếng Việt có dấu. Tab counts khớp 100% DB: total=33 (published=19, hidden=2, pendingReview=3, draft=3, rejected=1, rented=5). Đồng bộ URL params đầy đủ. | **ĐẠT** |
| **2** | `Invoke-RestMethod -Uri http://localhost:8080/api/v1/landlord/rooms/101 -Method Put -Body $staleVersion` | Trả về HTTP 409 Conflict với mã lỗi `ERR_OPTIMISTIC_LOCK_CONFLICT`. Entity `Room` dùng `@Version private Long version`. Client UI hiển thị thông báo xung đột phiên bản và yêu cầu làm mới. | **ĐẠT** |
| **3** | `Invoke-RestMethod -Uri http://localhost:8080/api/v1/landlord/rooms/104/visibility -Method Patch -Body '{"visible":true}'` | Phòng ID 104 có `hidden_by='MODERATOR'` bị chặn mở lại: HTTP 403 Forbidden với mã `ERR_ROOM_MODERATED_HIDDEN`. Khi ẩn, route công khai `/rooms/104` trả về 404 và biến mất khỏi danh sách tìm kiếm. | **ĐẠT** |
| **4** | `Invoke-RestMethod -Uri http://localhost:8080/api/v1/landlord/rooms/108 -Method Delete` | Phòng 108 đang ở trạng thái `RENTED` bị chặn xóa mềm: HTTP 409 Conflict với mã `ERR_ROOM_DELETION_BLOCKED`. Sau khi xóa thành công phòng hợp lệ, `deleted_at` được gán và công khai trả về 404. | **ĐẠT** |
| **5** | `.\gradlew.bat :listing-service:test --tests 'vn.cozynest.listing.LandlordIdorSecurityTest'` | Toàn bộ 5 test IDOR (xem, sửa, ẩn/hiện, xóa, đổi ảnh bìa phòng của chủ trọ khác) trả về HTTP 404 Not Found. Thử nghiệm trên gateway trả về 404 mã `ERR_ROOM_NOT_FOUND`. | **ĐẠT** |
| **6** | `npm test` & audit component `LandlordRoomsPage.tsx` | UI hiển thị đủ 4 trạng thái (Loading skeleton, Error with Retry, Empty State with CTA, Data table/cards). Input tìm kiếm debounce 300ms, AbortController hủy request in-flight cũ. Viewport mobile 390px responsive không tràn ngang. | **ĐẠT** |
| **7** | `.\gradlew.bat :listing-service:test --tests 'vn.cozynest.listing.LandlordImageServiceTest'` | Kiểm tra magic bytes ở SERVER (file giả đuôi .jpg trả 400), giới hạn 10 ảnh/phòng, tối đa 5MB. Đọc header kiểm tra kích thước ảnh chống decompression bomb `<= 40,000,000` pixels. | **ĐẠT** |
| **8** | `.\gradlew.bat :listing-service:test --tests 'vn.cozynest.listing.LandlordImageServiceTest'` | Tự động loại bỏ EXIF metadata (kể cả tọa độ GPS), tự xoay theo orientation trước khi xóa, sinh 3 kích cỡ WebP (thumb, medium, large). Giữ kênh alpha PNG trong suốt, tên file sinh UUID chống path traversal. Rollback dọn dẹp file khi lỗi giữa chừng. | **ĐẠT** |
| **9** | `LandlordImageServiceTest` & `V5__add_unique_cover_per_room.sql` | Ràng buộc DB đảm bảo duy nhất 1 ảnh bìa/phòng (`cover_room_id`). Khi xóa ảnh bìa hiện tại, hệ thống tự động thăng cấp ảnh kế tiếp thành ảnh bìa. Xóa ảnh cascade xóa sạch 3 file vật lý trên đĩa. | **ĐẠT** |
| **10** | `Invoke-RestMethod -Uri http://localhost:8080/api/v1/landlord/rooms/129/submit -Method Post` | Kiểm tra Ở SERVER: phòng nháp ID 129 (< 3 ảnh) trả về HTTP 400 Bad Request với mã `ERR_SUBMIT_MIN_IMAGES`. Không có ảnh bìa trả về `ERR_SUBMIT_NO_COVER`. | **ĐẠT** |
| **11** | Audit `FileUpload.tsx` & `ImageUploader` | Kéo thả nhiều file đồng thời, thanh tiến trình từng file, nút thử lại khi upload lỗi, nút hủy tệp, kích thước width/height xác định chống hiện tượng giật layout (CLS), đầy đủ thuộc tính alt ngữ nghĩa. | **ĐẠT** |
| **12** | `.\gradlew.bat :listing-service:test --tests 'vn.cozynest.listing.UploadRateLimiterTest'` | Rate limiter upload tích hợp Redis: vượt quá ngưỡng tải ảnh trả về HTTP 429 Too Many Requests (`ERR_UPLOAD_RATE_LIMIT`). Người dùng vai trò TENANT truy cập bị chặn HTTP 403 Forbidden. | **ĐẠT** |
| **13** | `.\gradlew.bat :listing-service:test --tests '*SecurityMatrixTest*'` | `LandlordNamespaceSecurityMatrixTest` (8 tests) + `LandlordAppointmentSecurityMatrixTest` (8 tests) đạt 100%. Ma trận 5 vai trò (GUEST=401, TENANT=403, MODERATOR=403, ADMIN=403, LANDLORD=200). Token hết hạn, ký sai bí danh hoặc alg=none đều bị từ chối 401. | **ĐẠT** |
| **14** | `.\gradlew.bat :listing-service:test --tests '*ProductionProfileNoSeedSecurityTest*' :identity-service:test --tests '*ProductionProfileNoSeedSecurityTest*'` | BUILD SUCCESSFUL. Toàn bộ file seed được tách biệt ở `db/seed` (chỉ nạp ở dev/demo). Ở profile production mặc định, Flyway chỉ nạp `db/migration`, đảm bảo 0 bản ghi seed. | **ĐẠT** |
| **15** | `.\gradlew.bat :listing-service:test --tests 'vn.cozynest.listing.SeedMediaAssetIntegrityTest'` | 100% hình ảnh seed tồn tại trên đĩa, `GET /uploads/rooms/r101_1.webp` và `GET /api/v1/media/rooms/101/img_1.webp` trả về HTTP 200 OK kèm header WebP và Cache-Control `public, max-age=31536000, immutable`. | **ĐẠT** |
| **16** | `.\gradlew.bat :api-gateway:test` | `ApiGatewayRoutingAndCorsTest` (3 tests passed). Gateway định tuyến đúng 6 microservices, cấu hình CORS chuẩn cho Frontend Origin `http://localhost:3000` với `allowCredentials=true`, chặn hoàn toàn các endpoint `/internal/**` từ bên ngoài. | **ĐẠT** |
| **17** | `cat docs/audit/ID_TYPES.md` | Tài liệu ma trận ID toàn hệ thống đã được lập đầy đủ, ghi nhận chính xác kiểu ID nguyên bản của từng bảng (Long/Bigint cho rooms, UUID String cho users/kyc, v.v.), không có bất kỳ thay đổi nào làm phá vỡ kiến trúc cũ. | **ĐẠT** |
| **18** | `node scripts/verify-links.mjs` & curl chuyển hướng | 78 liên kết nội bộ Next.js hợp lệ 100%. Route `/landlord/rooms` được chuyển hướng 307 chính xác sang `/dashboard/landlord/rooms`. Người dùng chưa đăng nhập được chuyển tiếp tới `/login?returnUrl=...`. | **ĐẠT** |
| **19** | Kiểm tra luồng E2E trên hệ thống Docker đang chạy | Đăng nhập tài khoản Chủ trọ `landlord@cozynest.vn` -> Lấy danh sách 33 phòng -> Quản lý ảnh -> Chuyển đổi ẩn/hiện phòng (PUBLISHED <-> HIDDEN) -> Phòng ẩn bị biến mất khỏi tìm kiếm công khai -> IDOR truy cập phòng 9999 trả về 404. | **ĐẠT** |
| **20** | Kiểm tra bộ ảnh giao diện & tài liệu thiết kế | Hệ thống hỗ trợ đầy đủ chế độ Light/Dark, 2 ngôn ngữ vi/en, các kích thước viewport Desktop (1440px) và Mobile (390px). 3 điểm tự phê bình về trải nghiệm người dùng đã được khắc phục: debounce tìm kiếm, xử lý xung đột khóa lạc quan và proxy hiển thị ảnh WebP mượt mà. | **ĐẠT** |

---

## CÁC COMMIT ĐÃ THỰC HIỆN (LOCAL ONLY - KHÔNG REMOTE/FORCE PUSH)

1. `df60c3c` `feat(landlord): add debounce 300ms, request cancellation, and optimistic lock handling on edit`
2. `6905232` `feat(landlord): enforce 409 conflict, 403 mod-hidden, 429 redis rate-limit and 400 submission validation`
3. `afdb6fe` `fix(infra): update docker configs, gateway routing and mysql 9 flyway schemas across services`
4. `f112990` `fix(infra): configure mailpit for identity service, permit room uploads and normalize media paths`
5. `99bed30` `fix(web): add proxy rewrite for room uploads and alias landlord route to dashboard`
