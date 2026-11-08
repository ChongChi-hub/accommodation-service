-- ============================================================
-- V7__create_landlord_profiles_and_application_flow.sql
-- Task [AUTH-FLOW-A]: Landlord Profiles and Approval Lifecycle
-- Pure DDL Schema Definition (0 demo/seed records)
-- ============================================================

CREATE TABLE IF NOT EXISTS landlord_profiles (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    approval_status ENUM('AWAITING_KYC', 'PENDING_REVIEW', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'AWAITING_KYC',
    applied_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    approved_at DATETIME(3) NULL,
    reviewed_by VARCHAR(36) NULL,
    reviewed_at DATETIME(3) NULL,
    reject_reason_code VARCHAR(50) NULL,
    reject_reason_note TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lp_user_id (user_id),
    INDEX idx_lp_status (approval_status),
    CONSTRAINT fk_lp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
