package vn.cozynest.listing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.common.storage.EncryptedLocalStorageService;
import vn.cozynest.common.storage.SignedDownloadTokenService;
import vn.cozynest.listing.application.service.PrivateStorageService;
import vn.cozynest.listing.domain.model.StorageAccessLog;
import vn.cozynest.listing.domain.repository.StorageAccessLogRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class PrivateStorageSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void enforcesOwnershipIdorAndAuditsEncryptedRoundTrip() {
        byte[] aesKey = new byte[32];
        for (int i = 0; i < 32; i++) aesKey[i] = (byte) (i + 7);
        var store = new EncryptedLocalStorageService(tempDir.toString(), Base64.getEncoder().encodeToString(aesKey), true);
        var signer = new SignedDownloadTokenService("listing-hmac-secret-test-32-chars");
        StorageAccessLogRepository auditRepo = Mockito.mock(StorageAccessLogRepository.class);
        Mockito.when(auditRepo.save(any(StorageAccessLog.class))).thenAnswer(inv -> inv.getArgument(0));

        PrivateStorageService service = new PrivateStorageService(store, signer, auditRepo);
        byte[] pdfBytes = "%PDF-1.7\n%OwnershipDeedContent".getBytes(StandardCharsets.UTF_8);

        var uploaded = service.uploadPrivateDoc("u-landlord-01", "ROLE_LANDLORD", 1L, "deed.pdf", pdfBytes, "application/pdf");
        assertThat(uploaded.objectKey()).startsWith("landlords/1/");

        // Another landlord attempts to issue signed URL for landlord 1's key -> IDOR denied
        assertThatThrownBy(() -> service.createSignedUrl("u-landlord-02", "ROLE_LANDLORD", 2L, uploaded.objectKey(), false))
                .isInstanceOf(AccessDeniedException.class);

        // Authorized landlord signs & downloads
        String signedUrl = service.createSignedUrl("u-landlord-01", "ROLE_LANDLORD", 1L, uploaded.objectKey(), false);
        String token = signedUrl.substring(signedUrl.indexOf("token=") + 6);

        // Different non-privileged user tries to replay token -> denied
        assertThatThrownBy(() -> service.downloadByToken("u-landlord-02", "ROLE_LANDLORD", false, token))
                .isInstanceOf(AccessDeniedException.class);

        var downloaded = service.downloadByToken("u-landlord-01", "ROLE_LANDLORD", false, token);
        assertThat(downloaded.bytes()).isEqualTo(pdfBytes);

        // Spoofed/invalid magic bytes rejected
        byte[] fakeScript = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.uploadPrivateDoc("u-landlord-01", "ROLE_LANDLORD", 1L, "fake.pdf", fakeScript, "application/pdf"))
                .isInstanceOf(RuntimeException.class);

        // Missing object rejected with IllegalArgumentException
        String missingUrl = service.createSignedUrl("u-landlord-01", "ROLE_LANDLORD", 1L, "landlords/1/nonexistent.pdf", false);
        String missingToken = missingUrl.substring(missingUrl.indexOf("token=") + 6);
        assertThatThrownBy(() -> service.downloadByToken("u-landlord-01", "ROLE_LANDLORD", false, missingToken))
                .isInstanceOf(IllegalArgumentException.class);

        // Expired token rejected
        var pastClock = java.time.Clock.fixed(java.time.Instant.now().minusSeconds(600), java.time.ZoneOffset.UTC);
        var expiredSigner = new SignedDownloadTokenService("listing-hmac-secret-test-32-chars", pastClock);
        String expiredToken = expiredSigner.createToken(PrivateStorageService.OWNERSHIP_BUCKET, uploaded.objectKey(), "u-landlord-01", 10);
        assertThatThrownBy(() -> service.downloadByToken("u-landlord-01", "ROLE_LANDLORD", false, expiredToken))
                .isInstanceOf(SecurityException.class);

        // Cross-landlord delete blocked (IDOR), then owner deletes & document is no longer downloadable
        assertThatThrownBy(() -> service.deletePrivateDoc("u-landlord-02", "ROLE_LANDLORD", 2L, uploaded.objectKey(), false))
                .isInstanceOf(AccessDeniedException.class);
        service.deletePrivateDoc("u-landlord-01", "ROLE_LANDLORD", 1L, uploaded.objectKey(), false);
        assertThatThrownBy(() -> service.downloadByToken("u-landlord-01", "ROLE_LANDLORD", false, token))
                .isInstanceOf(IllegalArgumentException.class);

        verify(auditRepo, atLeastOnce()).save(any(StorageAccessLog.class));
    }
}

