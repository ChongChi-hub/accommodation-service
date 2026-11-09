# Bảng Định Danh Dữ Liệu Mẫu Cố Định (Seed IDs Registry)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Mục đích:** Đảm bảo toàn vẹn tham chiếu và tính nhất quán tuyệt đối giữa các Microservice độc lập.

---

## 1. Người dùng & Tài khoản Demo (`cozynest_identity`)

| Mã định danh (ID) | Email | Vai trò | Họ và tên | Trạng thái | KYC |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`u-tenant-01`** | `tenant@cozynest.vn` | TENANT | Nguyễn Văn Thuê | ACTIVE | APPROVED |
| **`u-landlord-01`**| `landlord@cozynest.vn` | TENANT, LANDLORD | Trần Thị Chủ Trọ | ACTIVE | APPROVED |
| **`u-mod-01`** | `moderator@cozynest.vn`| MODERATOR | Phạm Minh Kiểm Duyệt | ACTIVE | APPROVED |
| **`u-admin-01`** | `admin@cozynest.vn` | ADMIN | Lê Hoàng Quản Trị | ACTIVE | APPROVED |
| `u-tenant-02` | `tenant2@cozynest.vn` | TENANT | Lê Hoàng Minh | ACTIVE | APPROVED |
| `u-tenant-03` | `tenant3@cozynest.vn` | TENANT | Vũ Phương Thảo | ACTIVE | APPROVED |
| `u-tenant-04` | `tenant4@cozynest.vn` | TENANT | Đặng Quốc Hưng | ACTIVE | PENDING |
| `u-tenant-05` | `tenant5@cozynest.vn` | TENANT | Bùi Thị Thanh Hà | ACTIVE | REJECTED |
| `u-tenant-06` | `tenant6@cozynest.vn` | TENANT | Đỗ Minh Tuấn | ACTIVE | APPROVED |
| `u-tenant-07` | `tenant7@cozynest.vn` | TENANT | Hoàng Thùy Linh | PENDING | PENDING |
| `u-tenant-08` | `tenant8@cozynest.vn` | TENANT | Ngô Quang Hải | LOCKED | APPROVED |
| `u-landlord-02` | `landlord2@cozynest.vn`| TENANT, LANDLORD | Trần Minh Tuấn | ACTIVE | APPROVED |
| `u-landlord-03` | `landlord3@cozynest.vn`| TENANT, LANDLORD | Hoàng Thị Mai Lan | ACTIVE | APPROVED |
| `u-landlord-04` | `landlord4@cozynest.vn`| TENANT, LANDLORD | Phan Thanh Bình | ACTIVE | APPROVED |
| `u-landlord-05` | `landlord5@cozynest.vn`| TENANT, LANDLORD | Nguyễn Thị Ngọc Ánh | ACTIVE | APPROVED |
| `u-landlord-06` | `landlord6@cozynest.vn`| TENANT, LANDLORD | Vũ Đình Trọng | LOCKED | APPROVED |
| `u-landlord-new`| `landlord.new@toam.vn` | TENANT | Chủ Trọ Mới Chờ KYC | ACTIVE | AWAITING_KYC |
| `u-landlord-pending` | `landlord.pending@toam.vn` | TENANT | Chủ Trọ Chờ Duyệt | ACTIVE | PENDING |
| `u-landlord-rejected`| `landlord.rejected@toam.vn`| TENANT | Chủ Trọ Bị Từ Chối | ACTIVE | REJECTED |
| `u-tenant-unverified`| `tenant.unverified@toam.vn`| TENANT | Người Thuê Chưa Xác Minh | ACTIVE | NONE |
| `u-mod-02` | `mod2@cozynest.vn` | MODERATOR | Đoàn Thu Trang | ACTIVE | APPROVED |
| `u-mod-03` | `mod3@cozynest.vn` | MODERATOR | Lương Gia Bảo | ACTIVE | APPROVED |
| `u-admin-02` | `admin2@cozynest.vn` | ADMIN | Trịnh Phương Dung | ACTIVE | APPROVED |

*Ghi chú Mật khẩu:* Mọi tài khoản demo sử dụng mật khẩu: `password123` (BCrypt cost 12 hash: `$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a`).

---

## 2. Phòng Trọ Tiêu Biểu (`cozynest_listing`)

| Mã số (ID) | UUID Tham chiếu | Tiêu đề tóm tắt (Mô hình 2 cấp: Xã/Phường, Tỉnh/Thành) | Chủ trọ sở hữu | Trạng thái tin đăng |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `room-00000001-0000-0000-0000-000000000001` | Phòng studio ban công thoáng Dịch Vọng Hậu, Hà Nội | `u-landlord-01` | `RENTED` |
| **2** | `room-00000001-0000-0000-0000-000000000002` | Căn hộ dịch vụ 1N1K trung tâm Bến Nghé, TP.HCM | `u-landlord-01` | `RENTED` |
| **3** | `room-00000001-0000-0000-0000-000000000003` | Phòng trọ gác lửng giá rẻ Phường 1, TP.HCM | `u-landlord-02` | `PUBLISHED` |
| **4** | `room-00000001-0000-0000-0000-000000000004` | Căn hộ mini đầy đủ nội thất Láng Hạ, Hà Nội | `u-landlord-02` | `PUBLISHED` |
| **5** | `room-00000001-0000-0000-0000-000000000005` | Phòng khép kín Bách Khoa, Hà Nội | `u-landlord-03` | `PUBLISHED` |
| **6** | `room-00000001-0000-0000-0000-000000000006` | Chung cư mini cao cấp Ô Chợ Dừa, Hà Nội | `u-landlord-03` | `PUBLISHED` |
| **7** | `room-00000001-0000-0000-0000-000000000007` | Phòng trọ sinh viên gần ĐH Quốc Gia HN, Dịch Vọng Hậu | `u-landlord-04` | `PENDING_REVIEW` |
| **8** | `room-00000001-0000-0000-0000-000000000008` | Studio view Landmark 81, Phường 5, TP.HCM | `u-landlord-01` | `RENTED` |
| ... | ... | *(Tổng cộng 78 phòng phủ khắp 6 thành phố trực thuộc Trung ương theo Nghị quyết 202/2025/QH15)* | ... | ... |

