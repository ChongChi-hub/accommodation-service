-- ============================================================
-- V2__admin_dead_letter_messages.sql
-- BROKER-01: Dead Letter Queue & Replay Ledger in admin-service
-- ============================================================

CREATE TABLE IF NOT EXISTS dead_letter_messages (
    id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    source_service VARCHAR(60) NOT NULL,
    payload_json JSON NOT NULL,
    failure_reason VARCHAR(500) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REPLAY',
    replayed_by VARCHAR(36) NULL,
    replayed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_dlq_event_id (event_id),
    INDEX idx_dlq_status (status),
    INDEX idx_dlq_type (event_type),
    CONSTRAINT chk_dlq_status CHECK (status IN ('PENDING_REPLAY', 'REPLAYED', 'DISCARDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

