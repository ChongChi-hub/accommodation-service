# Tài Liệu Yêu Cầu Kỹ Thuật (SRS) - Nền Tảng Thuê Trọ "Tổ Ấm"

## 1. Giới thiệu & Bối cảnh
"Tổ Ấm" là nền tảng số kết nối giữa người có nhu cầu tìm phòng trọ/nhà ở và các chủ trọ tại các đô thị lớn ở Việt Nam (Hà Nội, TP.HCM, Đà Nẵng, v.v.). Mục tiêu cốt lõi là mang đến trải nghiệm minh bạch, an toàn, tiện lợi với quy trình số hóa từ khâu tìm kiếm, xem phòng, ký hợp đồng điện tử cho đến quản lý điện nước và thanh toán.

---

## 2. Các Vai Trò Trong Hệ Thống (User Roles)
1. **Khách vãng lai (Guest):** Tìm kiếm phòng trọ, lọc đa tiêu chí, xem chi tiết phòng, xem hồ sơ chủ trọ, liên hệ qua form công khai.
2. **Người thuê (TENANT):** Đăng ký/đăng nhập, quản lý phòng yêu thích, đặt lịch hẹn xem phòng, gửi yêu cầu thuê, ký hợp đồng điện tử, nhận thông báo hóa đơn, thanh toán, đánh giá phòng, gửi báo cáo vi phạm.
3. **Chủ trọ (LANDLORD):** Đăng tin phòng trọ (kèm ảnh, tiện nghi, biểu phí đồng hồ), duyệt/từ chối lịch hẹn và yêu cầu thuê, quản lý hợp đồng, nhập chỉ số điện nước định kỳ, xuất hóa đơn, phản hồi đánh giá, xác minh danh tính KYC.
4. **Kiểm duyệt viên (MODERATOR):** Kiểm duyệt phòng trọ mới, rà soát hồ sơ KYC (CCCD), xử lý khiếu nại/báo cáo vi phạm (reports).
5. **Quản trị viên (ADMIN):** Toàn quyền hệ thống, xem bảng điều khiển chỉ số doanh thu & tăng trưởng, phân quyền người dùng, quản trị danh mục hệ thống (địa giới hành chính, tiện nghi, loại phòng), xem audit logs.

---

## 3. Yêu Cầu Chức Năng (Functional Requirements)

### 3.1. Xác thực & Quản lý người dùng (Identity Service)
* Đăng ký tài khoản (hỗ trợ phân vai trò ban đầu: Người thuê hoặc Chủ trọ).
* Đăng nhập bảo mật (BCrypt hệ số 12), JWT access token (ngắn hạn) + Refresh token (xoay vòng).
* Quản lý hồ sơ cá nhân và xác minh danh tính (KYC) với ảnh CCCD 2 mặt.
* Khóa tạm tài khoản nếu nhập sai mật khẩu quá 5 lần liên tiếp.

### 3.2. Quản lý Tin đăng & Tìm kiếm (Listing Service)
* Quản lý danh mục địa giới hành chính chuẩn 2 cấp theo Nghị quyết 202/2025/QH15 (34 đơn vị cấp Tỉnh/Thành -> Xã/Phường, bãi bỏ cấp trung gian quận/huyện).
* Định dạng địa chỉ chuẩn hóa: "Số nhà, đường, Phường/Xã, Tỉnh/Thành phố".
* Tìm kiếm phòng trọ với bộ lọc đa tiêu chí: khoảng giá thuê, diện tích, vị trí địa lý 2 cấp, tiện nghi (wifi, máy lạnh, máy giặt...), nội quy (cho nuôi thú cưng, cho nấu ăn, không chung chủ, giờ tự do).
* Đánh giá sao (1-5) và bình luận trải nghiệm thuê trọ.
* Lưu danh sách phòng yêu thích (Bookmarks) và lưu bộ lọc tìm kiếm (Saved Searches).

### 3.3. Đặt lịch & Hợp đồng (Booking Service)
* Lịch hẹn xem phòng (Viewing Appointments): Chọn ngày, giờ, gửi lời nhắn tới chủ trọ.
* Yêu cầu thuê phòng (Rental Requests): Gửi thông tin ngày dọn vào dự kiến, số người ở.
* Hợp đồng điện tử (Digital Contracts): Sinh hợp đồng có điều khoản pháp lý, hỗ trợ hai bên ký điện tử trực tuyến, lưu trữ bản chụp dữ liệu phòng (snapshot) tại thời điểm ký kết.

### 3.4. Chỉ số Điện nước & Hóa đơn (Billing Service)
* Ghi chỉ số điện/nước (Utility Readings) định kỳ hàng tháng kèm ảnh chụp công tơ.
* Tự động sinh hóa đơn (Invoices) với chi tiết: tiền phòng, tiền điện (theo số kWh), tiền nước (theo m3), tiền dịch vụ (rác, wifi, gửi xe).
* Quản lý vòng đời hóa đơn (DRAFT -> ISSUED -> PAID / OVERDUE).

### 3.5. Tương tác & Thông báo (Communication Service)
* Chat trực tiếp thời gian thực giữa người thuê và chủ trọ gắn liền với căn phòng quan tâm.
* Thông báo tức thời (In-app notifications) khi có sự kiện: lịch hẹn mới, hợp đồng được duyệt, hóa đơn đến hạn.

### 3.6. Quản trị & Vận hành (Admin Service)
* Dashboard thống kê tổng quan (phòng mới, người dùng mới, tỷ lệ lấp đầy, doanh thu ước tính).
* Phân hệ kiểm duyệt tin đăng và hồ sơ định danh KYC.
* Xử lý báo cáo vi phạm và ghi nhật ký hoạt động hệ thống (Audit Logs).

---

## 4. Yêu Cầu Phi Chức Năng (Non-Functional Requirements)
1. **Kiến trúc:** Microservices độc lập theo Domain-Driven Design (DDD) cho backend, Clean Architecture cho frontend.
2. **Tính độc lập dữ liệu:** Mỗi service sở hữu một schema MySQL riêng biệt, không dùng khóa ngoại liên service.
3. **Hiệu năng & Bảo mật:** Stateless JWT qua Gateway, CORS chặt chẽ, chống lỗ hổng IDOR, rate limit chống brute-force.
4. **Trải nghiệm người dùng (UX/UI):** Tuân thủ Taste Skill (anti-slop, typography chuẩn Việt ngữ, tỷ lệ màn hình 1-section-per-screen trên hero, micro-interactions tinh tế, responsive hoàn hảo).
5. **Đa ngôn ngữ:** Hỗ trợ đầy đủ tiếng Việt (`vi`) và tiếng Anh (`en`) qua `next-intl`.

