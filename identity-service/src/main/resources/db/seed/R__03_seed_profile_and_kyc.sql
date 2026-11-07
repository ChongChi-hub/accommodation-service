-- ============================================================
-- R__03_seed_profile_and_kyc.sql
-- Repeatable Seed for Enhanced User Profiles and Landlord KYC Verifications
-- Profile: dev, demo ONLY
-- ============================================================

-- 1. Update/Populate Enhanced Profiles for All Demo Users
UPDATE user_profiles SET
    full_name = 'Nguyễn Văn Toàn',
    phone = '0902000001',
    gender = 'MALE',
    date_of_birth = '1985-05-15',
    permanent_address = '125 Hoàng Hoa Thám, Phường 6, Bình Thạnh, TP.HCM',
    address_detail = '125 Hoàng Hoa Thám',
    province_code = '79',
    ward_id = '26914',
    bio = 'Chủ trọ lâu năm tại TP.HCM, nhiệt tình, hỗ trợ khách thuê 24/7.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-01.webp',
    avatar_thumb_key = 'avatars/u-landlord-01_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-01_medium.webp'
WHERE user_id = 'u-landlord-01';

UPDATE user_profiles SET
    full_name = 'Trần Minh Tuấn',
    phone = '0902000003',
    gender = 'MALE',
    date_of_birth = '1988-09-20',
    permanent_address = '45 Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội',
    address_detail = '45 Duy Tân',
    province_code = '01',
    ward_id = '00160',
    bio = 'Chuyên quản lý chuỗi căn hộ mini cao cấp khu vực Cầu Giấy.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-02.webp',
    avatar_thumb_key = 'avatars/u-landlord-02_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-02_medium.webp'
WHERE user_id = 'u-landlord-02';

UPDATE user_profiles SET
    full_name = 'Hoàng Thị Mai Lan',
    phone = '0902000004',
    gender = 'FEMALE',
    date_of_birth = '1990-11-05',
    permanent_address = '88 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM',
    address_detail = '88 Sư Vạn Hạnh',
    province_code = '79',
    ward_id = '27280',
    bio = 'Hệ thống phòng trọ sinh viên tiện ích đầy đủ, giá tốt.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-03.webp',
    avatar_thumb_key = 'avatars/u-landlord-03_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-03_medium.webp'
WHERE user_id = 'u-landlord-03';

UPDATE user_profiles SET
    full_name = 'Phan Thanh Bình',
    phone = '0902000005',
    gender = 'MALE',
    date_of_birth = '1987-03-14',
    permanent_address = '12 Võ Nguyên Giáp, Phước Mỹ, Sơn Trà, Đà Nẵng',
    address_detail = '12 Võ Nguyên Giáp',
    province_code = '48',
    ward_id = '20230',
    bio = 'Chủ chuỗi homestay và phòng trọ ven biển Sơn Trà.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-04.webp',
    avatar_thumb_key = 'avatars/u-landlord-04_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-04_medium.webp'
WHERE user_id = 'u-landlord-04';

UPDATE user_profiles SET
    full_name = 'Nguyễn Thị Ngọc Ánh',
    phone = '0902000006',
    gender = 'FEMALE',
    date_of_birth = '1993-07-22',
    permanent_address = '22 Nguyễn Trãi, Thanh Xuân Bắc, Thanh Xuân, Hà Nội',
    address_detail = '22 Nguyễn Trãi',
    province_code = '01',
    ward_id = '00205',
    bio = 'Kỹ sư công nghệ kiêm chủ nhà cho thuê văn phòng và phòng ở.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-05.webp',
    avatar_thumb_key = 'avatars/u-landlord-05_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-05_medium.webp'
WHERE user_id = 'u-landlord-05';

UPDATE user_profiles SET
    full_name = 'Vũ Đình Trọng',
    phone = '0902000007',
    gender = 'MALE',
    date_of_birth = '1992-04-18',
    permanent_address = '90 Điện Biên Phủ, Phường 15, Bình Thạnh, TP.HCM',
    address_detail = '90 Điện Biên Phủ',
    province_code = '79',
    ward_id = '26950',
    bio = 'Đầu tư căn hộ dịch vụ và phòng trọ cho thuê.',
    locale = 'vi',
    avatar_key = 'avatars/u-landlord-06.webp',
    avatar_thumb_key = 'avatars/u-landlord-06_thumb.webp',
    avatar_medium_key = 'avatars/u-landlord-06_medium.webp'
