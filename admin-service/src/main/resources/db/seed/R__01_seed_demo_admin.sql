-- ============================================================
-- V2__seed_demo_admin.sql
-- Seed Demo Data for Admin Service
-- Consistent with seed-ids.md, frontend UI repositories, and 90-day statistics
-- ============================================================

-- 1. Reports (Báo cáo vi phạm)
INSERT INTO reports (id, reporter_id, target_type, target_id, reason, description, evidence_urls_json, status, assigned_to, resolution_notes, resolved_at, created_at, updated_at) VALUES
('rpt-0001', 'u-tenant-01', 'ROOM', 'room-00000001-0000-0000-0000-000000000027', 'Thông tin phòng sai sự thật', 'Phòng đăng giá 500k/tháng nhưng khi gọi thì đòi cọc trước 2 triệu rồi chặn số.', '["/evidence/report_fake_room.jpg"]', 'RESOLVED', 'u-mod-01', 'Đã xác minh lừa đảo và gắn cờ REJECTED phòng trọ, khóa tài khoản vi phạm.', '2026-11-03 14:00:00.000', '2026-11-03 09:15:00.000', '2026-11-03 14:00:00.000'),
('rpt-0002', 'u-tenant-02', 'USER', 'u-landlord-06', 'Chủ trọ có hành vi đe dọa người thuê', 'Chủ nhà tự ý ngắt cầu dao điện và đe dọa đuổi người thuê trước hạn hợp đồng.', NULL, 'INVESTIGATING', 'u-mod-02', 'Đang liên hệ 2 bên để đối soát vi phạm hợp đồng.', NULL, '2026-11-05 16:30:00.000', '2026-11-06 09:00:00.000')
ON DUPLICATE KEY UPDATE reason=reason;

-- 2. Moderation Actions (Nhật ký kiểm duyệt)
INSERT INTO moderation_actions (id, moderator_id, target_type, target_id, action_type, reason, previous_status, new_status, created_at) VALUES
('ma-0001', 'u-mod-01', 'ROOM', 'room-00000001-0000-0000-0000-000000000001', 'APPROVE', 'Hồ sơ phòng trọ đầy đủ ảnh thực tế và giấy tờ chủ quyền hợp lệ', 'PENDING_REVIEW', 'PUBLISHED', '2026-10-20 10:00:00.000'),
('ma-0002', 'u-mod-01', 'ROOM', 'room-00000001-0000-0000-0000-000000000027', 'REJECT', 'Giá cả bất thường và địa chỉ không tồn tại', 'PENDING_REVIEW', 'REJECTED', '2026-11-03 14:00:00.000'),
('ma-0003', 'u-mod-01', 'KYC', 'uv-001', 'APPROVE', 'Ảnh CCCD người thuê rõ nét, số định danh khớp thông tin khai báo', 'PENDING', 'APPROVED', '2026-10-18 11:30:00.000')
ON DUPLICATE KEY UPDATE moderator_id=moderator_id;

