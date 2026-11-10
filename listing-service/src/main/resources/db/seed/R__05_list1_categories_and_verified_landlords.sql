-- ==============================================================================
-- Repeatable Migration R__05: LIST-1 Category Cleansing & Landlord Verification Alignment
-- 1. Ensure landlord_snapshots has accurate is_verified flags for landlords 1-6
-- 2. Update SHARED rooms (209-216) with available_spots, total_spots, and gender preferences
-- 3. Update FULL_HOUSE (201-208) with bedrooms and floor
-- 4. Update SINGLE (217-224) with bedrooms = 1 and floor
-- 5. Update Căn hộ (225-232) with bedrooms and floor
-- ==============================================================================

-- 1. Ensure landlord snapshots for landlords 1 through 6
INSERT INTO landlord_snapshots (id, landlord_id, name, phone, avatar_url, email, is_verified, rating_avg, review_count, response_rate_percent, response_time_minutes, room_count, joined_at)
VALUES 
(3, 3, 'Hoàng Thị Mai Lan', '0904000004', '/uploads/avatars/landlord3.jpg', 'lan.hoang@example.com', 1, 4.9, 15, 98, 15, 10, '2025-05-10 10:00:00'),
(4, 4, 'Phan Thanh Bình', '0905000005', '/uploads/avatars/landlord4.jpg', 'binh.phan@example.com', 0, 4.6, 8, 90, 45, 9, '2025-06-12 11:30:00'),
(5, 5, 'Lê Văn Nam', '0906000006', '/uploads/avatars/landlord5.jpg', 'nam.le@example.com', 0, 4.7, 12, 88, 60, 8, '2025-07-01 09:00:00'),
(6, 6, 'Vũ Đình Trọng', '0907000007', '/uploads/avatars/landlord6.jpg', 'trong.vu@example.com', 1, 5.0, 6, 100, 5, 2, '2025-08-15 14:00:00')
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    is_verified = VALUES(is_verified);

-- Update landlord 2, 4, 5 to NOT verified (0) to ensure platform has authentic verified and unverified landlords
UPDATE landlord_snapshots SET is_verified = 1 WHERE landlord_id IN (1, 3, 6);
UPDATE landlord_snapshots SET is_verified = 0 WHERE landlord_id IN (2, 4, 5);

-- 2. Populate available_spots, total_spots, price_unit, gender_preference for SHARED rooms (209-216)
UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 1,
    total_spots = 4,
    gender_preference = 'FEMALE',
    bedrooms = 1,
    floor = 2
WHERE id = 209;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 3,
    total_spots = 6,
    gender_preference = 'MALE',
    bedrooms = 1,
    floor = 3
WHERE id = 210;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 2,
    total_spots = 4,
    gender_preference = 'ALL',
    bedrooms = 1,
    floor = 2
WHERE id = 211;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 1,
    total_spots = 6,
    gender_preference = 'FEMALE',
    bedrooms = 1,
    floor = 4
WHERE id = 212;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 2,
    total_spots = 4,
    gender_preference = 'MALE',
    bedrooms = 1,
    floor = 2
WHERE id = 213;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 4,
    total_spots = 8,
    gender_preference = 'ALL',
    bedrooms = 2,
    floor = 3
WHERE id = 214;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 1,
    total_spots = 4,
    gender_preference = 'FEMALE',
    bedrooms = 1,
    floor = 2
WHERE id = 215;

UPDATE rooms 
SET price_unit = 'BED',
    available_spots = 2,
    total_spots = 6,
    gender_preference = 'MALE',
    bedrooms = 1,
    floor = 3
WHERE id = 216;

-- 3. Update FULL_HOUSE (Nhà nguyên căn, 201-208) with bedrooms (1, 2, 3, 4) and floor
UPDATE rooms SET bedrooms = 3, floor = 3, price_unit = 'ROOM' WHERE id = 201;
UPDATE rooms SET bedrooms = 3, floor = 2, price_unit = 'ROOM' WHERE id = 202;
UPDATE rooms SET bedrooms = 4, floor = 3, price_unit = 'ROOM' WHERE id = 203;
UPDATE rooms SET bedrooms = 2, floor = 2, price_unit = 'ROOM' WHERE id = 204;
UPDATE rooms SET bedrooms = 2, floor = 2, price_unit = 'ROOM' WHERE id = 205;
UPDATE rooms SET bedrooms = 4, floor = 3, price_unit = 'ROOM' WHERE id = 206;
UPDATE rooms SET bedrooms = 3, floor = 3, price_unit = 'ROOM' WHERE id = 207;
UPDATE rooms SET bedrooms = 3, floor = 2, price_unit = 'ROOM' WHERE id = 208;

-- 4. Update SINGLE (Phòng trọ, 217-224) with bedrooms = 1 and floor
UPDATE rooms SET bedrooms = 1, floor = 2, price_unit = 'ROOM' WHERE id BETWEEN 217 AND 224;

-- 5. Update Căn hộ (225-232) with bedrooms and floor
UPDATE rooms SET bedrooms = 1, floor = 4, price_unit = 'ROOM' WHERE id = 225;
UPDATE rooms SET bedrooms = 1, floor = 3, price_unit = 'ROOM' WHERE id = 226;
UPDATE rooms SET bedrooms = 1, floor = 5, price_unit = 'ROOM' WHERE id = 227;
UPDATE rooms SET bedrooms = 1, floor = 6, price_unit = 'ROOM' WHERE id = 228;
UPDATE rooms SET bedrooms = 1, floor = 4, price_unit = 'ROOM' WHERE id = 229;
UPDATE rooms SET bedrooms = 1, floor = 8, price_unit = 'ROOM' WHERE id = 230;
UPDATE rooms SET bedrooms = 1, floor = 5, price_unit = 'ROOM' WHERE id = 231;
UPDATE rooms SET bedrooms = 1, floor = 4, price_unit = 'ROOM' WHERE id = 232;

-- Also fix earlier seed room 5 & 105
UPDATE rooms SET room_type_id = 6, room_type_code = 'SINGLE' WHERE id = 5;
UPDATE rooms SET room_type_id = 8, room_type_code = 'SHARED', price_unit = 'BED', available_spots = 1, total_spots = 4 WHERE id = 105;