WHERE user_id = 'u-landlord-06';

-- Tenants, Mods, Admins profile updates
UPDATE user_profiles SET
    full_name = 'Nguyễn Thu Trang',
    phone = '0901000001',
    gender = 'FEMALE',
    date_of_birth = '1998-08-12',
    permanent_address = '10 Tôn Thất Tùng, Đống Đa, Hà Nội',
    address_detail = '10 Tôn Thất Tùng',
    province_code = '01',
    ward_id = '00180',
    bio = 'Sinh viên cao học tìm phòng ổn định lâu dài.',
    locale = 'vi',
    avatar_key = 'avatars/u-tenant-01.webp',
    avatar_thumb_key = 'avatars/u-tenant-01_thumb.webp',
    avatar_medium_key = 'avatars/u-tenant-01_medium.webp'
WHERE user_id = 'u-tenant-01';

UPDATE user_profiles SET
    full_name = 'Lê Hoàng Minh',
    phone = '0901000002',
    gender = 'MALE',
    date_of_birth = '1997-12-05',
    permanent_address = '60 Xô Viết Nghệ Tĩnh, Bình Thạnh, TP.HCM',
    address_detail = '60 Xô Viết Nghệ Tĩnh',
    province_code = '79',
    ward_id = '26914',
    bio = 'Chuyên viên Marketing làm việc tại trung tâm.',
    locale = 'vi',
    avatar_key = 'avatars/u-tenant-02.webp',
    avatar_thumb_key = 'avatars/u-tenant-02_thumb.webp',
    avatar_medium_key = 'avatars/u-tenant-02_medium.webp'
WHERE user_id = 'u-tenant-02';

UPDATE user_profiles SET
    full_name = 'Lê Văn An',
    phone = '0904000001',
    gender = 'MALE',
    date_of_birth = '1990-02-10',
    permanent_address = 'Cầu Giấy, Hà Nội',
    address_detail = 'Tòa nhà ToAm Central',
    province_code = '01',
    ward_id = '00160',
    bio = 'Kiểm duyệt viên an toàn và nội dung nền tảng Tổ Ấm.',
    locale = 'vi',
    avatar_key = 'avatars/u-mod-01.webp',
    avatar_thumb_key = 'avatars/u-mod-01_thumb.webp',
    avatar_medium_key = 'avatars/u-mod-01_medium.webp'
WHERE user_id = 'u-mod-01';

UPDATE user_profiles SET
    full_name = 'Phạm Thanh Sơn',
    phone = '0903000001',
    gender = 'MALE',
    date_of_birth = '1982-10-20',
    permanent_address = 'Ba Đình, Hà Nội',
    address_detail = 'Trung tâm Quản trị Tổ Ấm',
    province_code = '01',
    ward_id = '00001',
    bio = 'Quản trị viên hệ thống Tổ Ấm.',
    locale = 'vi',
    avatar_key = 'avatars/u-admin-01.webp',
    avatar_thumb_key = 'avatars/u-admin-01_thumb.webp',
    avatar_medium_key = 'avatars/u-admin-01_medium.webp'
WHERE user_id = 'u-admin-01';

-- 2. Clean Up and Re-seed Landlord KYC Verifications
-- Delete previous seed records for landlord verifications to insert complete encrypted records
DELETE FROM user_verifications WHERE user_id IN ('u-landlord-01', 'u-landlord-02', 'u-landlord-03', 'u-landlord-04', 'u-landlord-05', 'u-landlord-06');

