-- ============================================================
-- V6__allow_null_phone_and_password_for_oauth.sql
-- Allow nullable phone and password_hash for OAuth (Google) users
-- Update status column to support SUSPENDED status
-- Add login_history and user_events_outbox
-- ============================================================

ALTER TABLE users MODIFY phone VARCHAR(20) NULL;
ALTER TABLE users MODIFY password_hash VARCHAR(255) NULL;
ALTER TABLE users MODIFY status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE IF NOT EXISTS login_history (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    method VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_lh_user (user_id),
    CONSTRAINT fk_lh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_events_outbox (
    id VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ueo_pub (published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
