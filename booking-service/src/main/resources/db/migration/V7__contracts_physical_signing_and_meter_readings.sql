-- ============================================================
-- V6__contracts_physical_signing_and_meter_readings.sql
-- Sprint 3 / Enhancement: Physical contract signing & initial meter readings
-- ============================================================

ALTER TABLE contracts
    ADD COLUMN initial_electricity_reading DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Chỉ số điện ban đầu bàn giao',
    ADD COLUMN initial_water_reading DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Chỉ số nước ban đầu bàn giao',
    ADD COLUMN electricity_rate DECIMAL(10,2) NOT NULL DEFAULT 3500.00 COMMENT 'Đơn giá điện thỏa thuận (đ/kWh)',
    ADD COLUMN water_rate DECIMAL(10,2) NOT NULL DEFAULT 25000.00 COMMENT 'Đơn giá nước thỏa thuận (đ/m3)',
    ADD COLUMN physical_contract_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PHYSICAL_SIGN' COMMENT 'Trạng thái ký giấy: PENDING_PHYSICAL_SIGN, WAITING_TENANT_CONFIRMATION, PHYSICAL_SIGNED, REJECTED_BY_TENANT',
    ADD COLUMN physical_contract_scan_url VARCHAR(500) NULL COMMENT 'Đường dẫn ảnh scan hợp đồng đã ký hai bên',
    ADD COLUMN physical_signed_at DATETIME(3) NULL COMMENT 'Thời điểm chủ nhà tải lên bản scan hợp đồng',
    ADD COLUMN tenant_confirmed_at DATETIME(3) NULL COMMENT 'Thời điểm khách thuê xác nhận hợp đồng giấy chính xác',
    ADD COLUMN tenant_reject_reason VARCHAR(500) NULL COMMENT 'Lý do khách thuê từ chối bản scan';

CREATE INDEX idx_contracts_physical_status ON contracts (physical_contract_status);

