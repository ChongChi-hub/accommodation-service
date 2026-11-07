-- ============================================================
-- V2__seed_demo_billing.sql
-- Seed Demo Data for Billing Service
-- Fully consistent with seed-ids.md, booking-service, and frontend UI repositories
-- ============================================================

-- 1. Utility Readings
INSERT INTO utility_readings (id, contract_id, room_id, period_month, electric_prev, electric_curr, water_prev, water_curr, reading_images_json, recorded_by, recorded_at) VALUES
('ur-00000001', 'ct-00000001', 'room-00000001-0000-0000-0000-000000000001', '2026-11-01', 1250.00, 1315.00, 45.00, 48.00, '["/readings/electric_nov.jpg", "/readings/water_nov.jpg"]', 'u-landlord-01', '2026-11-01 08:00:00.000'),
('ur-00000002', 'ct-00000001', 'room-00000001-0000-0000-0000-000000000001', '2026-10-01', 1190.00, 1250.00, 42.00, 45.00, '["/readings/electric_oct.jpg", "/readings/water_oct.jpg"]', 'u-landlord-01', '2026-10-01 08:30:00.000'),
('ur-00000003', 'ct-00000002', 'room-00000001-0000-0000-0000-000000000002', '2026-11-01', 2100.00, 2180.00, 60.00, 65.00, '["/readings/electric_cobac.jpg"]', 'u-landlord-01', '2026-11-01 09:00:00.000')
ON DUPLICATE KEY UPDATE contract_id=contract_id;

-- 2. Invoices (Total matches sum of items)
-- inv-00000001: 4,500,000 (rent) + 247,000 (65 kWh * 3,800) + 84,000 (3 m³ * 28,000) + 44,000 (service) = 4,875,000
-- inv-00000002: 4,500,000 + 228,000 (60 kWh) + 84,000 (3 m³) + 8,000 = 4,820,000
-- inv-00000003: 7,200,000 (rent) + 320,000 (80 kWh * 4,000) + 130,000 (service) = 7,650,000
-- inv-00000004: 3,200,000 + 150,000 + 100,000 = 3,450,000 (paid 1,500,000)
INSERT INTO invoices (id, invoice_code, contract_id, utility_reading_id, tenant_id, landlord_id, period_month, base_rent, electricity_amount, water_amount, service_amount, other_amount, total_amount, paid_amount, due_date, status, version, created_at, updated_at) VALUES
('inv-00000001', 'INV-2026-11-01', 'ct-00000001', 'ur-00000001', 'u-tenant-01', 'u-landlord-01', '2026-11-01', 4500000, 247000, 84000, 44000, 0, 4875000, 4875000, '2026-11-05', 'PAID', 2, '2026-11-01 09:00:00.000', '2026-11-05 10:15:30.000'),
('inv-00000002', 'INV-2026-10-01', 'ct-00000001', 'ur-00000002', 'u-tenant-01', 'u-landlord-01', '2026-10-01', 4500000, 228000, 84000, 8000, 0, 4820000, 4820000, '2026-10-05', 'PAID', 2, '2026-10-01 09:00:00.000', '2026-10-04 16:20:00.000'),
('inv-00000003', 'INV-2026-11-02', 'ct-00000002', 'ur-00000003', 'u-tenant-02', 'u-landlord-01', '2026-11-01', 7200000, 320000, 0, 130000, 0, 7650000, 0, '2026-11-05', 'OVERDUE', 1, '2026-11-01 10:00:00.000', '2026-11-06 00:00:00.000'),
('inv-00000004', 'INV-2026-11-03', 'ct-00000003', NULL, 'u-tenant-03', 'u-landlord-02', '2026-11-01', 3200000, 150000, 0, 100000, 0, 3450000, 1500000, '2026-11-10', 'PARTIALLY_PAID', 2, '2026-11-02 11:00:00.000', '2026-11-05 14:00:00.000')
ON DUPLICATE KEY UPDATE invoice_code=invoice_code;

