-- ============================================================
-- V11__consent_histories_and_data_subject_rights.sql
-- Sprint 4: [LEGAL-01]
-- 1. consent_histories table (Decree 13 / Nghị định 13 audit log)
-- 2. anonymized_at column on users table
-- ============================================================

CREATE TABLE IF NOT EXISTS consent_histories (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NULL,
    consent_type VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    agreed BOOLEAN NOT NULL DEFAULT TRUE,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    metadata_json TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ch_user (user_id),
    INDEX idx_ch_type_version (consent_type, version),
    INDEX idx_ch_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN anonymized_at DATETIME(3) NULL AFTER deleted_at;
