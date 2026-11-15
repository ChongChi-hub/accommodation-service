-- ============================================================
-- R__06_seed_admin_2fa.sql
-- Seed Two-Factor Authentication (2FA TOTP) for Staff/Admin accounts
-- ============================================================

INSERT INTO user_two_factors (user_id, totp_secret, is_enabled, failed_attempts, locked_until, backup_codes_json, created_at, updated_at)
VALUES 
('u-admin-01', 'JBSWY3DPEHPK3PXP', 1, 0, NULL, '["sha256:ef537f25c895bfa782526529a9b63d97aa47e6d080c34e8e81d77a06f4772b78","sha256:8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-01', 'JBSWY3DPEHPK3PXP', 1, 0, NULL, '["sha256:ef537f25c895bfa782526529a9b63d97aa47e6d080c34e8e81d77a06f4772b78","sha256:8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4"]', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE 
    totp_secret = VALUES(totp_secret),
    is_enabled = 1,
    failed_attempts = 0,
    locked_until = NULL;

UPDATE users SET two_factor_enabled = 1 WHERE id IN ('u-admin-01', 'u-mod-01');
