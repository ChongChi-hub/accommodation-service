-- ==============================================================================
-- Repeatable Migration R__04: Seed Home Categories Coverage & Licensed Photos
-- Ensures all 4 categories have >= 6 active rooms across >= 3 provinces.
-- All 8 key provinces (HN, HCM, DNG, HP, CT, HUE, BDG, BN) have active listings.
-- SHARED rooms use price_unit = 'BED'. Natural Vietnamese text without banned words.
-- Safe and idempotent (DELETE and re-INSERT by fixed ID range 201-232).
-- ==============================================================================

DELETE FROM room_amenities WHERE room_id BETWEEN 201 AND 232;
DELETE FROM room_images WHERE room_id BETWEEN 201 AND 232;
DELETE FROM rooms WHERE id BETWEEN 201 AND 232;

INSERT INTO rooms (
    id, room_uuid, landlord_id, landlord_name, landlord_phone,
    room_type_id, room_type_code, title, title_search, slug, description,
    address, province_code, district_code, ward_code, ward_id,
    latitude, longitude, area, max_occupants,
    price, price_unit, deposit, electricity_cost, water_cost, internet_cost, service_cost,
    status, is_approved, allow_pets, allow_cooking, rating_avg, review_count, version,
    published_at, hidden_at, hidden_by, hidden_reason, rejected_reason, view_count
) VALUES
-- ==============================================================================
-- 1. NHA NGUYEN CAN (FULL_HOUSE, room_type_id=7) - 8 rooms across 8 provinces
-- ==============================================================================
(201, 'room-00000004-0000-0000-0000-000000000201', 1, 'Trần Thị Chủ Trọ', '0902000002',
 7, 'FULL_HOUSE', 'Nhà nguyên căn 3 tầng có ban công ngõ thông thoáng Cầu Giấy', 'nha nguyen can 3 tang co ban cong ngo thong thoang cau giay', 'nha-nguyen-can-cau-giay-201',
 'Cho thuê nhà nguyên căn 3 tầng, sân để xe riêng, có ban công đón gió mát. Khu dân trí cao, an ninh tốt, gần chợ và trạm xe buýt.',
 'Số 18 ngõ 260 Cầu Giấy, Phường Quan Hoa, Hà Nội', 'HN', NULL, 'HN_QH', 2,
 21.035, 105.795, 85.0, 6, 12000000, 'ROOM', 12000000, 3500.0, 25000.0, 100000.0, 0.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 11, 1, '2026-02-01 08:00:00', NULL, NULL, NULL, NULL, 510),

(202, 'room-00000004-0000-0000-0000-000000000202', 2, 'Trần Minh Tuấn', '0903000003',
 7, 'FULL_HOUSE', 'Nhà phố nguyên căn 1 trệt 2 lầu có ban công rộng Quận 10', 'nha pho nguyen can 1 tret 2 lau co ban cong rong quan 10', 'nha-nguyen-can-quan-10-202',
 'Nhà thiết kế hiện đại, 3 phòng ngủ riêng biệt, phòng khách rộng rãi, có ban công trồng cây xanh. Cho nuôi thú cưng nhỏ, giờ giấc tự do.',
 'Số 42 đường Tô Hiến Thành, Phường 15, TP.HCM', 'HCM', NULL, 'HCM_P15', 10,
 10.778, 106.665, 92.0, 6, 15000000, 'ROOM', 15000000, 3500.0, 25000.0, 120000.0, 0.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 5.0, 8, 1, '2026-02-05 09:30:00', NULL, NULL, NULL, NULL, 640),

(203, 'room-00000004-0000-0000-0000-000000000203', 3, 'Hoàng Thị Mai Lan', '0904000004',
 7, 'FULL_HOUSE', 'Nhà nguyên căn ven sông Hàn sân thượng thoáng mát Đà Nẵng', 'nha nguyen can ven song han san thuong thoang mat da nang', 'nha-nguyen-can-da-nang-203',
 'Nhà gần cầu Rồng, đường ô tô vào tận cửa, sân thượng ngắm trọn cảnh sông Hàn. Phù hợp cho gia đình hoặc nhóm bạn làm việc từ xa.',
 'Số 15 đường Trần Hưng Đạo, Phường An Hải Tây, Đà Nẵng', 'DNG', NULL, 'DNG_AHT', 20,
 16.061, 108.232, 78.0, 5, 9500000, 'ROOM', 9500000, 3200.0, 20000.0, 100000.0, 0.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 15, 1, '2026-02-08 14:00:00', NULL, NULL, NULL, NULL, 430),

