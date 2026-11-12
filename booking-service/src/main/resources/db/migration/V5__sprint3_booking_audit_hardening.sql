-- ============================================================
-- V5__sprint3_booking_audit_hardening.sql
-- Wave 3: APPT-RENT-BE, SAGA-01, CONTRACT-BE, CONTRACT-02
-- ============================================================

-- 1. Optimistic locking @Version on rental_requests and saga_instances
ALTER TABLE rental_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

ALTER TABLE saga_instances
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER status;

-- 2. Soft-delete audit trail for contract occupants (CONTRACT-02)
ALTER TABLE contract_occupants
    ADD COLUMN removed_at DATETIME(3) NULL AFTER is_primary_tenant,
    ADD COLUMN removed_by VARCHAR(36) NULL AFTER removed_at,
    ADD COLUMN removal_reason VARCHAR(255) NULL AFTER removed_by;

-- 3. Verification code / QR hash column for anti-tamper contract PDF (CONTRACT-BE)
ALTER TABLE contracts
    ADD COLUMN verification_code VARCHAR(64) NULL AFTER document_hash;

