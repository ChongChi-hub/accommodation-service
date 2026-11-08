-- ============================================================================
-- Migration V5: Face Verification (KYC-02) Schema
-- CozyNest Identity Service
-- ============================================================================

-- 1. Main face verifications table
CREATE TABLE IF NOT EXISTS face_verifications (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    kyc_id VARCHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'manual',
    status ENUM('PENDING', 'PASSED', 'FAILED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
    liveness_client_flag BOOLEAN NOT NULL DEFAULT FALSE,
    challenge_json TEXT NULL,
    challenge_nonce VARCHAR(64) NULL,
    challenge_expires_at DATETIME(3) NULL,
    attempt_no INT NOT NULL DEFAULT 1,
    consent_at DATETIME(3) NULL,
    consent_version VARCHAR(20) NULL,
    reviewed_by VARCHAR(36) NULL,
    reviewed_at DATETIME(3) NULL,
    reject_reason_code VARCHAR(50) NULL,
    reject_reason_note TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_fv_user (user_id),
    INDEX idx_fv_kyc (kyc_id),
    INDEX idx_fv_status (status),
    CONSTRAINT fk_fv_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_fv_kyc FOREIGN KEY (kyc_id) REFERENCES user_verifications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Face verification frames (Encrypted biometric frames storage references)
CREATE TABLE IF NOT EXISTS face_verification_frames (
    id VARCHAR(36) NOT NULL,
    face_verification_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_fvf_verification (face_verification_id),
    CONSTRAINT fk_fvf_verification FOREIGN KEY (face_verification_id) REFERENCES face_verifications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Append-only Face Access Logs (Audit who viewed which frame, when, and purpose)
CREATE TABLE IF NOT EXISTS face_access_logs (
    id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    frame_id VARCHAR(36) NULL,
    face_verification_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_fal_verification (face_verification_id),
    INDEX idx_fal_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Transactional Outbox for Face Verification Events
CREATE TABLE IF NOT EXISTS face_events_outbox (
    id VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_feo_published (published, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