(204, 'room-00000004-0000-0000-0000-000000000204', 4, 'Phan Thanh Bình', '0905000005',
 7, 'FULL_HOUSE', 'Nhà riêng 2 tầng mặt ngõ nông trung tâm Lê Chân Hải Phòng', 'nha rieng 2 tang mat ngo nong trung tam le chan hai phong', 'nha-nguyen-can-hai-phong-204',
 'Nhà nguyên căn sạch đẹp, 2 phòng ngủ có gác lửng để đồ rộng rãi, bếp ăn đầy đủ tiện nghi, nước máy mạnh.',
 'Số 29 ngõ Mê Linh, Phường An Biên, Hải Phòng', 'HP', NULL, 'HP_AB', 30,
 20.852, 106.681, 70.0, 4, 8000000, 'ROOM', 8000000, 3000.0, 22000.0, 90000.0, 0.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 6, 1, '2026-02-10 11:00:00', NULL, NULL, NULL, NULL, 310),

(205, 'room-00000004-0000-0000-0000-000000000205', 5, 'Nguyễn Thị Ngọc Ánh', '0906000006',
 7, 'FULL_HOUSE', 'Nhà vườn nguyên căn yên tĩnh ngõ Vỹ Dạ Cố đô Huế', 'nha vuon nguyen can yen tinh ngo vy da co do hue', 'nha-nguyen-can-hue-205',
 'Không gian nhà vườn thanh tịnh gần sông Hương, sân gạch trước nhà mát rượi, 2 phòng ngủ ấm cúng nội thất gỗ.',
 'Số 8 kiệt 120 Nguyễn Sinh Cung, Phường Vỹ Dạ, Huế', 'HUE', NULL, 'HUE_VD', 40,
 16.482, 107.604, 80.0, 4, 7000000, 'ROOM', 7000000, 3000.0, 18000.0, 80000.0, 0.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 10, 1, '2026-02-12 10:00:00', NULL, NULL, NULL, NULL, 280),

(206, 'room-00000004-0000-0000-0000-000000000206', 1, 'Trần Thị Chủ Trọ', '0902000002',
 7, 'FULL_HOUSE', 'Nhà phố liền kề 2 tầng khu dân cư Chánh Nghĩa Bình Dương', 'nha pho lien ke 2 tang khu dan cu chanh nghia binh duong', 'nha-nguyen-can-binh-duong-206',
 'Nhà mới xây xong trong khu dân cư yên tĩnh, đường trước nhà 6 mét ô tô đậu, 3 phòng ngủ có máy lạnh.',
 'Số 54 đường D8, Phường Chánh Nghĩa, Bình Dương', 'BDG', NULL, 'BDG_CN', 50,
 10.978, 106.661, 95.0, 6, 8500000, 'ROOM', 8500000, 3200.0, 20000.0, 100000.0, 0.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 7, 1, '2026-02-15 15:30:00', NULL, NULL, NULL, NULL, 390),

(207, 'room-00000004-0000-0000-0000-000000000207', 2, 'Trần Minh Tuấn', '0903000003',
 7, 'FULL_HOUSE', 'Nhà riêng 3 tầng có sân phơi và ban công Phường Tiền An Bắc Ninh', 'nha rieng 3 tang co san phoi va ban cong phuong tien an bac ninh', 'nha-nguyen-can-bac-ninh-207',
 'Nhà nằm ngay trung tâm thành phố Bắc Ninh, gần chợ và công viên, các phòng đều có cửa sổ đón nắng.',
 'Số 16 đường Ngô Gia Tự, Phường Tiền An, Bắc Ninh', 'BN', NULL, 'BN_TA', 60,
 21.185, 106.072, 88.0, 5, 7500000, 'ROOM', 7500000, 3000.0, 20000.0, 80000.0, 0.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 5, 1, '2026-02-18 09:00:00', NULL, NULL, NULL, NULL, 260),

(208, 'room-00000004-0000-0000-0000-000000000208', 3, 'Hoàng Thị Mai Lan', '0904000004',
 7, 'FULL_HOUSE', 'Nhà trệt có gác lửng đúc sân vườn mát mẻ Ninh Kiều Cần Thơ', 'nha tret co gac lung duc san vuon mat me ninh kieu can tho', 'nha-nguyen-can-can-tho-208',
 'Nhà có gác lửng đúc kiên cố, sân trước trồng cây ăn trái mát mẻ quanh năm, gần bến Ninh Kiều và các trường đại học.',
 'Số 77 đường Mậu Thân, Phường An Hòa, Cần Thơ', 'CT', NULL, 'CT_AH', 70,
 10.041, 105.772, 75.0, 4, 6500000, 'ROOM', 6500000, 3000.0, 18000.0, 80000.0, 0.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 9, 1, '2026-02-20 16:00:00', NULL, NULL, NULL, NULL, 340),

