package vn.cozynest.common.image;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageProcessingHelper {

    public static final int MAX_PIXEL_DIMENSION = 8000;
    public static final long MAX_FILE_SIZE_5MB = 5 * 1024 * 1024L;

    static {
        try {
            ImageIO.scanForPlugins();
        } catch (Throwable ignored) {
        }
    }

    private ImageProcessingHelper() {}

    public static boolean isValidImageMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }

        // JPEG: FF D8 FF
        boolean isJpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (bytes[0] & 0xFF) == 0x89
                && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E
                && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D
                && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0x0A;

        // WebP: Bytes 0-3 "RIFF", Bytes 8-11 "WEBP"
        boolean isWebP = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';

        return isJpeg || isPng || isWebP;
    }

    public static Dimension getImageDimensions(byte[] rawBytes) throws IOException {
        try (javax.imageio.stream.ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(rawBytes))) {
            if (in == null) throw new IllegalArgumentException("Khong the tao luong doc hinh anh");
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                javax.imageio.ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
        throw new IllegalArgumentException("Khong the doc header tep hinh anh");
    }

    public static BufferedImage decodeAndSanitize(byte[] rawBytes) throws IOException {
        if (!isValidImageMagicBytes(rawBytes)) {
            throw new IllegalArgumentException("Dinh dang anh khong hop le (chi ho tro JPG, PNG, WebP)");
        }

        // Fast Header Inspection: read dimensions WITHOUT full image raster decompression
        Dimension dim = getImageDimensions(rawBytes);
        if (dim.width > MAX_PIXEL_DIMENSION || dim.height > MAX_PIXEL_DIMENSION || dim.width <= 0 || dim.height <= 0
                || ((long) dim.width * dim.height > 40_000_000L)) {
            throw new IllegalArgumentException("Kich thuoc anh vuot qua gioi han (toi da 40 Megapixel hoac 8000x8000 pixel)");
        }

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(rawBytes));
        if (original == null) {
            throw new IllegalArgumentException("Khong the giai ma tep hinh anh");
        }

        // Redraw onto clean canvas with pure white background (sanitizes EXIF and renders transparent PNG properly)
        BufferedImage sanitized = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sanitized.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, original.getWidth(), original.getHeight());
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return sanitized;
    }

    public static BufferedImage cropCenterSquare(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        return src.getSubimage(x, y, size, size);
    }

    public static byte[] resizeToWebP(BufferedImage source, int targetWidth, int targetHeight, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Thumbnails.of(source)
                    .size(targetWidth, targetHeight)
                    .outputFormat("webp")
                    .outputQuality(quality)
                    .toOutputStream(baos);
        } catch (Exception e) {
            baos.reset();
            Thumbnails.of(source)
                    .size(targetWidth, targetHeight)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(baos);
        }
        return baos.toByteArray();
    }

    public static byte[] resizeBoundingBoxWebP(BufferedImage source, int maxEdge, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Thumbnails.of(source)
                    .size(maxEdge, maxEdge)
                    .outputFormat("webp")
                    .outputQuality(quality)
                    .toOutputStream(baos);
        } catch (Exception e) {
            baos.reset();
            Thumbnails.of(source)
                    .size(maxEdge, maxEdge)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(baos);
        }
        return baos.toByteArray();
    }
}

