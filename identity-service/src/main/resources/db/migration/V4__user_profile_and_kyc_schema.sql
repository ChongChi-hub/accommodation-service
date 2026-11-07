-- ============================================================
-- V4__user_profile_and_kyc_schema.sql
-- User Profile (USER-01) and Landlord KYC Verification (KYC-01) Schema
-- ============================================================

-- 1. Upgrade user_profiles for full profile attributes and optimistic locking
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(150) NULL AFTER user_id,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL AFTER full_name,
    ADD COLUMN IF NOT EXISTS gender ENUM('MALE', 'FEMALE', 'OTHER') NULL AFTER phone,
    ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL AFTER gender,
    ADD COLUMN IF NOT EXISTS address_detail VARCHAR(255) NULL AFTER permanent_address,
    ADD COLUMN IF NOT EXISTS province_code VARCHAR(20) NULL AFTER address_detail,
    ADD COLUMN IF NOT EXISTS ward_id VARCHAR(50) NULL AFTER province_code,
    ADD COLUMN IF NOT EXISTS locale VARCHAR(10) NOT NULL DEFAULT 'vi' AFTER language,
    ADD COLUMN IF NOT EXISTS avatar_key VARCHAR(255) NULL AFTER locale,
    ADD COLUMN IF NOT EXISTS avatar_thumb_key VARCHAR(255) NULL AFTER avatar_key,
    ADD COLUMN IF NOT EXISTS avatar_medium_key VARCHAR(255) NULL AFTER avatar_thumb_key,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 AFTER avatar_medium_key;

-- Populate initial full_name, phone, gender, date_of_birth from users table for existing records
UPDATE user_profiles up
JOIN users u ON up.user_id = u.id
SET up.full_name = COALESCE(up.full_name, u.full_name),
    up.phone = COALESCE(up.phone, u.phone),
    up.gender = COALESCE(up.gender, u.gender),
    up.date_of_birth = COALESCE(up.date_of_birth, u.date_of_birth)
WHERE up.full_name IS NULL;

-- Unique phone when not null
ALTER TABLE user_profiles
    ADD CONSTRAINT uk_user_profiles_phone UNIQUE (phone);

-- 2. Upgrade user_verifications for encrypted CCCD, HMAC hash uniqueness, and audit fields
ALTER TABLE user_verifications
    ADD COLUMN IF NOT EXISTS full_name_on_id VARCHAR(150) NULL AFTER user_id,
    ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL AFTER full_name_on_id,
    ADD COLUMN IF NOT EXISTS id_number_enc VARCHAR(500) NULL AFTER date_of_birth,
    ADD COLUMN IF NOT EXISTS id_number_hash VARCHAR(64) NULL AFTER id_number_enc,
    ADD COLUMN IF NOT EXISTS id_number_last4 VARCHAR(4) NULL AFTER id_number_hash,
    ADD COLUMN IF NOT EXISTS issue_date DATE NULL AFTER id_number_last4,
    ADD COLUMN IF NOT EXISTS issue_place VARCHAR(150) NULL AFTER issue_date,
    ADD COLUMN IF NOT EXISTS front_image_key VARCHAR(255) NULL AFTER issue_place,
    ADD COLUMN IF NOT EXISTS back_image_key VARCHAR(255) NULL AFTER front_image_key,
    ADD COLUMN IF NOT EXISTS selfie_key VARCHAR(255) NULL AFTER back_image_key,
    ADD COLUMN IF NOT EXISTS consent_at DATETIME(3) NULL AFTER selfie_key,
    ADD COLUMN IF NOT EXISTS consent_policy_version VARCHAR(20) NULL AFTER consent_at,
    ADD COLUMN IF NOT EXISTS submitted_at DATETIME(3) NULL AFTER consent_policy_version,
    ADD COLUMN IF NOT EXISTS reject_reason_code VARCHAR(30) NULL AFTER reviewed_at,
    ADD COLUMN IF NOT EXISTS reject_reason_note TEXT NULL AFTER reject_reason_code,
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 1 AFTER reject_reason_note,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at;

-- Enforce CCCD uniqueness only among PENDING and APPROVED states using stored generated column
ALTER TABLE user_verifications
    ADD COLUMN IF NOT EXISTS active_id_hash VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'APPROVED') THEN id_number_hash ELSE NULL END
    ) STORED AFTER id_number_hash;

ALTER TABLE user_verifications
    ADD CONSTRAINT uk_user_verifications_active_id_hash UNIQUE (active_id_hash);

-- 3. KYC Access Logs (Append-only access log for compliance and security audit)
CREATE TABLE IF NOT EXISTS kyc_access_logs (
    id VARCHAR(36) NOT NULL,
    verification_id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action ENUM('VIEW_FRONT', 'VIEW_BACK', 'VIEW_DETAIL', 'APPROVE', 'REJECT') NOT NULL,
    ip VARCHAR(45) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_kal_verification (verification_id),
    INDEX idx_kal_actor (actor_id),
    CONSTRAINT fk_kal_verification FOREIGN KEY (verification_id) REFERENCES user_verifications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. KYC Status History (Audit trail of all KYC state transitions)
CREATE TABLE IF NOT EXISTS kyc_status_history (
    id VARCHAR(36) NOT NULL,
    verification_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ksh_verification (verification_id),
    CONSTRAINT fk_ksh_verification FOREIGN KEY (verification_id) REFERENCES user_verifications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. KYC Events Outbox (Transactional Outbox for reliable event propagation)
CREATE TABLE IF NOT EXISTS kyc_events_outbox (
    id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'KYC_VERIFICATION',
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_keo_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