-- ==============================================================================
-- 2. O GHEP (SHARED, room_type_id=8, price_unit='BED') - 8 rooms across 8 provinces
-- ==============================================================================
(209, 'room-00000004-0000-0000-0000-000000000209', 1, 'Trần Thị Chủ Trọ', '0902000002',
 8, 'SHARED', 'Giường đơn phòng ở ghép sạch sẽ có bàn học ngõ Chùa Láng Hà Nội', 'giuong don phong o ghep sach se co ban hoc ngo chua lang ha noi', 'o-ghep-chua-lang-209',
 'Phòng ở ghép cao cấp chỉ 2 người một phòng, có gác lửng để đồ, điều hòa Inverter mát mẻ, bàn học riêng.',
 'Số 85 ngõ Chùa Láng, Phường Láng Thượng, Hà Nội', 'HN', NULL, 'HN_LT', 3,
 21.026, 105.801, 26.0, 2, 1800000, 'BED', 1800000, 3800.0, 30000.0, 50000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 12, 1, '2026-02-02 08:30:00', NULL, NULL, NULL, NULL, 480),

(210, 'room-00000004-0000-0000-0000-000000000210', 2, 'Trần Minh Tuấn', '0903000003',
 8, 'SHARED', 'Chỗ ở ghép căn hộ dịch vụ cao cấp ban công thoáng Quận Bình Thạnh', 'cho o ghep can ho dich vu cao cap ban cong thoang quan binh thanh', 'o-ghep-binh-thanh-210',
 'Phòng twin 2 giường đơn trong căn hộ cao cấp, có ban công view Landmark 81, giờ giấc tự do, máy giặt máy sấy.',
 'Số 135 đường D2 (Nguyễn Gia Trí), Phường 25, TP.HCM', 'HCM', NULL, 'HCM_P25', 12,
 10.803, 106.714, 32.0, 2, 2200000, 'BED', 2200000, 3800.0, 30000.0, 50000.0, 50000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 5.0, 16, 1, '2026-02-04 14:00:00', NULL, NULL, NULL, NULL, 620),

(211, 'room-00000004-0000-0000-0000-000000000211', 3, 'Hoàng Thị Mai Lan', '0904000004',
 8, 'SHARED', 'Giường ở ghép gần biển Mỹ Khê phòng thoáng ban công Đà Nẵng', 'giuong o ghep gan bien my khe phong thoang ban cong da nang', 'o-ghep-my-khe-da-nang-211',
 'Phòng ở ghép homestay thoáng gió biển, ban công rộng phơi đồ đón nắng, môi trường thân thiện sạch sẽ.',
 'Số 22 đường Võ Văn Kiệt, Phường Phước Mỹ, Đà Nẵng', 'DNG', NULL, 'DNG_PM', 22,
 16.065, 108.241, 30.0, 2, 1500000, 'BED', 1500000, 3200.0, 25000.0, 50000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 14, 1, '2026-02-06 10:15:00', NULL, NULL, NULL, NULL, 390),

(212, 'room-00000004-0000-0000-0000-000000000212', 4, 'Phan Thanh Bình', '0905000005',
 8, 'SHARED', 'Phòng ở ghép sinh viên ĐH Cần Thơ đầy đủ máy lạnh wifi', 'phong o ghep sinh vien dh can tho day du may lanh wifi', 'o-ghep-dai-hoc-can-tho-212',
 'Phòng 2 bạn nam, giường tầng gỗ chắc chắn, bàn học đèn led, bếp nấu riêng ngoài hành lang, ra vào khóa vân tay.',
 'Số 91 đường 3 Tháng 2, Phường Xuân Khánh, Cần Thơ', 'CT', NULL, 'CT_XK', 72,
 10.029, 105.768, 25.0, 2, 1400000, 'BED', 1400000, 3000.0, 20000.0, 40000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 8, 1, '2026-02-08 09:00:00', NULL, NULL, NULL, NULL, 270),

(213, 'room-00000004-0000-0000-0000-000000000213', 5, 'Nguyễn Thị Ngọc Ánh', '0906000006',
 8, 'SHARED', 'Giường ở ghép gần ĐH Sư Phạm Huế không gian học tập yên tĩnh', 'giuong o ghep gan dh su pham hue khong gian hoc tap yen tinh', 'o-ghep-su-pham-hue-213',
 'Phòng 2 giường đơn có cửa sổ thông thoáng nhìn ra vườn cây, bàn học đôi kèm giá sách, bình nóng lạnh.',
 'Số 34 đường Lê Lợi, Phường Phú Hội, Huế', 'HUE', NULL, 'HUE_PH', 42,
 16.467, 107.595, 24.0, 2, 1200000, 'BED', 1200000, 3000.0, 20000.0, 30000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 11, 1, '2026-02-11 11:30:00', NULL, NULL, NULL, NULL, 230),

