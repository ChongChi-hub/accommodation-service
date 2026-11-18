-- ============================================================
-- V12__email_verification_attempt_count.sql
-- Add attempt_count to email_verification_tokens
-- ============================================================

ALTER TABLE email_verification_tokens
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
