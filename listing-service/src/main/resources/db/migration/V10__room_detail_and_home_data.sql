-- ============================================================
-- V10__room_detail_and_home_data.sql
-- ROOM-01 & SEARCH-01 Schema and Data Enhancements:
-- 1. Slugs: ensure all existing rooms have clean unique slugs
-- 2. rooms: floor, bedrooms, bathrooms, curfew, available_date,
--    min_lease_months, rating criteria breakdown
-- 3. room_prices: billing_mode (METERED, PER_PERSON, FIXED, INCLUDED)
-- 4. landlord_snapshots: local DDD cache of landlord info
-- 5. provinces: slug, image_url for hot areas
-- 6. banners: active promotional/guidance banners for CMS
-- 7. reviews: is_verified_tenant
-- ============================================================

-- 1. Populate missing slugs in rooms table
UPDATE rooms SET slug = CONCAT('phong-tro-dich-vong-hau-', id) WHERE id = 1 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('studio-quan-hoa-view-song-', id) WHERE id = 2 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('can-ho-lang-ha-gan-rap-chieu-phim-', id) WHERE id = 3 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('studio-ben-nghe-pho-di-bo-', id) WHERE id = 4 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('phong-ngoai-thuong-sach-dep-', id) WHERE id = 5 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('can-ho-phu-my-hung-2pn-', id) WHERE id = 6 AND (slug IS NULL OR slug = '');
UPDATE rooms SET slug = CONCAT('phong-tro-demo-', id) WHERE slug IS NULL OR slug = '';

-- 2. Add room detail fields to rooms
ALTER TABLE rooms
    ADD COLUMN floor INT NOT NULL DEFAULT 2 AFTER area,
    ADD COLUMN bedrooms INT NOT NULL DEFAULT 1 AFTER floor,
    ADD COLUMN bathrooms INT NOT NULL DEFAULT 1 AFTER bedrooms,
    ADD COLUMN curfew VARCHAR(100) NOT NULL DEFAULT 'Giờ giấc tự do, khóa vân tay riêng' AFTER allow_cooking,
    ADD COLUMN available_date DATE NOT NULL DEFAULT (CURRENT_DATE) AFTER curfew,
    ADD COLUMN min_lease_months INT NOT NULL DEFAULT 6 AFTER available_date,
    ADD COLUMN rating_cleanliness DOUBLE NOT NULL DEFAULT 4.9 AFTER review_count,
    ADD COLUMN rating_location DOUBLE NOT NULL DEFAULT 4.8 AFTER rating_cleanliness,
    ADD COLUMN rating_price_quality DOUBLE NOT NULL DEFAULT 4.7 AFTER rating_location,
    ADD COLUMN rating_security DOUBLE NOT NULL DEFAULT 5.0 AFTER rating_price_quality;

-- 3. Add billing_mode to room_prices
ALTER TABLE room_prices
    ADD COLUMN billing_mode VARCHAR(30) NOT NULL DEFAULT 'METERED' AFTER unit;

-- Populate billing_mode based on meter type and unit
UPDATE room_prices SET billing_mode = 'METERED' WHERE is_metered = TRUE;
UPDATE room_prices SET billing_mode = 'PER_PERSON' WHERE unit LIKE '%người%' OR unit LIKE '%person%';
UPDATE room_prices SET billing_mode = 'FIXED' WHERE is_metered = FALSE AND billing_mode != 'PER_PERSON';

-- 4. Create landlord_snapshots table for DDD read model
CREATE TABLE IF NOT EXISTS landlord_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(500) NULL,
    email VARCHAR(150) NULL,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    rating_avg DOUBLE NOT NULL DEFAULT 4.9,
    review_count INT NOT NULL DEFAULT 28,
    response_rate_percent INT NOT NULL DEFAULT 99,
    response_time_minutes INT NOT NULL DEFAULT 10,
    room_count INT NOT NULL DEFAULT 6,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ls_landlord (landlord_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed landlord snapshot for existing landlord ID 1 & 2
INSERT INTO landlord_snapshots (landlord_id, name, phone, avatar_url, email, is_verified, rating_avg, review_count, response_rate_percent, response_time_minutes, room_count, joined_at)
VALUES 
(1, 'Trần Thị Chủ Trọ', '0902000002', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80', 'landlord@toam.vn', TRUE, 4.9, 36, 99, 10, 8, '2025-01-15 08:00:00'),
(2, 'Nguyễn Văn Hoàng', '0903000003', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', 'hoang.landlord@toam.vn', TRUE, 4.8, 19, 95, 20, 4, '2025-03-20 09:30:00')
ON DUPLICATE KEY UPDATE name = VALUES(name), phone = VALUES(phone);

-- 5. Add slug and image_url to provinces for hot areas
ALTER TABLE provinces
    ADD COLUMN slug VARCHAR(100) NULL AFTER name_en,
    ADD COLUMN image_url VARCHAR(500) NULL AFTER full_name_en;

UPDATE provinces SET slug = 'ha-noi', image_url = 'https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=800&q=80' WHERE code = 'HN';
UPDATE provinces SET slug = 'ho-chi-minh', image_url = 'https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=800&q=80' WHERE code = 'HCM';
UPDATE provinces SET slug = 'da-nang', image_url = 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=800&q=80' WHERE code = 'DNG';
UPDATE provinces SET slug = 'hai-phong', image_url = 'https://images.unsplash.com/photo-1628102491629-778571d893a3?auto=format&fit=crop&w=800&q=80' WHERE code = 'HP';
UPDATE provinces SET slug = 'can-tho', image_url = 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80' WHERE code = 'CT';
UPDATE provinces SET slug = 'hue', image_url = 'https://images.unsplash.com/photo-1569154941061-e231b4725ef1?auto=format&fit=crop&w=800&q=80' WHERE code = 'HUE';

-- 6. Create banners table for CMS marketing / announcements
CREATE TABLE IF NOT EXISTS banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255) NULL,
    badge_text VARCHAR(100) NULL,
    image_url VARCHAR(500) NOT NULL,
    link_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_banner_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO banners (title, subtitle, badge_text, image_url, link_url, sort_order, is_active)
VALUES 
('Bảo hiểm tiền cọc an tâm 100%', 'Tổ Ấm cam kết bồi hoàn tiền cọc nếu thực tế phòng không đúng mô tả', 'CHÍNH SÁCH MỚI', 'https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=1200&q=80', '/safety-guide', 1, TRUE),
('Hợp đồng thuê điện tử ký trực tuyến', 'Tiết kiệm thời gian, lưu trữ minh bạch, pháp lý bảo vệ cả hai bên', 'TIỆN ÍCH SỐ', 'https://images.unsplash.com/photo-1450133064473-71024230f91b?auto=format&fit=crop&w=1200&q=80', '/about', 2, TRUE),
('Biểu phí điện nước công khai hàng tháng', 'Ghi nhận chỉ số công tơ định kỳ minh chứng hình ảnh thực tế', 'MINH BẠCH', 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1200&q=80', '/pricing', 3, TRUE);

-- 7. Add is_verified_tenant to reviews
ALTER TABLE reviews
    ADD COLUMN is_verified_tenant BOOLEAN NOT NULL DEFAULT TRUE AFTER rating;
