-- ============================================================
-- V4__chat_participant_display_and_room_title.sql
-- Enrich Chat Conversations with Room Title and Participant Display
-- ============================================================

ALTER TABLE conversation_participants ADD COLUMN user_name VARCHAR(150) NULL;
ALTER TABLE conversation_participants ADD COLUMN user_avatar VARCHAR(500) NULL;
ALTER TABLE conversations ADD COLUMN room_title VARCHAR(255) NULL;

-- Update demo data if exists
UPDATE conversation_participants SET user_name = 'Nguyễn Văn Thuê' WHERE user_id = 'u-tenant-01';
UPDATE conversation_participants SET user_name = 'Trần Thị Chủ Trọ' WHERE user_id = 'u-landlord-01';
UPDATE conversations SET room_title = 'Phòng Studio Full Nội Thất Cầu Giấy' WHERE id = 'conv-0001';

