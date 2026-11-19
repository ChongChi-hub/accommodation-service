-- Allow AWAITING_DEPOSIT status in contracts check constraint
ALTER TABLE contracts DROP CHECK chk_ct_status;
ALTER TABLE contracts ADD CONSTRAINT chk_ct_status CHECK (status IN ('DRAFT', 'PENDING_SIGN', 'AWAITING_DEPOSIT', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'CANCELLED'));