-- Landlord 01 (landlord@toam.vn) -> APPROVED
INSERT INTO user_verifications (
    id, user_id, full_name_on_id, date_of_birth, id_number_enc, id_number_hash, id_number_last4,
    issue_date, issue_place, front_image_key, back_image_key, selfie_key, status,
    consent_at, consent_policy_version, submitted_at, reviewed_at, reviewed_by,
    reject_reason_code, reject_reason_note, attempt_count, created_at, updated_at
) VALUES (
    'uv-l01', 'u-landlord-01', 'Nguyễn Văn Toàn', '1985-05-15',
    '1vGAJe2xOWal7bPQys0xxiUH2QHbUg8+vvx9fuD8vJpCms2Rn9XALQ==',
    '67281ddb7e4ca3fd9447ea34765686a4eaf679b568573bcd2f690f8577e222d9',
    '0001', '2021-06-10', 'Cục Cảnh sát QLHC về TTXH',
    'kyc/private/landlord-01_front.webp', 'kyc/private/landlord-01_back.webp', NULL,
    'APPROVED', CURRENT_TIMESTAMP(3) - INTERVAL 15 DAY, 'v1.0', CURRENT_TIMESTAMP(3) - INTERVAL 15 DAY, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY, 'u-admin-01',
    NULL, NULL, 1, CURRENT_TIMESTAMP(3) - INTERVAL 15 DAY, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY
);

-- Landlord 02 (landlord2@toam.vn) -> PENDING (Chờ duyệt để kiểm tra quy trình Moderator)
INSERT INTO user_verifications (
    id, user_id, full_name_on_id, date_of_birth, id_number_enc, id_number_hash, id_number_last4,
    issue_date, issue_place, front_image_key, back_image_key, selfie_key, status,
    consent_at, consent_policy_version, submitted_at, reviewed_at, reviewed_by,
    reject_reason_code, reject_reason_note, attempt_count, created_at, updated_at
) VALUES (
    'uv-l02', 'u-landlord-02', 'Trần Minh Tuấn', '1988-09-20',
    '7XFfqbCPp7nLX0TerII+n9ecNlLQmzbEB3wpemjafypTLUaNVMhhmA==',
    'd4e041eb8faef2590865b863f77e13b39ff00c8582a8b09a9ca9bf45035e5de4',
    '0002', '2022-03-12', 'Cục Cảnh sát QLHC về TTXH',
    'kyc/private/landlord-02_front.webp', 'kyc/private/landlord-02_back.webp', NULL,
    'PENDING', CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, 'v1.0', CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, NULL, NULL,
    NULL, NULL, 1, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR
);

-- Landlord 03 (landlord3@toam.vn) -> REJECTED (Lý do BLURRY để thử giao diện nộp lại)
INSERT INTO user_verifications (
    id, user_id, full_name_on_id, date_of_birth, id_number_enc, id_number_hash, id_number_last4,
    issue_date, issue_place, front_image_key, back_image_key, selfie_key, status,
    consent_at, consent_policy_version, submitted_at, reviewed_at, reviewed_by,
    reject_reason_code, reject_reason_note, attempt_count, created_at, updated_at
) VALUES (
    'uv-l03', 'u-landlord-03', 'Hoàng Thị Mai Lan', '1990-11-05',
    'qhZbi+repmF/t6uwJmhqXyclhDYfBRptC72cerUjxm87Ny7YBhjz8Q==',
    'bfd0dc19e8bca05d70353971d812a3c061c40e8de6d176de1140c521bc203d8d',
    '0003', '2020-08-25', 'Cục Cảnh sát QLHC về TTXH',
    'kyc/private/landlord-03_front.webp', 'kyc/private/landlord-03_back.webp', NULL,
    'REJECTED', CURRENT_TIMESTAMP(3) - INTERVAL 1 DAY, 'v1.0', CURRENT_TIMESTAMP(3) - INTERVAL 1 DAY,
    CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR, 'u-mod-03',
    'BLURRY', 'Ảnh chụp mặt sau bị lóa sáng, mất góc trên bên phải, không đọc được số CCCD', 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 1 DAY, CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR
);

