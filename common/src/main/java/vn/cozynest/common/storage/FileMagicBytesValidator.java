package vn.cozynest.common.storage;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Validates uploaded files using magic bytes (not just Content-Type or file extension) and safe key rules.
 */
public final class FileMagicBytesValidator {

    public static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private FileMagicBytesValidator() {
    }

    public static String detectAndValidate(byte[] bytes, String declaredContentType, long maxBytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Tep tai len khong duoc rong");
        }
        long limit = maxBytes > 0 ? maxBytes : DEFAULT_MAX_BYTES;
        if (bytes.length > limit) {
            throw new IllegalArgumentException("Kich thuoc tep vuot qua gioi han cho phep");
        }

        String detectedMime = detectMimeByMagicBytes(bytes);
        if (detectedMime == null || !ALLOWED_MIME.contains(detectedMime)) {
            throw new IllegalArgumentException("Dinh dang tep khong duoc ho tro (chi chap nhan PDF, JPEG, PNG, WEBP)");
        }
        if (declaredContentType != null && !declaredContentType.isBlank()) {
            String normalized = declaredContentType.toLowerCase().split(";")[0].trim();
            if ("image/jpg".equals(normalized)) {
                normalized = "image/jpeg";
            }
            if (!normalized.equals(detectedMime) && !"application/octet-stream".equals(normalized)) {
                throw new IllegalArgumentException("Content-Type khai bao khong khop voi magic bytes thuc te cua tep");
            }
        }
        return detectedMime;
    }

    public static String detectMimeByMagicBytes(byte[] bytes) {
        if (bytes.length >= 5 && "%PDF-".equals(new String(bytes, 0, 5, StandardCharsets.US_ASCII))) {
            return "application/pdf";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 12
                && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        return null;
    }

    public static String sanitizeKey(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.indexOf('\0') >= 0) {
            throw new SecurityException("Khoa luu tru khong hop le");
        }
        String normalized = key.replace('\\', '/').replaceAll("^/+", "");
        if (!normalized.matches("^[a-zA-Z0-9._/-]+$")) {
            throw new SecurityException("Khoa luu tru chua ky tu khong hop le");
        }
        return normalized;
    }
}