(214, 'room-00000004-0000-0000-0000-000000000214', 1, 'Trần Thị Chủ Trọ', '0902000002',
 8, 'SHARED', 'Chỗ ở ghép căn hộ hiện đại gần khu công nghiệp VSIP Bình Dương', 'cho o ghep can ho hien dai gan khu cong nghiep vsip binh duong', 'o-ghep-vsip-binh-duong-214',
 'Phòng twin trong căn hộ chung cư cao cấp, hồ bơi và phòng gym nội khu, có tủ quần áo riêng có khóa cá nhân.',
 'Đại lộ Hữu Nghị, Phường Bình Hòa, Bình Dương', 'BDG', NULL, 'BDG_BH', 52,
 10.925, 106.702, 35.0, 2, 1700000, 'BED', 1700000, 3200.0, 25000.0, 50000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 7, 1, '2026-02-14 15:00:00', NULL, NULL, NULL, NULL, 310),

(215, 'room-00000004-0000-0000-0000-000000000215', 2, 'Trần Minh Tuấn', '0903000003',
 8, 'SHARED', 'Giường phòng ở ghép có gác lửng ngõ Lạch Tray Hải Phòng', 'giuong phong o ghep co gac lung ngo lach tray hai phong', 'o-ghep-lach-tray-hai-phong-215',
 'Nhà có gác lửng cao ráo, chia làm 2 khu vực ngủ và sinh hoạt riêng biệt, máy giặt dùng chung, sân phơi rộng.',
 'Số 112 ngõ Lạch Tray, Phường Lạch Tray, Hải Phòng', 'HP', NULL, 'HP_LT', 32,
 20.841, 106.692, 28.0, 2, 1600000, 'BED', 1600000, 3000.0, 20000.0, 40000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 6, 1, '2026-02-16 08:30:00', NULL, NULL, NULL, NULL, 250),

(216, 'room-00000004-0000-0000-0000-000000000216', 3, 'Hoàng Thị Mai Lan', '0904000004',
 8, 'SHARED', 'Phòng ở ghép cho công chức và kỹ sư khu đô thị mới Bắc Ninh', 'phong o ghep cho cong chuc va ky su khu do thi moi bac ninh', 'o-ghep-do-thi-bac-ninh-216',
 'Phòng ở ghép 2 người văn minh lịch sự, giường nệm cao su non êm ái, tủ locker riêng, có ban công hóng gió.',
 'Số 48 đường Lý Thái Tổ, Phường Võ Cường, Bắc Ninh', 'BN', NULL, 'BN_VC', 62,
 21.171, 106.061, 30.0, 2, 1500000, 'BED', 1500000, 3000.0, 20000.0, 40000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 9, 1, '2026-02-18 14:15:00', NULL, NULL, NULL, NULL, 290),

-- ==============================================================================
-- 3. PHONG TRO (SINGLE/HOMESTAY, room_type_id=6, price_unit='ROOM') - 8 rooms
-- ==============================================================================
(217, 'room-00000004-0000-0000-0000-000000000217', 4, 'Phan Thanh Bình', '0905000005',
 6, 'SINGLE', 'Phòng trọ khép kín có gác lửng đúc ngõ Hùng Vương Huế', 'phong tro khep kin co gac lung duc ngo hung vuong hue', 'phong-tro-gac-lung-hue-217',
 'Phòng trọ có gác lửng kiên cố đứng không đụng đầu, nhà vệ sinh sạch sẽ nước mạnh, giá sinh viên dưới 3 triệu.',
 'Số 19 kiệt 80 Hùng Vương, Phường Phú Hội, Huế', 'HUE', NULL, 'HUE_PH', 42,
 16.469, 107.592, 25.0, 2, 2500000, 'ROOM', 2500000, 3000.0, 20000.0, 80000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 13, 1, '2026-02-03 09:00:00', NULL, NULL, NULL, NULL, 370),

(218, 'room-00000004-0000-0000-0000-000000000218', 5, 'Nguyễn Thị Ngọc Ánh', '0906000006',
 6, 'SINGLE', 'Phòng trọ ban công riêng sạch sẽ gần ĐH Thủ Dầu Một Bình Dương', 'phong tro ban cong rieng sach se gan dh thu dau mot binh duong', 'phong-tro-ban-cong-binh-duong-218',
 'Phòng trọ có ban công riêng phơi đồ đón nắng, hành lang rộng rãi camera an ninh 24/7, giá dưới 3 triệu.',
 'Số 63 đường Trần Văn Ơn, Phường Phú Hòa, Bình Dương', 'BDG', NULL, 'BDG_PH', 53,
 10.972, 106.674, 28.0, 2, 2800000, 'ROOM', 2800000, 3200.0, 22000.0, 80000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 10, 1, '2026-02-05 11:00:00', NULL, NULL, NULL, NULL, 420),

