-- ============================================================
-- R__05_seed_landlord_profiles_and_demo_flow.sql
-- Repeatable Migration for Dev Seed: Landlord Profiles & Demo Accounts
-- ============================================================

-- R9: Ensure all room-owning landlords have both TENANT (r-001) and LANDLORD (r-002) roles
INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-01', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-01')
ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-02', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-02')
ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-03', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-03')
ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-04', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-04')
ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-05', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-05')
ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 'u-landlord-06', 'r-001' FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-06')
ON DUPLICATE KEY UPDATE user_id=user_id;

-- Seed APPROVED landlord profiles for all existing room-owning landlords
INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-001', 'u-landlord-01', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-01')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-002', 'u-landlord-02', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-02')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-003', 'u-landlord-03', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-03')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-004', 'u-landlord-04', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-04')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-005', 'u-landlord-05', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-05')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, version)
SELECT 'lp-006', 'u-landlord-06', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 'u-admin-01', CURRENT_TIMESTAMP(3), 0
FROM DUAL WHERE EXISTS (SELECT 1 FROM users WHERE id = 'u-landlord-06')
ON DUPLICATE KEY UPDATE approval_status='APPROVED';

-- Ensure KYC status for room-owning landlords is APPROVED
UPDATE user_verifications SET status = 'APPROVED', reviewed_by = 'u-admin-01', reviewed_at = CURRENT_TIMESTAMP(3)
WHERE user_id IN ('u-landlord-01', 'u-landlord-02', 'u-landlord-03', 'u-landlord-04', 'u-landlord-05', 'u-landlord-06');

-- ============================================================
-- Item 11: Add NEW Demo Accounts for Testing Flow (dev only)
-- ============================================================

INSERT INTO users (id, email, phone, password_hash, full_name, avatar_url, gender, status, email_verified_at, created_at, updated_at) VALUES
('u-landlord-new', 'landlord.new@toam.vn', '0905000001', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Chủ Trọ Mới Chờ KYC', '/uploads/avatars/default.png', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-pending', 'landlord.pending@toam.vn', '0905000002', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Chủ Trọ Đang Chờ Duyệt', '/uploads/avatars/default.png', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-landlord-rejected', 'landlord.rejected@toam.vn', '0905000003', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Chủ Trọ Bị Từ Chối', '/uploads/avatars/default.png', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-tenant-unverified', 'tenant.unverified@toam.vn', '0905000004', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Người Thuê Chưa Xác Minh', '/uploads/avatars/default.png', 'OTHER', 'ACTIVE', NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE email=email;

-- Roles for new demo accounts: ONLY TENANT (r-001)
INSERT INTO user_roles (user_id, role_id) VALUES
('u-landlord-new', 'r-001'),
('u-landlord-pending', 'r-001'),
('u-landlord-rejected', 'r-001'),
('u-tenant-unverified', 'r-001')
ON DUPLICATE KEY UPDATE user_id=user_id;

-- Landlord Profiles for demo accounts
INSERT INTO landlord_profiles (id, user_id, approval_status, applied_at, approved_at, reviewed_by, reviewed_at, reject_reason_code, reject_reason_note, version) VALUES
('lp-demo-new', 'u-landlord-new', 'AWAITING_KYC', CURRENT_TIMESTAMP(3), NULL, NULL, NULL, NULL, NULL, 0),
('lp-demo-pending', 'u-landlord-pending', 'PENDING_REVIEW', CURRENT_TIMESTAMP(3), NULL, NULL, NULL, NULL, NULL, 0),
('lp-demo-rejected', 'u-landlord-rejected', 'REJECTED', CURRENT_TIMESTAMP(3), NULL, 'u-admin-01', CURRENT_TIMESTAMP(3), 'BLURRY_IMAGE', 'Ảnh căn cước công dân bị mờ, không rõ số hiệu và ngày cấp', 0)
ON DUPLICATE KEY UPDATE approval_status=VALUES(approval_status);

-- KYC Verification records for pending and rejected demo accounts
INSERT INTO user_verifications (id, user_id, document_type, document_number, front_image_url, back_image_url, selfie_url, status, reject_reason_code, reject_reason_note, attempt_count, submitted_at) VALUES
('uv-demo-pending', 'u-landlord-pending', 'CCCD', '001099000001', '/uploads/kyc/demo-front.jpg', '/uploads/kyc/demo-back.jpg', '/uploads/kyc/demo-selfie.jpg', 'PENDING', NULL, NULL, 1, CURRENT_TIMESTAMP(3)),
('uv-demo-rejected', 'u-landlord-rejected', 'CCCD', '001099000002', '/uploads/kyc/demo-front.jpg', '/uploads/kyc/demo-back.jpg', '/uploads/kyc/demo-selfie.jpg', 'REJECTED', 'BLURRY_IMAGE', 'Ảnh căn cước công dân bị mờ, không rõ số hiệu và ngày cấp', 1, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE status=VALUES(status);

