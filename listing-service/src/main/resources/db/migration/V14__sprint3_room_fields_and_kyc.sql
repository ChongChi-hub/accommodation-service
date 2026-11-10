-- ============================================================
-- V12__sprint3_room_fields_and_kyc.sql
-- Sprint 3: ROOM-03 & KYC-03
-- 1. House rules, ownership verification badge and timestamps on rooms
-- 2. Property ownership documents table for landlord KYC verification
-- ============================================================

-- 1. Add fields to rooms
ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS house_rules TEXT NULL AFTER curfew,
    ADD COLUMN IF NOT EXISTS is_verified_ownership BOOLEAN NOT NULL DEFAULT FALSE AFTER is_approved,
    ADD COLUMN IF NOT EXISTS verified_at DATETIME(3) NULL AFTER is_verified_ownership;

CREATE INDEX idx_rooms_verified_status ON rooms (is_verified_ownership, status, publication_status);

-- 2. Create property_ownership_documents table
CREATE TABLE IF NOT EXISTS property_ownership_documents (
    id VARCHAR(36) NOT NULL,
    room_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    masked_number VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    reviewer_id VARCHAR(36) NULL,
    reviewed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pod_room (room_id),
    INDEX idx_pod_landlord (landlord_id),
    INDEX idx_pod_status (status),
    CONSTRAINT fk_pod_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

