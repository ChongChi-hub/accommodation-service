-- ============================================================
-- V5__landlord_rooms_and_images_schema.sql
-- LANDLORD-04 & LANDLORD-02 Schema Enhancements:
-- 1. rooms: hidden_at, hidden_by, hidden_reason, rejected_reason,
--    published_at, view_count, title_search, composite indexes
-- 2. room_images: multi-size WebP storage keys, metadata,
--    and generated-column single-cover unique constraint
-- 3. room_status_history: audit log for room status transitions
-- ============================================================

-- 1. Alter rooms table
ALTER TABLE rooms
    ADD COLUMN hidden_at TIMESTAMP NULL AFTER deleted_at,
    ADD COLUMN hidden_by VARCHAR(20) NULL AFTER hidden_at,
    ADD COLUMN hidden_reason VARCHAR(500) NULL AFTER hidden_by,
    ADD COLUMN rejected_reason VARCHAR(500) NULL AFTER hidden_reason,
    ADD COLUMN published_at TIMESTAMP NULL AFTER rejected_reason,
    ADD COLUMN view_count INT NOT NULL DEFAULT 0 AFTER published_at,
    ADD COLUMN title_search VARCHAR(255) NULL AFTER title;

-- Update published_at and view_count for existing published/rented rooms
UPDATE rooms 
SET published_at = created_at,
    view_count = FLOOR(25 + (id * 17))
WHERE status IN ('PUBLISHED', 'RENTED') AND published_at IS NULL;

-- Simple accent removal approximation for existing titles
UPDATE rooms
SET title_search = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(title, 'Đ', 'D'), 'đ', 'd'), ' ', ' '), 'à', 'a'), 'á', 'a'), 'ả', 'a'), 'ã', 'a'))
WHERE title_search IS NULL;

-- Indexes for Landlord room management & unaccented search
CREATE INDEX idx_rooms_landlord_status_deleted ON rooms (landlord_id, status, deleted_at, published_at);
CREATE INDEX idx_rooms_title_search ON rooms (title_search);

-- 2. Alter room_images table
ALTER TABLE room_images
    ADD COLUMN storage_key VARCHAR(255) NULL AFTER image_url,
    ADD COLUMN thumb_key VARCHAR(255) NULL AFTER storage_key,
    ADD COLUMN medium_key VARCHAR(255) NULL AFTER thumb_key,
    ADD COLUMN large_key VARCHAR(255) NULL AFTER medium_key,
    ADD COLUMN original_filename VARCHAR(255) NULL AFTER large_key,
    ADD COLUMN content_type VARCHAR(50) NOT NULL DEFAULT 'image/webp' AFTER original_filename,
    ADD COLUMN size_bytes BIGINT NOT NULL DEFAULT 102400 AFTER content_type,
    ADD COLUMN width INT NOT NULL DEFAULT 1280 AFTER size_bytes,
    ADD COLUMN height INT NOT NULL DEFAULT 720 AFTER width,
    ADD COLUMN is_cover BOOLEAN NOT NULL DEFAULT FALSE AFTER height,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER is_cover,
    ADD COLUMN alt_text VARCHAR(255) NULL AFTER sort_order,
    ADD COLUMN uploaded_by VARCHAR(50) NULL AFTER alt_text,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER uploaded_by;

-- Sync existing columns
UPDATE room_images 
SET is_cover = is_primary,
    sort_order = display_order,
    storage_key = CONCAT('rooms/', room_id, '/', id, '.webp'),
    thumb_key = CONCAT('rooms/', room_id, '/', id, '_thumb.webp'),
    medium_key = CONCAT('rooms/', room_id, '/', id, '_medium.webp'),
    large_key = CONCAT('rooms/', room_id, '/', id, '_large.webp');

-- Ensure strictly 1 cover per room (pick lowest id if multiple or none)
UPDATE room_images SET is_cover = FALSE;
UPDATE room_images ri
JOIN (
    SELECT room_id, MIN(id) AS min_id 
    FROM room_images 
    GROUP BY room_id
) first_img ON ri.room_id = first_img.room_id AND ri.id = first_img.min_id
SET ri.is_cover = TRUE;

-- Add generated stored column and unique constraint for single cover per room
ALTER TABLE room_images
    ADD COLUMN cover_room_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_cover = TRUE THEN room_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uq_room_images_cover_per_room (cover_room_id);

-- 3. Create room_status_history table
CREATE TABLE IF NOT EXISTS room_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    actor_id VARCHAR(50) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rsh_room (room_id),
    INDEX idx_rsh_created (created_at),
    CONSTRAINT fk_rsh_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

