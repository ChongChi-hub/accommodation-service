-- ============================================================================
-- DB-LEAST-PRIVILEGE-GRANTS.sql (DEVOPS-02 & LEDGER-02)
-- Run as DBA during staging/production DB provisioning.
-- Separates per-service credentials and enforces append-only / restricted
-- privileges on immutable financial tables.
-- ============================================================================

-- 1. Service-scoped users (1 DB account per microservice)
CREATE USER IF NOT EXISTS 'svc_identity'@'%' IDENTIFIED BY 'REPLACE_IDENTITY_PASS';
CREATE USER IF NOT EXISTS 'svc_listing'@'%' IDENTIFIED BY 'REPLACE_LISTING_PASS';
CREATE USER IF NOT EXISTS 'svc_booking'@'%' IDENTIFIED BY 'REPLACE_BOOKING_PASS';
CREATE USER IF NOT EXISTS 'svc_billing'@'%' IDENTIFIED BY 'REPLACE_BILLING_PASS';
CREATE USER IF NOT EXISTS 'svc_comm'@'%' IDENTIFIED BY 'REPLACE_COMM_PASS';
CREATE USER IF NOT EXISTS 'svc_admin'@'%' IDENTIFIED BY 'REPLACE_ADMIN_PASS';
CREATE USER IF NOT EXISTS 'svc_migrator'@'%' IDENTIFIED BY 'REPLACE_MIGRATOR_PASS';

-- 2. Application DML grants (NO DROP / ALTER / GRANT for runtime users)
GRANT SELECT, INSERT, UPDATE, DELETE ON cozynest_identity.* TO 'svc_identity'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cozynest_listing.* TO 'svc_listing'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cozynest_booking.* TO 'svc_booking'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cozynest_comm.* TO 'svc_comm'@'%';
GRANT SELECT, INSERT, UPDATE ON cozynest_admin.* TO 'svc_admin'@'%';

-- 3. Ledger least-privilege isolation (cozynest_billing)
-- billing runtime cannot DELETE or ALTER journal rows; ledger rows are append-only via triggers + grants
GRANT SELECT, INSERT, UPDATE ON cozynest_billing.* TO 'svc_billing'@'%';
REVOKE DELETE, DROP, ALTER ON cozynest_billing.journal_entries FROM 'svc_billing'@'%';
REVOKE UPDATE, DELETE, DROP, ALTER ON cozynest_billing.journal_lines FROM 'svc_billing'@'%';

-- 4. Dedicated Flyway migrator role (DDL used only during deploy window)
GRANT ALL PRIVILEGES ON cozynest_identity.* TO 'svc_migrator'@'%';
GRANT ALL PRIVILEGES ON cozynest_listing.* TO 'svc_migrator'@'%';
GRANT ALL PRIVILEGES ON cozynest_booking.* TO 'svc_migrator'@'%';
GRANT ALL PRIVILEGES ON cozynest_billing.* TO 'svc_migrator'@'%';
GRANT ALL PRIVILEGES ON cozynest_comm.* TO 'svc_migrator'@'%';
GRANT ALL PRIVILEGES ON cozynest_admin.* TO 'svc_migrator'@'%';

FLUSH PRIVILEGES;

