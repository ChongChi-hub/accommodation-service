-- ============================================================
-- R__06_seed_admin_2fa.sql
-- Seed Two-Factor Authentication (2FA TOTP) for Staff/Admin accounts
-- ============================================================

INSERT INTO user_two_factors (user_id, totp_secret, is_enabled, failed_attempts, locked_until, backup_codes_json, created_at, updated_at)
VALUES 
('u-admin-01', 'JBSWY3DPEHPK3PXP', 0, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-admin-02', 'JBSWY3DPEHPK3PXP', 0, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-01', 'JBSWY3DPEHPK3PXP', 0, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-02', 'JBSWY3DPEHPK3PXP', 0, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-03', 'JBSWY3DPEHPK3PXP', 0, 0, NULL, '["12345678", "87654321", "11223344", "55667788", "99001122", "33445566", "77889900", "99887766"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE 
    totp_secret = VALUES(totp_secret),
    is_enabled = 0,
    failed_attempts = 0,
    locked_until = NULL,
    backup_codes_json = VALUES(backup_codes_json);

UPDATE users SET two_factor_enabled = 0 WHERE id IN ('u-admin-01', 'u-admin-02', 'u-mod-01', 'u-mod-02', 'u-mod-03');
