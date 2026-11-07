# Bảng Định Danh Dữ Liệu Mẫu Cố Định (Seed IDs Registry)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Mục đích:** Đảm bảo toàn vẹn tham chiếu và tính nhất quán tuyệt đối giữa các Microservice độc lập.

---

## 1. Người dùng & Tài khoản Demo (`toam_identity`)

| Mã định danh (ID) | Email | Vai trò | Họ và tên | Trạng thái | KYC |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`u-tenant-01`** | `tenant@toam.vn` | TENANT | Nguyễn Văn Thuê | ACTIVE | APPROVED |
| **`u-landlord-01`**| `landlord@toam.vn` | LANDLORD | Trần Thị Chủ Trọ | ACTIVE | APPROVED |
| **`u-mod-01`** | `moderator@toam.vn`| MODERATOR | Phạm Minh Kiểm Duyệt | ACTIVE | APPROVED |
| **`u-admin-01`** | `admin@toam.vn` | ADMIN | Lê Hoàng Quản Trị | ACTIVE | APPROVED |
| `u-tenant-02` | `tenant2@toam.vn` | TENANT | Lê Hoàng Minh | ACTIVE | APPROVED |
| `u-tenant-03` | `tenant3@toam.vn` | TENANT | Vũ Phương Thảo | ACTIVE | APPROVED |
| `u-tenant-04` | `tenant4@toam.vn` | TENANT | Đặng Quốc Hưng | ACTIVE | PENDING |
| `u-tenant-05` | `tenant5@toam.vn` | TENANT | Bùi Thị Thanh Hà | ACTIVE | REJECTED |
| `u-tenant-06` | `tenant6@toam.vn` | TENANT | Đỗ Minh Tuấn | ACTIVE | APPROVED |
| `u-tenant-07` | `tenant7@toam.vn` | TENANT | Hoàng Thùy Linh | PENDING | PENDING |
| `u-tenant-08` | `tenant8@toam.vn` | TENANT | Ngô Quang Hải | LOCKED | APPROVED |
| `u-landlord-02` | `landlord2@toam.vn`| LANDLORD | Trần Minh Tuấn | ACTIVE | APPROVED |
| `u-landlord-03` | `landlord3@toam.vn`| LANDLORD | Hoàng Thị Mai Lan | ACTIVE | APPROVED |
| `u-landlord-04` | `landlord4@toam.vn`| LANDLORD | Phan Thanh Bình | ACTIVE | PENDING |
| `u-landlord-05` | `landlord5@toam.vn`| LANDLORD, TENANT | Nguyễn Thị Ngọc Ánh | ACTIVE | APPROVED |
| `u-landlord-06` | `landlord6@toam.vn`| LANDLORD, TENANT | Vũ Đình Trọng | LOCKED | APPROVED |
| `u-mod-02` | `mod2@toam.vn` | MODERATOR | Đoàn Thu Trang | ACTIVE | APPROVED |
| `u-mod-03` | `mod3@toam.vn` | MODERATOR | Lương Gia Bảo | ACTIVE | APPROVED |
| `u-admin-02` | `admin2@toam.vn` | ADMIN | Trịnh Phương Dung | ACTIVE | APPROVED |

*Ghi chú Mật khẩu:* Mọi tài khoản demo sử dụng mật khẩu: `password123` (BCrypt cost 12 hash: `$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a`).

---

## 2. Phòng Trọ Tiêu Biểu (`toam_listing`)

