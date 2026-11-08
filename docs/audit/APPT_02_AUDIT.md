# AUDIT & VERIFICATION REPORT: [APPT-02]
**Thẻ:** [APPT-02] Dashboard duyệt lịch hẹn cho chủ trọ (Chấp nhận, Hủy bỏ kèm lý do)  
**Trạng thái ban đầu:** Doing, 0/7 (Coi là CHƯA XÁC NHẬN)  
**Mục tiêu:** Kiểm chứng, sửa lỗi, hoàn thiện toàn bộ các hạng mục, mỗi lỗi một commit nhỏ (Conventional Commits, tiếng Anh), KHÔNG push, KHÔNG force.

---

## 1. ÁNH XẠ 8 MỤC NỘI DUNG (a) - (g) VÀO 16 HẠNG MỤC CHI TIẾT

| Mục chữ cái | Nội dung tóm lược | Ánh xạ vào các hạng mục số bên dưới |
|:---:|:---|:---|
| **(a)** | Danh sách lịch hẹn | **Mục 8** (Truy vấn, lọc khoảng ngày, trạng thái, phòng, sắp xếp, phân trang, index), **Mục 9** (Dashboard FE danh sách, đồng bộ URL) |
| **(b)** | Chấp nhận lịch hẹn | **Mục 1** (Máy trạng thái domain: PENDING -> CONFIRMED), **Mục 10** (Modal Chấp nhận) |
| **(c)** | Từ chối / Hủy kèm lý do bắt buộc | **Mục 2** (Lý do bắt buộc khi DECLINED và CANCELLED: mã chuẩn + ghi chú 10..500 ký tự), **Mục 10** (Modal Từ chối/Hủy) |
| **(d)** | Chống trùng giờ | **Mục 3** (Hai confirm đồng thời 2 lịch trùng giờ cùng phòng/chủ trọ: đúng 1 thành công, 1 trả 409) |
| **(e)** | Thông báo người thuê | **Mục 6** (Outbox: phát sự kiện Confirmed/Declined/Cancelled, communication consumer tạo thông báo vi/en kèm lý do, inbox idempotent), **Mục 14** (Phía người thuê `/tenant/appointments` chỉ đọc thấy trạng thái và lý do mới) |
| **(f)** | Dashboard nhiều chế độ xem | **Mục 9** (Dashboard FE: 6 tab trạng thái, 4 chế độ xem: danh sách, theo phòng, lịch tuần, lịch tháng; Asia/Ho_Chi_Minh), **Mục 10** (3 modal) |
| **(g)** | Kiểm thử toàn diện | **Mục 1** (Unit test máy trạng thái), **Mục 3** (Test concurrency Testcontainers), **Mục 4** (Test quét mask PII phone/email), **Mục 7** (Ma trận quyền 5 vai trò + IDOR), **Mục 12** (ArchUnit test + commit cố ý vi phạm rồi revert), **Mục 13** (Testcontainers MySQL 9.7 migration & seed), **Mục 15** (E2E), **Mục 16** (Ảnh chụp & tự phê bình) |

---

## 2. BẢNG CHECKLIST 16 HẠNG MỤC ĐÁNH SỐ (BƯỚC 0)

