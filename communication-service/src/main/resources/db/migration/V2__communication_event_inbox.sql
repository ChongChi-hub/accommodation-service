CREATE TABLE IF NOT EXISTS event_inbox (
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (event_id),
    INDEX idx_ei_agg_order (aggregate_type, aggregate_id, event_type, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

