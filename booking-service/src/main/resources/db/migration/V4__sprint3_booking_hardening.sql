-- Sprint 3 Wave 3: Booking, Contract E-Sign Audit, Private PDF & Saga Hardening

ALTER TABLE contracts
    ADD COLUMN pdf_object_key VARCHAR(255) NULL AFTER pdf_url,
    ADD COLUMN expiry_notified_days VARCHAR(64) NULL AFTER status;

CREATE INDEX idx_contracts_end_status ON contracts (status, end_date);

CREATE TABLE IF NOT EXISTS contract_signature_audits (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    signer_id CHAR(36) NOT NULL,
    signer_role VARCHAR(20) NOT NULL,
    otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    document_hash VARCHAR(64) NOT NULL,
    pdf_object_key VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    signed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_sig_audit_contract FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_sig_audit_contract (contract_id),
    INDEX idx_sig_audit_signer (signer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rental_req_holding ON rental_requests (status, holding_expires_at);

ALTER TABLE saga_instances
    ADD COLUMN compensation_step VARCHAR(100) NULL AFTER current_step;

CREATE INDEX idx_saga_status_updated ON saga_instances (status, updated_at);

