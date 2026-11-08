package vn.cozynest.identity.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.cozynest.common.image.ImageProcessingHelper;
import vn.cozynest.identity.domain.port.AvatarStoragePort;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Component
@Profile({"dev", "demo", "default"})
public class IdentitySeedAssetLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentitySeedAssetLoader.class);

    private final AvatarStoragePort avatarStorage;
    private final KycPrivateStoragePort kycStorage;

    public IdentitySeedAssetLoader(AvatarStoragePort avatarStorage, KycPrivateStoragePort kycStorage) {
        this.avatarStorage = avatarStorage;
        this.kycStorage = kycStorage;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking and populating demo avatar and KYC assets...");
        generateDemoAvatars();
        generateDemoKycDocuments();
        log.info("Demo avatar and KYC assets verification completed.");
    }

    private void generateDemoAvatars() {
        String[][] avatarUsers = {
                {"u-landlord-01", "VT", "#0F766E"},
                {"u-landlord-02", "MT", "#0D9488"},
                {"u-landlord-03", "ML", "#14B8A6"},
                {"u-landlord-04", "TB", "#4338CA"},
                {"u-landlord-05", "NA", "#6D28D9"},
                {"u-landlord-06", "DT", "#B45309"},
                {"u-tenant-01", "TT", "#2563EB"},
                {"u-tenant-02", "HM", "#1D4ED8"},
                {"u-mod-01", "VA", "#D97706"},
                {"u-admin-01", "TS", "#B91C1C"}
        };

        for (String[] u : avatarUsers) {
            String userId = u[0];
            String initials = u[1];
            Color bg = Color.decode(u[2]);

            String fullKey = "avatars/" + userId + ".webp";
            String thumbKey = "avatars/" + userId + "_thumb.webp";
            String medKey = "avatars/" + userId + "_medium.webp";

            if (!avatarStorage.exists(fullKey) || !avatarStorage.exists(thumbKey)) {
                try {
                    BufferedImage base = createInitialsImage(256, bg, initials);
                    byte[] fullBytes = ImageProcessingHelper.resizeToWebP(base, 256, 256, 0.9f);
                    byte[] medBytes = ImageProcessingHelper.resizeToWebP(base, 128, 128, 0.85f);
                    byte[] thumbBytes = ImageProcessingHelper.resizeToWebP(base, 64, 64, 0.85f);

                    avatarStorage.store(fullKey, fullBytes, "image/webp");
                    avatarStorage.store(medKey, medBytes, "image/webp");
                    avatarStorage.store(thumbKey, thumbBytes, "image/webp");
                } catch (IOException e) {
                    log.error("Failed to generate avatar for {}", userId, e);
                }
            }
        }
    }

    private void generateDemoKycDocuments() {
        String[][] kycDocs = {
                {"kyc/private/landlord-01_front.webp", "Nguyen Van Toan", "000085000001", "MAT TRUOC"},
                {"kyc/private/landlord-01_back.webp", "Nguyen Van Toan", "000085000001", "MAT SAU"},
                {"kyc/private/landlord-02_front.webp", "Tran Minh Tuan", "000085000002", "MAT TRUOC"},
                {"kyc/private/landlord-02_back.webp", "Tran Minh Tuan", "000085000002", "MAT SAU"},
                {"kyc/private/landlord-03_front.webp", "Hoang Thi Mai Lan", "000085000003", "MAT TRUOC"},
                {"kyc/private/landlord-03_back.webp", "Hoang Thi Mai Lan", "000085000003", "MAT SAU"},
                {"kyc/private/landlord-06_front.webp", "Vu Dinh Trong", "000085000006", "MAT TRUOC"},
                {"kyc/private/landlord-06_back.webp", "Vu Dinh Trong", "000085000006", "MAT SAU"}
        };

        for (String[] doc : kycDocs) {
            String key = doc[0];
            String name = doc[1];
            String idNum = doc[2];
            String side = doc[3];

            if (!kycStorage.exists(key)) {
                try {
                    BufferedImage card = createSyntheticCard(name, idNum, side);
                    byte[] webpBytes = ImageProcessingHelper.resizeBoundingBoxWebP(card, 1200, 0.9f);
                    kycStorage.store(key, webpBytes, "image/webp");
                } catch (IOException e) {
                    log.error("Failed to generate synthetic KYC document for {}", key, e);
                }
            }
        }
    }

    private BufferedImage createInitialsImage(int size, Color bg, String initials) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(bg);
        g.fillRect(0, 0, size, size);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
        FontMetrics fm = g.getFontMetrics();
        int x = (size - fm.stringWidth(initials)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(initials, x, y);
        g.dispose();
        return img;
    }

    private BufferedImage createSyntheticCard(String name, String idNum, String side) {
        int width = 856;
        int height = 540;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Card background (light greenish/blue bank card tint)
        g.setColor(new Color(241, 245, 249));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(15, 118, 110));
        g.setStroke(new BasicStroke(6));
        g.drawRoundRect(10, 10, width - 20, height - 20, 24, 24);

        // Header
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("CONG HOA XA HOI CHU NGHIA VIET NAM", 180, 50);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Doc lap - Tu do - Hanh phuc", 300, 75);

        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.drawString("CAN CUOC CONG DAN (" + side + ")", 220, 130);

        // Chip placeholder
        g.setColor(new Color(234, 179, 8));
        g.fillRoundRect(50, 150, 90, 70, 12, 12);
        g.setColor(new Color(161, 98, 7));
        g.drawRoundRect(50, 150, 90, 70, 12, 12);

        // Photo box
        g.setColor(new Color(203, 213, 225));
        g.fillRect(50, 240, 150, 200);
        g.setColor(new Color(100, 116, 139));
        g.drawRect(50, 240, 150, 200);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("ANH 3x4 (SAMPLE)", 65, 340);

        // Details
        g.setColor(new Color(30, 41, 59));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("So / No: " + idNum, 240, 200);
        g.drawString("Ho va ten / Full name: " + name, 240, 250);
        g.drawString("Quoc tich / Nationality: Viet Nam", 240, 300);
        g.drawString("Que quan: Ha Noi / TP. Ho Chi Minh", 240, 350);
        g.drawString("Noi thuong tru: To Am System Dev Seed", 240, 400);

        // Big diagonal watermark: "MAU - KHONG PHAI GIAY TO THAT"
        g.setColor(new Color(220, 38, 38, 80));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        g.rotate(-Math.toRadians(25), (double) width / 2, (double) height / 2);
        g.drawString("MAU - KHONG PHAI GIAY TO THAT", 60, height / 2);
        g.dispose();

        return img;
    }
}

