-- ============================================================
-- V6__seed_landlord_demo_rooms.sql
-- Seed Data for LANDLORD-04 & LANDLORD-02:
-- Comprehensive room inventory for landlord1@cozynest.vn (u-landlord-01)
-- covering all statuses: DRAFT, PENDING_REVIEW, PUBLISHED, HIDDEN (OWNER),
-- HIDDEN (MODERATOR with reason), REJECTED (with reason), RENTED.
-- Provides > 25 rooms for pagination testing, long titles/addresses,
-- 3-10 images per room with exact 1 cover and contiguous sort order.
-- ============================================================

INSERT INTO rooms (
    id, room_uuid, landlord_id, landlord_name, landlord_phone,
    room_type_id, room_type_code, title, title_search, slug, description,
    address, province_code, district_code, ward_code, ward_id,
    latitude, longitude, area, max_occupants,
    price, deposit, electricity_cost, water_cost, internet_cost, service_cost,
    status, is_approved, allow_pets, allow_cooking, rating_avg, review_count, version,
    published_at, hidden_at, hidden_by, hidden_reason, rejected_reason, view_count
) VALUES
-- 101. PUBLISHED standard
(101, 'room-00000001-0000-0000-0000-000000000101', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'SINGLE', 'Phòng trọ cao cấp ban công thoáng ngõ 82 Duy Tân Cầu Giấy', 'phong tro cao cap ban cong thoang ngo 82 duy tan cau giay', 'phong-tro-cao-cap-duy-tan-101',
 'Phòng đầy đủ nội thất cao cấp: giường nệm lò xo, tủ quần áo kịch trần, điều hòa Inverter hai chiều, máy giặt riêng.',
 'Số 15 ngõ 82 Duy Tân, Phường Dịch Vọng Hậu, Hà Nội', 'HN', NULL, 'HN_DVH', 1,
 21.032, 105.784, 28.5, 2, 4500000, 4500000, 3800.0, 28000.0, 100000.0, 50000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 14, 1,
 '2026-02-15 08:30:00', NULL, NULL, NULL, NULL, 482),

-- 102. PUBLISHED with extra long title & address
(102, 'room-00000001-0000-0000-0000-000000000102', 1, 'Trần Thị Chủ Trọ', '0902000002',
 2, 'MINI_APARTMENT', 'Căn hộ mini 1 phòng ngủ 1 phòng khách ngập tràn ánh sáng tự nhiên cạnh công viên Nghĩa Đô và bảo tàng Dân Tộc Học', 'can ho mini 1 phong ngu 1 phong khach ngap tran anh sang tu nhien canh cong vien nghia do va bao tang dan toc hoc', 'can-ho-mini-nghia-do-102',
 'Căn hộ thiết kế hiện đại chuẩn phong cách Bắc Âu. Bếp riêng biệt chống ám mùi, ban công cây xanh thoáng đãng view nhìn thẳng công viên.',
 'Tầng 4 tòa nhà Sunshine Home, Số 88 ngách 24/5 ngõ 106 Hoàng Quốc Việt, Phường Nghĩa Đô, Hà Nội', 'HN', NULL, 'HN_ND', 4,
 21.045, 105.798, 45.0, 3, 7200000, 7200000, 3800.0, 28000.0, 100000.0, 80000.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 8, 1,
 '2026-02-20 10:15:00', NULL, NULL, NULL, NULL, 620),

-- 103. HIDDEN by OWNER (chủ trọ tạm ẩn để sửa chữa)
(103, 'room-00000001-0000-0000-0000-000000000103', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'SINGLE', 'Studio gác lửng thông minh ngõ 165 Cầu Giấy', 'studio gac lung thong minh ngo 165 cau giay', 'studio-gac-lung-cau-giay-103',
 'Phòng trọ gác xép đúc bê tông kiên cố, cầu thang gỗ có tay vịn an toàn. Đang tạm ẩn để sơn sửa lại tường.',
 'Số 28 ngõ 165 Cầu Giấy, Phường Quan Hoa, Hà Nội', 'HN', NULL, 'HN_QH', 2,
 21.036, 105.792, 24.0, 2, 3800000, 3800000, 3800.0, 28000.0, 100000.0, 40000.0,
 'HIDDEN', TRUE, FALSE, TRUE, 4.6, 9, 2,
 '2026-01-10 14:00:00', '2026-03-01 09:00:00', 'OWNER', 'Tạm thời ẩn để sơn sửa tường và nâng cấp máy lạnh', NULL, 310),

-- 104. HIDDEN by MODERATOR (kiểm duyệt ẩn có lý do rõ ràng)
(104, 'room-00000001-0000-0000-0000-000000000104', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'SINGLE', 'Phòng trọ giá rẻ khép kín ngõ 381 Nguyễn Khang Cầu Giấy', 'phong tro gia re khep kin ngo 381 nguyen khang cau giay', 'phong-tro-nguyen-khang-104',
 'Phòng khép kín sạch sẽ. Tin bị kiểm duyệt ẩn do phản ánh vi phạm an toàn lối thoát hiểm.',
 'Số 45 ngõ 381 Nguyễn Khang, Phường Yên Hòa, Hà Nội', 'HN', NULL, 'HN_YH', 19,
 21.026, 105.796, 20.0, 2, 3000000, 3000000, 3800.0, 28000.0, 100000.0, 30000.0,
 'HIDDEN', TRUE, FALSE, TRUE, 4.2, 5, 3,
 '2026-01-05 11:00:00', '2026-03-02 16:30:00', 'MODERATOR', 'Hình ảnh chụp mờ và vi phạm quy chuẩn an toàn PCCC lối thoát hiểm, yêu cầu kiểm định lại', NULL, 195),

-- 105. REJECTED by MODERATOR
(105, 'room-00000001-0000-0000-0000-000000000105', 1, 'Trần Thị Chủ Trọ', '0902000002',
 4, 'HOMESTAY', 'Phòng dorm giường tầng sinh viên giá rẻ gần Đại học Sư Phạm', 'phong dorm giuong tang sinh vien gia re gan dai hoc su pham', 'dorm-su-pham-105',
 'Giường tầng ký túc xá tư nhân, có rèm che riêng tư, tủ khóa cá nhân an toàn.',
 'Số 12 ngách 15/3 Trần Quốc Hoàn, Phường Dịch Vọng Hậu, Hà Nội', 'HN', NULL, 'HN_DVH', 1,
 21.041, 105.786, 35.0, 6, 1800000, 1800000, 3800.0, 28000.0, 50000.0, 50000.0,
 'REJECTED', FALSE, FALSE, FALSE, 0.0, 0, 1,
 NULL, NULL, NULL, NULL, 'Địa chỉ phòng không chính xác, thiếu số nhà cụ thể và ảnh chụp chưa rõ khu vực sinh hoạt chung', 45),

