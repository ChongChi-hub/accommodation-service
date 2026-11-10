-- ============================================================
-- V2__sprint3_billing_fintech.sql
-- Sprint 3 Fintech: Ledger, Payments, Escrow, Risk, Reconciliation
-- ============================================================

-- 1. Ledger Accounts (Bảng tài khoản kế toán kép)
CREATE TABLE IF NOT EXISTS ledger_accounts (
    id VARCHAR(36) NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    owner_id VARCHAR(36) NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    is_frozen TINYINT(1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_la_owner (owner_id),
    INDEX idx_la_type (account_type),
    CONSTRAINT chk_la_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Journal Entries (Nhật ký bút toán bất biến)
CREATE TABLE IF NOT EXISTS journal_entries (
    id VARCHAR(36) NOT NULL,
    entry_number VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(64) NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'POSTED',
    created_by VARCHAR(36) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_je_ref (reference_type, reference_id),
    INDEX idx_je_created (created_at),
    CONSTRAINT chk_je_status CHECK (status IN ('POSTED', 'REVERSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Ledger Postings (Các định khoản Nợ / Có của bút toán)
CREATE TABLE IF NOT EXISTS ledger_postings (
    id VARCHAR(36) NOT NULL,
    journal_entry_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_lp_entry (journal_entry_id),
    INDEX idx_lp_account (account_id),
    CONSTRAINT chk_lp_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT fk_lp_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_lp_account FOREIGN KEY (account_id) REFERENCES ledger_accounts (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Maker-Checker Requests (Duyệt 2 người cho giao dịch đặc biệt)
CREATE TABLE IF NOT EXISTS maker_checker_requests (
    id VARCHAR(36) NOT NULL,
    maker_id VARCHAR(36) NOT NULL,
    checker_id VARCHAR(36) NULL,
    action_type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255) NOT NULL,
    payload_json JSON NULL,
    decided_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_mc_status (status),
    CONSTRAINT chk_mc_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Payment Intents (Ý định thanh toán cổng đối tác & VietQR)
CREATE TABLE IF NOT EXISTS payment_intents (
    id VARCHAR(36) NOT NULL,
    payment_code VARCHAR(64) NOT NULL UNIQUE,
    payer_id VARCHAR(36) NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    paid_amount BIGINT NOT NULL DEFAULT 0,
    qr_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pi_code (payment_code),
    INDEX idx_pi_order (order_type, order_id),
    INDEX idx_pi_status (status),
    CONSTRAINT chk_pi_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED', 'MISMATCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Payout Requests (Yêu cầu rút tiền về ngân hàng của chủ trọ)
CREATE TABLE IF NOT EXISTS payout_requests (
    id VARCHAR(36) NOT NULL,
    payout_code VARCHAR(64) NOT NULL UNIQUE,
    landlord_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    bank_code VARCHAR(30) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    otp_verified TINYINT(1) NOT NULL DEFAULT 0,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_po_landlord (landlord_id),
    INDEX idx_po_status (status),
    CONSTRAINT chk_po_status CHECK (status IN ('REQUESTED', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Risk Profiles (Giám sát hạn mức và rủi ro gian lận)
CREATE TABLE IF NOT EXISTS risk_profiles (
    user_id VARCHAR(36) NOT NULL,
    daily_limit BIGINT NOT NULL DEFAULT 20000000,
    daily_withdrawn_amount BIGINT NOT NULL DEFAULT 0,
    last_withdrawal_date DATE NULL,
    velocity_count INT NOT NULL DEFAULT 0,
    is_frozen TINYINT(1) NOT NULL DEFAULT 0,
    freeze_reason VARCHAR(255) NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Reconciliation Reports (Báo cáo đối soát hằng đêm)
CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id VARCHAR(36) NOT NULL,
    recon_date DATE NOT NULL UNIQUE,
    total_internal_tx INT NOT NULL DEFAULT 0,
    total_gateway_tx INT NOT NULL DEFAULT 0,
    matched_tx INT NOT NULL DEFAULT 0,
    mismatched_tx INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    summary_notes TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

