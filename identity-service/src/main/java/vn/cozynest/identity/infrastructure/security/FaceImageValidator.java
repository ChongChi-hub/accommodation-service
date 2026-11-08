package vn.cozynest.identity.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class FaceImageValidator {

    public static final long MAX_FILE_SIZE = 1024 * 1024; // 1 MB
    public static final int MIN_DIMENSION = 200;
    public static final int MAX_DIMENSION = 4096;

    public byte[] validateAndSanitize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IdentityDomainException("Tệp hình ảnh không được để trống", "ERR_FACE_FILE_EMPTY");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IdentityDomainException("Dung lượng ảnh vượt quá giới hạn 1 MB", "ERR_FACE_FILE_TOO_LARGE");
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new IdentityDomainException("Không thể đọc tệp hình ảnh", "ERR_FACE_FILE_READ");
        }

        if (!hasValidMagicBytes(rawBytes)) {
            throw new IdentityDomainException("Định dạng tệp không hợp lệ. Chỉ chấp nhận ảnh JPEG hoặc WebP", "ERR_FACE_INVALID_FORMAT");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (image == null) {
                throw new IdentityDomainException("Không thể giải mã dữ liệu hình ảnh", "ERR_FACE_INVALID_IMAGE");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                throw new IdentityDomainException("Độ phân giải ảnh quá nhỏ (tối thiểu 200x200 px)", "ERR_FACE_RESOLUTION_TOO_LOW");
            }
            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new IdentityDomainException("Độ phân giải ảnh quá lớn (tối đa 4096x4096 px)", "ERR_FACE_RESOLUTION_TOO_HIGH");
            }

            // Strip EXIF / metadata by drawing into a clean BufferedImage and encoding
            BufferedImage sanitized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sanitized.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(sanitized, "jpg", baos);
            if (!written) {
                throw new IdentityDomainException("Lỗi xử lý định dạng ảnh", "ERR_FACE_ENCODING");
            }
            return baos.toByteArray();
        } catch (IdentityDomainException ide) {
            throw ide;
        } catch (Exception e) {
            throw new IdentityDomainException("Lỗi kiểm tra tính hợp lệ của ảnh khuôn mặt", "ERR_FACE_IMAGE_PROCESSING");
        }
    }

    public static boolean hasValidMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return false;

        // JPEG magic bytes: FF D8 FF
        boolean isJpeg = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
        if (isJpeg) return true;

        // WebP magic bytes: RIFF....WEBP
        boolean isWebP = (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                          bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P');
        return isWebP;
    }
}