| # | Hạng mục kiểm tra | Hiện trạng đối chiếu (Grep, đọc code, chạy lệnh) | Kế hoạch & Test bổ sung | Trạng thái |
|---|---|---|---|---|
| **1** | Máy trạng thái ở domain thuần: PENDING -> CONFIRMED \| DECLINED \| CANCELLED \| EXPIRED; CONFIRMED -> CANCELLED \| COMPLETED \| NO_SHOW; trạng thái cuối không đổi; chuyển sai -> 409 mã riêng; unit test mọi cặp | `ViewingAppointment.java`, `AppointmentStatus.java` cần đối chiếu trạng thái cuối và mã lỗi 409 | Viết unit test ma trận toàn bộ 7x7 cặp chuyển trạng thái | Đang kiểm chứng |
| **2** | Lý do bắt buộc khi DECLINED và CANCELLED: mã chuẩn (trùng lịch, phòng đã cho thuê, không phù hợp, khác) + ghi chú 10..500 ký tự (bắt buộc với "khác"); kiểm ở server; lưu người thực hiện và thời điểm; người thuê thấy lý do | Kiểm tra `CancelReasonCode`, validation bean, entity history | Test server validation khi thiếu ghi chú với "khác", test lưu `cancelled_by` và `cancelled_at` | Đang kiểm chứng |
| **3** | Hai confirm đồng thời cho 2 lịch trùng giờ cùng phòng/chủ trọ: đúng 1 thành công, 1 trả 409 (test hai luồng thật bằng Testcontainers) | Kiểm tra pessimistic/optimistic lock và conflict check trong `LandlordAppointmentService` | Viết integration test 2 thread song song tranh chấp slot giờ | Đang kiểm chứng |
| **4** | Số điện thoại và email người thuê bị che ở MỌI nơi cho tới khi CONFIRMED: danh sách, chi tiết, lịch sử trạng thái, payload outbox, log, thông báo lỗi, ví dụ OpenAPI. Test tự động quét từng nơi | `PhoneMasker.java` đã có, cần kiểm tra email masking và quét toàn bộ DTO/Event/Log | Viết test tự động assert không lộ plaintext phone/email ở PENDING/DECLINED/EXPIRED | Đang kiểm chứng |
| **5** | Hết hạn tự động: PENDING quá hạn (cấu hình, mặc định 48 giờ hoặc quá giờ hẹn) -> EXPIRED bằng scheduler idempotent (clock giả, chạy hai lần không đổi); nhiều instance không xử lý trùng (khóa phân tán hoặc UPDATE có điều kiện) | `AppointmentExpirationScheduler.java` cần kiểm tra điều kiện UPDATE và idempotent | Viết test scheduler với Mock Clock, chạy 2 lần liên tiếp không lỗi | Đang kiểm chứng |
| **6** | Outbox: mỗi chuyển trạng thái phát AppointmentConfirmed/Declined/Cancelled; consumer ở communication-service tạo thông báo vi/en cho người thuê kèm lý do; idempotent bằng inbox (test gửi trùng) | `AppointmentEventOutbox` trong `booking-service`, `communication-service` consumer | Test outbox insertion và consumer inbox idempotency khi duplicate messageId | Đang kiểm chứng |
| **7** | Ma trận 200/401/403/404 đủ 5 vai trò mọi endpoint; chủ trọ B không đụng lịch chủ trọ A (404); TENANT gọi endpoint chủ trọ -> 403 | `LandlordAppointmentSecurityMatrixTest.java` | Chạy và mở rộng test ma trận 5 roles x các action (list, detail, confirm, cancel, complete) | Đang kiểm chứng |
| **8** | Truy vấn: lọc khoảng ngày (from, to), trạng thái, phòng; sắp xếp; phân trang; chỉ mục phù hợp (dán EXPLAIN); validate from <= to và tối đa 92 ngày | `AppointmentSpecificationBuilder.java`, DTO validation | Kiểm tra validate 92 ngày, chạy lệnh EXPLAIN query trong MySQL | Đang kiểm chứng |
| **9** | Dashboard FE: 6 tab theo trạng thái (đếm đúng), chế độ xem danh sách, theo phòng, lịch tuần, lịch tháng; đúng múi giờ Asia/Ho_Chi_Minh (test ranh giới nửa đêm); bộ lọc đồng bộ URL | `useLandlordAppointments.ts`, `LandlordAppointmentsPage.tsx` | Kiểm tra timezone, 4 view modes, URL sync | Đang kiểm chứng |
| **10** | 3 modal: Chấp nhận (cảnh báo nếu trùng lịch khác), Từ chối/Hủy (form lý do có kiểm tra), Hoàn tất/Vắng mặt; danh sách cập nhật ngay, có hoàn tác khi lỗi; chặn bấm đúp; nút khóa theo trạng thái | Modal components trong `presentation/components/landlord/` | Kiểm tra double click lock, optimistic update & rollback | Đang kiểm chứng |
| **11** | Seed: lịch hẹn đủ 7 trạng thái cho chủ trọ demo; lịch tương lai luôn nằm ở tương lai khi chạy (bộ làm mới ngày CHỈ ở dev, chạy lại nhiều lần không trùng, không lỗi); cấm R__ + CURRENT_DATE kiểu giả "động" | `R__02_seed_demo_viewing_appointments.sql` | Kiểm tra đủ 7 trạng thái, kiểm tra bộ làm mới ngày dev | Đang kiểm chứng |
| **12** | Tách JPA khỏi domain (entity JPA riêng + mapper); siết ArchUnit (domain/application không import jakarta.persistence và org.springframework.*); chứng minh luật có tác dụng bằng một commit thử cố ý vi phạm rồi revert, dán kết quả test thất bại | Rà soát `domain/model` và `application` của `booking-service`, sửa tách nếu đang dính JPA annotations | Thêm test ArchUnit siết chặt, làm commit thử nghiệm vi phạm -> dán output -> revert | Đang kiểm chứng |
| **13** | Migration (V2...) và seed kiểm bằng Testcontainers MySQL 9.7; không sửa migration cũ | `V1`, `V2` migration files | Viết test Testcontainers chạy migration V1, V2 và verify schema | Đang kiểm chứng |
| **14** | Phía người thuê: /tenant/appointments (chỉ đọc) thấy trạng thái và lý do mới. Việc tenant TẠO lịch KHÔNG thuộc thẻ này: dùng fixture/seed trong test, không thêm tính năng đặt lịch; nút đặt lịch ở trang phòng vẫn DEMO-STATIC | `TenantAppointmentController.java`, `/tenant/appointments` page | Test endpoint GET của tenant, đảm bảo nút đặt lịch trang phòng giữ DEMO-STATIC | Đang kiểm chứng |
| **15** | E2E (backend thật): chủ trọ confirm -> người thuê thấy CONFIRMED và có thông báo; decline thiếu lý do bị chặn; decline có lý do -> người thuê thấy lý do; confirm lịch trùng -> thông báo xung đột | Kiểm tra luồng E2E qua Gateway port 8080 | Script/test E2E gọi qua gateway kiểm tra chéo 2 bên | Đang kiểm chứng |
| **16** | Ảnh chụp 4 chế độ xem + 3 modal + rỗng/lỗi, 1440 và 390, light/dark, vi/en; tự phê bình 3 điểm CHƯA TỐT mỗi ảnh và sửa | Chụp màn hình bằng script/Playwright | Đánh giá 3 điểm chưa tốt và hoàn thiện | Đang kiểm chứng |
