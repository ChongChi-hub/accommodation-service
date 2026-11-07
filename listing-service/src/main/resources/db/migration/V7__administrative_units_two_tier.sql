-- ==============================================================================
-- V7: Cập nhật Địa giới hành chính theo mô hình 2 cấp (Nghị quyết 202/2025/QH15)
-- 34 Đơn vị cấp Tỉnh (28 Tỉnh + 6 Thành phố trực thuộc Trung ương) -> Xã/Phường
-- ==============================================================================

-- 1. Cập nhật bảng wards: bổ sung province_code và cho phép district_code NULL (legacy)
ALTER TABLE wards ADD COLUMN province_code VARCHAR(20) NULL;
ALTER TABLE wards MODIFY COLUMN district_code VARCHAR(20) NULL;
CREATE INDEX idx_ward_province_code ON wards (province_code);

-- 2. Cập nhật bảng rooms: bổ sung ward_id và cho phép district_code NULL (legacy)
ALTER TABLE rooms ADD COLUMN ward_id BIGINT NULL;
ALTER TABLE rooms MODIFY COLUMN district_code VARCHAR(20) NULL;
CREATE INDEX idx_rooms_ward_id ON rooms (ward_id);

-- 3. Danh mục 34 Đơn vị hành chính cấp Tỉnh (6 Thành phố trực thuộc TW + 28 Tỉnh)
INSERT INTO provinces (code, name, name_en, full_name, full_name_en) VALUES
-- 6 Thành phố trực thuộc Trung ương
('HN', 'Hà Nội', 'Ha Noi', 'Thành phố Hà Nội', 'Ha Noi City'),
('HCM', 'Hồ Chí Minh', 'Ho Chi Minh', 'Thành phố Hồ Chí Minh', 'Ho Chi Minh City'),
('HP', 'Hải Phòng', 'Hai Phong', 'Thành phố Hải Phòng', 'Hai Phong City'),
('DNG', 'Đà Nẵng', 'Da Nang', 'Thành phố Đà Nẵng', 'Da Nang City'),
('CT', 'Cần Thơ', 'Can Tho', 'Thành phố Cần Thơ', 'Can Tho City'),
('HUE', 'Huế', 'Hue', 'Thành phố Huế', 'Hue City'),
-- 28 Tỉnh
('AG', 'An Giang', 'An Giang', 'Tỉnh An Giang', 'An Giang Province'),
('BG', 'Bắc Giang', 'Bac Giang', 'Tỉnh Bắc Giang', 'Bac Giang Province'),
('BK', 'Bắc Kạn', 'Bac Kan', 'Tỉnh Bắc Kạn', 'Bac Kan Province'),
('BL', 'Bạc Liêu', 'Bac Lieu', 'Tỉnh Bạc Liêu', 'Bac Lieu Province'),
('BN', 'Bắc Ninh', 'Bac Ninh', 'Tỉnh Bắc Ninh', 'Bac Ninh Province'),
('BT', 'Bến Tre', 'Ben Tre', 'Tỉnh Bến Tre', 'Ben Tre Province'),
('BD', 'Bình Định', 'Binh Dinh', 'Tỉnh Bình Định', 'Binh Dinh Province'),
('BDG', 'Bình Dương', 'Binh Duong', 'Tỉnh Bình Dương', 'Binh Duong Province'),
('BP', 'Bình Phước', 'Binh Phuoc', 'Tỉnh Bình Phước', 'Binh Phuoc Province'),
('BTH', 'Bình Thuận', 'Binh Thuan', 'Tỉnh Bình Thuận', 'Binh Thuan Province'),
('CM', 'Cà Mau', 'Ca Mau', 'Tỉnh Cà Mau', 'Ca Mau Province'),
('CB', 'Cao Bằng', 'Cao Bang', 'Tỉnh Cao Bằng', 'Cao Bang Province'),
('DL', 'Đắk Lắk', 'Dak Lak', 'Tỉnh Đắk Lắk', 'Dak Lak Province'),
('DKN', 'Đắk Nông', 'Dak Nong', 'Tỉnh Đắk Nông', 'Dak Nong Province'),
('DB', 'Điện Biên', 'Dien Bien', 'Tỉnh Điện Biên', 'Dien Bien Province'),
('DNA', 'Đồng Nai', 'Dong Nai', 'Tỉnh Đồng Nai', 'Dong Nai Province'),
('DT', 'Đồng Tháp', 'Dong Thap', 'Tỉnh Đồng Tháp', 'Dong Thap Province'),
('GL', 'Gia Lai', 'Gia Lai', 'Tỉnh Gia Lai', 'Gia Lai Province'),
('HG', 'Hà Giang', 'Ha Giang', 'Tỉnh Hà Giang', 'Ha Giang Province'),
('HNA', 'Hà Nam', 'Ha Nam', 'Tỉnh Hà Nam', 'Ha Nam Province'),
('HT', 'Hà Tĩnh', 'Ha Tinh', 'Tỉnh Hà Tĩnh', 'Ha Tinh Province'),
('HD', 'Hải Dương', 'Hai Duong', 'Tỉnh Hải Dương', 'Hai Duong Province'),
('HGI', 'Hậu Giang', 'Hau Giang', 'Tỉnh Hậu Giang', 'Hau Giang Province'),
('HB', 'Hòa Bình', 'Hoa Binh', 'Tỉnh Hòa Bình', 'Hoa Binh Province'),
('HY', 'Hưng Yên', 'Hung Yen', 'Tỉnh Hưng Yên', 'Hung Yen Province'),
('KH', 'Khánh Hòa', 'Khanh Hoa', 'Tỉnh Khánh Hòa', 'Khanh Hoa Province'),
('KG', 'Kiên Giang', 'Kien Giang', 'Tỉnh Kiên Giang', 'Kien Giang Province'),
('KT', 'Kon Tum', 'Kon Tum', 'Tỉnh Kon Tum', 'Kon Tum Province')
ON DUPLICATE KEY UPDATE name = VALUES(name), full_name = VALUES(full_name);

