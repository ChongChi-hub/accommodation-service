CREATE TABLE IF NOT EXISTS ownership_doc_access_logs (
    id VARCHAR(36) PRIMARY KEY,
    doc_id VARCHAR(36) NULL,
    room_id BIGINT NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    accessed_at DATETIME(6) NOT NULL,
    INDEX idx_odal_room_time (room_id, accessed_at DESC),
    INDEX idx_odal_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

