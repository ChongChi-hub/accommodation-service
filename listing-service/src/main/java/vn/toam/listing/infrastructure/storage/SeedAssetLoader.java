package vn.toam.listing.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.toam.listing.domain.model.RoomImage;
import vn.toam.listing.domain.port.ImageStoragePort;
import vn.toam.listing.domain.repository.RoomImageRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedAssetLoader implements ApplicationRunner {

    private final ImageStoragePort imageStoragePort;
    private final RoomImageRepository roomImageRepository;

    private static final Color[][] GRADIENT_PALETTES = {
            {new Color(30, 41, 59), new Color(15, 118, 110)},     // Slate to Teal
            {new Color(30, 58, 138), new Color(13, 148, 136)},    // Blue to Teal
            {new Color(19, 78, 74), new Color(20, 184, 166)},     // Deep Teal to Emerald
            {new Color(67, 56, 202), new Color(147, 51, 234)},    // Indigo to Purple
            {new Color(76, 29, 149), new Color(168, 85, 247)},    // Deep Violet
            {new Color(120, 53, 15), new Color(217, 119, 6)}      // Amber Warm
    };

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking and populating demo seed image assets...");
        List<RoomImage> allImages = roomImageRepository.findAll();
        int generatedCount = 0;

        for (RoomImage img : allImages) {
            if (img.getStorageKey() != null && !imageStoragePort.exists(img.getStorageKey())) {
                generateAndSaveImages(img);
                generatedCount++;
            }
        }

        if (generatedCount > 0) {
            log.info("Successfully generated {} placeholder demo images for rooms", generatedCount);
        } else {
            log.info("All seed images already exist in media storage.");
        }
    }

    private void generateAndSaveImages(RoomImage img) {
        int paletteIdx = (int) (img.getRoomId() % GRADIENT_PALETTES.length);
        Color[] colors = GRADIENT_PALETTES[paletteIdx];

        // 1. Large image (1600x900)
        byte[] largeBytes = createPlaceholderImage(1600, 900, colors, "Tổ Ấm - Phòng #" + img.getRoomId(), img.getAltText());
        imageStoragePort.store(img.getStorageKey(), largeBytes, "image/webp");
        if (img.getLargeKey() != null) {
            imageStoragePort.store(img.getLargeKey(), largeBytes, "image/webp");
        }

        // 2. Medium image (960x540)
        if (img.getMediumKey() != null) {
            byte[] medBytes = createPlaceholderImage(960, 540, colors, "Tổ Ấm #" + img.getRoomId(), null);
            imageStoragePort.store(img.getMediumKey(), medBytes, "image/webp");
        }

        // 3. Thumb image (320x180)
        if (img.getThumbKey() != null) {
            byte[] thumbBytes = createPlaceholderImage(320, 180, colors, "#" + img.getRoomId(), null);
            imageStoragePort.store(img.getThumbKey(), thumbBytes, "image/webp");
        }
    }

    private byte[] createPlaceholderImage(int width, int height, Color[] palette, String title, String subtitle) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Gradient Background
        GradientPaint gradient = new GradientPaint(0, 0, palette[0], width, height, palette[1]);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Subtle geometric grid or accents
        g2d.setColor(new Color(255, 255, 255, 18));
        for (int i = 0; i < width; i += 80) {
            g2d.drawLine(i, 0, i, height);
        }
        for (int j = 0; j < height; j += 80) {
            g2d.drawLine(0, j, width, j);
        }

        // Text branding
        g2d.setColor(Color.WHITE);
        int titleFontSize = Math.max(16, width / 28);
        g2d.setFont(new Font("SansSerif", Font.BOLD, titleFontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        int titleY = height / 2 - (subtitle != null ? 10 : -10);
        g2d.drawString(title, titleX, titleY);

        if (subtitle != null && width >= 600) {
            int subFontSize = Math.max(12, width / 45);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, subFontSize));
            g2d.setColor(new Color(255, 255, 255, 210));
            FontMetrics subFm = g2d.getFontMetrics();
            int subX = (width - subFm.stringWidth(subtitle)) / 2;
            int subY = titleY + fm.getHeight() + 8;
            g2d.drawString(subtitle, subX, subY);
        }

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Write as webp or png if webp writer not registered
            boolean wrote = ImageIO.write(image, "webp", baos);
            if (!wrote) {
                ImageIO.write(image, "png", baos);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to encode placeholder image", e);
            return new byte[0];
        }
    }
}