-- 4. Danh mục Xã/Phường mới trực thuộc 6 Thành phố trực thuộc Trung ương
INSERT INTO wards (id, code, name, name_en, full_name, full_name_en, province_code, district_code) VALUES
-- Hà Nội (HN) - id 1..22
(1, 'HN_DVH', 'Dịch Vọng Hậu', 'Dich Vong Hau', 'Phường Dịch Vọng Hậu', 'Dich Vong Hau Ward', 'HN', NULL),
(2, 'HN_QH', 'Quan Hoa', 'Quan Hoa', 'Phường Quan Hoa', 'Quan Hoa Ward', 'HN', NULL),
(3, 'HN_DV', 'Dịch Vọng', 'Dich Vong', 'Phường Dịch Vọng', 'Dich Vong Ward', 'HN', NULL),
(4, 'HN_ND', 'Nghĩa Đô', 'Nghia Do', 'Phường Nghĩa Đô', 'Nghia Do Ward', 'HN', NULL),
(5, 'HN_MD', 'Mai Dịch', 'Mai Dich', 'Phường Mai Dịch', 'Mai Dich Ward', 'HN', NULL),
(6, 'HN_LH', 'Láng Hạ', 'Lang Ha', 'Phường Láng Hạ', 'Lang Ha Ward', 'HN', NULL),
(7, 'HN_OCD', 'Ô Chợ Dừa', 'O Cho Dua', 'Phường Ô Chợ Dừa', 'O Cho Dua Ward', 'HN', NULL),
(8, 'HN_CL', 'Cát Linh', 'Cat Linh', 'Phường Cát Linh', 'Cat Linh Ward', 'HN', NULL),
(9, 'HN_BK', 'Bách Khoa', 'Bach Khoa', 'Phường Bách Khoa', 'Bach Khoa Ward', 'HN', NULL),
(10, 'HN_DT', 'Đồng Tâm', 'Dong Tam', 'Phường Đồng Tâm', 'Dong Tam Ward', 'HN', NULL),
(11, 'HN_TT', 'Tràng Tiền', 'Trang Tien', 'Phường Tràng Tiền', 'Trang Tien Ward', 'HN', NULL),
(12, 'HN_HM', 'Hàng Mã', 'Hang Ma', 'Phường Hàng Mã', 'Hang Ma Ward', 'HN', NULL),
(13, 'HN_MD1', 'Mỹ Đình 1', 'My Dinh 1', 'Phường Mỹ Đình 1', 'My Dinh 1 Ward', 'HN', NULL),
(14, 'HN_MD2', 'Mỹ Đình 2', 'My Dinh 2', 'Phường Mỹ Đình 2', 'My Dinh 2 Ward', 'HN', NULL),
(15, 'HN_MT', 'Mễ Trì', 'Me Tri', 'Phường Mễ Trì', 'Me Tri Ward', 'HN', NULL),
(16, 'HN_CN1', 'Cổ Nhuế 1', 'Co Nhue 1', 'Phường Cổ Nhuế 1', 'Co Nhue 1 Ward', 'HN', NULL),
(17, 'HN_CN2', 'Cổ Nhuế 2', 'Co Nhue 2', 'Phường Cổ Nhuế 2', 'Co Nhue 2 Ward', 'HN', NULL),
(18, 'HN_XD', 'Xuân Đỉnh', 'Xuan Dinh', 'Phường Xuân Đỉnh', 'Xuan Dinh Ward', 'HN', NULL),
(19, 'HN_YH', 'Yên Hòa', 'Yen Hoa', 'Phường Yên Hòa', 'Yen Hoa Ward', 'HN', NULL),
(20, 'HN_TH', 'Trung Hòa', 'Trung Hoa', 'Phường Trung Hòa', 'Trung Hoa Ward', 'HN', NULL),
(21, 'HN_KM', 'Kim Mã', 'Kim Ma', 'Phường Kim Mã', 'Kim Ma Ward', 'HN', NULL),
(22, 'HN_GV', 'Giảng Võ', 'Giang Vo', 'Phường Giảng Võ', 'Giang Vo Ward', 'HN', NULL),

