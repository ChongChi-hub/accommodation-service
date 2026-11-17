-- ==============================================================================
-- CozyNest Master Reset & Fresh Seed Script (4 Accounts Only)
-- 1. Admin: admin@cozynest.vn (Password: password123)
-- 2. Moderator: moderator@cozynest.vn (Password: password123)
-- 3. Dev 1: dgiahuy2911@gmail.com (Password: password123, Roles: TENANT, LANDLORD)
-- 4. Dev 2: lehuunhathao2k6@gmail.com (Password: password123, Roles: TENANT, LANDLORD)
-- All other transactional/mock data wiped clean. Master catalogs preserved.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. cozynest_identity
-- ------------------------------------------------------------------------------
USE cozynest_identity;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE face_access_logs;
TRUNCATE TABLE face_verification_frames;
TRUNCATE TABLE face_verifications;
TRUNCATE TABLE kyc_access_logs;
TRUNCATE TABLE kyc_events_outbox;
TRUNCATE TABLE kyc_status_history;
TRUNCATE TABLE landlord_profiles;
TRUNCATE TABLE login_attempts;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE user_verifications;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE user_profiles;
TRUNCATE TABLE users;
TRUNCATE TABLE user_identities;
TRUNCATE TABLE consent_histories;

-- Master Roles
INSERT INTO roles (id, name, description, created_at) VALUES
('r-001', 'TENANT', 'Người thuê phòng trọ', CURRENT_TIMESTAMP(3)),
('r-002', 'LANDLORD', 'Chủ nhà trọ cho thuê', CURRENT_TIMESTAMP(3)),
('r-003', 'MODERATOR', 'Kiểm duyệt viên tin đăng và hồ sơ', CURRENT_TIMESTAMP(3)),
('r-004', 'ADMIN', 'Quản trị viên toàn hệ thống', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE name=name;

-- 4 Seed Accounts (Default password: password123)
-- BCrypt hash: $2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a
INSERT INTO users (id, email, phone, password_hash, full_name, avatar_url, gender, status, created_at, updated_at) VALUES
('u-admin-01', 'admin@cozynest.vn', '0903000003', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Quản Trị Viên (Admin)', NULL, 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-mod-01', 'moderator@cozynest.vn', '0904000004', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Kiểm Duyệt Viên (Mod)', NULL, 'OTHER', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-huy-01', 'dgiahuy2911@gmail.com', '0901000001', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Đoàn Gia Huy', NULL, 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('u-hao-01', 'lehuunhathao2k6@gmail.com', '0902000002', '$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a', 'Lê Hữu Nhật Hào', NULL, 'MALE', 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));

-- User Roles
INSERT INTO user_roles (user_id, role_id) VALUES
('u-admin-01', 'r-004'),
('u-mod-01', 'r-003'),
('u-huy-01', 'r-001'),
('u-huy-01', 'r-002'),
('u-hao-01', 'r-001'),
('u-hao-01', 'r-002');

-- Profiles
INSERT INTO user_profiles (id, user_id, full_name, phone, language, timezone, created_at, updated_at) VALUES
('up-admin-01', 'u-admin-01', 'Quản Trị Viên (Admin)', '0903000003', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-mod-01', 'u-mod-01', 'Kiểm Duyệt Viên (Mod)', '0904000004', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-huy-01', 'u-huy-01', 'Đoàn Gia Huy', '0901000001', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
('up-hao-01', 'u-hao-01', 'Lê Hữu Nhật Hào', '0902000002', 'vi', 'Asia/Ho_Chi_Minh', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 2. cozynest_listing
-- ------------------------------------------------------------------------------
USE cozynest_listing;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE reviews;
TRUNCATE TABLE room_amenities;
TRUNCATE TABLE room_images;
TRUNCATE TABLE room_prices;
TRUNCATE TABLE room_rules;
TRUNCATE TABLE room_status_history;
TRUNCATE TABLE landlord_snapshots;
TRUNCATE TABLE banners;
TRUNCATE TABLE cms_articles;
TRUNCATE TABLE rooms;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 3. cozynest_booking
-- ------------------------------------------------------------------------------
USE cozynest_booking;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE appointment_events_outbox;
TRUNCATE TABLE appointment_status_history;
TRUNCATE TABLE viewing_appointments;
TRUNCATE TABLE contract_signatures;
TRUNCATE TABLE contracts;
TRUNCATE TABLE rental_requests;
TRUNCATE TABLE maintenance_comments;
TRUNCATE TABLE maintenance_requests;
TRUNCATE TABLE room_snapshots;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 4. cozynest_billing
-- ------------------------------------------------------------------------------
USE cozynest_billing;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE deposit_transactions;
TRUNCATE TABLE deposits;
TRUNCATE TABLE escrow_holds;
TRUNCATE TABLE payout_requests;
TRUNCATE TABLE refund_requests;
TRUNCATE TABLE expenses;
TRUNCATE TABLE invoice_items;
TRUNCATE TABLE invoices;
TRUNCATE TABLE ledger_postings;
TRUNCATE TABLE ledger_daily_snapshots;
TRUNCATE TABLE journal_entries;
TRUNCATE TABLE maker_checker_requests;
TRUNCATE TABLE payment_intents;
TRUNCATE TABLE payment_transactions;
TRUNCATE TABLE payments;
TRUNCATE TABLE promo_codes;
TRUNCATE TABLE reconciliation_reports;
TRUNCATE TABLE utility_readings;
TRUNCATE TABLE wallets;

-- Reset landlord ledger accounts, reset system account balances to 0
DELETE FROM ledger_accounts WHERE owner_id != 'SYSTEM';
UPDATE ledger_accounts SET balance = 0 WHERE owner_id = 'SYSTEM';

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 5. cozynest_communication
-- ------------------------------------------------------------------------------
USE cozynest_communication;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE conversation_participants;
TRUNCATE TABLE messages;
TRUNCATE TABLE conversations;
TRUNCATE TABLE notification_preferences;
TRUNCATE TABLE notifications;
TRUNCATE TABLE support_ticket_replies;
TRUNCATE TABLE support_tickets;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 6. cozynest_admin
-- ------------------------------------------------------------------------------
USE cozynest_admin;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE audit_logs;
TRUNCATE TABLE content_posts;
TRUNCATE TABLE daily_statistics;
TRUNCATE TABLE dispute_events;
TRUNCATE TABLE disputes;
TRUNCATE TABLE moderation_actions;
TRUNCATE TABLE reports;
TRUNCATE TABLE dead_letter_messages;

SET FOREIGN_KEY_CHECKS = 1;
