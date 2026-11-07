-- ==============================================================================
-- V2: viewing_appointments enhancement, status history, and outbox schema
-- Bounded Context: Viewing Appointments ([APPT-02])
-- ==============================================================================

-- 1. Thêm các cột phục vụ máy trạng thái, snapshot và khóa lạc quan vào viewing_appointments
ALTER TABLE viewing_appointments
    ADD COLUMN scheduled_start DATETIME(3) NULL AFTER appointment_time,
    ADD COLUMN scheduled_end DATETIME(3) NULL AFTER scheduled_start,
    ADD COLUMN tenant_note TEXT NULL AFTER scheduled_end,
    ADD COLUMN landlord_note TEXT NULL AFTER tenant_note,
    ADD COLUMN cancel_reason_code VARCHAR(50) NULL AFTER landlord_note,
    ADD COLUMN cancel_reason_note VARCHAR(500) NULL AFTER cancel_reason_code,
    ADD COLUMN cancelled_by VARCHAR(30) NULL AFTER cancel_reason_note,
    ADD COLUMN confirmed_at DATETIME(3) NULL AFTER cancelled_by,
    ADD COLUMN completed_at DATETIME(3) NULL AFTER confirmed_at,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER completed_at,
    ADD COLUMN room_title VARCHAR(255) NULL AFTER version,
    ADD COLUMN room_address VARCHAR(300) NULL AFTER room_title,
    ADD COLUMN room_cover_thumb_key VARCHAR(500) NULL AFTER room_address,
    ADD COLUMN contact_name VARCHAR(100) NULL AFTER room_cover_thumb_key,
    ADD COLUMN contact_phone VARCHAR(20) NULL AFTER contact_name,
    ADD COLUMN contact_email VARCHAR(100) NULL AFTER contact_phone;

-- Đồng bộ dữ liệu cũ nếu có
UPDATE viewing_appointments
SET scheduled_start = TIMESTAMP(appointment_date, appointment_time),
    scheduled_end = DATE_ADD(TIMESTAMP(appointment_date, appointment_time), INTERVAL 45 MINUTE),
    tenant_note = message,
    cancel_reason_note = rejection_reason
WHERE scheduled_start IS NULL;

-- Cập nhật ràng buộc NOT NULL cho các mốc thời gian UTC
ALTER TABLE viewing_appointments
    MODIFY COLUMN scheduled_start DATETIME(3) NOT NULL,
    MODIFY COLUMN scheduled_end DATETIME(3) NOT NULL;

-- Bỏ check constraint cũ và thêm check constraint mới cho status, cancel_reason_code, cancelled_by
ALTER TABLE viewing_appointments DROP CHECK chk_va_status;

ALTER TABLE viewing_appointments
    ADD CONSTRAINT chk_va_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED', 'NO_SHOW', 'EXPIRED')),
    ADD CONSTRAINT chk_va_cancel_reason
        CHECK (cancel_reason_code IS NULL OR cancel_reason_code IN ('BUSY', 'ROOM_TAKEN', 'TENANT_NOT_ELIGIBLE', 'TIME_CONFLICT', 'OTHER')),
    ADD CONSTRAINT chk_va_cancelled_by
        CHECK (cancelled_by IS NULL OR cancelled_by IN ('LANDLORD', 'TENANT', 'SYSTEM'));

-- Cột sinh phục vụ Unique chống tạo trùng lịch đang hiệu lực (PENDING hoặc CONFIRMED)
ALTER TABLE viewing_appointments
    ADD COLUMN active_slot VARCHAR(150) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'CONFIRMED') THEN CONCAT(room_id, ':', tenant_id, ':', DATE_FORMAT(scheduled_start, '%Y%m%d%H%i')) ELSE NULL END
    ) STORED;

ALTER TABLE viewing_appointments
    ADD CONSTRAINT uq_va_active_slot UNIQUE (active_slot);

-- Thêm các chỉ mục phục vụ truy vấn landlord, room, tenant và thời gian
CREATE INDEX idx_va_landlord_status_start ON viewing_appointments (landlord_id, status, scheduled_start);
CREATE INDEX idx_va_room_start ON viewing_appointments (room_id, scheduled_start);
CREATE INDEX idx_va_tenant_start ON viewing_appointments (tenant_id, scheduled_start);
CREATE INDEX idx_va_scheduled_start ON viewing_appointments (scheduled_start);

-- 2. Bảng theo dõi lịch sử chuyển trạng thái (Audit Trail)
CREATE TABLE IF NOT EXISTS appointment_status_history (
    id BIGINT AUTO_INCREMENT NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ash_appointment (appointment_id),
    INDEX idx_ash_created_at (created_at),
    CONSTRAINT fk_ash_appointment FOREIGN KEY (appointment_id) REFERENCES viewing_appointments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng Transactional Outbox ghi nhận sự kiện miền
CREATE TABLE IF NOT EXISTS appointment_events_outbox (
    id BIGINT AUTO_INCREMENT NOT NULL,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'VIEWING_APPOINTMENT',
    aggregate_id VARCHAR(36) NOT NULL,
    payload JSON NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_aeo_status (published_at, created_at),
    INDEX idx_aeo_aggregate (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
