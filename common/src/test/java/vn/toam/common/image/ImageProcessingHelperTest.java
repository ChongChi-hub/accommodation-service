package vn.toam.common.image;

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
}