-- TP. Hồ Chí Minh (HCM) - id 23..35
(23, 'HCM_BN', 'Bến Nghé', 'Ben Nghe', 'Phường Bến Nghé', 'Ben Nghe Ward', 'HCM', NULL),
(24, 'HCM_BT', 'Bến Thành', 'Ben Thanh', 'Phường Bến Thành', 'Ben Thanh Ward', 'HCM', NULL),
(25, 'HCM_CG', 'Cô Giang', 'Co Giang', 'Phường Cô Giang', 'Co Giang Ward', 'HCM', NULL),
(26, 'HCM_COL', 'Cầu Ông Lãnh', 'Cau Ong Lanh', 'Phường Cầu Ông Lãnh', 'Cau Ong Lanh Ward', 'HCM', NULL),
(27, 'HCM_VTS', 'Võ Thị Sáu', 'Vo Thi Sau', 'Phường Võ Thị Sáu', 'Vo Thi Sau Ward', 'HCM', NULL),
(28, 'HCM_DK', 'Đa Kao', 'Da Kao', 'Phường Đa Kao', 'Da Kao Ward', 'HCM', NULL),
(29, 'HCM_TD', 'Thảo Điền', 'Thao Dien', 'Phường Thảo Điền', 'Thao Dien Ward', 'HCM', NULL),
(30, 'HCM_AP', 'An Phú', 'An Phu', 'Phường An Phú', 'An Phu Ward', 'HCM', NULL),
(31, 'HCM_TP', 'Tân Phong', 'Tan Phong', 'Phường Tân Phong', 'Tan Phong Ward', 'HCM', NULL),
(32, 'HCM_PM', 'Phú Mỹ', 'Phu My', 'Phường Phú Mỹ', 'Phu My Ward', 'HCM', NULL),
(33, 'HCM_P1', 'Phường 1', 'Ward 1', 'Phường 1', 'Ward 1', 'HCM', NULL),
(34, 'HCM_P2', 'Phường 2', 'Ward 2', 'Phường 2', 'Ward 2', 'HCM', NULL),
(35, 'HCM_P5', 'Phường 5', 'Ward 5', 'Phường 5', 'Ward 5', 'HCM', NULL),