-- Landlord 06 (landlord6@toam.vn) -> REJECTED (Lý do MISMATCH)
INSERT INTO user_verifications (
    id, user_id, full_name_on_id, date_of_birth, id_number_enc, id_number_hash, id_number_last4,
    issue_date, issue_place, front_image_key, back_image_key, selfie_key, status,
    consent_at, consent_policy_version, submitted_at, reviewed_at, reviewed_by,
    reject_reason_code, reject_reason_note, attempt_count, created_at, updated_at
) VALUES (
    'uv-l06', 'u-landlord-06', 'Vũ Đình Trọng', '1992-04-18',
    'kTxLQdwVbx31MZfSgjndpJGUYGqkF9aO/13VQ7wMA158sw0mz0lykA==',
    '3067d4cdcd576bab905bc1bf9ccfd0651f4f97a1b561bf58f4ff851f0a7d5791',
    '0006', '2023-01-15', 'Cục Cảnh sát QLHC về TTXH',
    'kyc/private/landlord-06_front.webp', 'kyc/private/landlord-06_back.webp', NULL,
    'REJECTED', CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, 'v1.0', CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY,
    CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR, 'u-mod-02',
    'MISMATCH', 'Họ tên trên CCCD không trùng khớp với hồ sơ tài khoản', 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR
);

-- 3. KYC Status History Records
DELETE FROM kyc_status_history WHERE verification_id IN ('uv-l01', 'uv-l02', 'uv-l03', 'uv-l06');

INSERT INTO kyc_status_history (id, verification_id, from_status, to_status, actor_id, reason, created_at) VALUES
('ksh-001', 'uv-l01', 'NONE', 'PENDING', 'u-landlord-01', 'Chủ trọ nộp hồ sơ xác minh danh tính', CURRENT_TIMESTAMP(3) - INTERVAL 15 DAY),
('ksh-002', 'uv-l01', 'PENDING', 'APPROVED', 'u-admin-01', 'Hồ sơ đầy đủ, CCCD rõ nét và trùng khớp dữ liệu', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY),
('ksh-003', 'uv-l02', 'NONE', 'PENDING', 'u-landlord-02', 'Chủ trọ nộp hồ sơ xác minh danh tính', CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR),
('ksh-004', 'uv-l03', 'NONE', 'PENDING', 'u-landlord-03', 'Chủ trọ nộp hồ sơ xác minh danh tính', CURRENT_TIMESTAMP(3) - INTERVAL 1 DAY),
('ksh-005', 'uv-l03', 'PENDING', 'REJECTED', 'u-mod-03', 'Ảnh chụp mặt sau bị lóa sáng, mất góc trên bên phải, không đọc được số CCCD', CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR),
('ksh-006', 'uv-l06', 'NONE', 'PENDING', 'u-landlord-06', 'Chủ trọ nộp hồ sơ xác minh danh tính', CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY),
('ksh-007', 'uv-l06', 'PENDING', 'REJECTED', 'u-mod-02', 'Họ tên trên CCCD không trùng khớp với hồ sơ tài khoản', CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR);

-- 4. KYC Access Logs (Initial audit logs for review actions)
DELETE FROM kyc_access_logs WHERE verification_id IN ('uv-l01', 'uv-l02', 'uv-l03', 'uv-l06');

INSERT INTO kyc_access_logs (id, verification_id, actor_id, actor_role, action, ip, created_at) VALUES
('kal-001', 'uv-l01', 'u-admin-01', 'ADMIN', 'VIEW_DETAIL', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 10 MINUTE),
('kal-002', 'uv-l01', 'u-admin-01', 'ADMIN', 'VIEW_FRONT', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 8 MINUTE),
('kal-003', 'uv-l01', 'u-admin-01', 'ADMIN', 'VIEW_BACK', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 7 MINUTE),
('kal-004', 'uv-l01', 'u-admin-01', 'ADMIN', 'APPROVE', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY),
('kal-005', 'uv-l03', 'u-mod-03', 'MODERATOR', 'VIEW_DETAIL', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR),
('kal-006', 'uv-l03', 'u-mod-03', 'MODERATOR', 'VIEW_BACK', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR),
('kal-007', 'uv-l03', 'u-mod-03', 'MODERATOR', 'REJECT', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 22 HOUR);

