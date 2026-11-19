package vn.cozynest.billing.domain.model;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class ProofImageCipherHelper {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ProofImageCipherHelper() {
    }

    public static byte[] decodeReceiptBytes(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Thiếu dữ liệu ảnh biên lai chuyển khoản");
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("data:image/")) {
            int comma = trimmed.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException("Định dạng Data URL ảnh biên lai không hợp lệ");
            return Base64.getDecoder().decode(trimmed.substring(comma + 1));
        }
        try {
            return Base64.getDecoder().decode(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu ảnh biên lai phải là PNG, JPG hoặc WebP hợp lệ");
        }
    }

    public static String verifyMagicBytes(byte[] b) {
        if (b == null || b.length < 12) {
            throw new IllegalArgumentException("Tệp ảnh biên lai quá nhỏ hoặc bị hỏng (Magic Bytes)");
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new IllegalArgumentException("Magic Bytes không hợp lệ: Chỉ chấp nhận ảnh PNG, JPG hoặc WebP thật");
    }

    public static BufferedImage readAndValidateImage(byte[] rawBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (img == null || img.getWidth() < 8 || img.getHeight() < 8 || img.getWidth() > 8000 || img.getHeight() > 8000) {
                throw new IllegalArgumentException("Kích thước ảnh biên lai không hợp lệ");
            }
            return img;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể giải mã nội dung ảnh biên lai", e);
        }
    }

    public static String computeDhash64(BufferedImage src) {
        BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, 9, 8, null);
        g.dispose();

        long hash = 0L;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int lumLeft = luminance(resized.getRGB(x, y));
                int lumRight = luminance(resized.getRGB(x + 1, y));
                hash = (hash << 1) | (lumLeft > lumRight ? 1L : 0L);
            }
        }
        return String.format("%016x", hash);
    }

    private static int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    public static byte[] reencodeWithoutMetadata(BufferedImage src) {
        try {
            BufferedImage clean = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = clean.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, clean.getWidth(), clean.getHeight());
            g.drawImage(src, 0, 0, null);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(clean, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi mã hóa lại ảnh biên lai để xóa metadata", e);
        }
    }

    public static void writeEncryptedFile(Path storageDir, String storageKey, SecretKeySpec aesKey, byte[] plain) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] enc = cipher.doFinal(plain);
            byte[] combined = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(enc, 0, combined, iv.length, enc.length);
            Files.createDirectories(storageDir);
            Files.write(storageDir.resolve(storageKey), combined);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể lưu trữ mã hóa biên lai riêng tư", e);
        }
    }

    public static byte[] readEncryptedFile(Path storageDir, String storageKey, SecretKeySpec aesKey) {
        try {
            byte[] combined = Files.readAllBytes(storageDir.resolve(storageKey));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] enc = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, enc, 0, enc.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(enc);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không tìm thấy tệp biên lai mã hóa");
        }
    }

    public static SecretKeySpec deriveAesKey(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

