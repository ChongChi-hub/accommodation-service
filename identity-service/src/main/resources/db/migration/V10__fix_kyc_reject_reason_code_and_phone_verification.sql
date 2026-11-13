-- ============================================================
-- V10__fix_kyc_reject_reason_code_and_phone_verification.sql
-- Sprint 3: Align reject_reason_code with KycRejectReasonCode enum & seed verified phone status
-- ============================================================

UPDATE user_verifications SET reject_reason_code = 'BLURRY' WHERE reject_reason_code = 'BLURRY_IMAGE';
UPDATE users SET phone_verified_at = CURRENT_TIMESTAMP(3) WHERE phone IS NOT NULL AND status = 'ACTIVE' AND phone_verified_at IS NULL;
