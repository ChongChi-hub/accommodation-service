-- ============================================================
-- V8__phone_otp_and_2fa_schema.sql
-- Sprint 3: PHONE-OTP-01 and SEC-PROD-01
-- ============================================================

CREATE TABLE IF NOT EXISTS phone_verifications (
    id VARCHAR(36) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    is_verified TINYINT(1) NOT NULL DEFAULT 0,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pv_phone_exp (phone_number, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_two_factors (
    user_id VARCHAR(36) NOT NULL,
    totp_secret VARCHAR(128) NOT NULL,
    is_enabled TINYINT(1) NOT NULL DEFAULT 0,
    backup_codes_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_u2f_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN phone_verified_at DATETIME(3) NULL,
    ADD COLUMN two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0;

