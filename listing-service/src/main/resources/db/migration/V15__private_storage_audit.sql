-- ============================================================
-- V15__private_storage_audit.sql
-- STORAGE-01: Audit log for sensitive private storage access
-- ============================================================

CREATE TABLE IF NOT EXISTS storage_access_logs (
    id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(40) NOT NULL,
    bucket VARCHAR(60) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    action VARCHAR(30) NOT NULL,
    allowed TINYINT(1) NOT NULL DEFAULT 1,
    reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_sal_actor (actor_id),
    INDEX idx_sal_key (bucket, object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

