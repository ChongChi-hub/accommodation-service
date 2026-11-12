package vn.cozynest.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrivateStorageSecurityUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsSpoofedMagicBytesAndPathTraversal() {
        byte[] fakePdf = "NOT_A_PDF_SCRIPT<script>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> FileMagicBytesValidator.detectAndValidate(fakePdf, "application/pdf", 1024))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FileMagicBytesValidator.sanitizeKey("../../etc/passwd"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void encryptsAtRestAndDecryptsOnAuthorizedRead() throws Exception {
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 1);
        EncryptedLocalStorageService store = new EncryptedLocalStorageService(
                tempDir.toString(), Base64.getEncoder().encodeToString(key), true);

        byte[] pdfBytes = "%PDF-1.7\n%CozyNestPrivateContract".getBytes(StandardCharsets.UTF_8);
        PrivateObjectStoragePort.StoredObjectMetadata meta = store.storePrivate(
                "ownership-docs", "room-101/deed.pdf", pdfBytes, "application/pdf");

        assertThat(meta.contentType()).isEqualTo("application/pdf");
        Path onDisk = tempDir.resolve("ownership-docs/room-101/deed.pdf.enc");
        assertThat(Files.exists(onDisk)).isTrue();
        assertThat(Files.readAllBytes(onDisk)).isNotEqualTo(pdfBytes);
        assertThat(store.loadPrivate("ownership-docs", "room-101/deed.pdf")).isEqualTo(pdfBytes);
    }

    @Test
    void signedDownloadTokenRejectsTamperingAndExpiry() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-11T10:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };

        SignedDownloadTokenService signer = new SignedDownloadTokenService("test-hmac-secret-32-chars-minimum", clock);
        String token = signer.createToken("ownership-docs", "room-1/doc.pdf", "landlord-1", 60);

        SignedDownloadTokenService.VerifiedToken verified = signer.verify(token);
        assertThat(verified.authorizedActorId()).isEqualTo("landlord-1");
        assertThat(verified.objectKey()).isEqualTo("room-1/doc.pdf");

        assertThatThrownBy(() -> signer.verify(token + "tamper"))
                .isInstanceOf(SecurityException.class);

        now.set(now.get().plusSeconds(61));
        assertThatThrownBy(() -> signer.verify(token))
                .isInstanceOf(SecurityException.class);
    }
}

