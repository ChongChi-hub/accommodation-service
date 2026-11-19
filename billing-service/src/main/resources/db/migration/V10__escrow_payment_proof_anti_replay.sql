ALTER TABLE escrow_holds
    ADD COLUMN payment_code VARCHAR(64) NULL,
    ADD COLUMN bank_tx_code VARCHAR(64) NULL,
    ADD COLUMN receipt_hash VARCHAR(128) NULL,
    ADD COLUMN receipt_url MEDIUMTEXT NULL;

CREATE INDEX idx_escrow_bank_tx_code ON escrow_holds (bank_tx_code);
CREATE INDEX idx_escrow_receipt_hash ON escrow_holds (receipt_hash);
