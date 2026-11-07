-- ============================================================
-- V3__auth_tokens_schema.sql
-- Password Reset Tokens, Email Verification Tokens, OAuth Accounts
-- and Auditing Columns for Identity Service
-- ============================================================

-- 1. Password Reset Tokens (For AUTH-04 OTP recovery)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    used_at DATETIME(3) NULL,
    attempts INT NOT NULL DEFAULT 0,
    request_ip VARCHAR(45) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_prt_user (user_id),
    INDEX idx_prt_expires (expires_at),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Email Verification Tokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    verified_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_evt_user (user_id),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. OAuth Accounts
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_oauth_provider_user (provider, provider_user_id),
    INDEX idx_oa_user (user_id),
    CONSTRAINT fk_oa_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Add Version and Soft Delete to users and user_verifications
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME(3) NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 AFTER deleted_at;

ALTER TABLE user_verifications
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME(3) NULL AFTER created_at,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 AFTER deleted_at;