| Mã số (ID) | UUID Tham chiếu | Tiêu đề tóm tắt | Chủ trọ sở hữu | Trạng thái tin đăng |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `room-00000001-0000-0000-0000-000000000001` | Phòng studio ban công thoáng Cầu Giấy | `u-landlord-01` | `RENTED` |
| **2** | `room-00000001-0000-0000-0000-000000000002` | Căn hộ dịch vụ 1N1K trung tâm Quận 1 | `u-landlord-01` | `RENTED` |
| **3** | `room-00000001-0000-0000-0000-000000000003` | Phòng trọ gác lửng giá rẻ Gò Vấp | `u-landlord-02` | `PUBLISHED` |
| **4** | `room-00000001-0000-0000-0000-000000000004` | Căn hộ mini đầy đủ nội thất Thanh Xuân | `u-landlord-02` | `PUBLISHED` |
| **5** | `room-00000001-0000-0000-0000-000000000005` | Phòng khép kín Bách Khoa Hai Bà Trưng | `u-landlord-03` | `PUBLISHED` |
| **6** | `room-00000001-0000-0000-0000-000000000006` | Chung cư mini cao cấp Đống Đa | `u-landlord-03` | `PUBLISHED` |
| **7** | `room-00000001-0000-0000-0000-000000000007` | Phòng trọ sinh viên gần ĐH Quốc Gia HN | `u-landlord-04` | `PENDING_REVIEW` |
| **8** | `room-00000001-0000-0000-0000-000000000008` | Studio view Landmark 81 Bình Thạnh | `u-landlord-01` | `RENTED` |
| ... | ... | *(Tổng cộng 75 phòng phủ khắp 5 thành phố lớn)* | ... | ... |

---

## 3. Quy Trình Thuê & Hợp Đồng (`toam_booking`)

| ID Hợp đồng | Mã HĐ | Người thuê | Chủ trọ | Phòng ID | Giá thuê | Tiền cọc | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ct-00000001` | `HD-2026-088` | `u-tenant-01` | `u-landlord-01` | `room-00000001-0000-0000-0000-000000000001` | 4,500,000 | 4,500,000 | `ACTIVE` |
| `ct-00000002` | `HD-2026-052` | `u-tenant-02` | `u-landlord-01` | `room-00000001-0000-0000-0000-000000000002` | 7,200,000 | 7,200,000 | `ACTIVE` |
| `ct-00000003` | `HD-2026-015` | `u-tenant-03` | `u-landlord-02` | `room-00000001-0000-0000-0000-000000000003` | 3,200,000 | 3,200,000 | `PENDING_SIGN` |
| `ct-00000004` | `HD-2025-099` | `u-tenant-06` | `u-landlord-03` | `room-00000001-0000-0000-0000-000000000006` | 5,500,000 | 5,500,000 | `TERMINATED` |

---

## 4. Hóa Đơn & Thanh Toán (`toam_billing`)

| ID Hóa đơn | Mã HĐ | Hợp đồng | Kỳ tháng | Tổng tiền | Đã thanh toán | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `inv-00000001` | `INV-2026-11-01` | `ct-00000001` | `2026-11-01` | 4,875,000 | 4,875,000 | `PAID` |
| `inv-00000002` | `INV-2026-10-01` | `ct-00000001` | `2026-10-01` | 4,820,000 | 4,820,000 | `PAID` |
| `inv-00000003` | `INV-2026-11-02` | `ct-00000002` | `2026-11-01` | 7,650,000 | 0 | `OVERDUE` |
| `inv-00000004` | `INV-2026-11-03` | `ct-00000003` | `2026-11-01` | 3,450,000 | 1,500,000 | `PARTIALLY_PAID` |

---

## 5. Phiếu Hỗ Trợ, Tranh Chấp & Báo Cáo

- **Phiếu hỗ trợ (`toam_communication`):**
  - `ticket-1001`: `TK-1001` (`u-tenant-01` gửi - `OPEN`)
  - `ticket-1002`: `TK-1002` (`u-landlord-01` gửi - `IN_PROGRESS`)
  - `ticket-1003`: `TK-1003` (`u-tenant-02` gửi - `RESOLVED`)
- **Vụ tranh chấp (`toam_admin`):**
  - `disp-00000001`: `DISP-501` (`ct-00000001` - `DEPOSIT_RETURN` - `MEDIATING`)
  - `disp-00000002`: `DISP-502` (`ct-00000002` - `DAMAGE_COMPENSATION` - `PENDING`)
- **Yêu cầu bảo trì (`toam_booking`):**
  - `maint-00000001`: `MN-201` (`u-tenant-01` / `room-00000001-0000-0000-0000-000000000001` - `HIGH` - `PENDING`)
  - `maint-00000002`: `MN-202` (`u-tenant-02` / `room-00000001-0000-0000-0000-000000000002` - `MEDIUM` - `IN_PROGRESS`)
  - `maint-00000003`: `MN-203` (`u-tenant-01` / `room-00000001-0000-0000-0000-000000000003` - `LOW` - `RESOLVED`)
