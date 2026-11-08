package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceImageValidatorTest {

    @Test
    @DisplayName("Validator: Valid JPEG passes and strips metadata")
    void testValidJpegPasses() throws Exception {
        FaceImageValidator validator = new FaceImageValidator();
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

        MockMultipartFile file = new MockMultipartFile("frame", "test.jpg", "image/jpeg", baos.toByteArray());
        byte[] sanitized = validator.validateAndSanitize(file);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.length).isGreaterThan(0);
        assertThat(FaceImageValidator.hasValidMagicBytes(sanitized)).isTrue();
    }

    @Test
    @DisplayName("Validator: Invalid file format throws ERR_FACE_INVALID_FORMAT")
    void testInvalidMagicBytesRejected() {
        FaceImageValidator validator = new FaceImageValidator();
        byte[] fakeBytes = "Hello this is a plain text file pretending to be image".getBytes();
        MockMultipartFile file = new MockMultipartFile("frame", "fake.jpg", "image/jpeg", fakeBytes);

        assertThatThrownBy(() -> validator.validateAndSanitize(file))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("Định dạng tệp không hợp lệ");
    }

    @Test
    @DisplayName("Validator: Image resolution too small (< 200px) is rejected")
    void testResolutionTooSmallRejected() throws Exception {
        FaceImageValidator validator = new FaceImageValidator();
        BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

        MockMultipartFile file = new MockMultipartFile("frame", "small.jpg", "image/jpeg", baos.toByteArray());

        assertThatThrownBy(() -> validator.validateAndSanitize(file))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("Độ phân giải ảnh quá nhỏ");
    }
}

