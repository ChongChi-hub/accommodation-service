package vn.cozynest.identity.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.infrastructure.security.FaceCryptoService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Component
@Profile("!prod & !production")
public class FaceSeedDataLoader {

    private static final Logger log = LoggerFactory.getLogger(FaceSeedDataLoader.class);

    private final FacePrivateStoragePort storagePort;
    private final FaceCryptoService cryptoService;

    public FaceSeedDataLoader(FacePrivateStoragePort storagePort, FaceCryptoService cryptoService) {
        this.storagePort = storagePort;
        this.cryptoService = cryptoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateDemoSilhouetteSeedImages() {
        try {
            ensureSeedFrame("face/seed_sample_neutral.enc", "HÌNH BÓNG MẪU - CHÍNH DIỆN");
            ensureSeedFrame("face/seed_sample_blink.enc", "HÌNH BÓNG MẪU - CHỚP MẮT");
            ensureSeedFrame("face/seed_sample_turn_left.enc", "HÌNH BÓNG MẪU - QUAY TRÁI");
        } catch (Exception e) {
            log.warn("Could not generate demo seed silhouette images: {}", e.getMessage());
        }
    }

    private void ensureSeedFrame(String storageKey, String subText) throws Exception {
        if (storagePort.load(storageKey) != null) {
            return;
        }

        int width = 480;
        int height = 480;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark background
        g.setColor(new Color(30, 41, 59));
        g.fillRect(0, 0, width, height);

        // Grid lines for technical biometric scanner aesthetic
        g.setColor(new Color(51, 65, 85));
        for (int x = 40; x < width; x += 40) g.drawLine(x, 0, x, height);
        for (int y = 40; y < height; y += 40) g.drawLine(0, y, width, y);

        // Abstract silhouette outline (head circle & curved shoulders)
        g.setColor(new Color(148, 163, 184));
        g.fillOval(170, 90, 140, 160); // head
        g.fillArc(100, 240, 280, 260, 0, 180); // shoulders

        // Face scanner target brackets
        g.setColor(new Color(13, 148, 136)); // teal accent
        g.setStroke(new BasicStroke(3));
        g.drawRect(120, 70, 240, 260);

        // Prominent disclaimer banner
        g.setColor(new Color(239, 68, 68)); // red banner
        g.fillRect(20, 360, width - 40, 48);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String bannerText = "MẪU - KHÔNG PHẢI KHUÔN MẶT THẬT";
        int textX = (width - fm.stringWidth(bannerText)) / 2;
        g.drawString(bannerText, textX, 390);

        // Subtext action label
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(203, 213, 225));
        FontMetrics subFm = g.getFontMetrics();
        int subX = (width - subFm.stringWidth(subText)) / 2;
        g.drawString(subText, subX, 435);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] rawBytes = baos.toByteArray();

        byte[] encrypted = cryptoService.encrypt(rawBytes);
        storagePort.store(storageKey, encrypted);
        log.info("Generated and encrypted demo silhouette seed image at storageKey: {}", storageKey);
    }
}

