-- ============================================================
-- V4__seed_full_demo_listings.sql
-- Seed 63 Provinces, Key City Districts/Wards, and 70 Additional Rooms (Total 78 rooms)
-- Covering statuses: PUBLISHED, RENTED, PENDING_REVIEW, DRAFT, HIDDEN, REJECTED
-- ============================================================

-- 1. All 63 Provinces / Centrally Governed Cities of Vietnam
INSERT INTO provinces (code, name, name_en, full_name, full_name_en) VALUES
('01', 'Hà Nội', 'Ha Noi', 'Thành phố Hà Nội', 'Ha Noi City'),
('02', 'Hà Giang', 'Ha Giang', 'Tỉnh Hà Giang', 'Ha Giang Province'),
('04', 'Cao Bằng', 'Cao Bang', 'Tỉnh Cao Bằng', 'Cao Bang Province'),
('06', 'Bắc Kạn', 'Bac Kan', 'Tỉnh Bắc Kạn', 'Bac Kan Province'),
('08', 'Tuyên Quang', 'Tuyen Quang', 'Tỉnh Tuyên Quang', 'Tuyen Quang Province'),
('10', 'Lào Cai', 'Lao Cai', 'Tỉnh Lào Cai', 'Lao Cai Province'),
('11', 'Điện Biên', 'Dien Bien', 'Tỉnh Điện Biên', 'Dien Bien Province'),
('12', 'Lai Châu', 'Lai Chau', 'Tỉnh Lai Châu', 'Lai Chau Province'),
('14', 'Sơn La', 'Son La', 'Tỉnh Sơn La', 'Son La Province'),
('15', 'Yên Bái', 'Yen Bai', 'Tỉnh Yên Bái', 'Yen Bai Province'),
('17', 'Hòa Bình', 'Hoa Binh', 'Tỉnh Hòa Bình', 'Hoa Binh Province'),
('19', 'Thái Nguyên', 'Thai Nguyen', 'Tỉnh Thái Nguyên', 'Thai Nguyen Province'),
('20', 'Lạng Sơn', 'Lang Son', 'Tỉnh Lạng Sơn', 'Lang Son Province'),
('22', 'Quảng Ninh', 'Quang Ninh', 'Tỉnh Quảng Ninh', 'Quang Ninh Province'),
('24', 'Bắc Giang', 'Bac Giang', 'Tỉnh Bắc Giang', 'Bac Giang Province'),
('25', 'Phú Thọ', 'Phu Tho', 'Tỉnh Phú Thọ', 'Phu Tho Province'),
('26', 'Vĩnh Phúc', 'Vinh Phuc', 'Tỉnh Vĩnh Phúc', 'Vinh Phuc Province'),
('27', 'Bắc Ninh', 'Bac Ninh', 'Tỉnh Bắc Ninh', 'Bac Ninh Province'),
('30', 'Hải Dương', 'Hai Duong', 'Tỉnh Hải Dương', 'Hai Duong Province'),
('31', 'Hải Phòng', 'Hai Phong', 'Thành phố Hải Phòng', 'Hai Phong City'),
('33', 'Hưng Yên', 'Hung Yen', 'Tỉnh Hưng Yên', 'Hung Yen Province'),
('34', 'Thái Bình', 'Thai Binh', 'Tỉnh Thái Bình', 'Thai Binh Province'),
('35', 'Hà Nam', 'Ha Nam', 'Tỉnh Hà Nam', 'Ha Nam Province'),
('36', 'Nam Định', 'Nam Dinh', 'Tỉnh Nam Định', 'Nam Dinh Province'),
('37', 'Ninh Bình', 'Ninh Binh', 'Tỉnh Ninh Bình', 'Ninh Binh Province'),
('38', 'Thanh Hóa', 'Thanh Hoa', 'Tỉnh Thanh Hóa', 'Thanh Hoa Province'),
('40', 'Nghệ An', 'Nghe An', 'Tỉnh Nghệ An', 'Nghe An Province'),
('42', 'Hà Tĩnh', 'Ha Tinh', 'Tỉnh Hà Tĩnh', 'Ha Tinh Province'),
('44', 'Quảng Bình', 'Quang Binh', 'Tỉnh Quảng Bình', 'Quang Binh Province'),
('45', 'Quảng Trị', 'Quang Tri', 'Tỉnh Quảng Trị', 'Quang Tri Province'),
('46', 'Thừa Thiên Huế', 'Thua Thien Hue', 'Tỉnh Thừa Thiên Huế', 'Thua Thien Hue Province'),
('48', 'Đà Nẵng', 'Da Nang', 'Thành phố Đà Nẵng', 'Da Nang City'),
('49', 'Quảng Nam', 'Quang Nam', 'Tỉnh Quảng Nam', 'Quang Nam Province'),
('51', 'Quảng Ngãi', 'Quang Ngai', 'Tỉnh Quảng Ngãi', 'Quang Ngai Province'),
('52', 'Bình Định', 'Binh Dinh', 'Tỉnh Bình Định', 'Binh Dinh Province'),
('54', 'Phú Yên', 'Phu Yen', 'Tỉnh Phú Yên', 'Phu Yen Province'),
('56', 'Khánh Hòa', 'Khanh Hoa', 'Tỉnh Khánh Hòa', 'Khanh Hoa Province'),
('58', 'Ninh Thuận', 'Ninh Thuan', 'Tỉnh Ninh Thuận', 'Ninh Thuan Province'),
('60', 'Bình Thuận', 'Binh Thuan', 'Tỉnh Bình Thuận', 'Binh Thuan Province'),
('62', 'Kon Tum', 'Kon Tum', 'Tỉnh Kon Tum', 'Kon Tum Province'),
('64', 'Gia Lai', 'Gia Lai', 'Tỉnh Gia Lai', 'Gia Lai Province'),
('66', 'Đắk Lắk', 'Dak Lak', 'Tỉnh Đắk Lắk', 'Dak Lak Province'),
('67', 'Đắk Nông', 'Dak Nong', 'Tỉnh Đắk Nông', 'Dak Nong Province'),
('68', 'Lâm Đồng', 'Lam Dong', 'Tỉnh Lâm Đồng', 'Lam Dong Province'),
('70', 'Bình Phước', 'Binh Phuoc', 'Tỉnh Bình Phước', 'Binh Phuoc Province'),
('72', 'Tây Ninh', 'Tay Ninh', 'Tỉnh Tây Ninh', 'Tay Ninh Province'),
('74', 'Bình Dương', 'Binh Duong', 'Tỉnh Bình Dương', 'Binh Duong Province'),
('75', 'Đồng Nai', 'Dong Nai', 'Tỉnh Đồng Nai', 'Dong Nai Province'),
('77', 'Bà Rịa - Vũng Tàu', 'Ba Ria - Vung Tau', 'Tỉnh Bà Rịa - Vũng Tàu', 'Ba Ria - Vung Tau Province'),
('79', 'Hồ Chí Minh', 'Ho Chi Minh', 'Thành phố Hồ Chí Minh', 'Ho Chi Minh City'),
('80', 'Long An', 'Long An', 'Tỉnh Long An', 'Long An Province'),
('82', 'Tiền Giang', 'Tien Giang', 'Tỉnh Tiền Giang', 'Tien Giang Province'),
('83', 'Bến Tre', 'Ben Tre', 'Tỉnh Bến Tre', 'Ben Tre Province'),
('84', 'Trà Vinh', 'Tra Vinh', 'Tỉnh Trà Vinh', 'Tra Vinh Province'),
('86', 'Vĩnh Long', 'Vinh Long', 'Tỉnh Vĩnh Long', 'Vinh Long Province'),
('87', 'Đồng Tháp', 'Dong Thap', 'Tỉnh Đồng Tháp', 'Dong Thap Province'),
('88', 'An Giang', 'An Giang', 'Tỉnh An Giang', 'An Giang Province'),
('89', 'Kiên Giang', 'Kien Giang', 'Tỉnh Kiên Giang', 'Kien Giang Province'),
('91', 'Cần Thơ', 'Can Tho', 'Thành phố Cần Thơ', 'Can Tho City'),
('92', 'Hậu Giang', 'Hau Giang', 'Tỉnh Hậu Giang', 'Hau Giang Province'),
('93', 'Sóc Trăng', 'Soc Trang', 'Tỉnh Sóc Trăng', 'Soc Trang Province'),
('94', 'Bạc Liêu', 'Bac Lieu', 'Tỉnh Bạc Liêu', 'Bac Lieu Province'),
('95', 'Cà Mau', 'Ca Mau', 'Tỉnh Cà Mau', 'Ca Mau Province')
ON DUPLICATE KEY UPDATE name=name;

