-- ============================================================
-- V6__sprint3_billing_audit_hardening.sql
-- Wave 2-4 Hardening: BROKER-01 Outbox, PAY-02 SLA Queue,
-- ESCROW-01 Two-party Move-in, FIN-RISK-01 Maker-Checker Payout,
-- REFUND-01 Failure/Retry Queue, INVOICE-01/02 Late-Fee & Credit
-- ============================================================

-- 1. Transactional Outbox for Billing -> RabbitMQ / Notification Bus (BROKER-01)
CREATE TABLE IF NOT EXISTS billing_event_outbox (
    id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_beo_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Two-party move-in confirmation columns on escrow_holds (ESCROW-01)
ALTER TABLE escrow_holds
    ADD COLUMN tenant_confirmed_move_in TINYINT(1) NOT NULL DEFAULT 0 AFTER sandbox_mode,
    ADD COLUMN landlord_confirmed_move_in TINYINT(1) NOT NULL DEFAULT 0 AFTER tenant_confirmed_move_in,
    ADD COLUMN move_in_confirmed_at DATETIME(3) NULL AFTER landlord_confirmed_move_in;

-- 3. Expand Payout status for Maker-Checker (FIN-RISK-01)
ALTER TABLE payout_requests
    ADD COLUMN requires_maker_checker TINYINT(1) NOT NULL DEFAULT 0 AFTER otp_verified,
    ADD COLUMN maker_checker_request_id VARCHAR(36) NULL AFTER requires_maker_checker;

ALTER TABLE payout_requests DROP CHECK chk_po_status;
ALTER TABLE payout_requests
    ADD CONSTRAINT chk_po_status CHECK (status IN ('REQUESTED', 'PENDING_APPROVAL', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'FAILED'));

-- 4. Expand Refund status & failure retry tracking (REFUND-01)
ALTER TABLE refund_requests
    ADD COLUMN failure_reason VARCHAR(500) NULL AFTER reason,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER failure_reason;

ALTER TABLE refund_requests DROP CHECK chk_ref_status;
ALTER TABLE refund_requests
    ADD CONSTRAINT chk_ref_status CHECK (status IN ('REQUESTED', 'PENDING_APPROVAL', 'APPROVED', 'PROCESSING', 'COMPLETED', 'FAILED', 'REJECTED'));

-- 5. Expand Reconciliation Reports for Gateway SLA & Mismatch tracking (PAY-02)
ALTER TABLE reconciliation_reports
    ADD COLUMN missing_internal_tx INT NOT NULL DEFAULT 0 AFTER mismatched_tx,
    ADD COLUMN missing_provider_tx INT NOT NULL DEFAULT 0 AFTER missing_internal_tx,
    ADD COLUMN sla_due_at DATETIME(3) NULL AFTER summary_notes,
    ADD COLUMN assigned_to VARCHAR(36) NULL AFTER sla_due_at,
    ADD COLUMN resolved_by VARCHAR(36) NULL AFTER assigned_to,
    ADD COLUMN resolved_at DATETIME(3) NULL AFTER resolved_by;

-- 6. Invoice late fee, overpayment credit, and billing reminder tracking (INVOICE-01/02)
ALTER TABLE invoices
    ADD COLUMN late_fee_amount BIGINT NOT NULL DEFAULT 0 AFTER other_amount,
    ADD COLUMN credit_applied_amount BIGINT NOT NULL DEFAULT 0 AFTER late_fee_amount,
    ADD COLUMN overpayment_credit_amount BIGINT NOT NULL DEFAULT 0 AFTER paid_amount,
    ADD COLUMN reminder_count INT NOT NULL DEFAULT 0 AFTER overdue_notified_at;

