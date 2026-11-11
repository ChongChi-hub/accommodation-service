-- Sprint 3 Wave 3: KYC-03 Document Expiry & Verified Ownership Badge Revocation

ALTER TABLE property_ownership_documents
    ADD COLUMN expires_at DATETIME(3) NULL AFTER reviewed_at;

CREATE INDEX idx_ownership_doc_status_expiry ON property_ownership_documents (status, expires_at);