(219, 'room-00000004-0000-0000-0000-000000000219', 1, 'Trần Thị Chủ Trọ', '0902000002',
 6, 'SINGLE', 'Phòng trọ có gác lửng cho nuôi thú cưng phường Đại Phúc Bắc Ninh', 'phong tro co gac lung cho nuoi thu cung phuong dai phuc bac ninh', 'phong-tro-gac-lung-bac-ninh-219',
 'Phòng có gác lửng lót sàn gỗ sạch đẹp, cho nuôi thú cưng mèo ngoan, giờ giấc tự do không chung chủ.',
 'Số 25 đường Hai Bà Trưng, Phường Đại Phúc, Bắc Ninh', 'BN', NULL, 'BN_DP', 63,
 21.168, 106.079, 24.0, 2, 2200000, 'ROOM', 2200000, 3000.0, 20000.0, 80000.0, 30000.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.7, 7, 1, '2026-02-07 14:30:00', NULL, NULL, NULL, NULL, 310),

(220, 'room-00000004-0000-0000-0000-000000000220', 2, 'Trần Minh Tuấn', '0903000003',
 6, 'SINGLE', 'Phòng khép kín có ban công ngõ thông Hoàng Văn Thụ Hải Phòng', 'phong khep kin co ban cong ngo thong hoang van thu hai phong', 'phong-tro-ban-cong-hai-phong-220',
 'Phòng đầy đủ điều hòa, bình nóng lạnh, ban công riêng phơi đồ, đồng hồ điện nước riêng từng phòng.',
 'Số 14 ngõ Hoàng Văn Thụ, Phường Quang Trung, Hải Phòng', 'HP', NULL, 'HP_QT', 33,
 20.861, 106.684, 27.0, 2, 2900000, 'ROOM', 2900000, 3000.0, 22000.0, 80000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 11, 1, '2026-02-09 16:00:00', NULL, NULL, NULL, NULL, 360),

(221, 'room-00000004-0000-0000-0000-000000000221', 3, 'Hoàng Thị Mai Lan', '0904000004',
 6, 'SINGLE', 'Phòng trọ giá rẻ có gác lửng gần cầu Cái Răng Cần Thơ', 'phong tro gia re co gac lung gan cau cai rang can tho', 'phong-tro-gac-lung-can-tho-221',
 'Phòng có gác lửng cao ráo, kệ bếp nấu ăn, wifi cáp quang tốc độ cao, giá chỉ 2,1 triệu dưới 3 triệu.',
 'Số 58 đường Tầm Vu, Phường Hưng Lợi, Cần Thơ', 'CT', NULL, 'CT_HL', 73,
 10.015, 105.762, 22.0, 2, 2100000, 'ROOM', 2100000, 3000.0, 18000.0, 60000.0, 20000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.6, 9, 1, '2026-02-12 08:30:00', NULL, NULL, NULL, NULL, 290),

(222, 'room-00000004-0000-0000-0000-000000000222', 4, 'Phan Thanh Bình', '0905000005',
 6, 'SINGLE', 'Phòng trọ có gác lửng cho nuôi thú cưng ngõ Tôn Thất Tùng Hà Nội', 'phong tro co gac lung cho nuoi thu cung ngo ton that tung ha noi', 'phong-tro-ton-that-tung-222',
 'Phòng trọ gác lửng đúc kiên cố, gần ĐH Y Hà Nội, cho nuôi thú cưng nhỏ, khóa cổng vân tay tự do.',
 'Số 8 ngõ 10 Tôn Thất Tùng, Phường Khương Thượng, Hà Nội', 'HN', NULL, 'HN_KT', 5,
 21.002, 105.831, 25.0, 2, 2700000, 'ROOM', 2700000, 3800.0, 28000.0, 90000.0, 40000.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 15, 1, '2026-02-14 10:00:00', NULL, NULL, NULL, NULL, 530),

(223, 'room-00000004-0000-0000-0000-000000000223', 5, 'Nguyễn Thị Ngọc Ánh', '0906000006',
 6, 'SINGLE', 'Phòng trọ có ban công đón gió ngõ Nguyễn Kiệm Phú Nhuận', 'phong tro co ban cong don gio ngo nguyen kiem phu nhuan', 'phong-tro-ban-cong-phu-nhuan-223',
 'Phòng trọ lầu 2 có ban công rộng rãi nhiều ánh sáng, giá thuê dưới 3 triệu phù hợp cho sinh viên và người mới đi làm.',
 'Số 142 ngõ Nguyễn Kiệm, Phường 4, TP.HCM', 'HCM', NULL, 'HCM_P4', 14,
 10.806, 106.681, 26.0, 2, 2950000, 'ROOM', 2950000, 3800.0, 28000.0, 90000.0, 40000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 12, 1, '2026-02-16 13:00:00', NULL, NULL, NULL, NULL, 470),

