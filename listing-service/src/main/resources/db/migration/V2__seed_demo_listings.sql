-- Seed Provinces
INSERT INTO provinces (code, name, name_en, full_name, full_name_en) VALUES
('HN', 'Hà Nội', 'Ha Noi', 'Thành phố Hà Nội', 'Ha Noi City'),
('HCM', 'Hồ Chí Minh', 'Ho Chi Minh', 'Thành phố Hồ Chí Minh', 'Ho Chi Minh City'),
('DNG', 'Đà Nẵng', 'Da Nang', 'Thành phố Đà Nẵng', 'Da Nang City')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed Districts
INSERT INTO districts (code, name, name_en, full_name, full_name_en, province_code) VALUES
('HN-CG', 'Cầu Giấy', 'Cau Giay', 'Quận Cầu Giấy', 'Cau Giay District', 'HN'),
('HN-DD', 'Đống Đa', 'Dong Da', 'Quận Đống Đa', 'Dong Da District', 'HN'),
('HN-TX', 'Thanh Xuân', 'Thanh Xuan', 'Quận Thanh Xuân', 'Thanh Xuan District', 'HN'),
('HN-BD', 'Ba Đình', 'Ba Dinh', 'Quận Ba Đình', 'Ba Dinh District', 'HN'),
('HN-NTL', 'Nam Từ Liêm', 'Nam Tu Liem', 'Quận Nam Từ Liêm', 'Nam Tu Liem District', 'HN'),
('HCM-Q1', 'Quận 1', 'District 1', 'Quận 1', 'District 1', 'HCM'),
('HCM-Q7', 'Quận 7', 'District 7', 'Quận 7', 'District 7', 'HCM'),
('HCM-BT', 'Bình Thạnh', 'Binh Thanh', 'Quận Bình Thạnh', 'Binh Thanh District', 'HCM')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed Wards
INSERT INTO wards (code, name, name_en, full_name, full_name_en, district_code) VALUES
('HN-CG-DH', 'Dịch Vọng Hậu', 'Dich Vong Hau', 'Phường Dịch Vọng Hậu', 'Dich Vong Hau Ward', 'HN-CG'),
('HN-CG-QC', 'Quan Hoa', 'Quan Hoa', 'Phường Quan Hoa', 'Quan Hoa Ward', 'HN-CG'),
('HN-DD-LH', 'Láng Hạ', 'Lang Ha', 'Phường Láng Hạ', 'Lang Ha Ward', 'HN-DD'),
('HCM-Q1-BN', 'Bến Nghé', 'Ben Nghe', 'Phường Bến Nghé', 'Ben Nghe Ward', 'HCM-Q1'),
('HCM-BT-W25', 'Phường 25', 'Ward 25', 'Phường 25', 'Ward 25', 'HCM-BT')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed Room Types
INSERT INTO room_types (code, name, description) VALUES
('MINI_APARTMENT', 'Chung cư mini', 'Phòng khép kín đầy đủ đồ tiện nghi hiện đại'),
('STUDIO', 'Căn hộ Studio', 'Không gian mở kết hợp phòng ngủ và bếp'),
('APARTMENT', 'Căn hộ nguyên căn', 'Căn hộ gia đình 2-3 phòng ngủ'),
('HOMESTAY', 'Phòng trọ cao cấp', 'Phòng riêng trong nhà nguyên căn sạch sẽ')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed Amenities
INSERT INTO amenities (id, code, name, icon, category) VALUES
(1, 'wifi', 'Wifi tốc độ cao', 'Wifi', 'BASIC'),
(2, 'air_conditioner', 'Điều hòa 2 chiều', 'Wind', 'BASIC'),
(3, 'water_heater', 'Bình nóng lạnh', 'Flame', 'BASIC'),
(4, 'refrigerator', 'Tủ lạnh Inverter', 'Refrigerator', 'FURNITURE'),
(5, 'washing_machine', 'Máy giặt riêng', 'WashingMachine', 'FURNITURE'),
(6, 'kitchen', 'Bếp nấu ăn riêng', 'Utensils', 'FACILITY'),
(7, 'balcony', 'Ban công thoáng mát', 'Sun', 'FACILITY'),
(8, 'elevator', 'Thang máy thẻ từ', 'ArrowUpDown', 'BUILDING'),
(9, 'parking', 'Nhà để xe rộng rãi', 'Car', 'BUILDING'),
(10, 'security', 'Bảo vệ & Khóa vân tay', 'ShieldCheck', 'BUILDING')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed Rooms
INSERT INTO rooms (id, landlord_id, landlord_name, landlord_phone, room_type_id, room_type_code, title, description, address, province_code, district_code, ward_code, latitude, longitude, area, max_occupants, price, deposit, electricity_cost, water_cost, internet_cost, service_cost, status, is_approved, allow_pets, allow_cooking, rating_avg, review_count) VALUES
(1, 2, 'Trần Minh Tuấn', '0912345678', 1, 'MINI_APARTMENT', 'Chung cư mini ban công rộng tại Dịch Vọng Hậu, Cầu Giấy', 'Phòng full nội thất cao cấp: giường đệm, tủ quần áo âm tường, điều hòa Daikin, tủ lạnh 2 cánh, máy giặt riêng trên ban công phơi đồ. Giờ giấc tự do, khóa cửa vân tay, camera an ninh 24/7.', 'Số 15 ngõ 92 Trần Thái Tông, Dịch Vọng Hậu', 'HN', 'HN-CG', 'HN-CG-DH', 21.0315, 105.7892, 32.0, 2, 4800000.00, 4800000.00, 3800.00, 30000.00, 100000.00, 50000.00, 'AVAILABLE', true, false, true, 4.9, 12),
(2, 2, 'Trần Minh Tuấn', '0912345678', 2, 'STUDIO', 'Studio ngập tràn ánh sáng view công viên Quan Hoa', 'Căn hộ Studio thoáng đãng với cửa sổ kính lớn, nội thất gỗ sồi ấm cúng. Bếp từ đôi, hút mùi hiện đại, máy nước nóng Ariston. Thích hợp cho người đi làm hoặc sinh viên các trường ĐH Quốc Gia, Sư Phạm.', 'Ngõ 118 Nguyễn Khánh Toàn, Quan Hoa', 'HN', 'HN-CG', 'HN-CG-QC', 21.0387, 105.7995, 28.0, 2, 4200000.00, 4200000.00, 3500.00, 25000.00, 80000.00, 40000.00, 'AVAILABLE', true, true, true, 4.8, 8),
(3, 2, 'Trần Minh Tuấn', '0912345678', 1, 'MINI_APARTMENT', 'Căn hộ mini trung tâm Láng Hạ, gần rạp chiếu phim Quốc Gia', 'Tòa nhà thang máy 7 tầng mới hoàn thiện, ô tô đỗ cửa. Phòng có sofa giường, tivi thông minh, máy sấy quần áo chung ở sân thượng. Khu dân trí cao, yên tĩnh.', 'Số 42 ngõ 105 Láng Hạ, Đống Đa', 'HN', 'HN-DD', 'HN-DD-LH', 21.0175, 105.8152, 35.0, 3, 5500000.00, 5500000.00, 4000.00, 100000.00, 100000.00, 70000.00, 'AVAILABLE', true, false, true, 5.0, 15),
(4, 2, 'Trần Minh Tuấn', '0912345678', 2, 'STUDIO', 'Studio cao cấp Bến Nghé Quận 1, phố đi bộ Nguyễn Huệ', 'Vị trí đắc địa ngay trung tâm Sài Gòn, cách chợ Bến Thành 5 phút. Nội thất phong cách Scandinavian tinh tế, tiện nghi 5 sao, máy giặt sấy trong phòng, lễ tân tòa nhà.', 'Đường Ngô Đức Kế, Phường Bến Nghé, Quận 1', 'HCM', 'HCM-Q1', 'HCM-Q1-BN', 10.7725, 106.7042, 30.0, 2, 8500000.00, 8500000.00, 4000.00, 30000.00, 120000.00, 100000.00, 'AVAILABLE', true, false, true, 4.9, 19),
(5, 2, 'Trần Minh Tuấn', '0912345678', 4, 'HOMESTAY', 'Phòng tiện nghi sạch đẹp gần ĐH Ngoại Thương, Bình Thạnh', 'Nhà mới xây khu D2 Bình Thạnh, gần các trường đại học Hutech, Ngoại Thương, Giao Thông Vận Tải. Có phòng tập gym mini tầng trệt cho cư dân, sân phơi rộng rãi.', 'Đường D2, Phường 25, Bình Thạnh', 'HCM', 'HCM-BT', 'HCM-BT-W25', 10.8032, 106.7145, 25.0, 2, 3800000.00, 3800000.00, 3800.00, 25000.00, 100000.00, 50000.00, 'AVAILABLE', true, true, true, 4.7, 6),
(6, 2, 'Trần Minh Tuấn', '0912345678', 3, 'APARTMENT', 'Căn hộ dịch vụ 2 phòng ngủ Quận 7 gần Phú Mỹ Hưng', 'Căn hộ 2 phòng ngủ riêng biệt, phòng khách rộng, ban công view sông thoáng mát. Hồ bơi và công viên nội khu miễn phí. Phù hợp cho nhóm bạn 3-4 người hoặc gia đình trẻ.', 'Nguyễn Thị Thập, Tân Phú, Quận 7', 'HCM', 'HCM-Q7', NULL, 10.7382, 106.7215, 65.0, 4, 11000000.00, 11000000.00, 3500.00, 25000.00, 150000.00, 120000.00, 'AVAILABLE', true, true, true, 4.9, 14)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- Seed Room Images
INSERT INTO room_images (room_id, image_url, is_primary, display_order) VALUES
(1, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', true, 1),
(1, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', false, 2),
(1, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', false, 3),
(2, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1200&q=80', true, 1),
(2, 'https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=800&q=80', false, 2),
(3, 'https://images.unsplash.com/photo-1502005229762-ee1b2b93e083?auto=format&fit=crop&w=1200&q=80', true, 1),
(3, 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', false, 2),
(4, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=1200&q=80', true, 1),
(5, 'https://images.unsplash.com/photo-1536376072261-38c75010e6c9?auto=format&fit=crop&w=1200&q=80', true, 1),
(6, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1200&q=80', true, 1)
ON DUPLICATE KEY UPDATE image_url=VALUES(image_url);

-- Seed Room Amenities
INSERT INTO room_amenities (room_id, amenity_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 10),
(2, 1), (2, 2), (2, 3), (2, 6), (2, 7), (2, 9), (2, 10),
(3, 1), (3, 2), (3, 3), (3, 4), (3, 6), (3, 8), (3, 9), (3, 10),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 8), (4, 10),
(5, 1), (5, 2), (5, 3), (5, 6), (5, 9), (5, 10),
(6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 7), (6, 8), (6, 9), (6, 10)
ON DUPLICATE KEY UPDATE amenity_id=VALUES(amenity_id);

-- Seed Reviews
INSERT INTO reviews (id, room_id, tenant_id, tenant_name, rating, comment, landlord_reply) VALUES
(1, 1, 1, 'Nguyễn Văn Thuê', 5, 'Phòng rất đẹp và mới giống y hình ảnh chụp. Anh Tuấn chủ nhà hỗ trợ rất nhiệt tình, thủ tục hợp đồng minh bạch.', 'Cảm ơn em đã tin tưởng và thuê phòng tại Tổ Ấm, có cần hỗ trợ gì cứ nhắn anh nhé!'),
(2, 1, 3, 'Lê Thị Mai', 5, 'Khu phố an ninh, yên tĩnh, đi bộ ra chợ và bến xe buýt rất gần. Phòng cách âm tốt, điều hòa mát rượi.', NULL),
(3, 2, 1, 'Nguyễn Văn Thuê', 4, 'Phòng thoáng mát nhiều ánh sáng, ban công ngắm hoàng hôn rất chill. Chỉ có ngõ vào hơi nhỏ một chút.', 'Cảm ơn em đã đóng góp ý kiến, bên anh đã lắp thêm đèn chiếu sáng ngõ để đi lại an toàn hơn rồi nhé.')
ON DUPLICATE KEY UPDATE rating=VALUES(rating);
