-- ============================================================
-- V3__sprint3_contracts_amendments_and_saga.sql
-- Sprint 3: Contracts, Amendments, Co-tenants, and Cross-service Saga
-- ============================================================

-- 1. Contract Amendments (Phụ lục hợp đồng)
CREATE TABLE IF NOT EXISTS contract_amendments (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL,
    amendment_code VARCHAR(64) NOT NULL UNIQUE,
    amendment_type VARCHAR(50) NOT NULL,
    new_rent BIGINT NULL,
    new_end_date DATE NULL,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    tenant_signed_at DATETIME(3) NULL,
    landlord_signed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ca_contract (contract_id),
    CONSTRAINT fk_ca_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Contract Occupants (Danh sách người ở cùng / đồng thuê)
CREATE TABLE IF NOT EXISTS contract_occupants (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    id_card_number VARCHAR(30) NOT NULL,
    relationship VARCHAR(50) NULL,
    is_primary_tenant TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_co_contract (contract_id),
    CONSTRAINT fk_co_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Saga Instances (Giao dịch xuyên service)
CREATE TABLE IF NOT EXISTS saga_instances (
    id VARCHAR(36) NOT NULL,
    saga_id VARCHAR(64) NOT NULL UNIQUE,
    saga_type VARCHAR(50) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    payload_json JSON NOT NULL,
    error_message VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_si_saga (saga_id),
    INDEX idx_si_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Contract enhancements
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS document_hash VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS pdf_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS holding_expires_at DATETIME(3) NULL,
    ADD COLUMN IF NOT EXISTS tenant_signed_at DATETIME(3) NULL,
    ADD COLUMN IF NOT EXISTS landlord_signed_at DATETIME(3) NULL;

-- 5. Rental request enhancements
ALTER TABLE rental_requests
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS holding_expires_at DATETIME(3) NULL;

