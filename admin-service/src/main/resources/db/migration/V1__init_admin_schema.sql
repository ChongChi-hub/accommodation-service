-- ============================================================
-- V1__init_admin_schema.sql
-- Admin Service Schema (cozynest_admin)
-- Bounded Context: Reports, Moderation, Audit, Stats, Disputes, Content
-- ============================================================

-- 1. Reports (Báo cáo vi phạm)
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(36) NOT NULL,
    reporter_id VARCHAR(36) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT NULL,
    evidence_urls_json JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_to VARCHAR(36) NULL,
    resolution_notes TEXT NULL,
    resolved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_rpt_reporter (reporter_id),
    INDEX idx_rpt_target (target_type, target_id),
    INDEX idx_rpt_status (status),
    CONSTRAINT chk_rpt_target CHECK (target_type IN ('ROOM', 'USER', 'REVIEW', 'CONTRACT')),
    CONSTRAINT chk_rpt_status CHECK (status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Moderation Actions (Nhật ký kiểm duyệt)
CREATE TABLE IF NOT EXISTS moderation_actions (
    id VARCHAR(36) NOT NULL,
    moderator_id VARCHAR(36) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    previous_status VARCHAR(50) NULL,
    new_status VARCHAR(50) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ma_moderator (moderator_id),
    INDEX idx_ma_target (target_type, target_id),
    INDEX idx_ma_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Audit Logs (Nhật ký kiểm toán an ninh)
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    payload_diff_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_al_actor (actor_id),
    INDEX idx_al_created (created_at),
    INDEX idx_al_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. System Settings (Cấu hình hệ thống)
CREATE TABLE IF NOT EXISTS system_settings (
    id VARCHAR(36) NOT NULL,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(255) NULL,
    updated_by VARCHAR(36) NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Daily Statistics (Bảng thống kê vận hành theo ngày)
CREATE TABLE IF NOT EXISTS daily_statistics (
    id VARCHAR(36) NOT NULL,
    stat_date DATE NOT NULL UNIQUE,
    total_users INT NOT NULL DEFAULT 0,
    new_users INT NOT NULL DEFAULT 0,
    total_rooms INT NOT NULL DEFAULT 0,
    active_rooms INT NOT NULL DEFAULT 0,
    pending_rooms INT NOT NULL DEFAULT 0,
    total_contracts INT NOT NULL DEFAULT 0,
    active_contracts INT NOT NULL DEFAULT 0,
    total_revenue BIGINT NOT NULL DEFAULT 0,
    platform_fee_revenue BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_ds_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Disputes (Tranh chấp & Khiếu nại)
CREATE TABLE IF NOT EXISTS disputes (
    id VARCHAR(36) NOT NULL,
    dispute_code VARCHAR(50) NOT NULL UNIQUE,
    contract_id VARCHAR(36) NOT NULL,
    room_title VARCHAR(255) NOT NULL,
    complainant_id VARCHAR(36) NOT NULL,
    complainant_name VARCHAR(150) NOT NULL,
    complainant_role VARCHAR(30) NOT NULL,
    defendant_id VARCHAR(36) NOT NULL,
    defendant_name VARCHAR(150) NOT NULL,
    defendant_role VARCHAR(30) NOT NULL,
    dispute_type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    reason TEXT NOT NULL,
    evidence_urls_json JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    mediator_id VARCHAR(36) NULL,
    verdict TEXT NULL,
    resolved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_disp_contract (contract_id),
    INDEX idx_disp_status (status),
    INDEX idx_disp_created (created_at),
    CONSTRAINT chk_disp_type CHECK (dispute_type IN ('DEPOSIT_RETURN', 'DAMAGE_COMPENSATION', 'EARLY_TERMINATION', 'UTILITY_CHARGE', 'OTHER')),
    CONSTRAINT chk_disp_status CHECK (status IN ('PENDING', 'MEDIATING', 'RESOLVED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Dispute Events (Sự kiện và chứng cứ tranh chấp)
CREATE TABLE IF NOT EXISTS dispute_events (
    id VARCHAR(36) NOT NULL,
    dispute_id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_de_dispute (dispute_id),
    CONSTRAINT fk_de_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Content Posts (Tin tức, FAQ, Cẩm nang, Banner)
CREATE TABLE IF NOT EXISTS content_posts (
    id VARCHAR(36) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    content MEDIUMTEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    author_id VARCHAR(36) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_cp_category (category),
    INDEX idx_cp_status (status),
    CONSTRAINT chk_cp_category CHECK (category IN ('BLOG', 'FAQ', 'BANNER', 'GUIDE')),
    CONSTRAINT chk_cp_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Email Templates (Mẫu email đa ngôn ngữ)
CREATE TABLE IF NOT EXISTS email_templates (
    id VARCHAR(36) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    subject VARCHAR(255) NOT NULL,
    body_html MEDIUMTEXT NOT NULL,
    body_text MEDIUMTEXT NOT NULL,
    variables_json JSON NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_et_code_lang (template_code, language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
