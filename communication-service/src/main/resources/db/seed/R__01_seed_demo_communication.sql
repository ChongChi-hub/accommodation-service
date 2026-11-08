-- ============================================================
-- V2__seed_demo_communication.sql
-- Seed Demo Data for Communication Service
-- Consistent with seed-ids.md and frontend UI repositories
-- ============================================================

-- 1. Notifications (Khớp Header Bell Dropdown & StaticNotificationRepository)
INSERT INTO notifications (id, user_id, type, title, content, is_read, link_url, read_at, created_at) VALUES
('notif-001', 'u-tenant-01', 'CONTRACT', 'Hợp đồng thuê phòng đã sẵn sàng ký', 'Chủ trọ Trần Thị Chủ Trọ đã gửi hợp đồng số HD-2026-088. Vui lòng kiểm tra và ký điện tử trước ngày 01/11.', 1, '/dashboard/tenant/contracts/ct-00000001', '2026-10-30 15:30:00.000', '2026-10-30 14:00:00.000'),
('notif-002', 'u-tenant-01', 'INVOICE', 'Hóa đơn tiền phòng tháng 11/2026 đã được tạo', 'Hóa đơn INV-2026-11-01 trị giá 4.875.000 đ đã đến hạn thanh toán vào ngày 05/11/2026.', 1, '/dashboard/tenant/invoices/inv-00000001', '2026-11-05 10:14:00.000', '2026-11-01 09:00:00.000'),
('notif-003', 'u-tenant-01', 'APPOINTMENT', 'Chủ trọ đã xác nhận lịch hẹn xem phòng', 'Lịch hẹn xem phòng Studio Cầu Giấy vào lúc 09:30 ngày 25/10 đã được xác nhận thành công.', 1, '/dashboard/tenant/appointments', '2026-10-24 10:00:00.000', '2026-10-23 09:00:00.000'),
('notif-004', 'u-tenant-01', 'MAINTENANCE', 'Yêu cầu sửa chữa máy lạnh đã được tiếp nhận', 'Chủ trọ đã cập nhật trạng thái yêu cầu MN-201: Đã hẹn thợ chiều mai 14h qua xử lý.', 0, '/dashboard/tenant/maintenance', NULL, '2026-11-06 14:00:00.000'),
('notif-005', 'u-landlord-01', 'RENTAL_REQUEST', 'Có yêu cầu thuê phòng mới', 'Người thuê Nguyễn Văn Thuê đã gửi yêu cầu thuê phòng Studio Cầu Giấy từ ngày 01/11/2026.', 1, '/dashboard/landlord/rental-requests', '2026-10-27 10:30:00.000', '2026-10-27 10:00:00.000'),
('notif-006', 'u-landlord-01', 'PAYMENT', 'Nhận tiền thanh toán hóa đơn thành công', 'Người thuê Nguyễn Văn Thuê đã thanh toán 4.875.000 đ qua VietQR cho hóa đơn INV-2026-11-01.', 0, '/dashboard/landlord/invoices', NULL, '2026-11-05 10:15:30.000')
ON DUPLICATE KEY UPDATE title=title;

