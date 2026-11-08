package vn.cozynest.listing.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.port.ImageStoragePort;
import vn.cozynest.listing.domain.repository.RoomImageRepository;

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

    private static final Color[][] BRAND_PALETTES = {
            {new Color(15, 118, 110), new Color(13, 148, 136)},   // Deep Teal to Primary Teal
            {new Color(180, 83, 9), new Color(217, 119, 6)},      // Amber Warm
            {new Color(19, 78, 74), new Color(45, 212, 191)},     // Dark Teal to Soft Teal
            {new Color(68, 64, 60), new Color(120, 113, 108)}     // Warm Stone
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
        int paletteIdx = (int) (img.getRoomId() % BRAND_PALETTES.length);
        Color[] colors = BRAND_PALETTES[paletteIdx];

        // 1. Large image (1600x1200 - 4:3)
        byte[] largeBytes = createPlaceholderImage(1600, 1200, colors);
        imageStoragePort.store(img.getStorageKey(), largeBytes, "image/webp");
        if (img.getLargeKey() != null) {
            imageStoragePort.store(img.getLargeKey(), largeBytes, "image/webp");
        }

        // 2. Medium image (960x720 - 4:3)
        if (img.getMediumKey() != null) {
            byte[] medBytes = createPlaceholderImage(960, 720, colors);
            imageStoragePort.store(img.getMediumKey(), medBytes, "image/webp");
        }

        // 3. Thumb image (320x240 - 4:3)
        if (img.getThumbKey() != null) {
            byte[] thumbBytes = createPlaceholderImage(320, 240, colors);
            imageStoragePort.store(img.getThumbKey(), thumbBytes, "image/webp");
        }
    }

    private byte[] createPlaceholderImage(int width, int height, Color[] palette) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(new Color(245, 245, 244)); // Warm stone background
        g2d.fillRect(0, 0, width, height);

        // Accent wall block with brand color
        g2d.setColor(palette[0]);
        g2d.fillRect(0, 0, width / 3, (int) (height * 0.75));

        // Floor line
        int floorY = (int) (height * 0.75);
        g2d.setColor(new Color(231, 229, 228));
        g2d.fillRect(0, floorY, width, height - floorY);
        g2d.setColor(new Color(168, 162, 158));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(0, floorY, width, floorY);

        // Architectural window frame in center
        int winX = (int) (width * 0.42);
        int winY = (int) (height * 0.15);
        int winW = (int) (width * 0.35);
        int winH = (int) (height * 0.45);

        g2d.setColor(new Color(224, 242, 254)); // Soft sky daylight
        g2d.fillRoundRect(winX, winY, winW, winH, 12, 12);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRoundRect(winX, winY, winW, winH, 12, 12);
        g2d.drawLine(winX + winW / 2, winY, winX + winW / 2, winY + winH);
        g2d.drawLine(winX, winY + winH / 2, winX + winW, winY + winH / 2);

        // Modern furniture block with secondary brand accent
        g2d.setColor(palette[1]);
        int furnW = (int) (width * 0.28);
        int furnH = (int) (height * 0.18);
        int furnX = (int) (width * 0.45);
        int furnY = floorY - furnH;
        g2d.fillRoundRect(furnX, furnY, furnW, furnH, 16, 16);

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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

