-- ============================================================
-- V2__seed_demo_booking.sql
-- Seed Demo Data for Booking Service
-- Fully consistent with seed-ids.md and frontend UI repositories
-- ============================================================

-- 1. Contract Templates
INSERT INTO contract_templates (id, template_code, name, description, content_template, version, is_active, created_at, updated_at) VALUES
('tmpl-001', 'TOAM_STD_2026', 'Mẫu hợp đồng thuê phòng chuẩn Tổ Ấm 2026', 'Mẫu hợp đồng tiêu chuẩn có điều khoản bảo vệ cả người thuê và chủ nhà theo Bộ Luật Dân sự 2015', 'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG THUÊ PHÒNG TRỌ VÀ CĂN HỘ\nSố: {{contract_code}}\n\nBên cho thuê (Bên A): {{landlord_name}}\nBên thuê (Bên B): {{tenant_name}}\nĐiều 1: Đối tượng hợp đồng: {{room_title}} tại địa chỉ {{room_address}}\nĐiều 2: Giá thuê {{monthly_rent}} VNĐ/tháng, tiền đặt cọc {{deposit_amount}} VNĐ.\nĐiều 3: Thanh toán vào ngày {{payment_due_day}} hàng tháng.', 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE name=name;

-- 2. Viewing Appointments (Lịch hẹn xem phòng)
INSERT INTO viewing_appointments (id, room_id, tenant_id, landlord_id, appointment_date, appointment_time, message, status, created_at, updated_at) VALUES
('app-00000001', 'room-00000001-0000-0000-0000-000000000001', 'u-tenant-01', 'u-landlord-01', '2026-10-25', '09:30:00', 'Em chào chị chủ, em muốn qua xem trực tiếp phòng studio vào sáng thứ 7 này ạ.', 'COMPLETED', '2026-10-23 08:15:00.000', '2026-10-25 10:30:00.000'),
('app-00000002', 'room-00000001-0000-0000-0000-000000000002', 'u-tenant-02', 'u-landlord-01', '2026-10-26', '14:00:00', 'Xin phép hẹn xem căn hộ 1PN lúc 14h chiều chủ nhật.', 'COMPLETED', '2026-10-24 11:20:00.000', '2026-10-26 15:00:00.000'),
('app-00000003', 'room-00000001-0000-0000-0000-000000000003', 'u-tenant-03', 'u-landlord-02', '2026-11-08', '16:00:00', 'Chào anh, em muốn xem gác lửng và thử áp lực nước.', 'CONFIRMED', '2026-11-06 09:00:00.000', '2026-11-06 10:00:00.000'),
('app-00000004', 'room-00000001-0000-0000-0000-000000000004', 'u-tenant-04', 'u-landlord-02', '2026-11-09', '18:30:00', 'Hẹn xem phòng sau giờ tan làm.', 'PENDING', '2026-11-07 08:30:00.000', '2026-11-07 08:30:00.000')
ON DUPLICATE KEY UPDATE message=message;

-- 3. Rental Requests (Yêu cầu thuê phòng)
INSERT INTO rental_requests (id, room_id, tenant_id, landlord_id, proposed_move_in_date, num_occupants, message, status, contract_id, created_at, updated_at) VALUES
('req-00000001', 'room-00000001-0000-0000-0000-000000000001', 'u-tenant-01', 'u-landlord-01', '2026-11-01', 1, 'Em ưng phòng rồi, xin phép gửi yêu cầu thuê chính thức từ ngày 01/11/2026.', 'APPROVED', 'ct-00000001', '2026-10-27 10:00:00.000', '2026-10-28 09:00:00.000'),
('req-00000002', 'room-00000001-0000-0000-0000-000000000002', 'u-tenant-02', 'u-landlord-01', '2026-11-01', 2, 'Tôi muốn thuê 1 năm, ở 2 người bạn làm cùng công ty.', 'APPROVED', 'ct-00000002', '2026-10-28 14:00:00.000', '2026-10-29 11:00:00.000'),
('req-00000003', 'room-00000001-0000-0000-0000-000000000003', 'u-tenant-03', 'u-landlord-02', '2026-11-15', 1, 'Dự kiến dọn vào giữa tháng này.', 'APPROVED', 'ct-00000003', '2026-11-05 16:00:00.000', '2026-11-06 08:00:00.000')
ON DUPLICATE KEY UPDATE message=message;

-- 4. Contracts (Hợp đồng thuê điện tử)
INSERT INTO contracts (id, contract_code, room_id, tenant_id, landlord_id, request_id, template_id, start_date, end_date, monthly_rent, deposit_amount, payment_cycle_months, payment_due_day, terms, status, version, created_at, updated_at) VALUES
('ct-00000001', 'HD-2026-088', 'room-00000001-0000-0000-0000-000000000001', 'u-tenant-01', 'u-landlord-01', 'req-00000001', 'tmpl-001', '2026-11-01', '2027-10-31', 4500000, 4500000, 1, 5, 'Hợp đồng thuê phòng studio 12 tháng, giữ gìn vệ sinh chung, không hút thuốc trong phòng.', 'ACTIVE', 1, '2026-10-29 09:00:00.000', '2026-10-30 15:30:00.000'),
('ct-00000002', 'HD-2026-052', 'room-00000001-0000-0000-0000-000000000002', 'u-tenant-02', 'u-landlord-01', 'req-00000002', 'tmpl-001', '2026-11-01', '2027-10-31', 7200000, 7200000, 1, 5, 'Hợp đồng thuê căn hộ dịch vụ 1PN, miễn phí dọn phòng 2 lần/tuần.', 'ACTIVE', 1, '2026-10-30 11:00:00.000', '2026-10-31 16:00:00.000'),
('ct-00000003', 'HD-2026-015', 'room-00000001-0000-0000-0000-000000000003', 'u-tenant-03', 'u-landlord-02', 'req-00000003', 'tmpl-001', '2026-11-15', '2027-05-14', 3200000, 3200000, 1, 5, 'Hợp đồng thuê phòng gác lửng 6 tháng.', 'PENDING_SIGN', 1, '2026-11-06 14:00:00.000', '2026-11-06 14:00:00.000'),
('ct-00000004', 'HD-2025-099', 'room-00000001-0000-0000-0000-000000000006', 'u-tenant-06', 'u-landlord-03', NULL, 'tmpl-001', '2025-11-01', '2026-10-31', 5500000, 5500000, 1, 5, 'Hợp đồng đã kết thúc đúng thời hạn và thanh lý bàn giao xong.', 'TERMINATED', 2, '2025-10-25 10:00:00.000', '2026-10-31 17:00:00.000')
ON DUPLICATE KEY UPDATE contract_code=contract_code;

-- 5. Contract Signatures
INSERT INTO contract_signatures (id, contract_id, signer_id, signer_role, signature_type, signature_hash, ip_address, signed_at) VALUES
('cs-00000001', 'ct-00000001', 'u-landlord-01', 'LANDLORD', 'OTP', 'sig_hash_landlord_882190', '14.161.20.12', '2026-10-30 14:00:00.000'),
('cs-00000002', 'ct-00000001', 'u-tenant-01', 'TENANT', 'OTP', 'sig_hash_tenant_882191', '118.70.120.45', '2026-10-30 15:30:00.000'),
('cs-00000003', 'ct-00000002', 'u-landlord-01', 'LANDLORD', 'OTP', 'sig_hash_landlord_882192', '14.161.20.12', '2026-10-31 10:00:00.000'),
('cs-00000004', 'ct-00000002', 'u-tenant-02', 'TENANT', 'OTP', 'sig_hash_tenant_882193', '115.79.80.14', '2026-10-31 16:00:00.000'),
('cs-00000005', 'ct-00000003', 'u-landlord-02', 'LANDLORD', 'OTP', 'sig_hash_landlord_882194', '27.72.100.8', '2026-11-06 14:30:00.000')
ON DUPLICATE KEY UPDATE contract_id=contract_id;

-- 6. Room Snapshots
INSERT INTO room_snapshots (id, contract_id, room_id, title, address, area, price, deposit, amenities_json, images_json, snapshot_time) VALUES
('snap-00000001', 'ct-00000001', 'room-00000001-0000-0000-0000-000000000001', 'Phòng studio full đồ ban công thoáng Cầu Giấy', 'Số 18 Ngõ 86 Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 28.0, 4500000, 4500000, '["WiFi", "Điều hòa", "Nóng lạnh", "Máy giặt"]', '["/rooms/room-1.jpg"]', '2026-10-30 15:30:00.000'),
('snap-00000002', 'ct-00000002', 'room-00000001-0000-0000-0000-000000000002', 'Căn hộ dịch vụ 1N1K trung tâm Quận 1 cao cấp', '142 Cô Bắc, Phường Cô Giang, Quận 1, TP. Hồ Chí Minh', 40.0, 7200000, 7200000, '["WiFi", "Điều hòa", "Tủ lạnh", "Bếp", "Thang máy"]', '["/rooms/room-2.jpg"]', '2026-10-31 16:00:00.000')
ON DUPLICATE KEY UPDATE contract_id=contract_id;

-- 7. Contract Terminations
INSERT INTO contract_terminations (id, contract_id, requested_by, reason, termination_date, refund_deposit_amount, deduction_amount, status, created_at, updated_at) VALUES
('ctm-00000001', 'ct-00000004', 'u-tenant-06', 'Hết hạn hợp đồng thuê 1 năm, người thuê chuyển công tác về Đà Nẵng', '2026-10-31', 5500000, 0, 'COMPLETED', '2026-10-20 09:00:00.000', '2026-10-31 17:00:00.000')
ON DUPLICATE KEY UPDATE contract_id=contract_id;

-- 8. Maintenance Requests (Khớp frontend StaticMaintenanceRepository)
INSERT INTO maintenance_requests (id, request_code, room_id, contract_id, tenant_id, landlord_id, title, description, category, priority, status, images_json, estimated_cost, actual_cost, scheduled_at, resolved_at, created_at, updated_at) VALUES
('maint-00000001', 'MN-201', 'room-00000001-0000-0000-0000-000000000001', 'ct-00000001', 'u-tenant-01', 'u-landlord-01', 'Máy lạnh Daikin bị chảy nước ở dàn lạnh', 'Máy lạnh bật được 30 phút thì bắt đầu nhỏ giọt xuống sàn gỗ, xin chủ trọ cho thợ qua vệ sinh và kiểm tra đường ống thoát nước.', 'APPLIANCE', 'HIGH', 'PENDING', '["/rooms/room-1.jpg"]', 250000, NULL, NULL, NULL, '2026-11-06 11:00:00.000', '2026-11-06 11:00:00.000'),
('maint-00000002', 'MN-202', 'room-00000001-0000-0000-0000-000000000002', 'ct-00000002', 'u-tenant-02', 'u-landlord-01', 'Áp lực nước vòi sen trong phòng tắm quá yếu', 'Nước chảy rất nhỏ vào buổi tối lúc 19h - 21h, cần kiểm tra máy bơm tăng áp của tòa nhà.', 'WATER', 'MEDIUM', 'IN_PROGRESS', '["/rooms/room-2.jpg"]', NULL, NULL, '2026-11-07 14:00:00.000', NULL, '2026-11-04 09:30:00.000', '2026-11-05 08:00:00.000'),
('maint-00000003', 'MN-203', 'room-00000001-0000-0000-0000-000000000003', 'ct-00000003', 'u-tenant-01', 'u-landlord-02', 'Bóng đèn tuýp LED trên gác lửng bị chập chờn', 'Đèn bật lúc sáng lúc tắt, đã được chủ nhà thay bóng mới chiều hôm qua.', 'ELECTRICITY', 'LOW', 'RESOLVED', '["/rooms/room-3.jpg"]', 120000, 120000, '2026-10-29 14:00:00.000', '2026-10-29 17:30:00.000', '2026-10-28 16:00:00.000', '2026-10-29 17:30:00.000')
ON DUPLICATE KEY UPDATE request_code=request_code;

-- 9. Maintenance Comments
INSERT INTO maintenance_comments (id, request_id, author_id, author_role, content, images_json, created_at) VALUES
('mc-00000001', 'maint-00000001', 'u-landlord-01', 'LANDLORD', 'Chị đã liên hệ thợ điện lạnh, hẹn chiều mai 14h thợ qua kiểm tra em nhé.', NULL, '2026-11-06 14:00:00.000'),
('mc-00000002', 'maint-00000002', 'u-landlord-01', 'LANDLORD', 'Đã cử kỹ thuật tòa nhà lên kiểm tra van bơm tăng áp tầng 3.', NULL, '2026-11-05 08:30:00.000')
ON DUPLICATE KEY UPDATE request_id=request_id;
