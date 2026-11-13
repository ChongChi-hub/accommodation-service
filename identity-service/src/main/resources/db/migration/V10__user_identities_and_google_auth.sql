-- ============================================================
-- V10__user_identities_and_google_auth.sql
-- Table user_identities & identity_audit_logs for Google & OAuth
-- ============================================================

CREATE TABLE IF NOT EXISTS user_identities (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(191) NOT NULL,
    email_at_link VARCHAR(255) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    display_name VARCHAR(255) NULL,
    picture_url VARCHAR(1024) NULL,
    locale VARCHAR(10) NULL,
    linked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_identities_provider_sub (provider, provider_subject),
    UNIQUE KEY uq_user_identities_user_provider (user_id, provider),
    INDEX idx_user_identities_user (user_id),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS identity_audit_logs (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NULL,
    actor_id VARCHAR(36) NULL,
    event_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    details VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ial_user (user_id),
    INDEX idx_ial_event (event_type),
    INDEX idx_ial_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate legacy oauth_accounts into user_identities idempotently if table exists
INSERT INTO user_identities (id, user_id, provider, provider_subject, email_at_link, email_verified, linked_at, created_at)
SELECT id, user_id, provider, provider_user_id, email, TRUE, created_at, created_at
FROM oauth_accounts oa
WHERE NOT EXISTS (
    SELECT 1 FROM user_identities ui
    WHERE ui.provider = oa.provider AND ui.provider_subject = oa.provider_user_id
);
