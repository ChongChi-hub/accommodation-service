# Báo cáo kiểm định: Lưu trữ dữ liệu, Docker, Redis, Ảnh, Mã hóa và Bản đồ

- Nhiệm vụ: [DATA-STORAGE-MAP-AUDIT] - Pha 0: Báo cáo kiểm định toàn diện
- Ngày thực hiện: 2026-11-19
- Nhánh: feat/data-storage-map
- Nguồn dữ liệu: [CHẠY LẠI HÔM NAY] và [ĐỌC TỪ MÃ]

## 1. Bảng tổng hợp hiện trạng và khuyến nghị

| Hạng mục | Hiện trạng | Khoảng cách | Rủi ro | Khuyến nghị |
|---|---|---|---|---|
| A. Seed và Data thật | Flyway load cả seed và migration; roles nằm trong R__01 | Chưa tách profile real; thiếu role cơ bản nếu bỏ seed | Mất dữ liệu demo khi cần; không khởi động được hệ thống ở môi trường thật | Tạo migration V__ bổ sung role; tách profile real độc lập |
| B. Docker & Độ bền | Dùng named volumes; tất cả port mở ra host 0.0.0.0 | Chưa có env.real riêng; cổng DB/Redis/MQ bị expose trực tiếp | Xóa nhầm volume demo (down -v); lộ hệ thống qua port công khai | Tạo docker-compose.real.yml; bind localhost hoặc chỉ mở 80/443 |
| C. Đánh giá Redis | Chỉ dùng cho Google nonce và rate limit; có in-memory fallback | Redis đang không có password; phụ thuộc không bắt buộc | Lộ Redis qua port 6380 ra ngoài; tốn RAM container trên máy yếu | Đặt requirepass, đóng port host; giữ in-memory fallback |
| D. Ảnh & Cloudinary | Upload client-side API route chưa auth; hardcode key/secret | Ảnh KYC (CCCD) đẩy lên Cloudinary public | Lộ thông tin cá nhân (CCCD) ra công cộng; lộ api secret | Chuyển CCCD sang Private Storage BE; Cloudinary chỉ cho ảnh phòng |
| E. Mã hóa nhạy cảm | CCCD số đã hash HMAC + AES-GCM; ảnh CCCD chưa mã hóa | Ảnh CCCD lưu URL public; chưa có envelope encryption chuẩn | Lộ ảnh mặt trước/sau CCCD; vi phạm bảo vệ dữ liệu cá nhân | Dùng AES-256-GCM lưu cục bộ/S3 private; xóa ảnh sau 30 ngày |
| F. Bản đồ & Vị trí | Trang tạo phòng chỉ có 1 ô text địa chỉ; map dùng iframe OSM | Thiếu nhập tọa độ; thiếu ghim bản đồ; lộ tọa độ chính xác | Khách xem bị lộ địa chỉ chính xác khi chưa đặt cọc | Thêm Leaflet chọn tọa độ/ghim; làm mờ bán kính 200m công khai |

## 2. Chi tiết kiểm định

### A. Dữ liệu seed và dữ liệu thật
- Kiểm tra Flyway: Các service (identity, listing, booking, billing, communication) có thư mục db/migration (V__) và db/seed (R__).
- Phát hiện nghiêm trọng: Bảng roles trong identity-service được INSERT tại R__01_seed_demo_users.sql. Nếu chạy profile prod/real chỉ đọc V__, bảng roles sẽ rỗng làm hệ thống lỗi.
- Danh mục hành chính: V19 đã nạp 34 tỉnh thành và 3321 phường xã theo mô hình 2 cấp.

### B. Docker và độ bền dữ liệu
- Named volumes: cozynest_mysql_data, cozynest_redis_data, cozynest_rabbitmq_data, cozynest_minio_data lưu trữ bền vững khi chạy down thông thường.
- Nguy cơ: Lệnh down -v sẽ xóa sạch volume. Các port 5555 (MySQL), 6380 (Redis), 5672/15672 (RabbitMQ), 9000/9001 (MinIO), 8081-8086 đều expose ra host.
- Giải pháp: Tạo compose file riêng (docker-compose.real.yml) với project name và volume độc lập.

