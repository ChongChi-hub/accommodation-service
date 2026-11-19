-- Allow SUBMITTED status for tenant transfer confirmation awaiting landlord verification
ALTER TABLE payment_intents DROP CHECK chk_pi_status;
ALTER TABLE payment_intents
    ADD CONSTRAINT chk_pi_status CHECK (
        status IN ('PENDING', 'SUBMITTED', 'PAID', 'PARTIAL_PAID', 'OVERPAID', 'EXPIRED', 'CANCELLED', 'MISMATCH')
    );

