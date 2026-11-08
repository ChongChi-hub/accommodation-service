-- ============================================================================
-- Seed Demo Data: Face Verification (KYC-02)
-- CozyNest Identity Service
-- ============================================================================

-- Clean existing face seed records
DELETE FROM face_access_logs WHERE face_verification_id IN ('fv-l01', 'fv-l02', 'fv-l06');
DELETE FROM face_verification_frames WHERE face_verification_id IN ('fv-l01', 'fv-l02', 'fv-l06');
DELETE FROM face_verifications WHERE id IN ('fv-l01', 'fv-l02', 'fv-l06');

-- 1. Landlord 01 (u-landlord-01) -> PASSED (Overall KYC APPROVED)
INSERT INTO face_verifications (
    id, user_id, kyc_id, provider, status, liveness_client_flag,
    challenge_json, challenge_nonce, challenge_expires_at, attempt_no,
    consent_at, consent_version, reviewed_by, reviewed_at,
    reject_reason_code, reject_reason_note, version, created_at, updated_at
) VALUES (
    'fv-l01', 'u-landlord-01', 'uv-l01', 'manual', 'PASSED', TRUE,
    '["CHOP_MAT","QUAY_TRAI"]', NULL, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY, 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 1 HOUR, 'v1.0', 'u-admin-01', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY,
    NULL, NULL, 1, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 1 HOUR, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY
);

INSERT INTO face_verification_frames (id, face_verification_id, purpose, storage_key, checksum, size, created_at, deleted_at) VALUES
('fvf-001', 'fv-l01', 'NEUTRAL', 'face/seed_sample_neutral.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 45200, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY, NULL),
('fvf-002', 'fv-l01', 'CHOP_MAT', 'face/seed_sample_blink.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 46100, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY, NULL),
('fvf-003', 'fv-l01', 'QUAY_TRAI', 'face/seed_sample_turn_left.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 47300, CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY, NULL);

-- 2. Landlord 02 (u-landlord-02) -> PENDING (Chờ kiểm duyệt viên duyệt đối chiếu CCCD)
INSERT INTO face_verifications (
    id, user_id, kyc_id, provider, status, liveness_client_flag,
    challenge_json, challenge_nonce, challenge_expires_at, attempt_no,
    consent_at, consent_version, reviewed_by, reviewed_at,
    reject_reason_code, reject_reason_note, version, created_at, updated_at
) VALUES (
    'fv-l02', 'u-landlord-02', 'uv-l02', 'manual', 'PENDING', TRUE,
    '["CHOP_MAT","QUAY_PHAI"]', 'demo_nonce_002', CURRENT_TIMESTAMP(3) + INTERVAL 2 MINUTE, 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, 'v1.0', NULL, NULL,
    NULL, NULL, 0, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR
);

INSERT INTO face_verification_frames (id, face_verification_id, purpose, storage_key, checksum, size, created_at, deleted_at) VALUES
('fvf-004', 'fv-l02', 'NEUTRAL', 'face/seed_sample_neutral.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 45200, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, NULL),
('fvf-005', 'fv-l02', 'CHOP_MAT', 'face/seed_sample_blink.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 46100, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, NULL),
('fvf-006', 'fv-l02', 'QUAY_PHAI', 'face/seed_sample_turn_left.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 47300, CURRENT_TIMESTAMP(3) - INTERVAL 2 HOUR, NULL);

-- 3. Landlord 06 (u-landlord-06) -> FAILED (Ví dụ bị từ chối do không khớp)
INSERT INTO face_verifications (
    id, user_id, kyc_id, provider, status, liveness_client_flag,
    challenge_json, challenge_nonce, challenge_expires_at, attempt_no,
    consent_at, consent_version, reviewed_by, reviewed_at,
    reject_reason_code, reject_reason_note, version, created_at, updated_at
) VALUES (
    'fv-l06', 'u-landlord-06', 'uv-l06', 'manual', 'FAILED', TRUE,
    '["MO_MIENG","QUAY_TRAI"]', NULL, CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR, 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, 'v1.0', 'u-mod-02', CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR,
    'KHONG_KHOP_GIAY_TO', 'Khuôn mặt trong video thử thách không khớp với ảnh chân dung trên CCCD', 1,
    CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR
);

INSERT INTO face_verification_frames (id, face_verification_id, purpose, storage_key, checksum, size, created_at, deleted_at) VALUES
('fvf-007', 'fv-l06', 'NEUTRAL', 'face/seed_sample_neutral.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 45200, CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, NULL),
('fvf-008', 'fv-l06', 'MO_MIENG', 'face/seed_sample_blink.enc', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 46100, CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY, NULL);

-- 4. Audit Access Logs
INSERT INTO face_access_logs (id, actor_id, actor_role, frame_id, face_verification_id, purpose, ip_address, created_at) VALUES
('fal-001', 'u-admin-01', 'ADMIN', NULL, 'fv-l01', 'VIEW_DETAIL', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 15 MINUTE),
('fal-002', 'u-admin-01', 'ADMIN', 'fvf-001', 'fv-l01', 'STREAM_FRAME', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY - INTERVAL 12 MINUTE),
('fal-003', 'u-admin-01', 'ADMIN', NULL, 'fv-l01', 'DECIDE_PASSED', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 14 DAY),
('fal-004', 'u-mod-02', 'MODERATOR', 'fvf-007', 'fv-l06', 'STREAM_FRAME', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR - INTERVAL 5 MINUTE),
('fal-005', 'u-mod-02', 'MODERATOR', NULL, 'fv-l06', 'DECIDE_FAILED', '127.0.0.1', CURRENT_TIMESTAMP(3) - INTERVAL 40 HOUR);