### C. Đánh giá sử dụng Redis
- Ứng dụng thực tế: identity-service dùng cho GoogleNonceManager và RedisRateLimiterAdapter; listing-service dùng cho UploadRateLimiter.
- Khả năng thay thế: Cả hai service đều đã có cơ chế in-memory fallback (ConcurrentHashMap) khi Redis down.
- Kết luận: Redis không bắt buộc ở quy mô đơn node, nhưng cần thiết khi scale đa instance để đồng bộ rate-limit và anti-replay. Đề xuất giữ Redis nhưng thêm mật khẩu và chặn port host.

### D. Ảnh và Cloudinary
- Lỗ hổng nghiêm trọng: Tại accommodation-web/src/app/api/upload/cloudinary/route.ts có fallback hardcode api_key và api_secret; không kiểm tra quyền người dùng.
- Ảnh KYC bị public: Tuyến đường upload KYC đang đẩy ảnh CCCD lên thư mục public của Cloudinary (cozynest/kyc).
- Giải pháp: Xóa fallback secret khỏi code; chuyển toàn bộ luồng upload ảnh nhạy cảm về backend PrivateObjectStoragePort (đã có sẵn cơ chế AES-256-GCM).

### E. Dữ liệu nhạy cảm và mã hóa
- Bảng user_verifications: id_number_enc và id_number_hash đã được mã hóa AES-256-GCM và blind index HMAC.
- Khuyết điểm: front_image_url và back_image_url đang lưu link ảnh public không mã hóa trên Cloudinary.
- Mật khẩu: Đang dùng BCrypt độ mạnh 12, an toàn cao. Token có hash SHA-256.

### F. Bản đồ và vị trí phòng
- Hiện tại: NewRoomPage chỉ có nhập chữ (tỉnh, phường xã, địa chỉ chi tiết). RoomMap dùng iframe nhúng từ OpenStreetMap.
- Bảng rooms: Đã có cột latitude và longitude nhưng chưa được nạp từ giao diện đăng phòng.
- Giải pháp: Tích hợp thư viện bản đồ Leaflet (OpenStreetMap) để bấm ghim và reverse geocoding bằng Nominatim; bổ sung ô nhập tọa độ thủ công (lat/lng); áp dụng cơ chế làm mờ tọa độ (hiển thị bán kính 100m-200m) cho khách vãng lai.

## 3. Danh sách quyết định cần chủ dự án (Kèm đề xuất mặc định)

1. Nơi lưu ảnh CCCD và tài liệu nhạy cảm:
   - Đề xuất: Lưu vào backend Private Storage (mã hóa AES-256-GCM trên ổ đĩa hoặc MinIO private bucket, xem qua URL có thời hạn), không dùng Cloudinary public.
2. Sử dụng Redis:
   - Đề xuất: Giữ Redis trong Docker cho rate limit và nonce, thêm requirepass và đóng port 6380 ra ngoài host.
3. Thuật toán mã hóa mật khẩu:
   - Đề xuất: Giữ nguyên BCrypt 12 (chuẩn bảo mật công nghiệp, tương thích cao).
4. Nhà cung cấp bản đồ:
   - Đề xuất: Leaflet + OpenStreetMap + Nominatim (miễn phí, không cần API key, sẵn sàng offline/self-host).
5. Chế độ bảo vệ vị trí phòng:
   - Đề xuất: Hiển thị vùng tròn bán kính 200m cho khách chưa đặt cọc/chưa có hẹn; chỉ hiện vị trí chính xác sau khi hẹn được chấp thuận hoặc đã cọc.
6. Thời gian lưu trữ ảnh CCCD:
   - Đề xuất: Xóa file ảnh vật lý sau 30 ngày kể từ khi duyệt eKYC (chỉ giữ lại hash và thông tin mã hóa để tra cứu).