(224, 'room-00000004-0000-0000-0000-000000000224', 1, 'Trần Thị Chủ Trọ', '0902000002',
 4, 'HOMESTAY', 'Phòng homestay ấm cúng có ban công nhìn ra đường hoa Đà Nẵng', 'phong homestay am cung co ban cong nhin ra duong hoa da nang', 'homestay-ban-cong-da-nang-224',
 'Phòng phong cách homestay mộc mạc, ban công thoáng trồng hoa cúc họa mi, trang bị sẵn máy lạnh và tủ lạnh mini.',
 'Số 31 đường Pasteur, Phường Hải Châu 1, Đà Nẵng', 'DNG', NULL, 'DNG_HC1', 24,
 16.068, 108.221, 28.0, 2, 2600000, 'ROOM', 2600000, 3200.0, 22000.0, 80000.0, 30000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 5.0, 18, 1, '2026-02-18 15:30:00', NULL, NULL, NULL, NULL, 410),

-- ==============================================================================
-- 4. CAN HO (MINI_APARTMENT/STUDIO/APARTMENT, room_type_id=1,2,3) - 8 rooms
-- ==============================================================================
(225, 'room-00000004-0000-0000-0000-000000000225', 2, 'Trần Minh Tuấn', '0903000003',
 2, 'STUDIO', 'Studio căn hộ có ban công view sông Hương thơ mộng Huế', 'studio can ho co ban cong view song huong tho mong hue', 'studio-ban-cong-hue-225',
 'Căn hộ studio nội thất mới tinh, ban công ngắm trọn bình minh sông Hương, thang máy thẻ từ, bảo vệ 24/24.',
 'Số 12 đường Đội Cung, Phường Phú Hội, Huế', 'HUE', NULL, 'HUE_PH', 42,
 16.471, 107.594, 38.0, 2, 4500000, 'ROOM', 4500000, 3200.0, 22000.0, 100000.0, 50000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.9, 14, 1, '2026-02-04 10:00:00', NULL, NULL, NULL, NULL, 380),

(226, 'room-00000004-0000-0000-0000-000000000226', 3, 'Hoàng Thị Mai Lan', '0904000004',
 1, 'MINI_APARTMENT', 'Căn hộ mini có gác lửng thang máy tòa nhà mới Thủ Dầu Một', 'can ho mini co gac lung thang may toa nha moi thu dau mot', 'can-ho-mini-gac-lung-binh-duong-226',
 'Chung cư mini có gác lửng trần cao 4 mét, thang máy êm ái, bãi đỗ xe rộng rãi, bảo vệ trực sảnh an toàn.',
 'Số 88 đường Yersin, Phường Hiệp Thành, Bình Dương', 'BDG', NULL, 'BDG_HT', 54,
 10.985, 106.669, 42.0, 3, 4800000, 'ROOM', 4800000, 3200.0, 22000.0, 100000.0, 60000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 11, 1, '2026-02-06 14:00:00', NULL, NULL, NULL, NULL, 430),

(227, 'room-00000004-0000-0000-0000-000000000227', 4, 'Phan Thanh Bình', '0905000005',
 3, 'APARTMENT', 'Căn hộ dịch vụ 1 phòng ngủ ban công rộng trung tâm Bắc Ninh', 'can ho dich vu 1 phong ngu ban cong rong trung tam bac ninh', 'can-ho-dich-vu-bac-ninh-227',
 'Căn hộ đầy đủ bếp từ, hút mùi, lò vi sóng, tủ lạnh hai cánh, ban công riêng giặt phơi quần áo tiện lợi.',
 'Số 102 đường Huyền Tụng, Phường Ninh Xá, Bắc Ninh', 'BN', NULL, 'BN_NX', 64,
 21.176, 106.068, 48.0, 3, 5000000, 'ROOM', 5000000, 3200.0, 22000.0, 100000.0, 60000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.7, 8, 1, '2026-02-08 16:30:00', NULL, NULL, NULL, NULL, 350),

