-- ============================================================
-- V21__anti_fraud_phash_and_trust_score.sql
-- Sprint 4: [ANTI-FRAUD-01]
-- 1. room_images: perceptual hash (pHash) column and index
-- 2. rooms: risk_level, risk_reasons columns and index
-- 3. landlord_snapshots: trust_score column
-- ============================================================

-- 1. Alter room_images table
ALTER TABLE room_images
    ADD COLUMN phash VARCHAR(64) NULL AFTER original_filename;

CREATE INDEX idx_room_images_phash ON room_images (phash);

-- 2. Alter rooms table
ALTER TABLE rooms
    ADD COLUMN risk_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL' AFTER status,
    ADD COLUMN risk_reasons VARCHAR(500) NULL AFTER risk_level;

CREATE INDEX idx_rooms_risk_level ON rooms (risk_level, status);

-- 3. Alter landlord_snapshots table
ALTER TABLE landlord_snapshots
    ADD COLUMN trust_score INT NOT NULL DEFAULT 85 AFTER room_count;

UPDATE landlord_snapshots SET trust_score = 96 WHERE landlord_id = 1;
UPDATE landlord_snapshots SET trust_score = 90 WHERE landlord_id = 2;