-- Đà Nẵng (DNG) - id 36..42
(36, 'DNG_HC1', 'Hải Châu 1', 'Hai Chau 1', 'Phường Hải Châu 1', 'Hai Chau 1 Ward', 'DNG', NULL),
(37, 'DNG_HC2', 'Hải Châu 2', 'Hai Chau 2', 'Phường Hải Châu 2', 'Hai Chau 2 Ward', 'DNG', NULL),
(38, 'DNG_TT', 'Thạch Thang', 'Thach Thang', 'Phường Thạch Thang', 'Thach Thang Ward', 'DNG', NULL),
(39, 'DNG_MA', 'Mỹ An', 'My An', 'Phường Mỹ An', 'My An Ward', 'DNG', NULL),
(40, 'DNG_KM', 'Khuê Mỹ', 'Khue My', 'Phường Khuê Mỹ', 'Khue My Ward', 'DNG', NULL),
(41, 'DNG_AHB', 'An Hải Bắc', 'An Hai Bac', 'Phường An Hải Bắc', 'An Hai Bac Ward', 'DNG', NULL),
(42, 'DNG_PM', 'Phước Mỹ', 'Phuoc My', 'Phường Phước Mỹ', 'Phuoc My Ward', 'DNG', NULL),

-- Hải Phòng (HP) - id 43..46
(43, 'HP_HVT', 'Hoàng Văn Thụ', 'Hoang Van Thu', 'Phường Hoàng Văn Thụ', 'Hoang Van Thu Ward', 'HP', NULL),
(44, 'HP_MK', 'Minh Khai', 'Minh Khai', 'Phường Minh Khai', 'Minh Khai Ward', 'HP', NULL),
(45, 'HP_LT', 'Lạch Tray', 'Lach Tray', 'Phường Lạch Tray', 'Lach Tray Ward', 'HP', NULL),
(46, 'HP_DG', 'Đằng Giang', 'Dang Giang', 'Phường Đằng Giang', 'Dang Giang Ward', 'HP', NULL),

-- Cần Thơ (CT) - id 47..50
(47, 'CT_NK', 'Ninh Kiều', 'Ninh Kieu', 'Phường Ninh Kiều', 'Ninh Kieu Ward', 'CT', NULL),
(48, 'CT_TA', 'Tân An', 'Tan An', 'Phường Tân An', 'Tan An Ward', 'CT', NULL),
(49, 'CT_AK', 'An Khánh', 'An Khanh', 'Phường An Khánh', 'An Khanh Ward', 'CT', NULL),
(50, 'CT_CK', 'Cái Khế', 'Cai Khe', 'Phường Cái Khế', 'Cai Khe Ward', 'CT', NULL),

-- Huế (HUE) - id 51..54
(51, 'HUE_VN', 'Vĩnh Ninh', 'Vinh Ninh', 'Phường Vĩnh Ninh', 'Vinh Ninh Ward', 'HUE', NULL),
(52, 'HUE_PN', 'Phú Nhuận', 'Phu Nhuan', 'Phường Phú Nhuận', 'Phu Nhuan Ward', 'HUE', NULL),
(53, 'HUE_TL', 'Thuận Lộc', 'Thuan Loc', 'Phường Thuận Lộc', 'Thuan Loc Ward', 'HUE', NULL),
(54, 'HUE_TT', 'Thuận Thành', 'Thuan Thanh', 'Phường Thuận Thành', 'Thuan Thanh Ward', 'HUE', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), full_name = VALUES(full_name), province_code = VALUES(province_code);
