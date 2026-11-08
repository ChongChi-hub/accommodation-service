-- ============================================================
-- V3__listing_schema_enhancements.sql
-- Enhanced listing schema: room prices, rules, review images,
-- saved searches, view logs, and cross-service UUIDs
-- ============================================================

-- 1. Alter rooms to support cross-service UUID, slug, version, soft delete
ALTER TABLE rooms
    ADD COLUMN room_uuid VARCHAR(64) NULL AFTER id,
    ADD COLUMN slug VARCHAR(300) NULL AFTER title,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER review_count,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at;

-- Populate room_uuid for existing records if null
UPDATE rooms SET room_uuid = CONCAT('room-00000001-0000-0000-0000-', LPAD(id, 12, '0')) WHERE room_uuid IS NULL;
UPDATE rooms SET slug = CONCAT('phong-tro-demo-', id) WHERE slug IS NULL;

-- Add indexes and unique constraint
ALTER TABLE rooms
    ADD UNIQUE KEY uq_rooms_uuid (room_uuid),
    ADD UNIQUE KEY uq_rooms_slug (slug);

-- 2. Room Service Unit Prices (Điện, Nước, Internet, Vệ sinh)
CREATE TABLE IF NOT EXISTS room_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    is_metered BOOLEAN NOT NULL DEFAULT FALSE,
    meter_type VARCHAR(20) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rp_room (room_id),
    CONSTRAINT fk_rp_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Room Rules (Nội quy chi tiết)
CREATE TABLE IF NOT EXISTS room_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_text VARCHAR(255) NOT NULL,
    is_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_rrule_room (room_id),
    CONSTRAINT fk_rrule_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Review Images (Ảnh đính kèm đánh giá)
CREATE TABLE IF NOT EXISTS review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rvi_review (review_id),
    CONSTRAINT fk_rvi_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Saved Searches (Bộ lọc đã lưu)
CREATE TABLE IF NOT EXISTS saved_searches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    query_params_json JSON NOT NULL,
    notify_new_matches BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ss_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Room View Logs (Nhật ký lượt xem)
CREATE TABLE IF NOT EXISTS room_view_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id VARCHAR(36) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rvl_room (room_id),
    INDEX idx_rvl_viewed (viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
