package vn.cozynest.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStoragePortUnitTest {

    @TempDir
    Path tempDir;

    private S3CompatibleStorageAdapter createLocalAdapter() {
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 7);
        return new S3CompatibleStorageAdapter(
                "local", "http://localhost:9000", "auto", "", "",
                "cozynest-public", "cozynest-private",
                "https://cdn.cozynest.vn", tempDir.toString(),
                Base64.getEncoder().encodeToString(key)
        );
    }

    private S3CompatibleStorageAdapter createCloudAdapter() {
        return new S3CompatibleStorageAdapter(
                "r2", "https://mock-account.r2.cloudflarestorage.com", "auto",
                "mock-r2-access-key", "mock-r2-secret-key-32-bytes-minimum!",
                "cozynest-public", "cozynest-private",
                "https://pub-cdn.cozynest.vn", tempDir.toString(), ""
        );
    }

    @Test
    @DisplayName("Stores public object unencrypted with CDN link and retrieves content")
    void storesAndLoadsPublicObjectWithCdn() {
        S3CompatibleStorageAdapter adapter = createLocalAdapter();
        byte[] webpBytes = ("RIFF\0\0\0\0WEBPVP8 ").getBytes(StandardCharsets.US_ASCII);

        ObjectStoragePort.StoredObjectMetadata meta = adapter.putObject(
                "cozynest-public", "rooms/101/photo1.webp", webpBytes, "image/webp", false
        );

        assertThat(meta.isPrivate()).isFalse();
        assertThat(meta.publicUrl()).isEqualTo("https://cdn.cozynest.vn/rooms/101/photo1.webp");
        assertThat(adapter.exists("cozynest-public", "rooms/101/photo1.webp", false)).isTrue();

        byte[] loaded = adapter.getObject("cozynest-public", "rooms/101/photo1.webp", false);
        assertThat(loaded).isEqualTo(webpBytes);
    }

    @Test
    @DisplayName("Stores private document encrypted at rest with AES-256-GCM")
    void storesPrivateObjectEncryptedAtRest() throws Exception {
        S3CompatibleStorageAdapter adapter = createLocalAdapter();
        byte[] pdfBytes = "%PDF-1.7\nSample Confidential Contract".getBytes(StandardCharsets.UTF_8);

        ObjectStoragePort.StoredObjectMetadata meta = adapter.putObject(
                "cozynest-private", "contracts/ctr-99.pdf", pdfBytes, "application/pdf", true
        );

        assertThat(meta.isPrivate()).isTrue();
        assertThat(meta.publicUrl()).isNull();

        Path onDisk = tempDir.resolve("private/cozynest-private/contracts/ctr-99.pdf.enc");
        assertThat(Files.exists(onDisk)).isTrue();
        assertThat(Files.readAllBytes(onDisk)).isNotEqualTo(pdfBytes);

        byte[] loaded = adapter.getObject("cozynest-private", "contracts/ctr-99.pdf", true);
        assertThat(loaded).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("Generates AWS SigV4 presigned upload and download URLs when cloud credentials provided")
    void generatesSigV4PresignedUrlsForCloud() {
        S3CompatibleStorageAdapter cloudAdapter = createCloudAdapter();

        ObjectStoragePort.PresignedUploadResponse presigned = cloudAdapter.generatePresignedUploadUrl(
                "cozynest-public", "rooms/102/img.webp", "image/webp", 5242880, 900, false
        );

        assertThat(presigned.uploadUrl()).contains("https://mock-account.r2.cloudflarestorage.com/cozynest-public/rooms/102/img.webp");
        assertThat(presigned.uploadUrl()).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(presigned.uploadUrl()).contains("X-Amz-Signature=");
        assertThat(presigned.publicCdnUrl()).isEqualTo("https://pub-cdn.cozynest.vn/rooms/102/img.webp");

        String downloadUrl = cloudAdapter.generatePresignedDownloadUrl("cozynest-private", "contracts/secret.pdf", 600, true);
        assertThat(downloadUrl).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(downloadUrl).contains("X-Amz-Signature=");
    }

    @Test
    @DisplayName("Lists objects by prefix and deletes safely")
    void listsObjectsByPrefixAndDeletes() {
        S3CompatibleStorageAdapter adapter = createLocalAdapter();
        byte[] webpBytes = ("RIFF\0\0\0\0WEBPVP8 ").getBytes(StandardCharsets.US_ASCII);

        adapter.putObject("cozynest-public", "rooms/201/a.webp", webpBytes, "image/webp", false);
        adapter.putObject("cozynest-public", "rooms/201/b.webp", webpBytes, "image/webp", false);
        adapter.putObject("cozynest-public", "rooms/999/c.webp", webpBytes, "image/webp", false);

        List<ObjectStoragePort.StoredObjectMetadata> room201List = adapter.listObjects("cozynest-public", "rooms/201/", false);
        assertThat(room201List).hasSize(2);

        adapter.deleteObject("cozynest-public", "rooms/201/a.webp", false);
        assertThat(adapter.exists("cozynest-public", "rooms/201/a.webp", false)).isFalse();
        assertThat(adapter.listObjects("cozynest-public", "rooms/201/", false)).hasSize(1);
    }

    @Test
    @DisplayName("Rejects path traversal in storage keys")
    void rejectsPathTraversalKeys() {
        S3CompatibleStorageAdapter adapter = createLocalAdapter();
        assertThatThrownBy(() -> adapter.putObject("cozynest-public", "../secret.txt", new byte[]{1}, "text/plain", false))
                .isInstanceOf(SecurityException.class);
    }
}
