-- ============================================================
-- V2__seed_demo_users.sql
-- Seed Roles and Demo Users for Identity Service
-- Default Password for all demo accounts: password123
-- Hash: $2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a
-- ============================================================

-- 1. Insert Roles
INSERT INTO roles (id, name, description, created_at) VALUES
('r-001', 'TENANT', 'Người thuê phòng trọ', CURRENT_TIMESTAMP(3)),
('r-002', 'LANDLORD', 'Chủ nhà trọ cho thuê', CURRENT_TIMESTAMP(3)),
('r-003', 'MODERATOR', 'Kiểm duyệt viên tin đăng và hồ sơ', CURRENT_TIMESTAMP(3)),
('r-004', 'ADMIN', 'Quản trị viên toàn hệ thống', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE name=name;

-- 2. Insert Demo Users
INSERT INTO users (id, email, phone, password_hash, full_name, avatar_url, gender, status, created_at, updated_at) VALUES
('u-tenant-01', 'tenant@cozynest.vn', '0901000001', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Nguyễn Văn Thuê (Demo Tenant)', '/uploads/avatars/tenant1.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-01', 'landlord@cozynest.vn', '0902000002', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Trần Thị Chủ Trọ (Demo Landlord)', '/uploads/avatars/landlord1.jpg', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-admin-01', 'admin@cozynest.vn', '0903000003', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Lê Hoàng Quản Trị (Demo Admin)', '/uploads/avatars/admin1.jpg', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-01', 'moderator@cozynest.vn', '0904000004', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Phạm Minh Kiểm Duyệt (Demo Mod)', '/uploads/avatars/mod1.jpg', 'OTHER', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE email=email;

-- 3. Assign User Roles
INSERT INTO user_roles (user_id, role_id) VALUES
('u-tenant-01', 'r-001'),
('u-landlord-01', 'r-002'),
('u-admin-01', 'r-004'),
('u-mod-01', 'r-003')
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 4. User Profiles
INSERT INTO user_profiles (id, user_id, permanent_address, occupation, bio, language, timezone, created_at, updated_at) VALUES
('up-001', 'u-tenant-01', 'Cầu Giấy, Hà Nội', 'Kỹ sư phần mềm', 'Tìm phòng trọ sạch sẽ, yên tĩnh gần khu Duy Tân', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-002', 'u-landlord-01', 'Đống Đa, Hà Nội', 'Kinh doanh cho thuê', 'Chủ hệ thống căn hộ dịch vụ và phòng trọ tiện nghi', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-003', 'u-admin-01', 'Ba Đình, Hà Nội', 'Quản trị viên Tổ Ấm', 'Quản trị hệ thống nền tảng Tổ Ấm', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-004', 'u-mod-01', 'Thanh Xuân, Hà Nội', 'Kiểm duyệt viên', 'Ban kiểm duyệt nội dung an toàn', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE user_id=user_id;