-- 2. Notification Preferences
INSERT INTO notification_preferences (id, user_id, email_enabled, push_enabled, sms_enabled, categories_json, updated_at) VALUES
('np-001', 'u-tenant-01', 1, 1, 1, '{"contract": true, "invoice": true, "chat": true, "maintenance": true}', CURRENT_TIMESTAMP(3)),
('np-002', 'u-landlord-01', 1, 1, 0, '{"contract": true, "invoice": true, "chat": true, "maintenance": true}', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 3. Conversations & Participants
INSERT INTO conversations (id, room_id, last_message_content, last_message_time, created_at, updated_at) VALUES
('conv-0001', 'room-00000001-0000-0000-0000-000000000001', 'Dạ em cảm ơn chị, em đã nhận được chìa khóa phòng rồi ạ!', '2026-11-01 10:00:00.000', '2026-10-23 08:15:00.000', '2026-11-01 10:00:00.000')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO conversation_participants (id, conversation_id, user_id, role, last_read_at, joined_at) VALUES
('cp-0001', 'conv-0001', 'u-tenant-01', 'TENANT', '2026-11-01 10:05:00.000', '2026-10-23 08:15:00.000'),
('cp-0002', 'conv-0001', 'u-landlord-01', 'LANDLORD', '2026-11-01 10:10:00.000', '2026-10-23 08:15:00.000')
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 4. Messages
INSERT INTO messages (id, conversation_id, sender_id, content, type, is_read, created_at) VALUES
('msg-0001', 'conv-0001', 'u-tenant-01', 'Chào chị, phòng studio bên mình có chỗ để xe máy miễn phí không ạ?', 'TEXT', 1, '2026-10-23 08:20:00.000'),
('msg-0002', 'conv-0001', 'u-landlord-01', 'Chào em, tòa nhà có tầng hầm để xe miễn phí 1 xe/phòng, có camera an ninh và khóa vân tay em nhé.', 'TEXT', 1, '2026-10-23 08:25:00.000'),
('msg-0003', 'conv-0001', 'u-tenant-01', 'Dạ em cảm ơn chị, em đã nhận được chìa khóa phòng rồi ạ!', 'TEXT', 1, '2026-11-01 10:00:00.000')
ON DUPLICATE KEY UPDATE content=content;

-- 5. Support Tickets (Khớp StaticSupportRepository)
INSERT INTO support_tickets (id, ticket_code, user_id, user_name, user_email, user_phone, subject, category, priority, status, description, assigned_to, assigned_agent_name, version, created_at, updated_at, resolved_at) VALUES
('ticket-1001', 'TK-1001', 'u-tenant-01', 'Nguyễn Văn Thuê', 'tenant@cozynest.vn', '0901000001', 'Lỗi quét mã VietQR thanh toán tiền phòng tháng 11', 'PAYMENT', 'HIGH', 'OPEN', 'Khi bấm quét mã VietQR ngân hàng báo không tìm thấy thông tin số tài khoản của chủ trọ, xin hỗ trợ kiểm tra.', NULL, NULL, 1, '2026-11-06 10:15:00.000', '2026-11-06 10:15:00.000', NULL),
('ticket-1002', 'TK-1002', 'u-landlord-01', 'Trần Thị Chủ Trọ', 'landlord@cozynest.vn', '0902000002', 'Yêu cầu hỗ trợ hướng dẫn ký hợp đồng điện tử bằng OTP', 'CONTRACT', 'MEDIUM', 'IN_PROGRESS', 'Tôi muốn biết sau khi người thuê ký thì hợp đồng có giá trị pháp lý ra sao và tải file PDF ở đâu?', 'u-admin-01', 'Lê Hoàng Quản Trị', 2, '2026-11-05 14:30:00.000', '2026-11-05 15:00:00.000', NULL),
('ticket-1003', 'TK-1003', 'u-tenant-02', 'Lê Hoàng Minh', 'tenant2@cozynest.vn', '0901000002', 'Khiếu nại chủ trọ không trả lại 100% tiền cọc', 'VIOLATION', 'URGENT', 'RESOLVED', 'Tôi đã bàn giao phòng sạch sẽ đúng hạn nhưng chủ trọ trừ 500k tiền vệ sinh vô lý.', 'u-admin-01', 'Lê Hoàng Quản Trị', 2, '2026-11-01 08:00:00.000', '2026-11-02 11:20:00.000', '2026-11-02 11:20:00.000')
ON DUPLICATE KEY UPDATE ticket_code=ticket_code;

-- 6. Support Ticket Replies (Khớp messages trong StaticSupportRepository)
INSERT INTO support_ticket_replies (id, ticket_id, sender_id, sender_name, sender_role, message, created_at) VALUES
('str-0001', 'ticket-1001', 'u-tenant-01', 'Nguyễn Văn Thuê', 'USER', 'Tôi đã thử quét qua app Vietcombank và MBBank nhưng đều báo lỗi chuỗi thanh toán.', '2026-11-06 10:15:00.000'),
('str-0002', 'ticket-1002', 'u-landlord-01', 'Trần Thị Chủ Trọ', 'USER', 'Hợp đồng điện tử ký trên sàn Tổ Ấm có tích hợp chứng thư số không?', '2026-11-05 14:30:00.000'),
('str-0003', 'ticket-1002', 'u-admin-01', 'Lê Hoàng Quản Trị', 'ADMIN', 'Chào chị Trần, hợp đồng điện tử trên Tổ Ấm được xác thực qua OTP số điện thoại và có đóng dấu mộc số timestamp hợp pháp theo Luật Giao dịch điện tử 2023.', '2026-11-05 15:00:00.000'),
('str-0004', 'ticket-1003', 'u-tenant-02', 'Lê Hoàng Minh', 'USER', 'Kèm theo video quay lại hiện trạng phòng lúc bàn giao.', '2026-11-01 08:00:00.000'),
('str-0005', 'ticket-1003', 'u-admin-01', 'Lê Hoàng Quản Trị', 'ADMIN', 'Tổ Ấm đã làm việc với chủ trọ và hoàn trả lại 500.000 đ vào ví của bạn thành công.', '2026-11-02 11:20:00.000')
ON DUPLICATE KEY UPDATE ticket_id=ticket_id;
