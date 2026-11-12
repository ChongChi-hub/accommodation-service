package vn.cozynest.identity.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.cozynest.identity.domain.port.AvatarStoragePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarMediaControllerTest {

    @Test
    @DisplayName("GET /uploads/avatars/default.png serves valid PNG bytes even before files exist on disk")
    void shouldServeDefaultAvatarPngFallback() {
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        when(storage.load("avatars/default.png")).thenReturn(null);

        AvatarMediaController controller = new AvatarMediaController(storage);
        ResponseEntity<byte[]> response = controller.getAvatar("default.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("GET stored avatar resolves WebP content and aliases legacy seeded filename")
    void shouldServeStoredAndAliasedAvatar() {
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        byte[] webpBytes = new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4};
        when(storage.load("avatars/tenant1.jpg")).thenReturn(null);
        when(storage.load("avatars/u-tenant-01.webp")).thenReturn(webpBytes);

        AvatarMediaController controller = new AvatarMediaController(storage);
        ResponseEntity<byte[]> response = controller.getAvatar("tenant1.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("image/webp");
        assertThat(response.getBody()).isEqualTo(webpBytes);
    }

    @Test
    @DisplayName("Unknown filename returns 404 and path traversal returns 400")
    void shouldRejectTraversalAndMissingAvatar() {
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        AvatarMediaController controller = new AvatarMediaController(storage);

        assertThat(controller.getAvatar("nonexistent-xyz.webp").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.getAvatar("..secret.png").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