-- 3. Audit Logs (Nhật ký kiểm toán an ninh)
INSERT INTO audit_logs (id, actor_id, actor_role, action, entity_type, entity_id, ip_address, user_agent, payload_diff_json, created_at) VALUES
('al-0001', 'u-admin-01', 'ADMIN', 'SYSTEM_CONFIG_UPDATE', 'SYSTEM_SETTING', 'cfg-001', '14.161.20.12', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', '{"platform_fee": "0.10"}', '2026-10-01 08:00:00.000'),
('al-0002', 'u-mod-01', 'MODERATOR', 'MODERATE_ROOM_REJECT', 'ROOM', 'room-00000001-0000-0000-0000-000000000027', '118.70.120.45', 'Mozilla/5.0', '{"status": "REJECTED"}', '2026-11-03 14:00:00.000')
ON DUPLICATE KEY UPDATE actor_id=actor_id;

-- 4. System Settings (Cấu hình hệ thống)
INSERT INTO system_settings (id, setting_key, setting_value, description, updated_by, updated_at) VALUES
('cfg-001', 'PLATFORM_FEE_PERCENT', '10', 'Phần trăm phí nền tảng Tổ Ấm trên mỗi giao dịch thành công (%)', 'u-admin-01', CURRENT_TIMESTAMP(3)),
('cfg-002', 'APPOINTMENT_EXPIRE_HOURS', '48', 'Thời gian tự động hủy lịch hẹn xem phòng nếu chủ trọ không phản hồi (giờ)', 'u-admin-01', CURRENT_TIMESTAMP(3)),
('cfg-003', 'CONTRACT_OTP_EXPIRE_MINUTES', '15', 'Thời hạn hiệu lực của mã OTP ký hợp đồng điện tử (phút)', 'u-admin-01', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE setting_value=setting_value;

-- 5. Daily Statistics (Số liệu thống kê 90 ngày gần nhất - mẫu các mốc đại diện)
INSERT INTO daily_statistics (id, stat_date, total_users, new_users, total_rooms, active_rooms, pending_rooms, total_contracts, active_contracts, total_revenue, platform_fee_revenue, created_at) VALUES
('ds-2026-11-06', '2026-11-06', 1420, 18, 680, 520, 12, 340, 295, 285000000, 28500000, '2026-11-06 23:59:59.000'),
('ds-2026-11-05', '2026-11-05', 1402, 22, 675, 515, 15, 336, 292, 278000000, 27800000, '2026-11-05 23:59:59.000'),
('ds-2026-11-04', '2026-11-04', 1380, 15, 668, 510, 14, 330, 288, 271000000, 27100000, '2026-11-04 23:59:59.000'),
('ds-2026-11-01', '2026-11-01', 1340, 25, 650, 498, 18, 315, 280, 255000000, 25500000, '2026-11-01 23:59:59.000'),
('ds-2026-10-15', '2026-10-15', 1210, 19, 610, 470, 20, 285, 250, 210000000, 21000000, '2026-10-15 23:59:59.000'),
('ds-2026-10-01', '2026-10-01', 1080, 28, 560, 430, 22, 240, 210, 185000000, 18500000, '2026-10-01 23:59:59.000')
ON DUPLICATE KEY UPDATE stat_date=stat_date;

-- 6. Disputes (Khớp StaticDisputeRepository)
INSERT INTO disputes (id, dispute_code, contract_id, room_title, complainant_id, complainant_name, complainant_role, defendant_id, defendant_name, defendant_role, dispute_type, amount, reason, evidence_urls_json, status, mediator_id, verdict, resolved_at, created_at, updated_at) VALUES
('disp-00000001', 'DISP-501', 'ct-00000001', 'Phòng Studio Ban Công Đẹp Quận 1', 'u-tenant-01', 'Nguyễn Văn Thuê', 'TENANT', 'u-landlord-01', 'Trần Thị Chủ Trọ', 'LANDLORD', 'DEPOSIT_RETURN', 4500000, 'Người thuê kết thúc hợp đồng đúng hạn và bàn giao chìa khóa nhưng chủ trọ chậm hoàn cọc quá 15 ngày làm việc theo cam kết hợp đồng.', '["/rooms/room-1.jpg"]', 'MEDIATING', 'u-mod-01', NULL, NULL, '2026-11-04 14:00:00.000', '2026-11-05 09:00:00.000'),
('disp-00000002', 'DISP-502', 'ct-00000002', 'Căn Hộ Dịch Vụ 1PN Bình Thạnh', 'u-landlord-01', 'Trần Thị Chủ Trọ', 'LANDLORD', 'u-tenant-02', 'Lê Hoàng Minh', 'TENANT', 'DAMAGE_COMPENSATION', 1200000, 'Khách thuê tự ý khoan tường làm vỡ gạch ốp tường phòng tắm và làm hỏng mặt kính bếp từ.', '["/rooms/room-2.jpg"]', 'PENDING', NULL, NULL, NULL, '2026-11-05 09:15:00.000', '2026-11-05 09:15:00.000')
ON DUPLICATE KEY UPDATE dispute_code=dispute_code;

-- 7. Dispute Events
INSERT INTO dispute_events (id, dispute_id, actor_id, event_type, message, created_at) VALUES
('de-0001', 'disp-00000001', 'u-tenant-01', 'COMMENT', 'Tôi đã gửi kèm biên bản nghiệm thu phòng có chữ ký xác nhận của bảo vệ tòa nhà.', '2026-11-04 14:10:00.000'),
('de-0002', 'disp-00000001', 'u-mod-01', 'STATUS_CHANGED', 'Ban hòa giải Tổ Ấm đã tiếp nhận hồ sơ và lên lịch làm việc trực tuyến với hai bên vào ngày 08/11/2026.', '2026-11-05 09:00:00.000')
ON DUPLICATE KEY UPDATE dispute_id=dispute_id;

-- 8. Content Posts (Cẩm nang, FAQ, Banner)
INSERT INTO content_posts (id, slug, title, summary, content, category, status, author_id, view_count, published_at, created_at, updated_at) VALUES
('cp-0001', 'cam-nang-thue-tro-an-toan-2026', 'Cẩm nang thuê phòng trọ an toàn, tránh bẫy lừa đảo cho sinh viên năm 2026', 'Những lưu ý cốt lõi khi đi xem phòng, ký hợp đồng và bảo vệ tiền cọc trên các nền tảng số.', '## 1. Không chuyển cọc khi chưa xem phòng thực tế\nTuyệt đối không chuyển tiền cọc giữ chỗ khi chỉ mới xem ảnh qua mạng...\n\n## 2. Ký hợp đồng điện tử có giá trị pháp lý\nTại Tổ Ấm, hợp đồng được bảo chứng số và có xác thực OTP qua số điện thoại chính chủ...', 'BLOG', 'PUBLISHED', 'u-admin-01', 3450, '2026-10-15 08:00:00.000', '2026-10-15 08:00:00.000', '2026-10-15 08:00:00.000'),
('cp-0002', 'quy-trinh-ky-hop-dong-dien-tu', 'Quy trình ký hợp đồng điện tử và chính sách hoàn tiền cọc', 'Giải đáp chi tiết về quy trình pháp lý và thời hạn giải ngân tiền cọc an toàn trên Tổ Ấm.', 'Mọi hợp đồng trên nền tảng Tổ Ấm đều tuân thủ Luật Giao dịch điện tử 2023...', 'FAQ', 'PUBLISHED', 'u-admin-01', 1820, '2026-10-20 09:00:00.000', '2026-10-20 09:00:00.000', '2026-10-20 09:00:00.000')
ON DUPLICATE KEY UPDATE slug=slug;

-- 9. Email Templates (Mẫu email đa ngôn ngữ - Dùng cho AUTH-04 OTP)
INSERT INTO email_templates (id, template_code, language, subject, body_html, body_text, variables_json, updated_at) VALUES
('et-otp-vi', 'AUTH_PASSWORD_RESET_OTP', 'vi', 'Mã xác thực OTP đặt lại mật khẩu - Tổ Ấm', '<div style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;">\n  <h2 style="color:#0f766e;margin-top:0;">Tổ Ấm - Nền Tảng Thuê Trọ An Toàn</h2>\n  <p>Xin chào <strong>{{user_name}}</strong>,</p>\n  <p>Bạn vừa yêu cầu khôi phục mật khẩu tài khoản Tổ Ấm. Dưới đây là mã xác thực OTP của bạn:</p>\n  <div style="font-size:32px;font-weight:bold;letter-spacing:6px;text-align:center;padding:18px;margin:24px 0;background:#f0fdfa;color:#0f766e;border-radius:8px;border:1px dashed #0f766e;">{{otp_code}}</div>\n  <p style="color:#64748b;font-size:14px;">Mã này có hiệu lực trong <strong>15 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>\n  <p style="color:#64748b;font-size:14px;">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email và kiểm tra lại bảo mật tài khoản.</p>\n  <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;" />\n  <p style="color:#94a3b8;font-size:12px;text-align:center;">© 2026 Nền tảng Thuê Trọ Tổ Ấm. Hotline hỗ trợ: 1900 6868.</p>\n</div>', 'Xin chào {{user_name}}, Mã OTP đặt lại mật khẩu của bạn là: {{otp_code}}. Mã có hiệu lực trong 15 phút. Nếu không phải bạn thực hiện, vui lòng bỏ qua.', '["user_name", "otp_code"]', CURRENT_TIMESTAMP(3)),

('et-otp-en', 'AUTH_PASSWORD_RESET_OTP', 'en', 'Password Reset Verification Code - To Am', '<div style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;">\n  <h2 style="color:#0f766e;margin-top:0;">To Am - Safe Rental Platform</h2>\n  <p>Hello <strong>{{user_name}}</strong>,</p>\n  <p>You requested a password reset for your To Am account. Your verification code is:</p>\n  <div style="font-size:32px;font-weight:bold;letter-spacing:6px;text-align:center;padding:18px;margin:24px 0;background:#f0fdfa;color:#0f766e;border-radius:8px;border:1px dashed #0f766e;">{{otp_code}}</div>\n  <p style="color:#64748b;font-size:14px;">This code expires in <strong>15 minutes</strong>. Never share this code with anyone.</p>\n  <p style="color:#64748b;font-size:14px;">If you did not make this request, please disregard this email.</p>\n  <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;" />\n  <p style="color:#94a3b8;font-size:12px;text-align:center;">© 2026 To Am Rental Platform.</p>\n</div>', 'Hello {{user_name}}, Your password reset OTP is: {{otp_code}}. It expires in 15 minutes. If not you, please ignore.', '["user_name", "otp_code"]', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE subject=subject;
