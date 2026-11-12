CREATE DATABASE IF NOT EXISTS cozynest_identity CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozynest_listing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozynest_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozynest_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozynest_communication CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozynest_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'identity_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_identity.* TO 'identity_user'@'%';

CREATE USER IF NOT EXISTS 'listing_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_listing.* TO 'listing_user'@'%';

CREATE USER IF NOT EXISTS 'booking_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_booking.* TO 'booking_user'@'%';

CREATE USER IF NOT EXISTS 'billing_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_billing.* TO 'billing_user'@'%';

CREATE USER IF NOT EXISTS 'communication_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_communication.* TO 'communication_user'@'%';

CREATE USER IF NOT EXISTS 'admin_user'@'%' IDENTIFIED BY 'cozynest_svc_pass_2026';
GRANT ALL PRIVILEGES ON cozynest_admin.* TO 'admin_user'@'%';

FLUSH PRIVILEGES;

