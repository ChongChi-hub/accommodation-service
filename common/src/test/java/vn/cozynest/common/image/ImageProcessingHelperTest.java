package vn.cozynest.common.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageProcessingHelperTest {

    @Test
    @DisplayName("Fast header inspection reads image dimensions without full decompression")
    void testGetImageDimensionsFromHeader() throws IOException {
        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        Dimension dim = ImageProcessingHelper.getImageDimensions(baos.toByteArray());
        assertThat(dim.width).isEqualTo(300);
        assertThat(dim.height).isEqualTo(200);
    }

    @Test
    @DisplayName("Oversized image dimension exceeding 8000px is rejected by header check")
    void testOversizedDimensionRejected() throws IOException {
        BufferedImage img = new BufferedImage(8001, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ImageProcessingHelper.decodeAndSanitize(baos.toByteArray()));
        assertThat(ex.getMessage()).contains("8000x8000");
    }

    @Test
    @DisplayName("Transparent PNG has transparent pixels rendered to solid white canvas instead of black")
    void testTransparentPngRenderedWithWhiteBackground() throws IOException {
        BufferedImage transparentPng = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(transparentPng, "png", baos);

        BufferedImage sanitized = ImageProcessingHelper.decodeAndSanitize(baos.toByteArray());
        int rgb = sanitized.getRGB(25, 25);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        assertThat(red).isEqualTo(255);
        assertThat(green).isEqualTo(255);
        assertThat(blue).isEqualTo(255);
    }

    @Test
    @DisplayName("Center square crop crops larger dimension equally on both sides")
    void testCenterSquareCrop() {
        BufferedImage rect = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage square = ImageProcessingHelper.cropCenterSquare(rect);

        assertThat(square.getWidth()).isEqualTo(100);
        assertThat(square.getHeight()).isEqualTo(100);
    }

    @Test
    @DisplayName("pHash calculation returns 16-hex characters and identical images have 0 distance")
    void testPHashIdenticalImages() {
        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.BLUE);
        g1.fillRect(0, 0, 50, 100);
        g1.setColor(Color.RED);
        g1.fillRect(50, 0, 50, 100);
        g1.dispose();

        BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 50, 100);
        g2.setColor(Color.RED);
        g2.fillRect(50, 0, 50, 100);
        g2.dispose();

        String hash1 = ImageProcessingHelper.calculatePHash(img1);
        String hash2 = ImageProcessingHelper.calculatePHash(img2);

        assertThat(hash1).isNotNull().hasSize(16);
        assertThat(hash2).isNotNull().hasSize(16);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(ImageProcessingHelper.hammingDistance(hash1, hash2)).isZero();
    }

    @Test
    @DisplayName("pHash detects near-duplicate images with small Hamming distance <= 10")
    void testPHashNearDuplicateImages() {
        BufferedImage img1 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 200, 200);
        g1.setColor(Color.BLACK);
        g1.fillOval(40, 40, 120, 120);
        g1.dispose();

        // Slightly resized and altered copy
        BufferedImage img2 = new BufferedImage(180, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 180, 180);
        g2.setColor(new Color(20, 20, 20));
        g2.fillOval(36, 36, 108, 108);
        g2.dispose();

        String h1 = ImageProcessingHelper.calculatePHash(img1);
        String h2 = ImageProcessingHelper.calculatePHash(img2);

        int distance = ImageProcessingHelper.hammingDistance(h1, h2);
        assertThat(distance).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Watermark is applied to image without altering dimensions")
    void testApplyWatermark() {
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        BufferedImage watermarked = ImageProcessingHelper.applyWatermark(img, "Tổ Ấm - #101");

        assertThat(watermarked).isNotNull();
        assertThat(watermarked.getWidth()).isEqualTo(400);
        assertThat(watermarked.getHeight()).isEqualTo(300);
    }
}

