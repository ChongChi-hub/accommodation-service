-- ============================================================
-- V22__media_uploads_and_orphan_tracking.sql
-- STORAGE-01: Object Storage Upload Ledger & Orphan Tracking
-- ============================================================

CREATE TABLE IF NOT EXISTS media_uploads (
    id VARCHAR(36) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    bucket VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_UPLOAD',
    uploader_id VARCHAR(64) NOT NULL,
    attached_entity_type VARCHAR(50) NULL,
    attached_entity_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    confirmed_at DATETIME(3) NULL,
    attached_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_media_object_key (object_key),
    INDEX idx_media_status_created (status, created_at),
    INDEX idx_media_uploader (uploader_id),
    INDEX idx_media_attached (attached_entity_type, attached_entity_id),
    CONSTRAINT chk_media_status CHECK (status IN ('PENDING_UPLOAD', 'CONFIRMED', 'ATTACHED', 'ORPHANED_DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
