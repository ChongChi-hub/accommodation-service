-- Sprint 3 Wave 4: METER-01, INVOICE-01/02, REFUND-01 Hardening

ALTER TABLE utility_readings
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER recorded_at,
    ADD COLUMN pricing_model VARCHAR(30) NOT NULL DEFAULT 'EVN_TIERED' AFTER status,
    ADD COLUMN tariff_config_json TEXT NULL AFTER pricing_model,
    ADD COLUMN electricity_cost BIGINT NOT NULL DEFAULT 0 AFTER tariff_config_json,
    ADD COLUMN water_cost BIGINT NOT NULL DEFAULT 0 AFTER electricity_cost,
    ADD COLUMN revision INT NOT NULL DEFAULT 1 AFTER water_cost,
    ADD COLUMN edit_reason VARCHAR(255) NULL AFTER revision;

CREATE TABLE IF NOT EXISTS utility_reading_audits (
    id VARCHAR(36) NOT NULL,
    reading_id VARCHAR(36) NOT NULL,
    action VARCHAR(30) NOT NULL,
    prev_snapshot_json TEXT NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ura_reading (reading_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE invoices
    ADD COLUMN invoice_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD' AFTER status,
    ADD COLUMN reference_invoice_id VARCHAR(36) NULL AFTER invoice_type,
    ADD COLUMN credit_note_reason VARCHAR(255) NULL AFTER reference_invoice_id,
    ADD COLUMN overdue_notified_at DATETIME(3) NULL AFTER credit_note_reason;

CREATE INDEX idx_inv_due_status ON invoices (status, due_date);
CREATE INDEX idx_inv_reading ON invoices (utility_reading_id);

CREATE TABLE IF NOT EXISTS refund_requests (
    id VARCHAR(36) NOT NULL,
    refund_code VARCHAR(64) NOT NULL UNIQUE,
    payment_intent_id VARCHAR(36) NULL,
    contract_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    bank_code VARCHAR(30) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(150) NOT NULL,
    otp_verified TINYINT(1) NOT NULL DEFAULT 0,
    kyc_name_matched TINYINT(1) NOT NULL DEFAULT 0,
    requires_maker_checker TINYINT(1) NOT NULL DEFAULT 0,
    maker_checker_request_id VARCHAR(36) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(255) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ref_tenant (tenant_id),
    INDEX idx_ref_status (status),
    CONSTRAINT chk_ref_status CHECK (status IN ('REQUESTED', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

