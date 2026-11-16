package vn.cozynest.listing.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.common.storage.ObjectStoragePort;
import vn.cozynest.listing.application.dto.PresignedUploadRequestDto;
import vn.cozynest.listing.application.dto.PresignedUploadResponseDto;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.domain.repository.MediaUploadRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StoragePresignedServiceTest {

    @Test
    @DisplayName("Rejects upload requests with unsupported mime types or exceeding size limit")
    void rejectsInvalidMimeOrSize() {
        ObjectStoragePort storagePort = mock(ObjectStoragePort.class);
        MediaUploadRepository repo = mock(MediaUploadRepository.class);
        StoragePresignedService service = new StoragePresignedService(storagePort, repo);

        PresignedUploadRequestDto badMime = new PresignedUploadRequestDto("script.sh", "application/x-sh", 500, "ROOM_IMAGE", "101");
        assertThatThrownBy(() -> service.requestPresignedUpload(badMime, "usr-1"))
                .isInstanceOf(IllegalArgumentException.class);

        PresignedUploadRequestDto oversized = new PresignedUploadRequestDto("huge.png", "image/png", 20L * 1024 * 1024, "ROOM_IMAGE", "101");
        assertThatThrownBy(() -> service.requestPresignedUpload(oversized, "usr-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Generates presigned upload URL and records pending upload in media_uploads")
    void requestsPresignedUploadSuccessfully() {
        ObjectStoragePort storagePort = mock(ObjectStoragePort.class);
        MediaUploadRepository repo = mock(MediaUploadRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        StoragePresignedService service = new StoragePresignedService(storagePort, repo);

        when(storagePort.generatePresignedUploadUrl(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(new ObjectStoragePort.PresignedUploadResponse(
                        "https://s3.example.com/upload", "rooms/101/uuid.webp", "cozynest-public", "PUT",
                        Instant.now().plusSeconds(900), "https://cdn.cozynest.vn/rooms/101/uuid.webp", false
                ));

        PresignedUploadRequestDto req = new PresignedUploadRequestDto("photo.jpg", "image/jpeg", 1024 * 500, "ROOM_IMAGE", "101");
        PresignedUploadResponseDto resp = service.requestPresignedUpload(req, "landlord-1");

        assertThat(resp.uploadUrl()).isEqualTo("https://s3.example.com/upload");
        assertThat(resp.bucket()).isEqualTo("cozynest-public");
        assertThat(resp.publicCdnUrl()).isEqualTo("https://cdn.cozynest.vn/rooms/101/uuid.webp");
        verify(repo).save(argThat(upload -> "PENDING_UPLOAD".equals(upload.getStatus()) && "ROOM_IMAGE".equals(upload.getCategory())));
    }

    @Test
    @DisplayName("Confirms upload, validates magic bytes and converts image to WebP")
    void confirmsUploadAndConvertsToWebp() {
        ObjectStoragePort storagePort = mock(ObjectStoragePort.class);
        MediaUploadRepository repo = mock(MediaUploadRepository.class);
        StoragePresignedService service = new StoragePresignedService(storagePort, repo);

        MediaUpload upload = new MediaUpload(
                "up-100", "rooms/101/test.webp", "cozynest-public", "ROOM_IMAGE",
                "image/jpeg", 1024, "PENDING_UPLOAD", "landlord-1",
                null, null, Instant.now(), null, null
        );
        when(repo.findById("up-100")).thenReturn(Optional.of(upload));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Valid WebP bytes
        byte[] validWebp = ("RIFF\0\0\0\0WEBPVP8 ").getBytes(StandardCharsets.US_ASCII);
        MediaUpload confirmed = service.confirmUpload("up-100", "landlord-1", validWebp);

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        verify(storagePort).putObject(eq("cozynest-public"), eq("rooms/101/test.webp"), any(), eq("image/webp"), eq(false));
    }
}