-- 3. Invoice Items (Strict mathematical equality with invoice total)
INSERT INTO invoice_items (id, invoice_id, item_name, unit, quantity, unit_price, total_price, created_at) VALUES
('ii-0001', 'inv-00000001', 'Tiền thuê phòng tháng 11/2026', 'tháng', 1.00, 4500000, 4500000, '2026-11-01 09:00:00.000'),
('ii-0002', 'inv-00000001', 'Tiền điện (Chỉ số: 1250 -> 1315)', 'kWh', 65.00, 3800, 247000, '2026-11-01 09:00:00.000'),
('ii-0003', 'inv-00000001', 'Tiền nước sinh hoạt (Chỉ số: 45 -> 48)', 'm³', 3.00, 28000, 84000, '2026-11-01 09:00:00.000'),
('ii-0004', 'inv-00000001', 'Phí dịch vụ vệ sinh và rác', 'người/tháng', 1.00, 44000, 44000, '2026-11-01 09:00:00.000'),

('ii-0005', 'inv-00000002', 'Tiền thuê phòng tháng 10/2026', 'tháng', 1.00, 4500000, 4500000, '2026-10-01 09:00:00.000'),
('ii-0006', 'inv-00000002', 'Tiền điện (Chỉ số: 1190 -> 1250)', 'kWh', 60.00, 3800, 228000, '2026-10-01 09:00:00.000'),
('ii-0007', 'inv-00000002', 'Tiền nước sinh hoạt (Chỉ số: 42 -> 45)', 'm³', 3.00, 28000, 84000, '2026-10-01 09:00:00.000'),
('ii-0008', 'inv-00000002', 'Phí phụ trội dịch vụ', 'lần', 1.00, 8000, 8000, '2026-10-01 09:00:00.000'),

('ii-0009', 'inv-00000003', 'Tiền thuê căn hộ dịch vụ tháng 11/2026', 'tháng', 1.00, 7200000, 7200000, '2026-11-01 10:00:00.000'),
('ii-0010', 'inv-00000003', 'Tiền điện (Chỉ số: 2100 -> 2180)', 'kWh', 80.00, 4000, 320000, '2026-11-01 10:00:00.000'),
('ii-0011', 'inv-00000003', 'Phí quản lý tòa nhà và thang máy', 'tháng', 1.00, 130000, 130000, '2026-11-01 10:00:00.000'),

('ii-0012', 'inv-00000004', 'Tiền thuê phòng gác lửng tháng 11/2026', 'tháng', 1.00, 3200000, 3200000, '2026-11-02 11:00:00.000'),
('ii-0013', 'inv-00000004', 'Tiền điện sinh hoạt khoán', 'tháng', 1.00, 150000, 150000, '2026-11-02 11:00:00.000'),
('ii-0014', 'inv-00000004', 'Tiền nước và vệ sinh', 'tháng', 1.00, 100000, 100000, '2026-11-02 11:00:00.000')
ON DUPLICATE KEY UPDATE invoice_id=invoice_id;

-- 4. Payments (Khớp StaticTransactionRepository)
INSERT INTO payments (id, invoice_id, payer_id, amount, method, transaction_ref, status, paid_at, created_at) VALUES
('pay-0001', 'inv-00000001', 'u-tenant-01', 4875000, 'VIETQR', 'TOAM-QR-882204', 'SUCCESS', '2026-11-05 10:15:30.000', '2026-11-05 10:14:00.000'),
('pay-0002', 'inv-00000002', 'u-tenant-01', 4820000, 'VIETQR', 'TOAM-QR-881900', 'SUCCESS', '2026-10-04 16:20:00.000', '2026-10-04 16:15:00.000'),
('pay-0003', 'inv-00000004', 'u-tenant-03', 1500000, 'BANK_TRANSFER', 'TOAM-BT-992014', 'SUCCESS', '2026-11-05 14:00:00.000', '2026-11-05 13:50:00.000')
ON DUPLICATE KEY UPDATE transaction_ref=transaction_ref;

