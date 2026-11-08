-- V9__search_filters_and_facets.sql
-- Enhanced multi-criteria search, publication/occupancy status separation,
-- accent-insensitive collation, and performance composite indexes

-- 1. Add publication_status and occupancy_status, gender_preference, furnishing
ALTER TABLE rooms
    ADD COLUMN publication_status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN occupancy_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    ADD COLUMN gender_preference VARCHAR(30) NOT NULL DEFAULT 'ALL',
    ADD COLUMN furnishing VARCHAR(30) NOT NULL DEFAULT 'FULL';

-- 2. Synchronize existing records to adhere to Common Data Contract
UPDATE rooms SET publication_status = status WHERE status IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'HIDDEN', 'EXPIRED');
UPDATE rooms SET publication_status = 'PUBLISHED', occupancy_status = 'OCCUPIED' WHERE status = 'RENTED';
UPDATE rooms SET publication_status = 'PUBLISHED', occupancy_status = 'AVAILABLE' WHERE status IN ('PUBLISHED', 'AVAILABLE');

-- 3. Set accent-insensitive & case-insensitive collation for diacritic-free search
ALTER TABLE rooms MODIFY title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE rooms MODIFY address VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE rooms MODIFY description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;

-- 4. Composite indexes for high-frequency filtering combinations & deterministic pagination
CREATE INDEX idx_rooms_pub_occ_price ON rooms (publication_status, occupancy_status, price, id);
CREATE INDEX idx_rooms_pub_occ_area ON rooms (publication_status, occupancy_status, area, id);
CREATE INDEX idx_rooms_location_pub_occ ON rooms (province_code, ward_id, publication_status, occupancy_status);
CREATE INDEX idx_rooms_type_pub_occ ON rooms (room_type_id, publication_status, occupancy_status);
CREATE INDEX idx_rooms_created_at_id ON rooms (created_at DESC, id DESC);

