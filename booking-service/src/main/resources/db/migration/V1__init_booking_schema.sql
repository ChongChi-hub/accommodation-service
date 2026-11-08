-- ============================================================
-- V1__init_booking_schema.sql
-- Booking Service Schema (cozynest_booking)
-- Bounded Context: Appointments, Requests, Contracts, Maintenance
-- ============================================================

-- 1. Contract Templates (Mẫu hợp đồng chuẩn)
CREATE TABLE IF NOT EXISTS contract_templates (
    id VARCHAR(36) NOT NULL,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255) NULL,
    content_template MEDIUMTEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Viewing Appointments (Lịch hẹn xem phòng)
CREATE TABLE IF NOT EXISTS viewing_appointments (
    id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    message TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_va_room (room_id),
    INDEX idx_va_tenant (tenant_id),
    INDEX idx_va_landlord (landlord_id),
    INDEX idx_va_status (status),
    INDEX idx_va_datetime (appointment_date, appointment_time),
    CONSTRAINT chk_va_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Rental Requests (Yêu cầu thuê phòng)
CREATE TABLE IF NOT EXISTS rental_requests (
    id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    proposed_move_in_date DATE NOT NULL,
    num_occupants INT NOT NULL DEFAULT 1,
    message TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    contract_id VARCHAR(36) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_rr_room (room_id),
    INDEX idx_rr_tenant (tenant_id),
    INDEX idx_rr_landlord (landlord_id),
    INDEX idx_rr_status (status),
    CONSTRAINT chk_rr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Contracts (Hợp đồng thuê điện tử)
CREATE TABLE IF NOT EXISTS contracts (
    id VARCHAR(36) NOT NULL,
    contract_code VARCHAR(50) NOT NULL UNIQUE,
    room_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    request_id VARCHAR(36) NULL,
    template_id VARCHAR(36) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent BIGINT NOT NULL,
    deposit_amount BIGINT NOT NULL DEFAULT 0,
    payment_cycle_months INT NOT NULL DEFAULT 1,
    payment_due_day INT NOT NULL DEFAULT 5,
    terms MEDIUMTEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ct_room (room_id),
    INDEX idx_ct_tenant (tenant_id),
    INDEX idx_ct_landlord (landlord_id),
    INDEX idx_ct_status (status),
    INDEX idx_ct_dates (start_date, end_date),
    CONSTRAINT chk_ct_status CHECK (status IN ('DRAFT', 'PENDING_SIGN', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'CANCELLED')),
    CONSTRAINT fk_ct_template FOREIGN KEY (template_id) REFERENCES contract_templates (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Contract Signatures (Chữ ký điện tử / Xác thực OTP)
CREATE TABLE IF NOT EXISTS contract_signatures (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL,
    signer_id VARCHAR(36) NOT NULL,
    signer_role VARCHAR(30) NOT NULL,
    signature_type VARCHAR(30) NOT NULL DEFAULT 'OTP',
    signature_hash VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    signed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_cs_contract (contract_id),
    INDEX idx_cs_signer (signer_id),
    CONSTRAINT fk_cs_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Contract Terminations (Thanh lý hợp đồng)
CREATE TABLE IF NOT EXISTS contract_terminations (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL UNIQUE,
    requested_by VARCHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    termination_date DATE NOT NULL,
    refund_deposit_amount BIGINT NOT NULL DEFAULT 0,
    deduction_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_ctm_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Room Snapshots (Ảnh chụp trạng thái phòng khi ký kết)
CREATE TABLE IF NOT EXISTS room_snapshots (
    id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NOT NULL UNIQUE,
    room_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    address VARCHAR(300) NOT NULL,
    area DOUBLE NOT NULL,
    price BIGINT NOT NULL,
    deposit BIGINT NOT NULL,
    amenities_json JSON NULL,
    images_json JSON NULL,
    snapshot_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_rs_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Maintenance Requests (Yêu cầu bảo trì, sửa chữa)
CREATE TABLE IF NOT EXISTS maintenance_requests (
    id VARCHAR(36) NOT NULL,
    request_code VARCHAR(50) NOT NULL UNIQUE,
    room_id VARCHAR(36) NOT NULL,
    contract_id VARCHAR(36) NULL,
    tenant_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    images_json JSON NULL,
    estimated_cost BIGINT NULL,
    actual_cost BIGINT NULL,
    scheduled_at DATETIME(3) NULL,
    resolved_at DATETIME(3) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_mr_room (room_id),
    INDEX idx_mr_contract (contract_id),
    INDEX idx_mr_tenant (tenant_id),
    INDEX idx_mr_landlord (landlord_id),
    INDEX idx_mr_status (status),
    CONSTRAINT chk_mr_category CHECK (category IN ('ELECTRICITY', 'WATER', 'APPLIANCE', 'FURNITURE', 'STRUCTURE', 'OTHER')),
    CONSTRAINT chk_mr_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_mr_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Maintenance Comments (Trao đổi trong yêu cầu bảo trì)
CREATE TABLE IF NOT EXISTS maintenance_comments (
    id VARCHAR(36) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    author_role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    images_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_mc_request (request_id),
    CONSTRAINT fk_mc_request FOREIGN KEY (request_id) REFERENCES maintenance_requests (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
