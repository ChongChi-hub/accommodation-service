-- PHA 2: Add human-readable order_code, check-in disclosure fields, deposit timestamp, and listing sync status to contracts
ALTER TABLE contracts
    ADD COLUMN order_code VARCHAR(32) NULL UNIQUE AFTER contract_code,
    ADD COLUMN checkin_code VARCHAR(12) NULL AFTER verification_code,
    ADD COLUMN checkin_instructions TEXT NULL AFTER checkin_code,
    ADD COLUMN room_full_address VARCHAR(300) NULL AFTER checkin_instructions,
    ADD COLUMN landlord_name VARCHAR(150) NULL AFTER room_full_address,
    ADD COLUMN landlord_phone VARCHAR(30) NULL AFTER landlord_name,
    ADD COLUMN listing_sync_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' AFTER status,
    ADD COLUMN deposit_paid_at DATETIME(6) NULL AFTER landlord_signed_at;

CREATE INDEX idx_contracts_order_code ON contracts (order_code);
CREATE INDEX idx_contracts_listing_sync ON contracts (listing_sync_status);

-- Backfill existing ACTIVE contracts (e.g., demo seed ct-00000001) with deterministic order codes
UPDATE contracts
SET order_code = CONCAT('TOAM-260101-', UPPER(RIGHT(REPLACE(id, '-', ''), 6))),
    listing_sync_status = 'SYNCED'
WHERE status = 'ACTIVE' AND order_code IS NULL;

