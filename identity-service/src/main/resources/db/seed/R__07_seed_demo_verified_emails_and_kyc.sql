-- BUG-01: Ensure primary demo accounts have verified emails and approved landlord KYC
-- Repeatable Flyway seed (idempotent, additive, never modifies past versioned migrations)

UPDATE users
SET email_verified_at = COALESCE(email_verified_at, CURRENT_TIMESTAMP(3))
WHERE id IN ('u-tenant-01', 'u-tenant-02', 'u-landlord-01');

UPDATE user_verifications
SET status = 'APPROVED',
    reviewed_at = COALESCE(reviewed_at, CURRENT_TIMESTAMP(3))
WHERE user_id = 'u-landlord-01' AND status <> 'APPROVED';

