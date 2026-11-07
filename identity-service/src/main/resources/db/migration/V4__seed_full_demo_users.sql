-- ============================================================
-- V4__seed_full_demo_users.sql
-- Seed Full Multi-Role User Base for Identity Service
-- Default Password: password123 (BCrypt cost 12)
-- Hash: $2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a
-- ============================================================

-- 1. Insert Additional Users (Tenant 02..08, Landlord 02..06, Mod 02..03, Admin 02)
INSERT INTO users (id, email, phone, password_hash, full_name, avatar_url, gender, status, email_verified_at, created_at, updated_at) VALUES
-- Additional Tenants
('u-tenant-02', 'tenant2@toam.vn', '0901000002', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Lê Hoàng Minh', '/uploads/avatars/tenant2.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-03', 'tenant3@toam.vn', '0901000003', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Vũ Phương Thảo', '/uploads/avatars/tenant3.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-04', 'tenant4@toam.vn', '0901000004', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Đặng Quốc Hưng', '/uploads/avatars/tenant4.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-05', 'tenant5@toam.vn', '0901000005', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Bùi Thị Thanh Hà', '/uploads/avatars/tenant5.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-06', 'tenant6@toam.vn', '0901000006', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Đỗ Minh Tuấn', '/uploads/avatars/tenant6.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-07', 'tenant7@toam.vn', '0901000007', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Hoàng Thùy Linh', '/uploads/avatars/tenant7.jpg', 'FEMALE', 'PENDING', NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-08', 'tenant8@toam.vn', '0901000008', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Ngô Quang Hải', '/uploads/avatars/tenant8.jpg', 'MALE', 'LOCKED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),

-- Additional Landlords
('u-landlord-02', 'landlord2@toam.vn', '0902000003', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Trần Minh Tuấn', '/uploads/avatars/landlord2.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-03', 'landlord3@toam.vn', '0902000004', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Hoàng Thị Mai Lan', '/uploads/avatars/landlord3.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-04', 'landlord4@toam.vn', '0902000005', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Phan Thanh Bình', '/uploads/avatars/landlord4.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-05', 'landlord5@toam.vn', '0902000006', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Nguyễn Thị Ngọc Ánh', '/uploads/avatars/landlord5.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-06', 'landlord6@toam.vn', '0902000007', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Vũ Đình Trọng', '/uploads/avatars/landlord6.jpg', 'MALE', 'LOCKED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),

-- Additional Moderators
('u-mod-02', 'mod2@toam.vn', '0904000005', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Đoàn Thu Trang', '/uploads/avatars/mod2.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-03', 'mod3@toam.vn', '0904000006', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Lương Gia Bảo', '/uploads/avatars/mod3.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),

-- Additional Admin
('u-admin-02', 'admin2@toam.vn', '0903000005', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Trịnh Phương Dung', '/uploads/avatars/admin2.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE email=email;

-- 2. Assign Roles (u-landlord-05 và u-landlord-06 là tài khoản đa vai trò: TENANT + LANDLORD)
INSERT INTO user_roles (user_id, role_id) VALUES
('u-tenant-02', 'r-001'),
('u-tenant-03', 'r-001'),
('u-tenant-04', 'r-001'),
('u-tenant-05', 'r-001'),
('u-tenant-06', 'r-001'),
('u-tenant-07', 'r-001'),
('u-tenant-08', 'r-001'),

('u-landlord-02', 'r-002'),
('u-landlord-03', 'r-002'),
('u-landlord-04', 'r-002'),
('u-landlord-05', 'r-001'), -- Multi-role (Tenant)
('u-landlord-05', 'r-002'), -- Multi-role (Landlord)
('u-landlord-06', 'r-001'), -- Multi-role (Tenant)
('u-landlord-06', 'r-002'), -- Multi-role (Landlord)

('u-mod-02', 'r-003'),
('u-mod-03', 'r-003'),

('u-admin-02', 'r-004')
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 3. Profiles for New Users
INSERT INTO user_profiles (id, user_id, permanent_address, occupation, bio, language, timezone, created_at, updated_at) VALUES
('up-005', 'u-tenant-02', 'Phường 25, Bình Thạnh, TP.HCM', 'Chuyên viên Marketing', 'Thích không gian thoáng, gần chỗ làm', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-006', 'u-tenant-03', 'Hải Châu, Đà Nẵng', 'Nhà thiết kế đồ họa', 'Cần phòng có ban công và ánh sáng tự nhiên', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-007', 'u-tenant-04', 'Ninh Kiều, Cần Thơ', 'Sinh viên Y Dược', 'Tìm phòng yên tĩnh, an ninh cao', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-008', 'u-tenant-05', 'Ngô Quyền, Hải Phòng', 'Nhân viên Ngân hàng', 'Ưu tiên phòng full nội thất tiện nghi', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-009', 'u-tenant-06', 'Bách Khoa, Hai Bà Trưng, Hà Nội', 'Sinh viên Bách Khoa', 'Tìm bạn ở ghép phòng rộng', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-010', 'u-tenant-07', 'Dịch Vọng, Cầu Giấy, Hà Nội', 'Chuyên viên Nhân sự', 'Chờ duyệt tài khoản đăng ký', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-011', 'u-tenant-08', 'Tân Bình, TP.HCM', 'Kế toán viên', 'Tài khoản tạm khóa do vi phạm nội quy', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-012', 'u-landlord-02', 'Duy Tân, Cầu Giấy, Hà Nội', 'Quản lý tòa nhà mini', 'Chuyên căn hộ mini cao cấp khu Cầu Giấy - Nam Từ Liêm', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-013', 'u-landlord-03', 'Quận 10, TP.HCM', 'Kinh doanh lưu trú', 'Hệ thống phòng trọ sinh viên tiện ích đầy đủ', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-014', 'u-landlord-04', 'Sơn Trà, Đà Nẵng', 'Chủ homestay & phòng trọ', 'Cho thuê dài hạn căn hộ view biển', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-015', 'u-landlord-05', 'Thanh Xuân, Hà Nội', 'Kỹ sư & Chủ nhà cho thuê', 'Vừa cho thuê phòng vừa thuê văn phòng làm việc', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-016', 'u-landlord-06', 'Bình Thạnh, TP.HCM', 'Đầu tư bất động sản', 'Tài khoản đang bị khóa tạm thời', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-017', 'u-mod-02', 'Cầu Giấy, Hà Nội', 'Kiểm duyệt viên tin đăng', 'Chuyên viên duyệt phòng trọ khu vực miền Bắc', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-018', 'u-mod-03', 'Quận 3, TP.HCM', 'Kiểm duyệt viên KYC', 'Chuyên viên kiểm tra hồ sơ định danh CCCD', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-019', 'u-admin-02', 'Hoàn Kiếm, Hà Nội', 'Quản trị viên An ninh', 'Phụ trách bảo mật và đối soát hệ thống Tổ Ấm', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 4. User Verifications (KYC đa dạng trạng thái: APPROVED, PENDING, REJECTED)
INSERT INTO user_verifications (id, user_id, document_type, document_number, front_image_url, back_image_url, selfie_url, status, rejection_reason, reviewed_by, reviewed_at, created_at) VALUES
('uv-001', 'u-tenant-01', 'CCCD', '001098012341', '/uploads/kyc/u1_front.jpg', '/uploads/kyc/u1_back.jpg', '/uploads/kyc/u1_selfie.jpg', 'APPROVED', NULL, 'u-mod-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-002', 'u-tenant-02', 'CCCD', '001098012342', '/uploads/kyc/u2_front.jpg', '/uploads/kyc/u2_back.jpg', '/uploads/kyc/u2_selfie.jpg', 'APPROVED', NULL, 'u-mod-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-003', 'u-tenant-03', 'CCCD', '001098012343', '/uploads/kyc/u3_front.jpg', '/uploads/kyc/u3_back.jpg', '/uploads/kyc/u3_selfie.jpg', 'APPROVED', NULL, 'u-mod-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-004', 'u-tenant-04', 'CCCD', '001098012344', '/uploads/kyc/u4_front.jpg', '/uploads/kyc/u4_back.jpg', '/uploads/kyc/u4_selfie.jpg', 'PENDING', NULL, NULL, NULL, CURRENT_TIMESTAMP(3)),
('uv-005', 'u-tenant-05', 'CCCD', '001098012345', '/uploads/kyc/u5_front.jpg', '/uploads/kyc/u5_back.jpg', '/uploads/kyc/u5_selfie.jpg', 'REJECTED', 'Ảnh CCCD mặt trước bị mờ, lóa sáng không nhận diện được số', 'u-mod-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-006', 'u-landlord-01', 'CCCD', '001085023451', '/uploads/kyc/l1_front.jpg', '/uploads/kyc/l1_back.jpg', '/uploads/kyc/l1_selfie.jpg', 'APPROVED', NULL, 'u-admin-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-007', 'u-landlord-02', 'CCCD', '001085023452', '/uploads/kyc/l2_front.jpg', '/uploads/kyc/l2_back.jpg', '/uploads/kyc/l2_selfie.jpg', 'APPROVED', NULL, 'u-admin-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-008', 'u-landlord-03', 'CCCD', '001085023453', '/uploads/kyc/l3_front.jpg', '/uploads/kyc/l3_back.jpg', '/uploads/kyc/l3_selfie.jpg', 'APPROVED', NULL, 'u-admin-01', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('uv-009', 'u-landlord-04', 'CCCD', '001085023454', '/uploads/kyc/l4_front.jpg', '/uploads/kyc/l4_back.jpg', '/uploads/kyc/l4_selfie.jpg', 'PENDING', NULL, NULL, NULL, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE user_id=user_id;
