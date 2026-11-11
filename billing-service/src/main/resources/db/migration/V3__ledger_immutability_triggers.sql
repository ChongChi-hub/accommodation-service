-- ============================================================
-- V3__ledger_immutability_triggers.sql
-- Sprint 3 LEDGER-02: Enforce Ledger Immutability via Triggers
-- and seed default system ledger accounts
-- ============================================================

-- 1. Chặn UPDATE và DELETE trên bảng journal_entries
DROP TRIGGER IF EXISTS trg_journal_entries_prevent_update;
CREATE TRIGGER trg_journal_entries_prevent_update
BEFORE UPDATE ON journal_entries
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Bảng sổ cái journal_entries là bất biến (immutable), nghiêm cấm mọi thao tác UPDATE!';

DROP TRIGGER IF EXISTS trg_journal_entries_prevent_delete;
CREATE TRIGGER trg_journal_entries_prevent_delete
BEFORE DELETE ON journal_entries
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Bảng sổ cái journal_entries là bất biến (immutable), nghiêm cấm mọi thao tác DELETE!';

-- 2. Chặn UPDATE và DELETE trên bảng ledger_postings
DROP TRIGGER IF EXISTS trg_ledger_postings_prevent_update;
CREATE TRIGGER trg_ledger_postings_prevent_update
BEFORE UPDATE ON ledger_postings
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Bảng định khoản ledger_postings là bất biến (immutable), nghiêm cấm mọi thao tác UPDATE!';

DROP TRIGGER IF EXISTS trg_ledger_postings_prevent_delete;
CREATE TRIGGER trg_ledger_postings_prevent_delete
BEFORE DELETE ON ledger_postings
FOR EACH ROW
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Bảng định khoản ledger_postings là bất biến (immutable), nghiêm cấm mọi thao tác DELETE!';

-- 3. Khởi tạo các tài khoản hệ thống mặc định (nếu chưa có)
INSERT IGNORE INTO ledger_accounts (id, account_number, account_name, account_type, owner_id, balance, currency, is_frozen, version, created_at, updated_at)
VALUES
('sys-acc-1010', '1010-GATEWAY', 'Tiền gửi cổng thanh toán (Gateway Cash)', 'ASSET', 'SYSTEM', 0, 'VND', 0, 0, NOW(3), NOW(3)),
('sys-acc-2010', '2010-ESCROW', 'Ký quỹ cọc giữ chỗ (Deposit Escrow Liability)', 'LIABILITY', 'SYSTEM', 0, 'VND', 0, 0, NOW(3), NOW(3)),
('sys-acc-4010', '4010-REVENUE', 'Doanh thu dịch vụ sàn (Platform Service Revenue)', 'REVENUE', 'SYSTEM', 0, 'VND', 0, 0, NOW(3), NOW(3)),
('sys-acc-5010', '5010-EXPENSE', 'Chi phí chi trả vận hành (Operating Expense)', 'EXPENSE', 'SYSTEM', 0, 'VND', 0, 0, NOW(3), NOW(3));

