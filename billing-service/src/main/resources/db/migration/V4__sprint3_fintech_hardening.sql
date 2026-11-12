-- ============================================================
-- V4__sprint3_fintech_hardening.sql
-- Wave 2: LEDGER-01/02, PAY-01/02, ESCROW-01, FIN-RISK-01
-- ============================================================

-- 1. Idempotency key on journal entries (LEDGER-01)
ALTER TABLE journal_entries
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER reference_id,
    ADD UNIQUE KEY uq_je_idempotency (idempotency_key);

-- 2. Daily ledger balance snapshots (LEDGER-01)
CREATE TABLE IF NOT EXISTS ledger_daily_snapshots (
    id VARCHAR(36) NOT NULL,
    snapshot_date DATE NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    closing_balance BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_lds_date_acc (snapshot_date, account_id),
    INDEX idx_lds_date (snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Payment webhook raw logs & state expansion (PAY-01 / PAY-02)
ALTER TABLE payment_intents DROP CHECK chk_pi_status;
ALTER TABLE payment_intents
    ADD CONSTRAINT chk_pi_status CHECK (status IN ('PENDING', 'PAID', 'PARTIAL_PAID', 'OVERPAID', 'EXPIRED', 'CANCELLED', 'MISMATCH'));

CREATE TABLE IF NOT EXISTS payment_webhook_logs (
    id VARCHAR(36) NOT NULL,
    payment_code VARCHAR(64) NULL,
    source_ip VARCHAR(64) NULL,
    signature_valid TINYINT(1) NOT NULL DEFAULT 0,
    processing_status VARCHAR(40) NOT NULL,
    error_message VARCHAR(500) NULL,
    raw_payload TEXT NOT NULL,
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pwl_code (payment_code),
    INDEX idx_pwl_received (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Escrow holds ledger tracking (ESCROW-01 / LEGAL-02 sandbox gate)
CREATE TABLE IF NOT EXISTS escrow_holds (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(64) NOT NULL UNIQUE,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD',
    sandbox_mode TINYINT(1) NOT NULL DEFAULT 1,
    inspection_ends_at DATETIME(3) NOT NULL,
    dispute_reason VARCHAR(500) NULL,
    released_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_eh_landlord (landlord_id),
    INDEX idx_eh_status (status),
    CONSTRAINT chk_eh_status CHECK (status IN ('HELD', 'DISPUTE_FROZEN', 'RELEASED', 'REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Anti-fraud & 24h bank-change lock on risk_profiles (FIN-RISK-01)
ALTER TABLE risk_profiles
    ADD COLUMN bank_changed_at DATETIME(3) NULL AFTER freeze_reason,
    ADD COLUMN is_blacklisted TINYINT(1) NOT NULL DEFAULT 0 AFTER bank_changed_at,
    ADD COLUMN blacklist_reason VARCHAR(255) NULL AFTER is_blacklisted;

