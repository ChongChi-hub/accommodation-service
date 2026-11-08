-- ============================================================
-- V1__init_communication_schema.sql
-- Communication Service Schema (cozynest_communication)
-- Bounded Context: Notifications, Realtime Chat, Support Tickets
-- ============================================================

-- 1. Notifications (Thông báo in-app)
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    link_url VARCHAR(500) NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_read (user_id, is_read),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Notification Preferences (Cấu hình nhận thông báo)
CREATE TABLE IF NOT EXISTS notification_preferences (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    email_enabled TINYINT(1) NOT NULL DEFAULT 1,
    push_enabled TINYINT(1) NOT NULL DEFAULT 1,
    sms_enabled TINYINT(1) NOT NULL DEFAULT 0,
    categories_json JSON NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Conversations (Cuộc trò chuyện giữa Tenant và Landlord)
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NULL,
    last_message_content TEXT NULL,
    last_message_time DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_conv_room (room_id),
    INDEX idx_conv_last_msg (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Conversation Participants (Thành viên cuộc trò chuyện)
CREATE TABLE IF NOT EXISTS conversation_participants (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(30) NOT NULL,
    last_read_at DATETIME(3) NULL,
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_cp_conv_user (conversation_id, user_id),
    INDEX idx_cp_user (user_id),
    CONSTRAINT fk_cp_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Messages (Tin nhắn trò chuyện)
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_msg_conv (conversation_id),
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_created (created_at),
    CONSTRAINT chk_msg_type CHECK (type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM_NOTICE')),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Message Attachments (Tệp đính kèm tin nhắn)
CREATE TABLE IF NOT EXISTS message_attachments (
    id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ma_message (message_id),
    CONSTRAINT fk_ma_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Support Tickets (Phiếu yêu cầu trợ giúp khách hàng)
CREATE TABLE IF NOT EXISTS support_tickets (
    id VARCHAR(36) NOT NULL,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    user_name VARCHAR(150) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_phone VARCHAR(20) NULL,
    subject VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description TEXT NOT NULL,
    assigned_to VARCHAR(36) NULL,
    assigned_agent_name VARCHAR(150) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    resolved_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_st_user (user_id),
    INDEX idx_st_status (status),
    INDEX idx_st_created (created_at),
    CONSTRAINT chk_st_category CHECK (category IN ('PAYMENT', 'CONTRACT', 'ACCOUNT', 'VIOLATION', 'TECHNICAL', 'OTHER')),
    CONSTRAINT chk_st_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_st_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Support Ticket Replies (Trao đổi phản hồi ticket)
CREATE TABLE IF NOT EXISTS support_ticket_replies (
    id VARCHAR(36) NOT NULL,
    ticket_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_name VARCHAR(150) NOT NULL,
    sender_role VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_str_ticket (ticket_id),
    CONSTRAINT chk_str_role CHECK (sender_role IN ('USER', 'AGENT', 'ADMIN')),
    CONSTRAINT fk_str_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
