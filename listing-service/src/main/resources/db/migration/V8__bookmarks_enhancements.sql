-- V8__bookmarks_enhancements.sql
-- Modify user_id in bookmarks to VARCHAR(36) to match identity UUIDs
-- Add bookmark_count to rooms table

ALTER TABLE bookmarks MODIFY COLUMN user_id VARCHAR(36) NOT NULL;

ALTER TABLE rooms ADD COLUMN bookmark_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_bookmarks_room ON bookmarks (room_id);
