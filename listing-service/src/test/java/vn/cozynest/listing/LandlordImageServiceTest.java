package vn.cozynest.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.listing.application.dto.ImageUploadResponse;
import vn.cozynest.listing.application.service.LandlordImageService;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.port.ImageStoragePort;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandlordImageServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomImageRepository roomImageRepository;

    @Mock
    private ImageStoragePort imageStoragePort;
    @Mock
    private vn.cozynest.listing.application.service.UploadRateLimiter rateLimiter;
    @InjectMocks
    private LandlordImageService imageService;

    private Room mockRoom;

    @BeforeEach
    void setup() {
        mockRoom = Room.builder()
                .id(101L)
                .landlordId(1L)
                .title("Phòng Demo")
                .build();
    }

    private byte[] createTestJpegBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Upload valid JPEG image: resized, 3 sizes generated, defaults to cover if first image")
    void testUploadValidImage() throws IOException {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(0L);
        when(roomImageRepository.save(any(RoomImage.class))).thenAnswer(i -> {
            RoomImage img = i.getArgument(0);
            img.setId(501L);
            return img;
        });

        byte[] jpegBytes = createTestJpegBytes(800, 600);
        MockMultipartFile file = new MockMultipartFile("files", "living_room.jpg", "image/jpeg", jpegBytes);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(1);
        assertThat(response.getTotalFailed()).isEqualTo(0);
        assertThat(response.getItems().get(0).isCover()).isTrue();
        verify(imageStoragePort, times(3)).store(anyString(), any(byte[].class), eq("image/webp"));
    }

    @Test
    @DisplayName("Upload non-image file (GIF or TXT) is rejected by magic bytes check")
    void testUploadInvalidMagicBytesRejected() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile("files", "script.sh.jpg", "image/jpeg", "echo 'not an image'".getBytes());

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("Định dạng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Upload file > 5MB is rejected")
    void testUploadOver5MBRejected() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(0L);

        byte[] bigBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("files", "huge.jpg", "image/jpeg", bigBytes);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("vượt quá dung lượng");
    }

    @Test
    @DisplayName("Delete cover image promotes next image in sort order to cover")
    void testDeleteCoverPromotesNext() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));

        RoomImage coverImg = RoomImage.builder()
                .id(201L)
                .roomId(101L)
                .cover(true)
                .storageKey("rooms/101/201.webp")
                .build();

        RoomImage nextImg = RoomImage.builder()
                .id(202L)
                .roomId(101L)
                .cover(false)
                .sortOrder(1)
                .build();

        when(roomImageRepository.findByIdAndRoomId(201L, 101L)).thenReturn(Optional.of(coverImg));
        when(roomImageRepository.findByRoomIdOrderBySortOrderAsc(101L)).thenReturn(List.of(nextImg));

        imageService.deleteImage(1L, 101L, 201L);

        verify(imageStoragePort, times(1)).delete("rooms/101/201.webp");
        verify(roomImageRepository, times(1)).delete(coverImg);
        verify(roomImageRepository, times(1)).setCoverForRoom(101L, 202L);
    }

    @Test
    @DisplayName("Upload GIF format is rejected because only JPG, PNG, WebP are permitted")
    void testUploadGifRejected() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(0L);

        byte[] gifBytes = "GIF89a sample content".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", "sample.gif", "image/gif", gifBytes);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("Định dạng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Upload spoofed extension with payload is rejected by magic byte inspection")
    void testUploadSpoofedExtensionRejected() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(0L);

        byte[] payload = "plain text fake script payload".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", "shell.php.jpg", "image/jpeg", payload);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("Định dạng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Upload rate limiting locks out user when exceeding 30 uploads per minute")
    void testUploadRateLimitingLockout() {
        doThrow(new vn.cozynest.listing.domain.exception.RateLimitExceededException("Rate limit", "ERR_UPLOAD_RATE_LIMIT"))
                .when(rateLimiter).checkRateLimit(eq(1L), eq(31));

        MultipartFile[] batch = new MultipartFile[31];
        for (int i = 0; i < 31; i++) {
            batch[i] = new MockMultipartFile("files", "img_" + i + ".jpg", "image/jpeg", new byte[10]);
        }

        org.junit.jupiter.api.Assertions.assertThrows(
                vn.cozynest.listing.domain.exception.RateLimitExceededException.class,
                () -> imageService.uploadImages(1L, 101L, batch, "u-landlord-01"));
    }

    @Test
    @DisplayName("Image IDOR: Landlord B cannot upload to, set cover for, or delete images from Landlord A's room")
    void testImageEndpointIdorPrevention() {
        Long attackerLandlordId = 999L;
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, attackerLandlordId))
                .thenReturn(Optional.empty());

        // 1. Upload IDOR
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, () ->
                imageService.uploadImages(attackerLandlordId, 101L, new MultipartFile[]{}, "u-attacker"));

        // 2. Set Cover IDOR
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, () ->
                imageService.setCoverImage(attackerLandlordId, 101L, 202L));

        // 3. Delete IDOR
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, () ->
                imageService.deleteImage(attackerLandlordId, 101L, 202L));

        // 4. Reorder IDOR
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, () ->
                imageService.reorderImages(attackerLandlordId, 101L, List.of(201L, 202L)));
    }
}
