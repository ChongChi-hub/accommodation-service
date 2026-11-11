-- Sprint 3 SEC-PROD-01: 2FA Attempt Lockout & 2-Admin Recovery Workflow

ALTER TABLE user_two_factors
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0 AFTER is_enabled,
    ADD COLUMN locked_until DATETIME(3) NULL AFTER failed_attempts;

CREATE TABLE IF NOT EXISTS two_factor_recovery_requests (
    id CHAR(36) PRIMARY KEY,
    target_user_id CHAR(36) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    approved_by CHAR(36) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at DATETIME(3) NULL,
    INDEX idx_2fa_rec_target (target_user_id),
    INDEX idx_2fa_rec_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