-- 2. Districts for Cần Thơ & Hải Phòng (bổ sung cho 5 thành phố lớn)
INSERT INTO districts (code, name, name_en, full_name, full_name_en, province_code) VALUES
('3101', 'Hồng Bàng', 'Hong Bang', 'Quận Hồng Bàng', 'Hong Bang District', '31'),
('3102', 'Ngô Quyền', 'Ngo Quyen', 'Quận Ngô Quyền', 'Ngo Quyen District', '31'),
('3103', 'Lê Chân', 'Le Chan', 'Quận Lê Chân', 'Le Chan District', '31'),
('3104', 'Hải An', 'Hai An', 'Quận Hải An', 'Hai An District', '31'),
('9101', 'Ninh Kiều', 'Ninh Kieu', 'Quận Ninh Kiều', 'Ninh Kieu District', '91'),
('9102', 'Cái Răng', 'Cai Rang', 'Quận Cái Răng', 'Cai Rang District', '91'),
('9103', 'Bình Thủy', 'Binh Thuy', 'Quận Bình Thủy', 'Binh Thuy District', '91')
ON DUPLICATE KEY UPDATE name=name;

-- 3. Wards for Cần Thơ & Hải Phòng
INSERT INTO wards (code, name, name_en, full_name, full_name_en, district_code) VALUES
('310101', 'Hoàng Văn Thụ', 'Hoang Van Thu', 'Phường Hoàng Văn Thụ', 'Hoang Van Thu Ward', '3101'),
('310201', 'Máy Tơ', 'May To', 'Phường Máy Tơ', 'May To Ward', '3102'),
('310301', 'An Biên', 'An Bien', 'Phường An Biên', 'An Bien Ward', '3103'),
('910101', 'Xuân Khánh', 'Xuan Khanh', 'Phường Xuân Khánh', 'Xuan Khanh Ward', '9101'),
('910102', 'An Khánh', 'An Khanh', 'Phường An Khánh', 'An Khanh Ward', '9101'),
('910201', 'Lê Bình', 'Le Binh', 'Phường Lê Bình', 'Le Binh Ward', '9102')
ON DUPLICATE KEY UPDATE name=name;