(228, 'room-00000004-0000-0000-0000-000000000228', 5, 'Nguyễn Thị Ngọc Ánh', '0906000006',
 2, 'STUDIO', 'Studio căn hộ có gác lửng ban công cây xanh Ngô Quyền Hải Phòng', 'studio can ho co gac lung ban cong cay xanh ngo quyen hai phong', 'studio-gac-lung-hai-phong-228',
 'Studio có gác lửng đúc xinh xắn, ban công nhìn ra vườn hoa, sàn gỗ chống trơn, thiết bị vệ sinh nhập khẩu.',
 'Số 204 đường Đà Nẵng, Phường Cầu Tre, Hải Phòng', 'HP', NULL, 'HP_CT', 34,
 20.858, 106.701, 36.0, 2, 4200000, 'ROOM', 4200000, 3000.0, 22000.0, 90000.0, 50000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 12, 1, '2026-02-11 09:30:00', NULL, NULL, NULL, NULL, 390),

(229, 'room-00000004-0000-0000-0000-000000000229', 1, 'Trần Thị Chủ Trọ', '0902000002',
 1, 'MINI_APARTMENT', 'Căn hộ mini cho nuôi thú cưng view sông thoáng mát Cần Thơ', 'can ho mini cho nuoi thu cung view song thoang mat can tho', 'can-ho-mini-can-tho-229',
 'Căn hộ mini thoáng mát lầu 3, cho nuôi thú cưng có cam kết giữ vệ sinh, cửa sổ kính lớn ngắm sông Hậu.',
 'Số 36 đường Hai Bà Trưng, Phường Tân An, Cần Thơ', 'CT', NULL, 'CT_TA', 74,
 10.033, 105.785, 40.0, 3, 3800000, 'ROOM', 3800000, 3000.0, 20000.0, 80000.0, 40000.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 15, 1, '2026-02-13 11:00:00', NULL, NULL, NULL, NULL, 410),

(230, 'room-00000004-0000-0000-0000-000000000230', 2, 'Trần Minh Tuấn', '0903000003',
 2, 'STUDIO', 'Studio ban công ngập nắng ngõ 198 Xã Đàn Đống Đa Hà Nội', 'studio ban cong ngap nang ngo 198 xa dan dong da ha noi', 'studio-ban-cong-xa-dan-230',
 'Studio có ban công rộng rãi trồng cây cảnh, nội thất đóng đo ni đóng giày, máy giặt riêng trong phòng.',
 'Số 45 ngõ 198 Xã Đàn, Phường Phương Liên, Hà Nội', 'HN', NULL, 'HN_PL', 6,
 21.015, 105.836, 35.0, 2, 5500000, 'ROOM', 5500000, 3800.0, 28000.0, 100000.0, 60000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 5.0, 17, 1, '2026-02-15 14:00:00', NULL, NULL, NULL, NULL, 580),

(231, 'room-00000004-0000-0000-0000-000000000231', 3, 'Hoàng Thị Mai Lan', '0904000004',
 1, 'MINI_APARTMENT', 'Căn hộ mini có gác lửng đúc cao cấp đường Hoàng Sa Quận 3', 'can ho mini co gac lung duc cao cap duong hoang sa quan 3', 'can-ho-mini-hoang-sa-231',
 'Căn hộ mini mặt tiền kênh Nhiêu Lộc đường Hoàng Sa, gác lửng kiên cố lót nệm êm, bếp riêng biệt có quầy bar.',
 'Số 820 đường Hoàng Sa, Phường 14, TP.HCM', 'HCM', NULL, 'HCM_P14', 16,
 10.791, 106.679, 45.0, 3, 6000000, 'ROOM', 6000000, 3800.0, 28000.0, 100000.0, 70000.0,
 'PUBLISHED', TRUE, TRUE, TRUE, 4.9, 13, 1, '2026-02-17 16:30:00', NULL, NULL, NULL, NULL, 620),

(232, 'room-00000004-0000-0000-0000-000000000232', 4, 'Phan Thanh Bình', '0905000005',
 2, 'STUDIO', 'Studio view biển Mỹ Khê có ban công thoáng đãng Đà Nẵng', 'studio view bien my khe co ban cong thoang dang da nang', 'studio-view-bien-da-nang-232',
 'Căn hộ studio cách bãi biển 300 mét, ban công đón gió biển trong lành, đầy đủ tiện nghi tiêu chuẩn căn hộ nghỉ dưỡng.',
 'Số 74 đường Hà Bổng, Phường Phước Mỹ, Đà Nẵng', 'DNG', NULL, 'DNG_PM', 22,
 16.069, 108.243, 38.0, 2, 4600000, 'ROOM', 4600000, 3200.0, 22000.0, 100000.0, 50000.0,
 'PUBLISHED', TRUE, FALSE, TRUE, 4.8, 16, 1, '2026-02-19 10:00:00', NULL, NULL, NULL, NULL, 490);

