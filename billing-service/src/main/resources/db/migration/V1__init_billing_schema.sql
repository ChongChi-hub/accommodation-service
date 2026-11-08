-- ============================================================
-- V1__init_billing_schema.sql
-- Billing Service Schema (cozynest_billing)
-- Bounded Context: Utility Readings, Invoices, Payments, Deposits
-- ============================================================

-- 1. Utility Readings (Chỉ số điện nước định kỳ)
CREATE TABLE IF NOT EXISTS utility_readings (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(64) NOT NULL,
    period_month DATE NOT NULL,
    electric_prev DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    electric_curr DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    water_prev DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    water_curr DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    reading_images_json JSON NULL,
    recorded_by VARCHAR(36) NOT NULL,
    recorded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_ur_contract_month (contract_id, period_month),
    INDEX idx_ur_contract (contract_id),
    INDEX idx_ur_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Invoices (Hóa đơn tiền phòng hàng tháng)
CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(36) NOT NULL,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    contract_id VARCHAR(36) NOT NULL,
    utility_reading_id VARCHAR(36) NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    period_month DATE NOT NULL,
    base_rent BIGINT NOT NULL,
    electricity_amount BIGINT NOT NULL DEFAULT 0,
    water_amount BIGINT NOT NULL DEFAULT 0,
    service_amount BIGINT NOT NULL DEFAULT 0,
    other_amount BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL,
    paid_amount BIGINT NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_inv_contract (contract_id),
    INDEX idx_inv_tenant (tenant_id),
    INDEX idx_inv_landlord (landlord_id),
    INDEX idx_inv_status (status),
    INDEX idx_inv_due (due_date),
    CONSTRAINT chk_inv_status CHECK (status IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT fk_inv_reading FOREIGN KEY (utility_reading_id) REFERENCES utility_readings (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Invoice Items (Chi tiết các khoản tiền trong hóa đơn)
CREATE TABLE IF NOT EXISTS invoice_items (
    id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ii_invoice (invoice_id),
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Payments (Lịch sử thanh toán hóa đơn)
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    payer_id VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    method VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pay_invoice (invoice_id),
    INDEX idx_pay_payer (payer_id),
    INDEX idx_pay_ref (transaction_ref),
    CONSTRAINT chk_pay_method CHECK (method IN ('VIETQR', 'BANK_TRANSFER', 'CASH', 'MOMO', 'VNPAY')),
    CONSTRAINT chk_pay_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT fk_pay_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Deposits (Khoản ký quỹ đặt cọc hợp đồng)
CREATE TABLE IF NOT EXISTS deposits (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL UNIQUE,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    original_amount BIGINT NOT NULL,
    current_balance BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_dep_contract (contract_id),
    INDEX idx_dep_tenant (tenant_id),
    INDEX idx_dep_landlord (landlord_id),
    CONSTRAINT chk_dep_status CHECK (status IN ('HELD', 'DEDUCTED', 'REFUNDED', 'FORFEITED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Deposit Transactions (Biến động tiền đặt cọc)
CREATE TABLE IF NOT EXISTS deposit_transactions (
    id VARCHAR(36) NOT NULL,
    deposit_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference_id VARCHAR(36) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_dt_deposit (deposit_id),
    CONSTRAINT chk_dt_type CHECK (type IN ('HOLD', 'DEDUCTION', 'REFUND', 'TOPUP')),
    CONSTRAINT fk_dt_deposit FOREIGN KEY (deposit_id) REFERENCES deposits (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Expenses (Chi phí vận hành của chủ trọ)
CREATE TABLE IF NOT EXISTS expenses (
    id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(64) NULL,
    category VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    receipt_image_url VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_exp_landlord (landlord_id),
    INDEX idx_exp_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Listing Plans (Gói tin đăng VIP)
CREATE TABLE IF NOT EXISTS listing_plans (
    id VARCHAR(36) NOT NULL,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    duration_days INT NOT NULL,
    features_json JSON NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Promo Codes (Mã khuyến mại)
CREATE TABLE IF NOT EXISTS promo_codes (
    id VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value BIGINT NOT NULL,
    max_discount BIGINT NULL,
    min_order_value BIGINT NOT NULL DEFAULT 0,
    usage_limit INT NOT NULL DEFAULT 100,
    times_used INT NOT NULL DEFAULT 0,
    start_date DATETIME(3) NOT NULL,
    end_date DATETIME(3) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT chk_promo_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Payment Gateway Logs (Nhật ký giao dịch cổng thanh toán)
CREATE TABLE IF NOT EXISTS payment_gateway_logs (
    id VARCHAR(36) NOT NULL,
    payment_id VARCHAR(36) NULL,
    gateway VARCHAR(30) NOT NULL,
    request_payload MEDIUMTEXT NULL,
    response_payload MEDIUMTEXT NULL,
    status_code INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_pgl_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