-- 106. PENDING_REVIEW
(106, 'room-00000001-0000-0000-0000-000000000106', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'SINGLE', 'Phòng trọ mới xây 100% ngõ 120 Trần Cung gần bệnh viện E', 'phong tro moi xay 100 ngo 120 tran cung gan benh vien e', 'phong-tro-tran-cung-106',
 'Tòa nhà mới bàn giao, thang máy thẻ từ, bảo vệ 24/7, máy giặt máy sấy công nghiệp tầng thượng.',
 'Số 58 ngõ 120 Trần Cung, Phường Cổ Nhuế 1, Hà Nội', 'HN', NULL, 'HN_CN1', 16,
 21.052, 105.788, 26.0, 2, 4200000, 4200000, 3800.0, 28000.0, 100000.0, 60000.0,
 'PENDING_REVIEW', FALSE, FALSE, TRUE, 0.0, 0, 1,
 NULL, NULL, NULL, NULL, NULL, 12),

-- 107. DRAFT
(107, 'room-00000001-0000-0000-0000-000000000107', 1, 'Trần Thị Chủ Trọ', '0902000002',
 2, 'MINI_APARTMENT', 'Bản nháp căn hộ dịch vụ cao cấp Lạc Long Quân Tây Hồ', 'ban nhap can ho dich vu cao cap lac long quan tay ho', 'nhap-can-ho-tay-ho-107',
 'Bản nháp đang nhập liệu dở, chuẩn bị bổ sung ảnh chụp phòng khách và video 360 độ.',
 'Số 264 Lạc Long Quân, Phường Xuân Đỉnh, Hà Nội', 'HN', NULL, 'HN_XD', 18,
 21.055, 105.811, 50.0, 3, 9000000, 9000000, 4000.0, 30000.0, 120000.0, 100000.0,
 'DRAFT', FALSE, TRUE, TRUE, 0.0, 0, 1,
 NULL, NULL, NULL, NULL, NULL, 5),

-- 108. RENTED (hợp đồng đang hiệu lực)
(108, 'room-00000001-0000-0000-0000-000000000108', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'SINGLE', 'Studio view hồ điều hòa ngõ 20 Hồ Tùng Mậu Cầu Giấy', 'studio view ho dieu hoa ngo 20 ho tung mau cau giay', 'studio-ho-tung-mau-108',
 'Khách thuê đã dọn vào ở từ tháng 01/2026, phòng luôn kín người thuê quanh năm.',
 'Số 20 Hồ Tùng Mậu, Phường Mai Dịch, Hà Nội', 'HN', NULL, 'HN_MD', 5,
 21.037, 105.778, 30.0, 2, 5200000, 5200000, 3800.0, 28000.0, 100000.0, 50000.0,
 'RENTED', TRUE, FALSE, TRUE, 4.9, 18, 2,
 '2026-01-02 09:00:00', NULL, NULL, NULL, NULL, 890),

