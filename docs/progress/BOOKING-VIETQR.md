# TIẾN ĐỘ THỰC HIỆN: [BOOKING-VIETQR-FULL]

> Nhánh làm việc: `feat/booking-vietqr-full` (tách từ `feat/auth-google-full` chứa BFF và Sprint 3)
> Thời gian bắt đầu: 2026-11-13
> Trạng thái hiện tại: **PHA 0 - CHẨN ĐOÁN HOÀN TẤT (CHƯA SỬA MÃ)**

---

## BẢNG TỔNG HỢP CHẨN ĐOÁN PHA 0 (KIỂM CHỨNG TRÌNH DUYỆT THẬT)

| Bước / Thành phần | Kết quả | Nguyên nhân gốc | File liên quan | Cách sửa dự kiến (Pha 1–3) |
|---|---|---|---|---|
| **1. Đăng nhập Tenant** (`/vi/login`) | **CHẠY ĐƯỢC** [CHẠY LẠI HÔM NAY] | Xác thực thành công tài khoản `tenant@cozynest.vn`, nhận JWT HttpOnly và cookie `cozynest_role=TENANT`. | `src/app/(auth)/login/page.tsx`, `src/app/api/auth/login/route.ts` | Giữ nguyên luồng BFF an toàn. |
| **2. Xem chi tiết phòng** (`/rooms/[slug]`) | **CHẠY ĐƯỢC MỘT PHẦN** [CHẠY LẠI HÔM NAY] | Dữ liệu phòng hiển thị đầy đủ. Tuy nhiên ảnh avatar chủ trọ bị lỗi 500 do `next.config.ts` rewrite về `localhost:8080` trong container thay vì `api-gateway`. | `next.config.ts`, `src/presentation/components/rooms/RoomSidebar.tsx` | Đổi rewrite sang biến `INTERNAL_API_URL` / `api-gateway`. |
| **3. Đặt lịch xem phòng** (Appointment) | **CHẠY ĐƯỢC** [CHẠY LẠI HÔM NAY] | Form modal mở, nhập ngày giờ gửi về `POST /api/v1/tenant/appointments` thành công (201 Created), hiển thị trong danh sách cuộc hẹn. | `RoomBookingModal.tsx`, `TenantAppointmentController.java` | Hoàn thiện thêm thông báo và đồng bộ thời gian. |
| **4. Gửi yêu cầu đặt thuê** (Rental Request) | **HỎNG (400 BAD_REQUEST)** [CHẠY LẠI HÔM NAY] | `cozynest_booking` gọi sang `identity-service` qua `HttpIdentityVerificationAdapter` dùng fallback `http://localhost:8081` (thiếu mapping `IDENTITY_SERVICE_URL`). Kết nối bị từ chối (`ECONNREFUSED`), CircuitBreaker mở, trả về lỗi: *"Vui long xac thuc so dien thoai truoc khi gui yeu cau thue phong"*. | `HttpIdentityVerificationAdapter.java`, `booking-service/application.yml`, `docker-compose.yml` | Ánh xạ biến `app.identity-service.url=${IDENTITY_SERVICE_URL:http://identity-service:8081}` và cấu hình bypass dev an toàn. |
| **5. Kiểm tra phòng trống & giữ phòng** | **HỎNG NGẦM** [ĐỌC TỪ MÃ] | `HttpBookingListingAdapter` cấu hình gọi `localhost:8082` thay vì `http://listing-service:8082`. Khi gọi thật sẽ fail và rơi vào fallback, không khóa giữ phòng thật được. | `HttpBookingListingAdapter.java`, `application.yml` | Ánh xạ `app.listing-service.url=${LISTING_SERVICE_URL:http://listing-service:8082}`. |
| **6. Đăng ký giữ cọc sang Billing** | **CHƯA CÓ (STUB)** [ĐỌC TỪ MÃ] | `HttpBookingBillingAdapter` chỉ in `log.info`, chưa gọi API `billing-service` để khởi tạo `EscrowHold` hay giao dịch thanh toán. | `HttpBookingBillingAdapter.java` | Triển khai client gọi REST nội bộ sang `billing-service` với timeout và retry. |
| **7. Định dạng mã đơn & mã tham chiếu** | **THIẾU** [ĐỌC TỪ MÃ] | Hiện chỉ dùng UUID hoặc số tự tăng, chưa có định dạng `TOAM-YYMMDD-XXXXXX` (Base32 an toàn, duy nhất) và mã chuyển khoản ngắn `TOAMxxxx`. | `RentalRequest.java`, `V1__...sql` | Bổ sung cột `booking_code` (UNIQUE index) và `payment_reference`. |
| **8. Máy trạng thái đơn thuê (Booking State Machine)** | **THIẾU TRẠNG THÁI** [ĐỌC TỪ MÃ] | Hiện tại chỉ có `PENDING -> APPROVED / REJECTED -> COMPLETED`. Thiếu `AWAITING_PAYMENT`, `PAID_PENDING_CONFIRM`, `CONFIRMED`, `CHECKED_IN`, `EXPIRED`. | `RentalRequest.java`, `BookingStatus.java` | Nâng cấp toàn diện Enum và State Machine chuyển đổi có kiểm tra chặt chẽ. |
| **9. Tiết lộ thông tin theo giai đoạn** | **CHƯA CÓ** [ĐỌC TỪ MÃ] | API trả nguyên địa chỉ nhà và số điện thoại chủ trọ ngay cả khi đơn mới gửi/chưa duyệt; người không liên quan chưa bị chặn 404 IDOR đầy đủ. | `RentalRequestService.java`, `RentalRequestDto.java` | Che số nhà và SĐT trước `CONFIRMED`. Trả 404 cho IDOR. |
| **10. Thanh toán VietQR động** | **CHƯA CÓ** [ĐỌC TỪ MÃ] | Chưa có bộ sinh payload EMVCo / VietQR NAPAS 247 (Tag 38, Tag 54, Tag 62, CRC16), chưa có trang thanh toán đếm ngược và xác nhận nộp tiền. | Backend billing / booking, Frontend payments | Xây dựng engine sinh payload chuẩn EMVCo và giao diện thanh toán có QR động. |

---

## DANH SÁCH ẢNH CHỤP KIỂM CHỨNG PHA 0

1. `pha0-01-tenant-login.png`: Đăng nhập Tenant thành công.
2. `pha0-02-room-detail.png`: Chi tiết phòng với nút "Đặt lịch xem phòng" và "Gửi yêu cầu thuê".
3. `pha0-03-appointment-modal.png`: Modal đặt lịch xem phòng mở thành công.
4. `pha0-04-appointment-submitted.png`: Đặt lịch xem phòng thành công, toast xanh hiển thị.
5. `pha0-05-rental-modal.png`: Modal gửi yêu cầu thuê phòng mở thành công.
6. `pha0-06-rental-submitted.png`: Gửi yêu cầu thuê phòng thất bại, lỗi 400 hiển thị: *"Vui long xac thuc so dien thoai truoc khi gui yeu cau thue phong"*.
7. `pha0-07-tenant-appointments.png`: Danh sách cuộc hẹn của Tenant hiển thị lịch hẹn vừa tạo.
