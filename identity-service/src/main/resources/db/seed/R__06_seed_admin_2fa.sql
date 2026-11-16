-- ============================================================
-- R__06_seed_admin_2fa.sql
-- Seed Two-Factor Authentication (2FA TOTP) for Staff/Admin accounts
-- ============================================================

INSERT INTO user_two_factors (user_id, totp_secret, is_enabled, failed_attempts, locked_until, backup_codes_json, created_at, updated_at)
VALUES 
('u-admin-01', 'JBSWY3DPEHPK3PXP', 1, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-01', 'JBSWY3DPEHPK3PXP', 1, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE 
    totp_secret = VALUES(totp_secret),
    is_enabled = 1,
    failed_attempts = 0,
    locked_until = NULL;

UPDATE users SET two_factor_enabled = 1 WHERE id IN ('u-admin-01', 'u-mod-01');