-- 109 - 128. Additional 20 rooms for u-landlord-01 to ensure > 25 total rooms for pagination test
(109, 'room-00000001-0000-0000-0000-000000000109', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng trọ ngõ 105 Doãn Kế Thiện gần ĐH Thương Mại', 'phong tro ngo 105 doan ke thien gan dh thuong mai', 'doan-ke-thien-109', 'Phòng tiện nghi sạch sẽ, giờ giấc tự do.', '105 Doãn Kế Thiện, Phường Mai Dịch, Hà Nội', 'HN', NULL, 'HN_MD', 5, 21.040, 105.775, 22.0, 2, 3400000, 3400000, 3800.0, 28000.0, 100000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 11, 1, '2026-02-01 10:00:00', NULL, NULL, NULL, NULL, 315),
(110, 'room-00000001-0000-0000-0000-000000000110', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng khép kín ngõ 180 Trung Kính gần viện Huyết Học', 'phong khep kin ngo 180 trung kinh gan vien huyet hoc', 'trung-kinh-110', 'Phòng có ban công phơi đồ riêng, máy lạnh.', '180 Trung Kính, Phường Trung Hòa, Hà Nội', 'HN', NULL, 'HN_TH', 20, 21.019, 105.793, 25.0, 2, 4000000, 4000000, 3800.0, 28000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 15, 1, '2026-02-02 11:00:00', NULL, NULL, NULL, NULL, 420),
(111, 'room-00000001-0000-0000-0000-000000000111', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Căn hộ mini 2 phòng ngủ ngõ 68 Trung Kính', 'can ho mini 2 phong ngu ngo 68 trung kinh', 'trung-kinh-111', 'Phù hợp gia đình nhỏ hoặc nhóm 3-4 bạn.', '68 Trung Kính, Phường Trung Hòa, Hà Nội', 'HN', NULL, 'HN_TH', 20, 21.015, 105.795, 52.0, 4, 8500000, 8500000, 3800.0, 28000.0, 100000.0, 80000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 6, 1, '2026-02-05 14:00:00', NULL, NULL, NULL, NULL, 530),
(112, 'room-00000001-0000-0000-0000-000000000112', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Studio đầy đủ đồ ngõ 110 Trần Duy Hưng gần BigC Thăng Long', 'studio day du do ngo 110 tran duy hung gan bigc thang long', 'tran-duy-hung-112', 'Vị trí trung tâm kết nối quận Thanh Xuân, Đống Đa.', '110 Trần Duy Hưng, Phường Trung Hòa, Hà Nội', 'HN', NULL, 'HN_TH', 20, 21.009, 105.798, 28.0, 2, 4800000, 4800000, 3800.0, 28000.0, 100000.0, 60000.0, 'RENTED', TRUE, FALSE, TRUE, 4.9, 21, 1, '2026-01-15 09:00:00', NULL, NULL, NULL, NULL, 780),
(113, 'room-00000001-0000-0000-0000-000000000113', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng trọ giá tốt ngõ 25 Vũ Ngọc Phan Láng Hạ', 'phong tro gia tot ngo 25 vu ngoc phan lang ha', 'vu-ngoc-phan-113', 'Gần rạp chiếu phim Quốc Gia, thuận tiện đi lại.', '25 Vũ Ngọc Phan, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.014, 105.812, 20.0, 2, 3200000, 3200000, 3500.0, 25000.0, 100000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 8, 1, '2026-02-10 16:00:00', NULL, NULL, NULL, NULL, 290),
(114, 'room-00000001-0000-0000-0000-000000000114', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Căn hộ studio ban công rộng ngõ 105 Láng Hạ view hồ', 'can ho studio ban cong rong ngo 105 lang ha view ho', 'lang-ha-114', 'Căn hộ mới setup nội thất phong cách Nhật Bản.', '105 Láng Hạ, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.016, 105.815, 34.0, 2, 5800000, 5800000, 3500.0, 25000.0, 100000.0, 70000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 13, 1, '2026-02-12 11:30:00', NULL, NULL, NULL, NULL, 610),
(115, 'room-00000001-0000-0000-0000-000000000115', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng trọ sinh viên ngõ 89 Thái Hà gần ĐH Thủy Lợi', 'phong tro sinh vien ngo 89 thai ha gan dh thuy loi', 'thai-ha-115', 'Khu trung tâm sầm uất, an ninh đảm bảo.', '89 Thái Hà, Phường Ô Chợ Dừa, Hà Nội', 'HN', NULL, 'HN_OCD', 7, 21.011, 105.821, 23.0, 2, 3600000, 3600000, 3500.0, 25000.0, 80000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.5, 9, 1, '2026-02-14 15:00:00', NULL, NULL, NULL, NULL, 340),
(116, 'room-00000001-0000-0000-0000-000000000116', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng khép kín ngõ 34 Nguyên Hồng Đống Đa', 'phong khep kin ngo 34 nguyen hong dong da', 'nguyen-hong-116', 'Nhà xây kiên cố, để xe tầng 1 miễn phí.', '34 Nguyên Hồng, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.018, 105.811, 24.0, 2, 3900000, 3900000, 3500.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 7, 1, '2026-02-16 10:00:00', NULL, NULL, NULL, NULL, 280),
(117, 'room-00000001-0000-0000-0000-000000000117', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Studio gác lửng ngõ 298 Tây Sơn ngã tư Sở', 'studio gac lung ngo 298 tay son nga tu so', 'tay-son-117', 'Gần tuyến tàu điện Cát Linh, đi lại cực tiện.', '298 Tây Sơn, Phường Ô Chợ Dừa, Hà Nội', 'HN', NULL, 'HN_OCD', 7, 21.008, 105.824, 26.0, 2, 4200000, 4200000, 3500.0, 25000.0, 100000.0, 50000.0, 'DRAFT', FALSE, FALSE, TRUE, 0.0, 0, 1, NULL, NULL, NULL, NULL, NULL, 18),
(118, 'room-00000001-0000-0000-0000-000000000118', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng nhỏ ban công ngõ 119 Hồ Đắc Di', 'phong nho ban cong ngo 119 ho dac di', 'ho-dac-di-118', 'View hồ thoáng mát lộng gió, khu dân trí cao.', '119 Hồ Đắc Di, Phường Ô Chợ Dừa, Hà Nội', 'HN', NULL, 'HN_OCD', 7, 21.012, 105.829, 21.0, 2, 3500000, 3500000, 3500.0, 25000.0, 100000.0, 40000.0, 'PENDING_REVIEW', FALSE, FALSE, TRUE, 0.0, 0, 1, NULL, NULL, NULL, NULL, NULL, 22),
(119, 'room-00000001-0000-0000-0000-000000000119', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Căn hộ mini 1 phòng ngủ phố Đặng Văn Ngữ', 'can ho mini 1 phong ngu pho dang van ngu', 'dang-van-ngu-119', 'Khu phố ẩm thực cà phê sầm uất giới trẻ.', '45 Đặng Văn Ngữ, Phường Ô Chợ Dừa, Hà Nội', 'HN', NULL, 'HN_OCD', 7, 21.009, 105.832, 36.0, 2, 6000000, 6000000, 3500.0, 25000.0, 100000.0, 70000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.8, 12, 1, '2026-02-18 14:00:00', NULL, NULL, NULL, NULL, 510),
(120, 'room-00000001-0000-0000-0000-000000000120', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng trọ ngõ 42 Phương Mai gần BV Bạch Mai', 'phong tro ngo 42 phuong mai gan bv bach mai', 'phuong-mai-120', 'Tiện sinh viên trường Y Hà Nội, Bách Khoa.', '42 Phương Mai, Phường Ô Chợ Dừa, Hà Nội', 'HN', NULL, 'HN_OCD', 7, 21.003, 105.838, 22.0, 2, 3300000, 3300000, 3500.0, 25000.0, 80000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 10, 1, '2026-02-20 09:00:00', NULL, NULL, NULL, NULL, 390),
(121, 'room-00000001-0000-0000-0000-000000000121', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng trọ ngõ 185 Chùa Láng đối diện hồ Quan Hệ', 'phong tro ngo 185 chua lang doi dien ho quan he', 'chua-lang-121', 'Khu sinh viên Đại học Ngoại Thương.', '185 Chùa Láng, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.024, 105.802, 25.0, 2, 4200000, 4200000, 3500.0, 25000.0, 100000.0, 50000.0, 'RENTED', TRUE, FALSE, TRUE, 4.9, 16, 1, '2026-01-20 10:00:00', NULL, NULL, NULL, NULL, 640),
(122, 'room-00000001-0000-0000-0000-000000000122', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Studio ban công ngõ 62 Nguyễn Chí Thanh', 'studio ban cong ngo 62 nguyen chi thanh', 'nguyen-chi-thanh-122', 'Ngay chân cầu vượt, giao thông cực kỳ thuận tiện.', '62 Nguyễn Chí Thanh, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.021, 105.808, 28.0, 2, 4600000, 4600000, 3500.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 14, 1, '2026-02-22 16:00:00', NULL, NULL, NULL, NULL, 480),
(123, 'room-00000001-0000-0000-0000-000000000123', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Căn hộ mini 1N1K ngõ 102 Huỳnh Thúc Kháng', 'can ho mini 1n1k ngo 102 huynh thuc khang', 'huynh-thuc-khang-123', 'Khu trung tâm văn phòng, dân trí văn minh.', '102 Huỳnh Thúc Kháng, Phường Láng Hạ, Hà Nội', 'HN', NULL, 'HN_LH', 6, 21.018, 105.811, 40.0, 2, 6800000, 6800000, 3500.0, 25000.0, 100000.0, 80000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 8, 1, '2026-02-24 11:00:00', NULL, NULL, NULL, NULL, 590),
(124, 'room-00000001-0000-0000-0000-000000000124', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng nhỏ ngõ 20 Cát Linh gần ga tàu điện', 'phong nho ngo 20 cat linh gan ga tau dien', 'cat-linh-124', 'Đi bộ ra ga Cát Linh 100m, tiện đi làm công sở.', '20 Cát Linh, Phường Cát Linh, Hà Nội', 'HN', NULL, 'HN_CL', 8, 21.028, 105.828, 19.0, 1, 2900000, 2900000, 3500.0, 25000.0, 80000.0, 30000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.4, 6, 1, '2026-02-25 09:30:00', NULL, NULL, NULL, NULL, 230),
(125, 'room-00000001-0000-0000-0000-000000000125', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng gác lửng ngõ 12 Núi Trúc Ba Đình', 'phong gac lung ngo 12 nui truc ba dinh', 'nui-truc-125', 'Khu trung tâm gần Giảng Võ.', '12 Núi Trúc, Phường Kim Mã, Hà Nội', 'HN', NULL, 'HN_KM', 21, 21.029, 105.821, 24.0, 2, 4000000, 4000000, 3500.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 12, 1, '2026-02-26 14:00:00', NULL, NULL, NULL, NULL, 410),
(126, 'room-00000001-0000-0000-0000-000000000126', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Studio ban công ngõ 55 Hoàng Hoa Thám Ba Đình', 'studio ban cong ngo 55 hoang hoa tham ba dinh', 'hoang-hoa-tham-126', 'Gần công viên Bách Thảo, không khí trong lành.', '55 Hoàng Hoa Thám, Phường Kim Mã, Hà Nội', 'HN', NULL, 'HN_KM', 21, 21.039, 105.824, 27.0, 2, 4500000, 4500000, 3500.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.8, 15, 1, '2026-02-27 10:00:00', NULL, NULL, NULL, NULL, 470),
(127, 'room-00000001-0000-0000-0000-000000000127', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng khép kín ngõ 28 Đội Cấn Ba Đình', 'phong khep kin ngo 28 doi can ba dinh', 'doi-can-127', 'Phòng sạch đẹp, ban công phơi đồ thoáng.', '28 Đội Cấn, Phường Giảng Võ, Hà Nội', 'HN', NULL, 'HN_GV', 22, 21.034, 105.827, 23.0, 2, 3800000, 3800000, 3500.0, 25000.0, 100000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 9, 1, '2026-02-28 15:00:00', NULL, NULL, NULL, NULL, 360),
(128, 'room-00000001-0000-0000-0000-000000000128', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Căn hộ mini 1 phòng ngủ ngõ 19 Liễu Giai cạnh Lotte Center', 'can ho mini 1 phong ngu ngo 19 lieu giai canh lotte center', 'lieu-giai-128', 'Tòa nhà cao cấp có bảo vệ 24/7, thang máy tốc độ cao.', '19 Liễu Giai, Phường Giảng Võ, Hà Nội', 'HN', NULL, 'HN_GV', 22, 21.033, 105.814, 42.0, 2, 7500000, 7500000, 3800.0, 28000.0, 100000.0, 80000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 19, 1, '2026-03-01 10:00:00', NULL, NULL, NULL, NULL, 820);

-- Insert Images for rooms 101 to 128 (3 to 6 images per room, exactly 1 cover photo per room)
INSERT INTO room_images (
    room_id, image_url, storage_key, thumb_key, medium_key, large_key,
    original_filename, content_type, size_bytes, width, height,
    is_cover, sort_order, alt_text, uploaded_by, is_primary, display_order
) VALUES
-- Room 101 (4 images)
(101, '/uploads/rooms/r101_1.webp', 'rooms/101/img_1.webp', 'rooms/101/img_1_thumb.webp', 'rooms/101/img_1_medium.webp', 'rooms/101/img_1_large.webp', 'phong_ngu_chinh.jpg', 'image/webp', 245000, 1920, 1080, TRUE, 0, 'Phòng ngủ chính đầy đủ giường nệm và điều hòa', 'u-landlord-01', TRUE, 0),
(101, '/uploads/rooms/r101_2.webp', 'rooms/101/img_2.webp', 'rooms/101/img_2_thumb.webp', 'rooms/101/img_2_medium.webp', 'rooms/101/img_2_large.webp', 'ban_cong_thoang.jpg', 'image/webp', 198000, 1920, 1080, FALSE, 1, 'Ban công rộng rãi đón ánh sáng tự nhiên', 'u-landlord-01', FALSE, 1),
(101, '/uploads/rooms/r101_3.webp', 'rooms/101/img_3.webp', 'rooms/101/img_3_thumb.webp', 'rooms/101/img_3_medium.webp', 'rooms/101/img_3_large.webp', 'nha_ve_sinh.jpg', 'image/webp', 165000, 1920, 1080, FALSE, 2, 'Nhà vệ sinh khép kín có bình nóng lạnh', 'u-landlord-01', FALSE, 2),
(101, '/uploads/rooms/r101_4.webp', 'rooms/101/img_4.webp', 'rooms/101/img_4_thumb.webp', 'rooms/101/img_4_medium.webp', 'rooms/101/img_4_large.webp', 'khu_bep.jpg', 'image/webp', 210000, 1920, 1080, FALSE, 3, 'Khu vực bếp nấu tiện nghi', 'u-landlord-01', FALSE, 3),

-- Room 102 (3 images)
(102, '/uploads/rooms/r102_1.webp', 'rooms/102/img_1.webp', 'rooms/102/img_1_thumb.webp', 'rooms/102/img_1_medium.webp', 'rooms/102/img_1_large.webp', 'phong_khach.jpg', 'image/webp', 275000, 1920, 1080, TRUE, 0, 'Phòng khách sang trọng có sofa và smart TV', 'u-landlord-01', TRUE, 0),
(102, '/uploads/rooms/r102_2.webp', 'rooms/102/img_2.webp', 'rooms/102/img_2_thumb.webp', 'rooms/102/img_2_medium.webp', 'rooms/102/img_2_large.webp', 'phong_ngu.jpg', 'image/webp', 230000, 1920, 1080, FALSE, 1, 'Phòng ngủ riêng tư cửa sổ lớn', 'u-landlord-01', FALSE, 1),
(102, '/uploads/rooms/r102_3.webp', 'rooms/102/img_3.webp', 'rooms/102/img_3_thumb.webp', 'rooms/102/img_3_medium.webp', 'rooms/102/img_3_large.webp', 'khu_bep_chau_au.jpg', 'image/webp', 215000, 1920, 1080, FALSE, 2, 'Khu bếp hiện đại phong cách Bắc Âu', 'u-landlord-01', FALSE, 2),

-- Room 103 (3 images)
(103, '/uploads/rooms/r103_1.webp', 'rooms/103/img_1.webp', 'rooms/103/img_1_thumb.webp', 'rooms/103/img_1_medium.webp', 'rooms/103/img_1_large.webp', 'gac_lung.jpg', 'image/webp', 220000, 1920, 1080, TRUE, 0, 'Gác lửng đúc kiên cố cao ráo', 'u-landlord-01', TRUE, 0),
(103, '/uploads/rooms/r103_2.webp', 'rooms/103/img_2.webp', 'rooms/103/img_2_thumb.webp', 'rooms/103/img_2_medium.webp', 'rooms/103/img_2_large.webp', 'tang_tret.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Tầng trệt thoáng rộng để bàn học và tủ', 'u-landlord-01', FALSE, 1),
(103, '/uploads/rooms/r103_3.webp', 'rooms/103/img_3.webp', 'rooms/103/img_3_thumb.webp', 'rooms/103/img_3_medium.webp', 'rooms/103/img_3_large.webp', 've_sinh.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Khu vệ sinh sạch sẽ khép kín', 'u-landlord-01', FALSE, 2),

-- Room 104 (3 images)
(104, '/uploads/rooms/r104_1.webp', 'rooms/104/img_1.webp', 'rooms/104/img_1_thumb.webp', 'rooms/104/img_1_medium.webp', 'rooms/104/img_1_large.webp', 'phong_nguyen_khang.jpg', 'image/webp', 185000, 1920, 1080, TRUE, 0, 'Toàn cảnh phòng khép kín', 'u-landlord-01', TRUE, 0),
(104, '/uploads/rooms/r104_2.webp', 'rooms/104/img_2.webp', 'rooms/104/img_2_thumb.webp', 'rooms/104/img_2_medium.webp', 'rooms/104/img_2_large.webp', 'hanh_lang.jpg', 'image/webp', 160000, 1920, 1080, FALSE, 1, 'Hành lang chung', 'u-landlord-01', FALSE, 1),
(104, '/uploads/rooms/r104_3.webp', 'rooms/104/img_3.webp', 'rooms/104/img_3_thumb.webp', 'rooms/104/img_3_medium.webp', 'rooms/104/img_3_large.webp', 'loi_thoat_hiem.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 2, 'Lối thoát hiểm và cửa sổ phụ', 'u-landlord-01', FALSE, 2),

-- Room 105 (3 images)
(105, '/uploads/rooms/r105_1.webp', 'rooms/105/img_1.webp', 'rooms/105/img_1_thumb.webp', 'rooms/105/img_1_medium.webp', 'rooms/105/img_1_large.webp', 'giuong_tang.jpg', 'image/webp', 210000, 1920, 1080, TRUE, 0, 'Hệ thống giường tầng homestay sinh viên', 'u-landlord-01', TRUE, 0),
(105, '/uploads/rooms/r105_2.webp', 'rooms/105/img_2.webp', 'rooms/105/img_2_thumb.webp', 'rooms/105/img_2_medium.webp', 'rooms/105/img_2_large.webp', 'tu_ca_nhan.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 1, 'Tủ đồ khóa cá nhân từng người', 'u-landlord-01', FALSE, 1),
(105, '/uploads/rooms/r105_3.webp', 'rooms/105/img_3.webp', 'rooms/105/img_3_thumb.webp', 'rooms/105/img_3_medium.webp', 'rooms/105/img_3_large.webp', 'bep_chung.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 2, 'Khu sinh hoạt nấu ăn chung', 'u-landlord-01', FALSE, 2),

-- Room 106 (3 images)
(106, '/uploads/rooms/r106_1.webp', 'rooms/106/img_1.webp', 'rooms/106/img_1_thumb.webp', 'rooms/106/img_1_medium.webp', 'rooms/106/img_1_large.webp', 'phong_moi_1.jpg', 'image/webp', 240000, 1920, 1080, TRUE, 0, 'Phòng mới bàn giao sơn trắng sáng', 'u-landlord-01', TRUE, 0),
(106, '/uploads/rooms/r106_2.webp', 'rooms/106/img_2.webp', 'rooms/106/img_2_thumb.webp', 'rooms/106/img_2_medium.webp', 'rooms/106/img_2_large.webp', 'phong_moi_2.jpg', 'image/webp', 225000, 1920, 1080, FALSE, 1, 'Hệ tủ bếp và kệ lạnh Inverter', 'u-landlord-01', FALSE, 1),
(106, '/uploads/rooms/r106_3.webp', 'rooms/106/img_3.webp', 'rooms/106/img_3_thumb.webp', 'rooms/106/img_3_medium.webp', 'rooms/106/img_3_large.webp', 'phong_moi_3.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 2, 'Phòng tắm hiện đại vách kính', 'u-landlord-01', FALSE, 2),

-- Room 107 (3 images)
(107, '/uploads/rooms/r107_1.webp', 'rooms/107/img_1.webp', 'rooms/107/img_1_thumb.webp', 'rooms/107/img_1_medium.webp', 'rooms/107/img_1_large.webp', 'nhap_tayho_1.jpg', 'image/webp', 250000, 1920, 1080, TRUE, 0, 'Ảnh bản nháp phòng khách Tây Hồ', 'u-landlord-01', TRUE, 0),
(107, '/uploads/rooms/r107_2.webp', 'rooms/107/img_2.webp', 'rooms/107/img_2_thumb.webp', 'rooms/107/img_2_medium.webp', 'rooms/107/img_2_large.webp', 'nhap_tayho_2.jpg', 'image/webp', 235000, 1920, 1080, FALSE, 1, 'Khu ban công nhìn ra hồ Tây', 'u-landlord-01', FALSE, 1),
(107, '/uploads/rooms/r107_3.webp', 'rooms/107/img_3.webp', 'rooms/107/img_3_thumb.webp', 'rooms/107/img_3_medium.webp', 'rooms/107/img_3_large.webp', 'nhap_tayho_3.jpg', 'image/webp', 205000, 1920, 1080, FALSE, 2, 'Khu bếp gỗ sồi cao cấp', 'u-landlord-01', FALSE, 2),

-- Room 108 (3 images)
(108, '/uploads/rooms/r108_1.webp', 'rooms/108/img_1.webp', 'rooms/108/img_1_thumb.webp', 'rooms/108/img_1_medium.webp', 'rooms/108/img_1_large.webp', 'ho_tung_mau_1.jpg', 'image/webp', 260000, 1920, 1080, TRUE, 0, 'Studio view hồ điều hòa lộng gió', 'u-landlord-01', TRUE, 0),
(108, '/uploads/rooms/r108_2.webp', 'rooms/108/img_2.webp', 'rooms/108/img_2_thumb.webp', 'rooms/108/img_2_medium.webp', 'rooms/108/img_2_large.webp', 'ho_tung_mau_2.jpg', 'image/webp', 220000, 1920, 1080, FALSE, 1, 'Góc làm việc và giường ngủ', 'u-landlord-01', FALSE, 1),
(108, '/uploads/rooms/r108_3.webp', 'rooms/108/img_3.webp', 'rooms/108/img_3_thumb.webp', 'rooms/108/img_3_medium.webp', 'rooms/108/img_3_large.webp', 'ho_tung_mau_3.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 2, 'Khu vệ sinh thiết bị Caesar', 'u-landlord-01', FALSE, 2),

-- Rooms 109 to 128 (At least 3 images each, exactly 1 cover)
(109, '/uploads/rooms/r109_1.webp', 'rooms/109/img_1.webp', 'rooms/109/img_1_thumb.webp', 'rooms/109/img_1_medium.webp', 'rooms/109/img_1_large.webp', 'img1.jpg', 'image/webp', 200000, 1920, 1080, TRUE, 0, 'Ảnh bìa phòng trọ Doãn Kế Thiện', 'u-landlord-01', TRUE, 0),
(109, '/uploads/rooms/r109_2.webp', 'rooms/109/img_2.webp', 'rooms/109/img_2_thumb.webp', 'rooms/109/img_2_medium.webp', 'rooms/109/img_2_large.webp', 'img2.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 1, 'Ảnh chi tiết góc bếp', 'u-landlord-01', FALSE, 1),
(109, '/uploads/rooms/r109_3.webp', 'rooms/109/img_3.webp', 'rooms/109/img_3_thumb.webp', 'rooms/109/img_3_medium.webp', 'rooms/109/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Ảnh nhà vệ sinh', 'u-landlord-01', FALSE, 2),

(110, '/uploads/rooms/r110_1.webp', 'rooms/110/img_1.webp', 'rooms/110/img_1_thumb.webp', 'rooms/110/img_1_medium.webp', 'rooms/110/img_1_large.webp', 'img1.jpg', 'image/webp', 200000, 1920, 1080, TRUE, 0, 'Ảnh bìa phòng Trung Kính', 'u-landlord-01', TRUE, 0),
(110, '/uploads/rooms/r110_2.webp', 'rooms/110/img_2.webp', 'rooms/110/img_2_thumb.webp', 'rooms/110/img_2_medium.webp', 'rooms/110/img_2_large.webp', 'img2.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 1, 'Góc ban công thoáng', 'u-landlord-01', FALSE, 1),
(110, '/uploads/rooms/r110_3.webp', 'rooms/110/img_3.webp', 'rooms/110/img_3_thumb.webp', 'rooms/110/img_3_medium.webp', 'rooms/110/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Khu bếp riêng biệt', 'u-landlord-01', FALSE, 2),

(111, '/uploads/rooms/r111_1.webp', 'rooms/111/img_1.webp', 'rooms/111/img_1_thumb.webp', 'rooms/111/img_1_medium.webp', 'rooms/111/img_1_large.webp', 'img1.jpg', 'image/webp', 210000, 1920, 1080, TRUE, 0, 'Ảnh bìa căn hộ 2PN', 'u-landlord-01', TRUE, 0),
(111, '/uploads/rooms/r111_2.webp', 'rooms/111/img_2.webp', 'rooms/111/img_2_thumb.webp', 'rooms/111/img_2_medium.webp', 'rooms/111/img_2_large.webp', 'img2.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Phòng ngủ số 1', 'u-landlord-01', FALSE, 1),
(111, '/uploads/rooms/r111_3.webp', 'rooms/111/img_3.webp', 'rooms/111/img_3_thumb.webp', 'rooms/111/img_3_medium.webp', 'rooms/111/img_3_large.webp', 'img3.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 2, 'Phòng ngủ số 2', 'u-landlord-01', FALSE, 2),

(112, '/uploads/rooms/r112_1.webp', 'rooms/112/img_1.webp', 'rooms/112/img_1_thumb.webp', 'rooms/112/img_1_medium.webp', 'rooms/112/img_1_large.webp', 'img1.jpg', 'image/webp', 205000, 1920, 1080, TRUE, 0, 'Ảnh bìa studio Trần Duy Hưng', 'u-landlord-01', TRUE, 0),
(112, '/uploads/rooms/r112_2.webp', 'rooms/112/img_2.webp', 'rooms/112/img_2_thumb.webp', 'rooms/112/img_2_medium.webp', 'rooms/112/img_2_large.webp', 'img2.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 1, 'Khu bếp đầy đủ đồ', 'u-landlord-01', FALSE, 1),
(112, '/uploads/rooms/r112_3.webp', 'rooms/112/img_3.webp', 'rooms/112/img_3_thumb.webp', 'rooms/112/img_3_medium.webp', 'rooms/112/img_3_large.webp', 'img3.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 2, 'Phòng tắm máy nước nóng', 'u-landlord-01', FALSE, 2),

(113, '/uploads/rooms/r113_1.webp', 'rooms/113/img_1.webp', 'rooms/113/img_1_thumb.webp', 'rooms/113/img_1_medium.webp', 'rooms/113/img_1_large.webp', 'img1.jpg', 'image/webp', 195000, 1920, 1080, TRUE, 0, 'Ảnh bìa Vũ Ngọc Phan', 'u-landlord-01', TRUE, 0),
(113, '/uploads/rooms/r113_2.webp', 'rooms/113/img_2.webp', 'rooms/113/img_2_thumb.webp', 'rooms/113/img_2_medium.webp', 'rooms/113/img_2_large.webp', 'img2.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 1, 'Bàn học và tủ áo', 'u-landlord-01', FALSE, 1),
(113, '/uploads/rooms/r113_3.webp', 'rooms/113/img_3.webp', 'rooms/113/img_3_thumb.webp', 'rooms/113/img_3_medium.webp', 'rooms/113/img_3_large.webp', 'img3.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Khu để xe tầng trệt', 'u-landlord-01', FALSE, 2),

(114, '/uploads/rooms/r114_1.webp', 'rooms/114/img_1.webp', 'rooms/114/img_1_thumb.webp', 'rooms/114/img_1_medium.webp', 'rooms/114/img_1_large.webp', 'img1.jpg', 'image/webp', 215000, 1920, 1080, TRUE, 0, 'Ảnh bìa studio Láng Hạ', 'u-landlord-01', TRUE, 0),
(114, '/uploads/rooms/r114_2.webp', 'rooms/114/img_2.webp', 'rooms/114/img_2_thumb.webp', 'rooms/114/img_2_medium.webp', 'rooms/114/img_2_large.webp', 'img2.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Nội thất phong cách Nhật', 'u-landlord-01', FALSE, 1),
(114, '/uploads/rooms/r114_3.webp', 'rooms/114/img_3.webp', 'rooms/114/img_3_thumb.webp', 'rooms/114/img_3_medium.webp', 'rooms/114/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Ban công ngắm hồ', 'u-landlord-01', FALSE, 2),

(115, '/uploads/rooms/r115_1.webp', 'rooms/115/img_1.webp', 'rooms/115/img_1_thumb.webp', 'rooms/115/img_1_medium.webp', 'rooms/115/img_1_large.webp', 'img1.jpg', 'image/webp', 190000, 1920, 1080, TRUE, 0, 'Ảnh bìa phòng Thái Hà', 'u-landlord-01', TRUE, 0),
(115, '/uploads/rooms/r115_2.webp', 'rooms/115/img_2.webp', 'rooms/115/img_2_thumb.webp', 'rooms/115/img_2_medium.webp', 'rooms/115/img_2_large.webp', 'img2.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 1, 'Góc học tập và nghỉ ngơi', 'u-landlord-01', FALSE, 1),
(115, '/uploads/rooms/r115_3.webp', 'rooms/115/img_3.webp', 'rooms/115/img_3_thumb.webp', 'rooms/115/img_3_medium.webp', 'rooms/115/img_3_large.webp', 'img3.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Khu vệ sinh sạch', 'u-landlord-01', FALSE, 2),

(116, '/uploads/rooms/r116_1.webp', 'rooms/116/img_1.webp', 'rooms/116/img_1_thumb.webp', 'rooms/116/img_1_medium.webp', 'rooms/116/img_1_large.webp', 'img1.jpg', 'image/webp', 200000, 1920, 1080, TRUE, 0, 'Ảnh bìa Nguyên Hồng', 'u-landlord-01', TRUE, 0),
(116, '/uploads/rooms/r116_2.webp', 'rooms/116/img_2.webp', 'rooms/116/img_2_thumb.webp', 'rooms/116/img_2_medium.webp', 'rooms/116/img_2_large.webp', 'img2.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 1, 'Giường ngủ và tủ áo', 'u-landlord-01', FALSE, 1),
(116, '/uploads/rooms/r116_3.webp', 'rooms/116/img_3.webp', 'rooms/116/img_3_thumb.webp', 'rooms/116/img_3_medium.webp', 'rooms/116/img_3_large.webp', 'img3.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 2, 'Hành lang và cầu thang', 'u-landlord-01', FALSE, 2),

(117, '/uploads/rooms/r117_1.webp', 'rooms/117/img_1.webp', 'rooms/117/img_1_thumb.webp', 'rooms/117/img_1_medium.webp', 'rooms/117/img_1_large.webp', 'img1.jpg', 'image/webp', 205000, 1920, 1080, TRUE, 0, 'Ảnh bìa Tây Sơn', 'u-landlord-01', TRUE, 0),
(117, '/uploads/rooms/r117_2.webp', 'rooms/117/img_2.webp', 'rooms/117/img_2_thumb.webp', 'rooms/117/img_2_medium.webp', 'rooms/117/img_2_large.webp', 'img2.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 1, 'Gác lửng kiên cố', 'u-landlord-01', FALSE, 1),
(117, '/uploads/rooms/r117_3.webp', 'rooms/117/img_3.webp', 'rooms/117/img_3_thumb.webp', 'rooms/117/img_3_medium.webp', 'rooms/117/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Khu bếp riêng', 'u-landlord-01', FALSE, 2),

(118, '/uploads/rooms/r118_1.webp', 'rooms/118/img_1.webp', 'rooms/118/img_1_thumb.webp', 'rooms/118/img_1_medium.webp', 'rooms/118/img_1_large.webp', 'img1.jpg', 'image/webp', 195000, 1920, 1080, TRUE, 0, 'Ảnh bìa Hồ Đắc Di', 'u-landlord-01', TRUE, 0),
(118, '/uploads/rooms/r118_2.webp', 'rooms/118/img_2.webp', 'rooms/118/img_2_thumb.webp', 'rooms/118/img_2_medium.webp', 'rooms/118/img_2_large.webp', 'img2.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 1, 'Ban công nhìn ra hồ', 'u-landlord-01', FALSE, 1),
(118, '/uploads/rooms/r118_3.webp', 'rooms/118/img_3.webp', 'rooms/118/img_3_thumb.webp', 'rooms/118/img_3_medium.webp', 'rooms/118/img_3_large.webp', 'img3.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Nhà vệ sinh sạch', 'u-landlord-01', FALSE, 2),

(119, '/uploads/rooms/r119_1.webp', 'rooms/119/img_1.webp', 'rooms/119/img_1_thumb.webp', 'rooms/119/img_1_medium.webp', 'rooms/119/img_1_large.webp', 'img1.jpg', 'image/webp', 215000, 1920, 1080, TRUE, 0, 'Ảnh bìa Đặng Văn Ngữ', 'u-landlord-01', TRUE, 0),
(119, '/uploads/rooms/r119_2.webp', 'rooms/119/img_2.webp', 'rooms/119/img_2_thumb.webp', 'rooms/119/img_2_medium.webp', 'rooms/119/img_2_large.webp', 'img2.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Phòng khách có sofa', 'u-landlord-01', FALSE, 1),
(119, '/uploads/rooms/r119_3.webp', 'rooms/119/img_3.webp', 'rooms/119/img_3_thumb.webp', 'rooms/119/img_3_medium.webp', 'rooms/119/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Phòng ngủ riêng tư', 'u-landlord-01', FALSE, 2),

(120, '/uploads/rooms/r120_1.webp', 'rooms/120/img_1.webp', 'rooms/120/img_1_thumb.webp', 'rooms/120/img_1_medium.webp', 'rooms/120/img_1_large.webp', 'img1.jpg', 'image/webp', 190000, 1920, 1080, TRUE, 0, 'Ảnh bìa Phương Mai', 'u-landlord-01', TRUE, 0),
(120, '/uploads/rooms/r120_2.webp', 'rooms/120/img_2.webp', 'rooms/120/img_2_thumb.webp', 'rooms/120/img_2_medium.webp', 'rooms/120/img_2_large.webp', 'img2.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 1, 'Khu nấu ăn khép kín', 'u-landlord-01', FALSE, 1),
(120, '/uploads/rooms/r120_3.webp', 'rooms/120/img_3.webp', 'rooms/120/img_3_thumb.webp', 'rooms/120/img_3_medium.webp', 'rooms/120/img_3_large.webp', 'img3.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Khu giặt phơi', 'u-landlord-01', FALSE, 2),

(121, '/uploads/rooms/r121_1.webp', 'rooms/121/img_1.webp', 'rooms/121/img_1_thumb.webp', 'rooms/121/img_1_medium.webp', 'rooms/121/img_1_large.webp', 'img1.jpg', 'image/webp', 205000, 1920, 1080, TRUE, 0, 'Ảnh bìa Chùa Láng', 'u-landlord-01', TRUE, 0),
(121, '/uploads/rooms/r121_2.webp', 'rooms/121/img_2.webp', 'rooms/121/img_2_thumb.webp', 'rooms/121/img_2_medium.webp', 'rooms/121/img_2_large.webp', 'img2.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 1, 'View nhìn ra hồ Quan Hệ', 'u-landlord-01', FALSE, 1),
(121, '/uploads/rooms/r121_3.webp', 'rooms/121/img_3.webp', 'rooms/121/img_3_thumb.webp', 'rooms/121/img_3_medium.webp', 'rooms/121/img_3_large.webp', 'img3.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 2, 'Nhà vệ sinh sạch sẽ', 'u-landlord-01', FALSE, 2),

(122, '/uploads/rooms/r122_1.webp', 'rooms/122/img_1.webp', 'rooms/122/img_1_thumb.webp', 'rooms/122/img_1_medium.webp', 'rooms/122/img_1_large.webp', 'img1.jpg', 'image/webp', 210000, 1920, 1080, TRUE, 0, 'Ảnh bìa Nguyễn Chí Thanh', 'u-landlord-01', TRUE, 0),
(122, '/uploads/rooms/r122_2.webp', 'rooms/122/img_2.webp', 'rooms/122/img_2_thumb.webp', 'rooms/122/img_2_medium.webp', 'rooms/122/img_2_large.webp', 'img2.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Ban công ngập nắng', 'u-landlord-01', FALSE, 1),
(122, '/uploads/rooms/r122_3.webp', 'rooms/122/img_3.webp', 'rooms/122/img_3_thumb.webp', 'rooms/122/img_3_medium.webp', 'rooms/122/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Khu bếp từ an toàn', 'u-landlord-01', FALSE, 2),

(123, '/uploads/rooms/r123_1.webp', 'rooms/123/img_1.webp', 'rooms/123/img_1_thumb.webp', 'rooms/123/img_1_medium.webp', 'rooms/123/img_1_large.webp', 'img1.jpg', 'image/webp', 225000, 1920, 1080, TRUE, 0, 'Ảnh bìa Huỳnh Thúc Kháng', 'u-landlord-01', TRUE, 0),
(123, '/uploads/rooms/r123_2.webp', 'rooms/123/img_2.webp', 'rooms/123/img_2_thumb.webp', 'rooms/123/img_2_medium.webp', 'rooms/123/img_2_large.webp', 'img2.jpg', 'image/webp', 200000, 1920, 1080, FALSE, 1, 'Phòng khách cao cấp', 'u-landlord-01', FALSE, 1),
(123, '/uploads/rooms/r123_3.webp', 'rooms/123/img_3.webp', 'rooms/123/img_3_thumb.webp', 'rooms/123/img_3_medium.webp', 'rooms/123/img_3_large.webp', 'img3.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 2, 'Phòng tắm có vách kính', 'u-landlord-01', FALSE, 2),

(124, '/uploads/rooms/r124_1.webp', 'rooms/124/img_1.webp', 'rooms/124/img_1_thumb.webp', 'rooms/124/img_1_medium.webp', 'rooms/124/img_1_large.webp', 'img1.jpg', 'image/webp', 185000, 1920, 1080, TRUE, 0, 'Ảnh bìa Cát Linh', 'u-landlord-01', TRUE, 0),
(124, '/uploads/rooms/r124_2.webp', 'rooms/124/img_2.webp', 'rooms/124/img_2_thumb.webp', 'rooms/124/img_2_medium.webp', 'rooms/124/img_2_large.webp', 'img2.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 1, 'Giường đệm và tủ đầu giường', 'u-landlord-01', FALSE, 1),
(124, '/uploads/rooms/r124_3.webp', 'rooms/124/img_3.webp', 'rooms/124/img_3_thumb.webp', 'rooms/124/img_3_medium.webp', 'rooms/124/img_3_large.webp', 'img3.jpg', 'image/webp', 165000, 1920, 1080, FALSE, 2, 'Khu vệ sinh riêng', 'u-landlord-01', FALSE, 2),

(125, '/uploads/rooms/r125_1.webp', 'rooms/125/img_1.webp', 'rooms/125/img_1_thumb.webp', 'rooms/125/img_1_medium.webp', 'rooms/125/img_1_large.webp', 'img1.jpg', 'image/webp', 200000, 1920, 1080, TRUE, 0, 'Ảnh bìa Núi Trúc', 'u-landlord-01', TRUE, 0),
(125, '/uploads/rooms/r125_2.webp', 'rooms/125/img_2.webp', 'rooms/125/img_2_thumb.webp', 'rooms/125/img_2_medium.webp', 'rooms/125/img_2_large.webp', 'img2.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 1, 'Gác lửng kiên cố cao ráo', 'u-landlord-01', FALSE, 1),
(125, '/uploads/rooms/r125_3.webp', 'rooms/125/img_3.webp', 'rooms/125/img_3_thumb.webp', 'rooms/125/img_3_medium.webp', 'rooms/125/img_3_large.webp', 'img3.jpg', 'image/webp', 175000, 1920, 1080, FALSE, 2, 'Tầng trệt thoáng rộng', 'u-landlord-01', FALSE, 2),

(126, '/uploads/rooms/r126_1.webp', 'rooms/126/img_1.webp', 'rooms/126/img_1_thumb.webp', 'rooms/126/img_1_medium.webp', 'rooms/126/img_1_large.webp', 'img1.jpg', 'image/webp', 210000, 1920, 1080, TRUE, 0, 'Ảnh bìa Hoàng Hoa Thám', 'u-landlord-01', TRUE, 0),
(126, '/uploads/rooms/r126_2.webp', 'rooms/126/img_2.webp', 'rooms/126/img_2_thumb.webp', 'rooms/126/img_2_medium.webp', 'rooms/126/img_2_large.webp', 'img2.jpg', 'image/webp', 195000, 1920, 1080, FALSE, 1, 'Ban công ngập nắng', 'u-landlord-01', FALSE, 1),
(126, '/uploads/rooms/r126_3.webp', 'rooms/126/img_3.webp', 'rooms/126/img_3_thumb.webp', 'rooms/126/img_3_medium.webp', 'rooms/126/img_3_large.webp', 'img3.jpg', 'image/webp', 180000, 1920, 1080, FALSE, 2, 'Khu bếp nấu tiện nghi', 'u-landlord-01', FALSE, 2),

(127, '/uploads/rooms/r127_1.webp', 'rooms/127/img_1.webp', 'rooms/127/img_1_thumb.webp', 'rooms/127/img_1_medium.webp', 'rooms/127/img_1_large.webp', 'img1.jpg', 'image/webp', 200000, 1920, 1080, TRUE, 0, 'Ảnh bìa Đội Cấn', 'u-landlord-01', TRUE, 0),
(127, '/uploads/rooms/r127_2.webp', 'rooms/127/img_2.webp', 'rooms/127/img_2_thumb.webp', 'rooms/127/img_2_medium.webp', 'rooms/127/img_2_large.webp', 'img2.jpg', 'image/webp', 185000, 1920, 1080, FALSE, 1, 'Góc học tập và nghỉ ngơi', 'u-landlord-01', FALSE, 1),
(127, '/uploads/rooms/r127_3.webp', 'rooms/127/img_3.webp', 'rooms/127/img_3_thumb.webp', 'rooms/127/img_3_medium.webp', 'rooms/127/img_3_large.webp', 'img3.jpg', 'image/webp', 170000, 1920, 1080, FALSE, 2, 'Khu vệ sinh sạch sẽ', 'u-landlord-01', FALSE, 2),

(128, '/uploads/rooms/r128_1.webp', 'rooms/128/img_1.webp', 'rooms/128/img_1_thumb.webp', 'rooms/128/img_1_medium.webp', 'rooms/128/img_1_large.webp', 'img1.jpg', 'image/webp', 230000, 1920, 1080, TRUE, 0, 'Ảnh bìa Liễu Giai Lotte', 'u-landlord-01', TRUE, 0),
(128, '/uploads/rooms/r128_2.webp', 'rooms/128/img_2.webp', 'rooms/128/img_2_thumb.webp', 'rooms/128/img_2_medium.webp', 'rooms/128/img_2_large.webp', 'img2.jpg', 'image/webp', 210000, 1920, 1080, FALSE, 1, 'Phòng khách view trung tâm', 'u-landlord-01', FALSE, 1),
(128, '/uploads/rooms/r128_3.webp', 'rooms/128/img_3.webp', 'rooms/128/img_3_thumb.webp', 'rooms/128/img_3_medium.webp', 'rooms/128/img_3_large.webp', 'img3.jpg', 'image/webp', 190000, 1920, 1080, FALSE, 2, 'Phòng ngủ phong cách hiện đại', 'u-landlord-01', FALSE, 2);

-- Insert status history for demo transitions
INSERT INTO room_status_history (room_id, from_status, to_status, actor_id, actor_role, reason, created_at) VALUES
(101, 'PENDING_REVIEW', 'PUBLISHED', 'u-mod-01', 'MODERATOR', 'Hồ sơ và thông tin phòng trọ hợp lệ đầy đủ', '2026-02-15 08:30:00'),
(102, 'PENDING_REVIEW', 'PUBLISHED', 'u-mod-01', 'MODERATOR', 'Phòng đạt tiêu chuẩn chất lượng hình ảnh cao', '2026-02-20 10:15:00'),
(103, 'PUBLISHED', 'HIDDEN', 'u-landlord-01', 'LANDLORD', 'Tạm thời ẩn để sơn sửa tường và nâng cấp máy lạnh', '2026-03-01 09:00:00'),
(104, 'PUBLISHED', 'HIDDEN', 'u-mod-01', 'MODERATOR', 'Hình ảnh chụp mờ và vi phạm quy chuẩn an toàn PCCC lối thoát hiểm, yêu cầu kiểm định lại', '2026-03-02 16:30:00'),
(105, 'PENDING_REVIEW', 'REJECTED', 'u-mod-02', 'MODERATOR', 'Địa chỉ phòng không chính xác, thiếu số nhà cụ thể và ảnh chụp chưa rõ khu vực sinh hoạt chung', '2026-02-28 14:00:00'),
(108, 'PUBLISHED', 'RENTED', 'u-landlord-01', 'LANDLORD', 'Khách thuê ký hợp đồng dài hạn HD-2026-088', '2026-01-02 09:00:00');