-- ==============================================================================
-- SEED IMAGES FOR ROOMS 201-232 (EACH ROOM HAS 3-4 LICENSED WEBP PHOTOS)
-- Exactly 1 is_cover=1 AND is_primary=1 per room!
-- ==============================================================================
INSERT INTO room_images (
    room_id, image_url, thumb_key, medium_key, large_key, is_cover, is_primary, sort_order, display_order, created_at
)
SELECT
    r.id,
    CASE ((r.id + offset_val) % 11)
        WHEN 0 THEN '/images/rooms/room-1-1080.webp'
        WHEN 1 THEN '/images/rooms/room-2-1080.webp'
        WHEN 2 THEN '/images/rooms/room-3-1080.webp'
        WHEN 3 THEN '/images/rooms/room-4-1080.webp'
        WHEN 4 THEN '/images/rooms/room-5-1080.webp'
        WHEN 5 THEN '/images/rooms/room-6-1080.webp'
        WHEN 6 THEN '/images/rooms/room-7-1080.webp'
        WHEN 7 THEN '/images/rooms/room-8-1080.webp'
        WHEN 8 THEN '/images/rooms/room-9-1080.webp'
        WHEN 9 THEN '/images/rooms/room-10-1080.webp'
        ELSE '/images/rooms/room-11-1080.webp'
    END AS image_url,
    CASE ((r.id + offset_val) % 11)
        WHEN 0 THEN '/images/rooms/room-1-640.webp'
        WHEN 1 THEN '/images/rooms/room-2-640.webp'
        WHEN 2 THEN '/images/rooms/room-3-640.webp'
        WHEN 3 THEN '/images/rooms/room-4-640.webp'
        WHEN 4 THEN '/images/rooms/room-5-640.webp'
        WHEN 5 THEN '/images/rooms/room-6-640.webp'
        WHEN 6 THEN '/images/rooms/room-7-640.webp'
        WHEN 7 THEN '/images/rooms/room-8-640.webp'
        WHEN 8 THEN '/images/rooms/room-9-640.webp'
        WHEN 9 THEN '/images/rooms/room-10-640.webp'
        ELSE '/images/rooms/room-11-640.webp'
    END AS thumb_url,
    CASE ((r.id + offset_val) % 11)
        WHEN 0 THEN '/images/rooms/room-1-1080.webp'
        WHEN 1 THEN '/images/rooms/room-2-1080.webp'
        WHEN 2 THEN '/images/rooms/room-3-1080.webp'
        WHEN 3 THEN '/images/rooms/room-4-1080.webp'
        WHEN 4 THEN '/images/rooms/room-5-1080.webp'
        WHEN 5 THEN '/images/rooms/room-6-1080.webp'
        WHEN 6 THEN '/images/rooms/room-7-1080.webp'
        WHEN 7 THEN '/images/rooms/room-8-1080.webp'
        WHEN 8 THEN '/images/rooms/room-9-1080.webp'
        WHEN 9 THEN '/images/rooms/room-10-1080.webp'
        ELSE '/images/rooms/room-11-1080.webp'
    END AS medium_url,
    CASE ((r.id + offset_val) % 11)
        WHEN 0 THEN '/images/rooms/room-1-1920.webp'
        WHEN 1 THEN '/images/rooms/room-2-1920.webp'
        WHEN 2 THEN '/images/rooms/room-3-1920.webp'
        WHEN 3 THEN '/images/rooms/room-4-1920.webp'
        WHEN 4 THEN '/images/rooms/room-5-1920.webp'
        WHEN 5 THEN '/images/rooms/room-6-1920.webp'
        WHEN 6 THEN '/images/rooms/room-7-1920.webp'
        WHEN 7 THEN '/images/rooms/room-8-1920.webp'
        WHEN 8 THEN '/images/rooms/room-9-1920.webp'
        WHEN 9 THEN '/images/rooms/room-10-1920.webp'
        ELSE '/images/rooms/room-11-1920.webp'
    END AS large_url,
    CASE WHEN offset_val = 0 THEN TRUE ELSE FALSE END AS is_cover,
    CASE WHEN offset_val = 0 THEN TRUE ELSE FALSE END AS is_primary,
    offset_val AS sort_order,
    offset_val AS display_order,
    NOW() AS created_at
FROM rooms r
CROSS JOIN (
    SELECT 0 AS offset_val
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) offsets
WHERE r.id BETWEEN 201 AND 232;

-- ==============================================================================
-- SEED AMENITIES FOR ROOMS 201-232
-- ==============================================================================
INSERT INTO room_amenities (room_id, amenity_id)
SELECT r.id, a.id
FROM rooms r
CROSS JOIN amenities a
WHERE r.id BETWEEN 201 AND 232
  AND ((r.id + a.id) % 3 = 0 OR a.id IN (1, 2, 3));

