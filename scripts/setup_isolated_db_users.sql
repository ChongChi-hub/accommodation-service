-- Create isolated users for each microservice
CREATE USER IF NOT EXISTS 'identity_user'@'%' IDENTIFIED BY 'identity_pass123';
GRANT ALL PRIVILEGES ON cozynest_identity.* TO 'identity_user'@'%';

CREATE USER IF NOT EXISTS 'listing_user'@'%' IDENTIFIED BY 'listing_pass123';
GRANT ALL PRIVILEGES ON cozynest_listing.* TO 'listing_user'@'%';

CREATE USER IF NOT EXISTS 'booking_user'@'%' IDENTIFIED BY 'booking_pass123';
GRANT ALL PRIVILEGES ON cozynest_booking.* TO 'booking_user'@'%';

CREATE USER IF NOT EXISTS 'billing_user'@'%' IDENTIFIED BY 'billing_pass123';
GRANT ALL PRIVILEGES ON cozynest_billing.* TO 'billing_user'@'%';

CREATE USER IF NOT EXISTS 'communication_user'@'%' IDENTIFIED BY 'communication_pass123';
GRANT ALL PRIVILEGES ON cozynest_communication.* TO 'communication_user'@'%';

CREATE USER IF NOT EXISTS 'admin_user'@'%' IDENTIFIED BY 'admin_pass123';
GRANT ALL PRIVILEGES ON cozynest_admin.* TO 'admin_user'@'%';

FLUSH PRIVILEGES;