### 2.1 Phòng Dữ Liệu Mở Rộng 4 Loại Hình & 8 Tỉnh Thành (Seed R__04: IDs 201 - 232)

| Mã số (ID) | UUID Tham chiếu | Nhóm loại hình | Tỉnh/Thành | Đơn vị giá | Giá niêm yết | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **201 - 208** | `room-...0201` đến `0208` | Nhà nguyên căn (`FULL_HOUSE`) | HN, HCM, DNG, HP, HUE, BDG, BN, CT | `ROOM` (/tháng) | 6.5M - 15.0M | `PUBLISHED` |
| **209 - 216** | `room-...0209` đến `0216` | Ở ghép (`SHARED`) | HN, HCM, DNG, CT, HUE, BDG, HP, BN | `BED` (/giường/tháng) | 1.2M - 2.2M | `PUBLISHED` |
| **217 - 224** | `room-...0217` đến `0224` | Phòng trọ (`SINGLE`, `HOMESTAY`) | HUE, BDG, BN, HP, CT, HN, HCM, DNG | `ROOM` (/tháng) | 2.1M - 2.95M | `PUBLISHED` |
| **225 - 232** | `room-...0225` đến `0232` | Căn hộ (`MINI_APARTMENT`, `STUDIO`, `APARTMENT`) | HUE, BDG, BN, HP, CT, HN, HCM, DNG | `ROOM` (/tháng) | 3.8M - 6.0M | `PUBLISHED` |

---

## 3. Quy Trình Thuê & Hợp Đồng (`cozynest_booking`)

| ID Hợp đồng | Mã HĐ | Người thuê | Chủ trọ | Phòng ID | Giá thuê | Tiền cọc | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ct-00000001` | `HD-2026-088` | `u-tenant-01` | `u-landlord-01` | `room-00000001-0000-0000-0000-000000000001` | 4,500,000 | 4,500,000 | `ACTIVE` |
| `ct-00000002` | `HD-2026-052` | `u-tenant-02` | `u-landlord-01` | `room-00000001-0000-0000-0000-000000000002` | 7,200,000 | 7,200,000 | `ACTIVE` |
| `ct-00000003` | `HD-2026-015` | `u-tenant-03` | `u-landlord-02` | `room-00000001-0000-0000-0000-000000000003` | 3,200,000 | 3,200,000 | `PENDING_SIGN` |
| `ct-00000004` | `HD-2025-099` | `u-tenant-06` | `u-landlord-03` | `room-00000001-0000-0000-0000-000000000006` | 5,500,000 | 5,500,000 | `TERMINATED` |

---

## 4. Hóa Đơn & Thanh Toán (`cozynest_billing`)

| ID Hóa đơn | Mã HĐ | Hợp đồng | Kỳ tháng | Tổng tiền | Đã thanh toán | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `inv-00000001` | `INV-2026-11-01` | `ct-00000001` | `2026-11-01` | 4,875,000 | 4,875,000 | `PAID` |
| `inv-00000002` | `INV-2026-10-01` | `ct-00000001` | `2026-10-01` | 4,820,000 | 4,820,000 | `PAID` |
| `inv-00000003` | `INV-2026-11-02` | `ct-00000002` | `2026-11-01` | 7,650,000 | 0 | `OVERDUE` |
| `inv-00000004` | `INV-2026-11-03` | `ct-00000003` | `2026-11-01` | 3,450,000 | 1,500,000 | `PARTIALLY_PAID` |

---

## 5. Phiếu Hỗ Trợ, Tranh Chấp & Báo Cáo

- **Phiếu hỗ trợ (`cozynest_communication`):**
  - `ticket-1001`: `TK-1001` (`u-tenant-01` gửi - `OPEN`)
  - `ticket-1002`: `TK-1002` (`u-landlord-01` gửi - `IN_PROGRESS`)
  - `ticket-1003`: `TK-1003` (`u-tenant-02` gửi - `RESOLVED`)
- **Vụ tranh chấp (`cozynest_admin`):**
  - `disp-00000001`: `DISP-501` (`ct-00000001` - `DEPOSIT_RETURN` - `MEDIATING`)
  - `disp-00000002`: `DISP-502` (`ct-00000002` - `DAMAGE_COMPENSATION` - `PENDING`)
- **Yêu cầu bảo trì (`cozynest_booking`):**
  - `maint-00000001`: `MN-201` (`u-tenant-01` / `room-00000001-0000-0000-0000-000000000001` - `HIGH` - `PENDING`)
  - `maint-00000002`: `MN-202` (`u-tenant-02` / `room-00000001-0000-0000-0000-000000000002` - `MEDIUM` - `IN_PROGRESS`)
  - `maint-00000003`: `MN-203` (`u-tenant-01` / `room-00000001-0000-0000-0000-000000000003` - `LOW` - `RESOLVED`)
