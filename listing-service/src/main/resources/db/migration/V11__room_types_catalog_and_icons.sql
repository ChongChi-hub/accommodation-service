-- ============================================================
-- V11__room_types_catalog_and_icons.sql
-- HDR-1 Catalog Alignment:
-- 1. Add icon column to room_types table
-- 2. Backfill icons for existing room types
-- 3. Add SINGLE (Phòng trọ), FULL_HOUSE (Nhà nguyên căn), SHARED (Ở ghép)
-- 4. Ensure top 8 provinces have slugs for hot-areas menu
-- ============================================================

ALTER TABLE room_types
    ADD COLUMN icon VARCHAR(50) NULL AFTER description;

UPDATE room_types SET icon = 'Building2' WHERE code = 'MINI_APARTMENT';
UPDATE room_types SET icon = 'Sparkles' WHERE code = 'STUDIO';
UPDATE room_types SET icon = 'Building' WHERE code = 'APARTMENT';
UPDATE room_types SET icon = 'Hotel' WHERE code = 'HOMESTAY';

INSERT INTO room_types (code, name, description, icon) VALUES
('SINGLE', 'Phòng trọ', 'Phòng trọ khép kín độc lập, tiện nghi cơ bản với chi phí hợp lý', 'DoorOpen'),
('FULL_HOUSE', 'Nhà nguyên căn', 'Nhà riêng nguyên căn nhiều phòng ngủ, phù hợp hộ gia đình hoặc nhóm bạn', 'Home'),
('SHARED', 'Ở ghép', 'Ở ghép chia sẻ phòng trọ hoặc căn hộ tiết kiệm chi phí', 'Users')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    icon = VALUES(icon);

UPDATE provinces SET slug = 'bac-ninh' WHERE code = 'BN' AND (slug IS NULL OR slug = '');
UPDATE provinces SET slug = 'binh-duong' WHERE code = 'BDG' AND (slug IS NULL OR slug = '');