-- 5. Deposits (Khoản đặt cọc)
INSERT INTO deposits (id, contract_id, tenant_id, landlord_id, original_amount, current_balance, status, version, created_at, updated_at) VALUES
('dep-0001', 'ct-00000001', 'u-tenant-01', 'u-landlord-01', 4500000, 4500000, 'HELD', 1, '2026-10-30 15:30:00.000', '2026-10-30 15:30:00.000'),
('dep-0002', 'ct-00000002', 'u-tenant-02', 'u-landlord-01', 7200000, 7200000, 'HELD', 1, '2026-10-31 16:00:00.000', '2026-10-31 16:00:00.000'),
('dep-0003', 'ct-00000003', 'u-tenant-03', 'u-landlord-02', 3200000, 3200000, 'HELD', 1, '2026-11-06 14:00:00.000', '2026-11-06 14:00:00.000'),
('dep-0004', 'ct-00000004', 'u-tenant-06', 'u-landlord-03', 5500000, 0, 'REFUNDED', 2, '2025-10-25 10:00:00.000', '2026-10-31 17:00:00.000')
ON DUPLICATE KEY UPDATE contract_id=contract_id;

-- 6. Deposit Transactions (Khớp TXN-9001 và TXN-9003 trong frontend)
INSERT INTO deposit_transactions (id, deposit_id, type, amount, reason, reference_id, created_at) VALUES
('dt-0001', 'dep-0001', 'HOLD', 4500000, 'Tiền đặt cọc giữ chỗ phòng Studio Quận 1 (khóa phòng 7 ngày)', 'TOAM-QR-882193', '2026-11-01 08:32:15.000'),
('dt-0002', 'dep-0002', 'HOLD', 7200000, 'Tiền đặt cọc hợp đồng căn hộ 1PN Bình Thạnh', 'TOAM-QR-882194', '2026-10-31 16:00:00.000'),
('dt-0003', 'dep-0004', 'REFUND', 5500000, 'Lệnh hoàn trả tiền cọc hợp đồng đã kết thúc đúng hạn', 'TOAM-RF-331002', '2026-10-31 17:00:00.000')
ON DUPLICATE KEY UPDATE deposit_id=deposit_id;

-- 7. Expenses (Chi phí vận hành của chủ trọ)
INSERT INTO expenses (id, landlord_id, room_id, category, amount, expense_date, description, receipt_image_url, created_at) VALUES
('exp-0001', 'u-landlord-01', 'room-00000001-0000-0000-0000-000000000001', 'REPAIR', 250000, '2026-10-20', 'Bảo dưỡng và vệ sinh điều hòa định kỳ trước khi khách vào ở', '/receipts/daikin_service.jpg', '2026-10-20 15:00:00.000'),
('exp-0002', 'u-landlord-01', 'room-00000001-0000-0000-0000-000000000002', 'CLEANING', 400000, '2026-10-28', 'Dịch vụ dọn vệ sinh công nghiệp căn hộ dịch vụ', '/receipts/cleaning_cobac.jpg', '2026-10-28 17:00:00.000')
ON DUPLICATE KEY UPDATE landlord_id=landlord_id;

-- 8. Listing Plans (Gói tin đăng)
INSERT INTO listing_plans (id, plan_code, name, price, duration_days, features_json, is_active, created_at) VALUES
('plan-free', 'PLAN_FREE', 'Gói Tiêu Chuẩn', 0, 30, '{"max_photos": 5, "priority_support": false, "top_search": false}', 1, CURRENT_TIMESTAMP(3)),
('plan-vip1', 'PLAN_VIP1', 'Gói Nổi Bật', 150000, 30, '{"max_photos": 15, "priority_support": true, "top_search": true, "badge": "NỔI BẬT"}', 1, CURRENT_TIMESTAMP(3)),
('plan-vip2', 'PLAN_VIP2', 'Gói Kim Cương', 350000, 30, '{"max_photos": 30, "priority_support": true, "top_search": true, "banner_homepage": true, "badge": "VIP PRO"}', 1, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE name=name;

-- 9. Promo Codes (Mã khuyến mại)
INSERT INTO promo_codes (id, code, discount_type, discount_value, max_discount, min_order_value, usage_limit, times_used, start_date, end_date, is_active, created_at) VALUES
('promo-001', 'TOAM2026', 'PERCENTAGE', 10, 50000, 200000, 500, 35, '2026-01-01 00:00:00.000', '2026-12-31 23:59:59.000', 1, CURRENT_TIMESTAMP(3)),
('promo-002', 'TANTHU50K', 'FIXED_AMOUNT', 50000, 50000, 150000, 200, 80, '2026-10-01 00:00:00.000', '2026-11-30 23:59:59.000', 1, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE code=code;