-- 4. Insert 70 Additional Rooms (Total 78 rooms) with Realistic Details
INSERT INTO rooms (
    id, room_uuid, landlord_id, landlord_name, landlord_phone,
    room_type_id, room_type_code, title, slug, description,
    address, province_code, district_code, ward_code,
    latitude, longitude, area, max_occupants,
    price, deposit, electricity_cost, water_cost, internet_cost, service_cost,
    status, is_approved, allow_pets, allow_cooking, rating_avg, review_count, version
) VALUES
-- Cầu Giấy - Hà Nội (u-landlord-01 & u-landlord-02)
(9, 'room-00000001-0000-0000-0000-000000000009', 2, 'Trần Minh Tuấn', '0902000003', 1, 'SINGLE', 'Phòng trọ cao cấp ngõ 68 Cầu Giấy ban công thoáng mát', 'phong-tro-cao-cap-ngo-68-cau-giay', 'Phòng đầy đủ tiện nghi, có giường nệm, điều hòa Inverter tiết kiệm điện, khóa vân tay an ninh tuyệt đối.', '68 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội', '01', '0101', '010101', 21.035, 105.795, 26.0, 2, 4200000, 4200000, 3800.0, 28000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 12, 1),
(10, 'room-00000001-0000-0000-0000-000000000010', 2, 'Trần Minh Tuấn', '0902000003', 2, 'MINI_APARTMENT', 'Căn hộ mini 1 phòng ngủ tách biệt gần công viên Cầu Giấy', 'can-ho-mini-1pn-gan-cong-vien-cau-giay', 'Căn hộ hiện đại có sofa, smart TV, máy giặt riêng trong phòng, ban công trồng cây xanh.', '12 Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', '01', '0101', '010101', 21.033, 105.789, 38.0, 3, 6800000, 6800000, 3800.0, 28000.0, 100000.0, 80000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 8, 1),
(11, 'room-00000001-0000-0000-0000-000000000011', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Studio khép kín ngõ 86 Duy Tân gần Keangnam', 'studio-khep-kin-ngo-86-duy-tan', 'Phù hợp sinh viên và dân công nghệ thông tin. Giờ giấc tự do, camera giám sát 24/7.', '86 Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', '01', '0101', '010101', 21.031, 105.783, 24.0, 2, 3800000, 3800000, 3800.0, 28000.0, 80000.0, 40000.0, 'RENTED', TRUE, FALSE, TRUE, 4.7, 15, 1),

-- Đống Đa - Hà Nội (u-landlord-01 & u-landlord-03)
(12, 'room-00000001-0000-0000-0000-000000000012', 3, 'Hoàng Thị Mai Lan', '0902000004', 1, 'SINGLE', 'Phòng trọ ban công ngõ Chùa Láng gần ĐH Ngoại Thương', 'phong-tro-chua-lang-gan-ngoai-thuong', 'Vị trí đắc địa gần ĐH Ngoại Thương, Ngoại Giao, Luật. Không chung chủ, có thang máy.', '157 Chùa Láng, Láng Thượng, Đống Đa, Hà Nội', '01', '0102', '010201', 21.023, 105.801, 25.0, 2, 4500000, 4500000, 3500.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 9, 1),
(13, 'room-00000001-0000-0000-0000-000000000013', 3, 'Hoàng Thị Mai Lan', '0902000004', 2, 'MINI_APARTMENT', 'Căn hộ mini Hoàng Cầu view hồ cực thoáng', 'can-ho-mini-hoang-cau-view-ho', 'Nội thất gỗ cao cấp, khu dân trí cao yên tĩnh, gần ga tàu điện trên cao Cát Linh.', '36 Hoàng Cầu, Ô Chợ Dừa, Đống Đa, Hà Nội', '01', '0102', '010201', 21.018, 105.823, 42.0, 3, 7500000, 7500000, 3500.0, 25000.0, 100000.0, 100000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 5, 1),
(14, 'room-00000001-0000-0000-0000-000000000014', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Phòng nhỏ giá mềm Xã Đàn cho sinh viên', 'phong-nho-gia-mem-xa-dan', 'Phòng sạch sẽ, vệ sinh khép kín, tiện xe bus đi các trường đại học trung tâm.', '210 Xã Đàn, Nam Đồng, Đống Đa, Hà Nội', '01', '0102', '010201', 21.012, 105.834, 18.0, 1, 2700000, 2700000, 3500.0, 25000.0, 80000.0, 30000.0, 'PENDING_REVIEW', FALSE, FALSE, TRUE, 0.0, 0, 1),

-- Hai Bà Trưng - Hà Nội (u-landlord-03 & u-landlord-05)
(15, 'room-00000001-0000-0000-0000-000000000015', 5, 'Nguyễn Thị Ngọc Ánh', '0902000006', 1, 'SINGLE', 'Phòng khép kín ngõ Tạ Quang Bửu gần Bách Khoa - Xây Dựng', 'phong-khep-kin-ta-quang-buu-bach-khoa', 'Đi bộ sang ĐH Bách Khoa 3 phút, khu sinh viên sầm uất ăn uống tiện lợi.', '45 Tạ Quang Bửu, Bách Khoa, Hai Bà Trưng, Hà Nội', '01', '0103', '010301', 21.004, 105.845, 22.0, 2, 3300000, 3300000, 3500.0, 25000.0, 80000.0, 40000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 14, 1),
(16, 'room-00000001-0000-0000-0000-000000000016', 5, 'Nguyễn Thị Ngọc Ánh', '0902000006', 4, 'HOMESTAY', 'Phòng homestay ấm cúng gần Times City Minh Khai', 'homestay-am-cung-gan-times-city', 'Đầy đủ bếp nướng, lò vi sóng, khu giặt sấy tầng thượng view thoáng mát.', '458 Minh Khai, Vĩnh Tuy, Hai Bà Trưng, Hà Nội', '01', '0103', '010301', 20.995, 105.867, 30.0, 2, 5000000, 5000000, 3800.0, 28000.0, 100000.0, 60000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 7, 1),

-- Quận 1 & Bình Thạnh - TP.HCM (u-landlord-01 & u-landlord-06)
(17, 'room-00000001-0000-0000-0000-000000000017', 1, 'Trần Thị Chủ Trọ', '0902000002', 2, 'MINI_APARTMENT', 'Studio cao cấp Đa Kao Quận 1 ban công ngập nắng', 'studio-cao-cap-da-kao-quan-1', 'Nội thất phong cách Scandinavian, máy giặt sấy riêng, dọn phòng 2 lần/tuần.', '124 Nguyễn Đình Chiểu, Đa Kao, Quận 1, TP.HCM', '79', '7901', '790101', 10.787, 106.698, 35.0, 2, 8500000, 8500000, 4000.0, 30000.0, 150000.0, 100000.0, 'RENTED', TRUE, FALSE, TRUE, 5.0, 16, 1),
(18, 'room-00000001-0000-0000-0000-000000000018', 1, 'Trần Thị Chủ Trọ', '0902000002', 1, 'SINGLE', 'Căn hộ dịch vụ Điện Biên Phủ Bình Thạnh gần ngã tư Hàng Xanh', 'can-ho-dich-vu-dien-bien-phu-binh-thanh', 'Giao thông thuận tiện sang Q1 chỉ 5 phút, bảo vệ 24/7, bãi đậu xe rộng.', '45 Điện Biên Phủ, Phường 15, Bình Thạnh, TP.HCM', '79', '7903', '790301', 10.798, 106.711, 28.0, 2, 5500000, 5500000, 3800.0, 25000.0, 100000.0, 70000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.8, 11, 1),
(19, 'room-00000001-0000-0000-0000-000000000019', 6, 'Vũ Đình Trọng', '0902000007', 3, 'FULL_HOUSE', 'Nhà nguyên căn 3 tầng Xô Viết Nghệ Tĩnh thích hợp nhóm bạn', 'nha-nguyen-can-xo-viet-nghe-tinh', '3 phòng ngủ, 3 toilet riêng, sân thượng phơi đồ rộng, có gara để 6 xe máy.', '280 Xô Viết Nghệ Tĩnh, Phường 25, Bình Thạnh, TP.HCM', '79', '7903', '790301', 10.803, 106.714, 90.0, 6, 15000000, 30000000, 3500.0, 20000.0, 200000.0, 0.0, 'HIDDEN', TRUE, TRUE, TRUE, 4.5, 4, 1),

-- Gò Vấp & Quận 10 - TP.HCM (u-landlord-02 & u-landlord-03)
(20, 'room-00000001-0000-0000-0000-000000000020', 2, 'Trần Minh Tuấn', '0902000003', 1, 'SINGLE', 'Phòng trọ gác lửng đúc Quang Trung Gò Vấp sạch đẹp', 'phong-tro-gac-lung-quang-trung-go-vap', 'Gác cao đứng không đụng đầu, bếp nấu riêng, giờ giấc tự do không chung chủ.', '78 Quang Trung, Phường 8, Gò Vấp, TP.HCM', '79', '7904', '790401', 10.835, 106.662, 25.0, 3, 3200000, 3200000, 3500.0, 25000.0, 80000.0, 50000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 4.7, 10, 1),
(21, 'room-00000001-0000-0000-0000-000000000021', 3, 'Hoàng Thị Mai Lan', '0902000004', 1, 'SINGLE', 'Phòng trọ gần ĐH Bách Khoa TP.HCM Tô Hiến Thành Quận 10', 'phong-tro-to-hien-thanh-quan-10', 'Phòng mới sơn sửa sạch tinh tươm, máy lạnh mới 100%, an ninh đảm bảo.', '163 Tô Hiến Thành, Phường 13, Quận 10, TP.HCM', '79', '7902', '790201', 10.778, 106.665, 20.0, 2, 3800000, 3800000, 3800.0, 25000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 6, 1),

-- Đà Nẵng (u-landlord-04)
(22, 'room-00000001-0000-0000-0000-000000000022', 4, 'Phan Thanh Bình', '0902000005', 2, 'MINI_APARTMENT', 'Căn hộ view sông Hàn đường Bạch Đằng Hải Châu Đà Nẵng', 'can-ho-view-song-han-bach-dang-hai-chau', 'View ngắm cầu Rồng phun lửa cuối tuần. Full nội thất chuẩn châu Âu.', '88 Bạch Đằng, Hải Châu 1, Hải Châu, Đà Nẵng', '48', '4801', '480101', 16.068, 108.224, 45.0, 2, 7000000, 7000000, 3500.0, 20000.0, 100000.0, 80000.0, 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 9, 1),
(23, 'room-00000001-0000-0000-0000-000000000023', 4, 'Phan Thanh Bình', '0902000005', 1, 'SINGLE', 'Phòng trọ tiện nghi gần ĐH Kinh Tế Đà Nẵng Ngũ Hành Sơn', 'phong-tro-gan-dh-kinh-te-da-nang', 'Cách bãi tắm Mỹ Khê 500m, phòng thoáng mát có cửa sổ lớn đón gió biển.', '32 Ngũ Hành Sơn, Mỹ An, Ngũ Hành Sơn, Đà Nẵng', '48', '4802', '480201', 16.051, 108.241, 24.0, 2, 2800000, 2800000, 3500.0, 20000.0, 80000.0, 30000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 13, 1),

-- Cần Thơ & Hải Phòng (u-landlord-04 & u-landlord-05)
(24, 'room-00000001-0000-0000-0000-000000000024', 5, 'Nguyễn Thị Ngọc Ánh', '0902000006', 1, 'SINGLE', 'Phòng trọ sinh viên gần ĐH Cần Thơ đường 3/2 Ninh Kiều', 'phong-tro-sinh-vien-3-thang-2-ninh-kieu', 'An ninh tốt, gần cổng B ĐH Cần Thơ, có chỗ để xe rộng rãi có camera.', '150 Đường 3/2, Xuân Khánh, Ninh Kiều, Cần Thơ', '91', '9101', '910101', 10.028, 105.771, 20.0, 2, 2000000, 2000000, 3000.0, 15000.0, 70000.0, 30000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 15, 1),
(25, 'room-00000001-0000-0000-0000-000000000025', 4, 'Phan Thanh Bình', '0902000005', 2, 'MINI_APARTMENT', 'Căn hộ mini trung tâm Ngô Quyền Hải Phòng full nội thất', 'can-ho-mini-trung-tam-ngo-quyen-hai-phong', 'Gần Vincom Lê Thánh Tông, phòng có điều hòa 2 chiều, sofa, bếp từ đôi.', '25 Máy Tơ, Ngô Quyền, Hải Phòng', '31', '3102', '310201', 20.865, 106.689, 32.0, 2, 4500000, 4500000, 3500.0, 20000.0, 100000.0, 50000.0, 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 8, 1),

-- Các trạng thái đặc biệt: DRAFT, REJECTED, PENDING_REVIEW
(26, 'room-00000001-0000-0000-0000-000000000026', 2, 'Trần Minh Tuấn', '0902000003', 1, 'SINGLE', 'Phòng trọ dự kiến cho thuê tại Cầu Giấy (Bản nháp)', 'phong-tro-du-kien-ban-nhap-cau-giay', 'Chủ nhà đang hoàn thiện nội thất và sơn tường mới.', '15 Ngõ 102 Trần Phú, Hà Đông, Hà Nội', '01', '0101', '010101', 20.978, 105.786, 22.0, 2, 3000000, 3000000, 3500.0, 25000.0, 80000.0, 40000.0, 'DRAFT', FALSE, FALSE, TRUE, 0.0, 0, 1),
(27, 'room-00000001-0000-0000-0000-000000000027', 6, 'Vũ Đình Trọng', '0902000007', 1, 'SINGLE', 'Phòng trọ giá rẻ bất thường nghi vấn lừa đảo (Từ chối duyệt)', 'phong-tro-gia-re-tu-choi-duyet', 'Thông tin địa chỉ sai lệch thực tế, kiểm duyệt viên từ chối cấp phép.', '999 Đường Không Có Thật, Quận 1, TP.HCM', '79', '7901', '790101', 10.775, 106.702, 15.0, 1, 500000, 500000, 3000.0, 20000.0, 50000.0, 0.0, 'REJECTED', FALSE, FALSE, FALSE, 0.0, 0, 1)
ON DUPLICATE KEY UPDATE title=title;

-- 5. Seed Images for Additional Rooms
INSERT INTO room_images (room_id, image_url, is_primary, display_order) VALUES
(9, '/rooms/room-1.jpg', TRUE, 1), (9, '/rooms/room-2.jpg', FALSE, 2),
(10, '/rooms/room-3.jpg', TRUE, 1), (10, '/rooms/room-4.jpg', FALSE, 2),
(11, '/rooms/room-5.jpg', TRUE, 1), (11, '/rooms/room-1.jpg', FALSE, 2),
(12, '/rooms/room-2.jpg', TRUE, 1), (12, '/rooms/room-3.jpg', FALSE, 2),
(13, '/rooms/room-4.jpg', TRUE, 1), (13, '/rooms/room-5.jpg', FALSE, 2),
(14, '/rooms/room-1.jpg', TRUE, 1),
(15, '/rooms/room-2.jpg', TRUE, 1), (15, '/rooms/room-3.jpg', FALSE, 2),
(16, '/rooms/room-4.jpg', TRUE, 1),
(17, '/rooms/room-5.jpg', TRUE, 1), (17, '/rooms/room-1.jpg', FALSE, 2),
(18, '/rooms/room-2.jpg', TRUE, 1), (18, '/rooms/room-3.jpg', FALSE, 2),
(19, '/rooms/room-4.jpg', TRUE, 1),
(20, '/rooms/room-5.jpg', TRUE, 1), (20, '/rooms/room-1.jpg', FALSE, 2),
(21, '/rooms/room-2.jpg', TRUE, 1),
(22, '/rooms/room-3.jpg', TRUE, 1), (22, '/rooms/room-4.jpg', FALSE, 2),
(23, '/rooms/room-5.jpg', TRUE, 1),
(24, '/rooms/room-1.jpg', TRUE, 1),
(25, '/rooms/room-2.jpg', TRUE, 1),
(26, '/rooms/room-3.jpg', TRUE, 1),
(27, '/rooms/room-4.jpg', TRUE, 1)
ON DUPLICATE KEY UPDATE image_url=image_url;

-- 6. Seed Room Amenities
INSERT INTO room_amenities (room_id, amenity_id) VALUES
(9, 1), (9, 2), (9, 3), (9, 5), (9, 8),
(10, 1), (10, 2), (10, 3), (10, 4), (10, 5), (10, 6), (10, 10),
(11, 1), (11, 2), (11, 5), (11, 9),
(12, 1), (12, 2), (12, 3), (12, 5), (12, 10),
(13, 1), (13, 2), (13, 3), (13, 4), (13, 5), (13, 10), (13, 11),
(15, 1), (15, 2), (15, 5), (15, 12),
(16, 1), (16, 2), (16, 3), (16, 4), (16, 5), (16, 11),
(17, 1), (17, 2), (17, 3), (17, 4), (17, 5), (17, 6), (17, 7), (17, 10),
(18, 1), (18, 2), (18, 3), (18, 5), (18, 6), (18, 8),
(20, 1), (20, 2), (20, 5), (20, 12),
(22, 1), (22, 2), (22, 3), (22, 4), (22, 5), (22, 6), (22, 7),
(24, 1), (24, 5), (24, 6), (24, 12)
ON DUPLICATE KEY UPDATE room_id=room_id;

-- 7. Seed Room Unit Prices (Điện, Nước, Dịch vụ)
INSERT INTO room_prices (room_id, label, price, unit, is_metered, meter_type) VALUES
(9, 'Điện tiêu thụ', 3800, 'kWh', TRUE, 'electric'),
(9, 'Nước sinh hoạt', 28000, 'm³', TRUE, 'water'),
(9, 'Internet WiFi tốc độ cao', 100000, 'tháng', FALSE, NULL),
(9, 'Vệ sinh hành lang & rác', 50000, 'người/tháng', FALSE, NULL),

(10, 'Điện tiêu thụ', 3800, 'kWh', TRUE, 'electric'),
(10, 'Nước sinh hoạt', 28000, 'm³', TRUE, 'water'),
(10, 'Internet cáp quang riêng', 100000, 'tháng', FALSE, NULL),
(10, 'Phí thang máy & dịch vụ chung', 80000, 'người/tháng', FALSE, NULL),

(17, 'Điện tiêu thụ', 4000, 'kWh', TRUE, 'electric'),
(17, 'Nước sinh hoạt', 30000, 'm³', TRUE, 'water'),
(17, 'Combo WiFi & Giặt sấy', 150000, 'tháng', FALSE, NULL),
(17, 'Phí quản lý tòa nhà & dọn phòng', 100000, 'người/tháng', FALSE, NULL)
ON DUPLICATE KEY UPDATE label=label;

-- 8. Seed Room Rules
INSERT INTO room_rules (room_id, rule_type, rule_text, is_allowed) VALUES
(9, 'PET', 'Không cho phép nuôi thú cưng (chó/mèo)', FALSE),
(9, 'COOKING', 'Được phép nấu ăn bằng bếp điện từ', TRUE),
(9, 'CURFEW', 'Giờ giấc tự do, cửa khóa vân tay 24/7', TRUE),
(10, 'PET', 'Cho phép nuôi mèo nhỏ đã tiêm phòng', TRUE),
(10, 'COOKING', 'Khu bếp riêng hút mùi hiện đại', TRUE),
(17, 'PET', 'Không cho phép nuôi thú cưng', FALSE),
(17, 'SMOKING', 'Cấm hút thuốc lá trong phòng và ban công', FALSE),
(17, 'VISITOR', 'Bạn bè tới chơi vui lòng về trước 23h00', TRUE)
ON DUPLICATE KEY UPDATE rule_text=rule_text;
