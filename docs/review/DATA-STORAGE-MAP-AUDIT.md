# Báo cáo kiểm định: Lưu trữ dữ liệu, Docker, Redis, Ảnh, Mã hóa và Bản đồ

- Nhiệm vụ: [DATA-STORAGE-MAP-AUDIT] - Pha 0: Báo cáo kiểm định toàn diện
- Ngày thực hiện: 2026-11-19
- Nhánh: feat/data-storage-map
- Trạng thái: DONE-VERIFIED (Pha 0 - Chỉ đọc, không sửa mã, dừng chờ phê duyệt)
- Nguồn kiểm tra: [CHẠY LẠI HÔM NAY] và [ĐỌC TỪ MÃ]

## 1. Quyết định đã chốt từ Chủ dự án
1. Cloudinary: Đang dùng và giữ lại cho ảnh phòng, ảnh đại diện công khai (ký qua backend, bỏ EXIF, lưu public_id). Kiểm tra và tắt unsigned preset.
2. VPS, tên miền, TLS: Đánh dấu [HUMAN-HOÃN]. Không chặn luồng kỹ thuật; chuẩn bị sẵn file cấu hình mẫu (reverse proxy, HSTS, secure cookie).
3. Pháp lý dữ liệu cá nhân (CCCD, sinh trắc học): Đánh dấu [HUMAN-PHÁP LÝ]. Hiện chỉ dùng dữ liệu mẫu; chủ dự án làm việc với luật sư trước khi tiếp nhận dữ liệu người dùng thật.
4. Thứ tự triển khai: Hoàn thành bảo mật và mã hóa (Pha 3) đạt DONE-VERIFIED trước, sau đó chủ dự án mới nhập dữ liệu thật để thử nghiệm.
5. Biến môi trường: Giữ nguyên .env hiện tại cho dev/demo. Môi trường real dùng .env.real riêng biệt (gitignore) với bí mật sinh từ scripts/gen-real-secrets.ps1.

## 2. Bảng tổng hợp hiện trạng và khuyến nghị

| Hạng mục | Hiện trạng | Khoảng cách | Rủi ro | Khuyến nghị |
|---|---|---|---|---|
| A. Seed & Data thật | Flyway nạp chung db/seed và db/migration; roles nằm trong R__01 | Chưa tách profile real; thiếu role cơ bản nếu bỏ seed | Mất dữ liệu demo; hệ thống không thể chạy ở môi trường thật | Tạo migration V__ bổ sung role; tạo profile real độc lập |
| B. Docker & Độ bền | Dùng named volumes; toàn bộ cổng nội bộ expose ra host 0.0.0.0 | Chưa có .env.real và compose real riêng; cổng DB/MQ/Redis lộ ra ngoài | Xóa nhầm volume demo khi down -v; lộ hệ thống qua port công khai | Tạo docker-compose.real.yml; bind localhost hoặc chỉ mở port 80/443 |
| C. Đánh giá Redis | Dùng cho Google nonce và rate limiter; có fallback ConcurrentHashMap | Redis không đặt mật khẩu; cổng 6380 mở ra ngoài host | Lộ Redis không xác thực; tốn tài nguyên trên môi trường nhỏ | Thêm requirepass, đóng port host; duy trì fallback in-memory |
| D. Ảnh & Cloudinary | Route upload /api/upload/cloudinary không auth; hardcode key dự phòng | Ảnh KYC đẩy lên Cloudinary public; chưa dùng signed upload chuẩn | Lộ thông tin cá nhân (CCCD) ra internet; lộ api secret | Chuyển CCCD sang Private Storage BE; Cloudinary chỉ cho ảnh phòng |
| E. Mã hóa nhạy cảm | Số CCCD đã mã hóa AES-256-GCM + blind index HMAC; ảnh CCCD chưa mã hóa | Ảnh CCCD lưu URL public; chưa có cơ chế envelope encryption | Lộ ảnh CCCD hai mặt; vi phạm an toàn dữ liệu cá nhân | Dùng AES-256-GCM lưu cục bộ/MinIO private; xóa ảnh sau 30 ngày |
| F. Bản đồ & Vị trí | Đăng phòng chỉ có ô text địa chỉ; bản đồ dùng iframe OpenStreetMap | Thiếu ghim bản đồ tương tác; thiếu nhập tọa độ; lộ tọa độ thật | Khách vãng lai biết chính xác vị trí trước khi liên hệ/cọc | Tích hợp Leaflet + Nominatim; hỗ trợ nhập tọa độ; làm mờ 200m |

## 3. Chi tiết kiểm định mã nguồn

### A. Dữ liệu seed và dữ liệu thật
- Cấu hình Flyway: Các microservice (identity, listing, booking, billing, communication) đều chia thư mục db/migration (V__) và db/seed (R__).
- Phát hiện rủi ro: Bảng roles trong identity-service được INSERT tại R__01_seed_demo_users.sql. Nếu khởi động môi trường production/real không nạp seed, bảng roles sẽ rỗng làm sập luồng đăng ký/đăng nhập.
- Danh mục hành chính: V19 nạp chuẩn 34 tỉnh thành và 3321 phường xã theo mô hình 2 cấp.

