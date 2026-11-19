-- Migration V11: [WALLET-VERIFY-LEDGER] Real bank inflow verification, unmatched queue,
-- immutable bank tx & proof registries, partial deposit release, hold days, and admin withdrawal verification

-- 1. Shared immutable bank transaction registry (Webhook + Admin manual confirm + Admin payout)
CREATE TABLE IF NOT EXISTS bank_transaction_registry (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    normalized_tx_code VARCHAR(64) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    reference_code VARCHAR(64) NULL,
    order_type VARCHAR(40) NULL,
    order_id VARCHAR(64) NULL,
    receiver_account_masked VARCHAR(32) NULL,
    raw_content VARCHAR(500) NULL,
    sanitized_payload TEXT NULL,
    recorded_by VARCHAR(36) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_btr_ref (reference_code),
    INDEX idx_btr_order (order_type, order_id),
    CONSTRAINT chk_btr_dir CHECK (direction IN ('IN', 'OUT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Unmatched / Mismatched bank inflows queue ("Tien ve chua khop")
CREATE TABLE IF NOT EXISTS unmatched_bank_inflows (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_tx_id VARCHAR(64) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    raw_content VARCHAR(500) NULL,
    extracted_reference VARCHAR(64) NULL,
    mismatch_reason VARCHAR(100) NOT NULL,
    matched_payment_code VARCHAR(64) NULL,
    receiver_account_masked VARCHAR(32) NULL,
    sanitized_payload TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED',
    resolved_by VARCHAR(36) NULL,
    resolution_note VARCHAR(500) NULL,
    resolved_at DATETIME(3) NULL,
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_ubi_status (status, received_at),
    INDEX idx_ubi_ref (extracted_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Immutable supporting payment proof registry (never deleted, SHA-256 + normalized bank tx UNIQUE + dHash)
CREATE TABLE IF NOT EXISTS payment_proof_registry (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_code VARCHAR(64) NOT NULL,
    order_type VARCHAR(40) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    uploader_id VARCHAR(36) NOT NULL,
    normalized_bank_tx_code VARCHAR(64) NOT NULL UNIQUE,
    sha256_hash VARCHAR(128) NOT NULL UNIQUE,
    dhash VARCHAR(32) NOT NULL,
    near_duplicate_flag TINYINT(1) NOT NULL DEFAULT 0,
    near_duplicate_of_id VARCHAR(36) NULL,
    mime_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_ppr_payment (payment_code),
    INDEX idx_ppr_order (order_type, order_id),
    INDEX idx_ppr_dhash (dhash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Immutable payment proof access audit log
CREATE TABLE IF NOT EXISTS payment_proof_access_logs (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    proof_id VARCHAR(36) NOT NULL,
    payment_code VARCHAR(64) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(40) NOT NULL,
    action VARCHAR(40) NOT NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_ppal_proof (proof_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Expand escrow_holds for partial release (50%), hold days, and end-of-contract deduction claims
ALTER TABLE escrow_holds
    ADD COLUMN released_amount BIGINT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN retained_amount BIGINT NOT NULL DEFAULT 0 AFTER released_amount,
    ADD COLUMN pending_release_until DATETIME(3) NULL AFTER move_in_confirmed_at,
    ADD COLUMN pending_settled TINYINT(1) NOT NULL DEFAULT 0 AFTER pending_release_until,
    ADD COLUMN deduction_claim_amount BIGINT NULL AFTER receipt_url,
    ADD COLUMN deduction_claim_reason VARCHAR(500) NULL AFTER deduction_claim_amount,
    ADD COLUMN deduction_claim_proof_url VARCHAR(500) NULL AFTER deduction_claim_reason,
    ADD COLUMN deduction_claim_status VARCHAR(30) NULL DEFAULT 'NONE' AFTER deduction_claim_proof_url;

ALTER TABLE escrow_holds DROP CHECK chk_eh_status;
ALTER TABLE escrow_holds
    ADD CONSTRAINT chk_eh_status CHECK (status IN ('AWAITING_DEPOSIT', 'HELD', 'PARTIALLY_RELEASED', 'DISPUTE_FROZEN', 'RELEASED', 'REFUNDED', 'CANCELLED'));

-- 6. Expand payout_requests for idempotency, bank_tx_code, proof_url, and PAID/CANCELLED states
ALTER TABLE payout_requests
    ADD COLUMN idempotency_key VARCHAR(64) NULL UNIQUE AFTER payout_code,
    ADD COLUMN bank_tx_code VARCHAR(64) NULL UNIQUE AFTER failure_reason,
    ADD COLUMN proof_url VARCHAR(500) NULL AFTER bank_tx_code,
    ADD COLUMN approved_by VARCHAR(36) NULL AFTER proof_url,
    ADD COLUMN paid_by VARCHAR(36) NULL AFTER approved_by,
    ADD COLUMN paid_at DATETIME(3) NULL AFTER paid_by;

ALTER TABLE payout_requests DROP CHECK chk_po_status;
ALTER TABLE payout_requests
    ADD CONSTRAINT chk_po_status CHECK (status IN ('REQUESTED', 'PENDING_APPROVAL', 'APPROVED', 'PROCESSING', 'PAID', 'COMPLETED', 'REJECTED', 'CANCELLED', 'FAILED'));

-- 7. Expand user_bank_accounts with verification_status (MATCHED)
ALTER TABLE user_bank_accounts
    ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT 'MATCHED' AFTER qr_code_url;

