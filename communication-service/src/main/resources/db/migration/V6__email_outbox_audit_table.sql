CREATE TABLE IF NOT EXISTS email_outbox (
    id VARCHAR(36) PRIMARY KEY,
    event_key VARCHAR(160) NULL,
    provider VARCHAR(20) NOT NULL,
    recipient_masked VARCHAR(160) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    month_key VARCHAR(7) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_email_outbox_event_key (event_key),
    INDEX idx_email_outbox_month_status (month_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