### B. Docker và độ bền dữ liệu
- Named volumes: cozynest_mysql_data, cozynest_redis_data, cozynest_rabbitmq_data, cozynest_minio_data giữ nguyên dữ liệu khi chạy docker compose down.
- Nguy cơ: Lệnh docker compose down -v xóa sạch volume demo. Toàn bộ cổng cơ sở dữ liệu (5555), Redis (6380), RabbitMQ (5672/15672), MinIO (9000/9001) đang mở ra 0.0.0.0 trên host.
- Khắc phục: Khởi tạo docker-compose.real.yml với COMPOSE_PROJECT_NAME riêng (cozynest_real), volume riêng và không mở cổng nội bộ ra host.

### C. Đánh giá sử dụng Redis
- Vị trí sử dụng: identity-service (GoogleNonceManager, RedisRateLimiterAdapter), listing-service (UploadRateLimiter).
- Độ chịu lỗi: Cả hai service đều có fallback in-memory (ConcurrentHashMap) khi Redis mất kết nối.
- Khuyến nghị: VẪN NÊN DÙNG Redis để đồng bộ rate-limit chống tấn công brute-force, chống replay nonce Google OAuth, và hỗ trợ thu hồi token khi scale đa instance. Cần thêm requirepass và đóng port 6380.

### D. Ảnh và Cloudinary (Kèm kiểm tra Unsigned Preset)
- Kiểm tra Unsigned Preset: Mã nguồn KHÔNG sử dụng preset không ký (unsigned preset) từ trình duyệt. Mọi thao tác tải lên đều qua SDK máy chủ.
- Lỗ hổng bảo mật: File accommodation-web/src/app/api/upload/cloudinary/route.ts hardcode api_key và api_secret dự phòng, không kiểm tra quyền người dùng, mặc định upload ảnh vào thư mục public cozynest/kyc.
- Khắc phục: Xóa bỏ route upload public này; ảnh phòng chuyển sang luồng backend có chữ ký; kiểm tra trên Cloudinary Dashboard để vô hiệu hóa các upload preset unsigned.

### E. Dữ liệu nhạy cảm và mã hóa
- user_verifications: Cột id_number_enc đã mã hóa AES-256-GCM, id_number_hash dùng HMAC SHA-256 blind index. Tuy nhiên front_image_url và back_image_url trỏ tới Cloudinary public.
- Mật khẩu: Đang dùng BCrypt độ mạnh 12 (đáp ứng tiêu chuẩn OWASP, băm ổn định trong 200-300ms).
- Khuyến nghị: Chuyển toàn bộ ảnh CCCD và selfie sinh trắc học sang PrivateObjectStoragePort mã hóa AES-256-GCM lưu cục bộ/MinIO private.

### F. Bản đồ và vị trí phòng
- Giao diện hiện tại: NewRoomPage chỉ có ô nhập tỉnh thành, phường xã và địa chỉ chi tiết; chưa có bản đồ tương tác. RoomMap hiển thị qua thẻ iframe tĩnh.
- Cơ sở dữ liệu: Bảng rooms đã có sẵn 2 cột latitude và longitude kiểu DOUBLE nhưng chưa được nạp từ giao diện.
- Khắc phục: Bổ sung Leaflet + Nominatim cho phép ghim bản đồ hoặc nhập tọa độ thủ công; áp dụng cơ chế làm mờ vị trí bán kính 200m cho khách công khai.

## 4. Khuyến nghị cho các mục còn mở
1. Nơi lưu ảnh nhạy cảm: Lưu trong Backend Private Storage (mã hóa AES-256-GCM trên volume Docker hoặc MinIO private bucket), chỉ xem qua stream endpoint có xác thực JWT và kiểm tra quyền.
2. Redis: Giữ lại phục vụ rate limiting, OAuth nonce và token revocation; bật requirepass và chặn cổng 6380 ra ngoài host.
3. Thuật toán băm mật khẩu: Giữ nguyên BCrypt 12, chưa cần chuyển Argon2id để tránh phụ thuộc thư viện C/JNI trên máy Windows/Docker và tránh rủi ro di chuyển mật khẩu cũ.
4. Nhà cung cấp bản đồ & Geocoding: Dùng Leaflet kết hợp OpenStreetMap gạch bản đồ và Nominatim geocoding (hoàn toàn miễn phí, không cần thẻ tín dụng hay API key).
5. Bán kính làm mờ vị trí công khai: Áp dụng hình tròn ước lượng bán kính 200m cho khách vãng lai; chỉ mở khóa địa chỉ cụ thể khi lịch hẹn được duyệt hoặc hợp đồng cọc được thanh toán.
6. Thời hạn lưu ảnh CCCD và sinh trắc: Tự động xóa file ảnh vật lý sau 30 ngày kể từ khi xét duyệt eKYC hoàn tất; lưu giữ vĩnh viễn chuỗi hash và dữ liệu mã hóa để đối soát.

## 5. Danh mục các mục phụ thuộc con người (HUMAN)
- [HUMAN-HOÃN] Cấu hình VPS, chứng chỉ TLS/SSL, tên miền thật, reverse proxy (chỉ viết tài liệu và file mẫu).
- [HUMAN-PHÁP LÝ] Rà soát quy định bảo vệ dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP) cho CCCD và sinh trắc học với luật sư trước khi đưa vào vận hành thật.
- [HUMAN-CLOUDINARY] Kiểm tra trang quản trị Cloudinary Dashboard (Settings > Upload > Upload presets) để tắt các unsigned preset nếu có.
