-- Allow pre-payment escrow reservation (AWAITING_DEPOSIT) and unpaid cancellation (CANCELLED)
-- so unpaid contract escrows cannot be auto-released or refunded from 2010-ESCROW.
ALTER TABLE escrow_holds DROP CHECK chk_eh_status;
ALTER TABLE escrow_holds
    ADD CONSTRAINT chk_eh_status CHECK (
        status IN ('AWAITING_DEPOSIT', 'HELD', 'DISPUTE_FROZEN', 'RELEASED', 'REFUNDED', 'CANCELLED')
    );
