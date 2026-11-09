-- ==============================================================================
-- Flyway Migration V12: Add price_unit to rooms, create cms_articles, and clean banner text
-- ==============================================================================

-- 1. Add price_unit to rooms (ROOM or BED, defaults to ROOM)
ALTER TABLE rooms
ADD COLUMN price_unit VARCHAR(16) NOT NULL DEFAULT 'ROOM' AFTER price;

-- 2. Create cms_articles table for safety & guide content
CREATE TABLE IF NOT EXISTS cms_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title_vi VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    summary_vi TEXT NOT NULL,
    summary_en TEXT NOT NULL,
    content_vi LONGTEXT NOT NULL,
    content_en LONGTEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GUIDE',
    reading_minutes INT NOT NULL DEFAULT 5,
    image_url VARCHAR(255) NOT NULL,
    image_alt_vi VARCHAR(255) NOT NULL,
    image_alt_en VARCHAR(255) NOT NULL,
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cms_articles_active_sort (is_active, sort_order ASC, published_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Seed 3 authentic CMS articles
INSERT INTO cms_articles (
    slug, title_vi, title_en, summary_vi, summary_en, content_vi, content_en,
    category, reading_minutes, image_url, image_alt_vi, image_alt_en, sort_order, is_active
) VALUES
(
    'hop-dong-thue-nha-nhung-dieu-can-biet',
    'Nhung dieu can kiem tra ky tren hop dong thue phong',
    'Essential clauses to check before signing a rental lease',
    'Nam ro 5 dieu khoan then chot ve tien dat coc, thoi han bao truoc khi tra phong va trach nhiem sua chua thiet bi de tranh tranh chap.',
    'Understand 5 critical clauses regarding security deposits, advance notice periods, and maintenance duties to avoid disputes.',
    'Hop dong thue phong la co so phap ly quan trong nhat bao ve quyen loi nguoi thue. Truoc khi ky, can doi chieu so CCCD chu nha, thoi han thue, dieu kien hoan coc va danh muc tai san ban giao.',
    'The rental contract is the most vital legal protection for tenants. Before signing, verify landlord identification, lease terms, deposit refund conditions, and equipment handover inventory.',
    'GUIDE', 5, '/images/guides/guide-contract-1080.webp',
    'But viet va so hop dong thue nha tren ban lam viec',
    'Rental contract notebook and pen on wooden desk',
    1, TRUE
),
(
    'minh-bach-chi-phi-dien-nuoc',
    'Kinh nghiem ghi nhan va tinh toan chi phi dien nuoc sinh hoat',
    'Guide to transparent electricity and water metering and billing',
    'Huong dan cach doc dong ho cong to dien co, dong ho nuoc va doi chieu don gia thuc te tren hop dong de tranh bi thu chenh lech.',
    'Step-by-step guidance on reading electricity and water meters, calculating rates, and verifying contract utility terms.',
    'Chi phi dien nuoc can duoc ghi ro trong hop dong kem chi so chot ngay nhan phong. Yeu cau chup anh dong ho ngay dau tien va theo doi hoa don dinh ky hang thang.',
    'Utility rates should be clearly specified with initial meter readings on handover day. Always capture meter photos and verify monthly billing statements.',
    'GUIDE', 4, '/images/guides/guide-utility-1080.webp',
    'Bang theo doi chi phi dien nuoc minh bach hang thang',
    'Transparent utility calculation and meter checklist',
    2, TRUE
),
(
    'kinh-nghiem-nhan-phong-kiem-tra-co-so-vat-chat',
    'Checklist 10 phut kiem tra phong tro truoc khi dat coc',
    '10-minute rental room inspection checklist before deposit',
    'Kiem tra ap luc nuoc, o cam dien, cua so thoat hiem va tinh trang tuong am moc truoc khi quyet dinh dat coc giu cho.',
    'Check water pressure, electrical outlets, emergency ventilation, and wall conditions thoroughly before placing a deposit.',
    'Dung voi vang chuyen tien coc khi chua truc tiep xem phong. Hay kiem tra he thong dien nuoc, khoa cua, khoa van tay va an ninh khu vuc xung quanh vao cac khung gio khac nhau.',
    'Never transfer deposit funds before visiting in person. Inspect electrical sockets, door locks, biometric access, and neighbourhood security across different hours.',
    'GUIDE', 6, '/images/guides/guide-inspection-1080.webp',
    'Kiem tra phong don anh sang tu nhien qua cua so',
    'Room inspection checklist with natural window lighting',
    3, TRUE
)
ON DUPLICATE KEY UPDATE
    title_vi = VALUES(title_vi),
    title_en = VALUES(title_en),
    summary_vi = VALUES(summary_vi),
    summary_en = VALUES(summary_en),
    image_url = VALUES(image_url),
    sort_order = VALUES(sort_order);

-- 4. Clean banner row #1 (remove "100%", update images to licensed assets)
UPDATE banners
SET
    title = 'Bao hiem tien coc ro dieu khoan',
    subtitle = 'Chinh sach hoan tien ro rang theo bien ban ban giao phong',
    image_url = '/images/banners/banner-deposit-1080.webp'
WHERE id = 1;

UPDATE banners
SET
    image_url = '/images/banners/banner-utility-1080.webp'
WHERE id = 2;

UPDATE banners
SET
    image_url = '/images/banners/banner-landlord-1080.webp'
WHERE id = 3;

-- 5. Point provinces to licensed area photos
UPDATE provinces SET image_url = '/images/areas/ha-noi-1080.webp' WHERE code = 'HN';
UPDATE provinces SET image_url = '/images/areas/ho-chi-minh-1080.webp' WHERE code = 'HCM';
UPDATE provinces SET image_url = '/images/areas/da-nang-1080.webp' WHERE code = 'DNG';
UPDATE provinces SET image_url = '/images/areas/hai-phong-1080.webp' WHERE code = 'HP';
UPDATE provinces SET image_url = '/images/areas/can-tho-1080.webp' WHERE code = 'CT';
UPDATE provinces SET image_url = '/images/areas/hue-1080.webp' WHERE code = 'HUE';
UPDATE provinces SET image_url = '/images/areas/binh-duong-1080.webp' WHERE code = 'BDG';
UPDATE provinces SET image_url = '/images/areas/bac-ninh-1080.webp' WHERE code = 'BN';

