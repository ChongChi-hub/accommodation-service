package vn.toam.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.toam.listing.application.dto.ImageUploadResponse;
import vn.toam.listing.application.service.LandlordImageService;
import vn.toam.listing.domain.model.Room;
import vn.toam.listing.domain.model.RoomImage;
import vn.toam.listing.domain.port.ImageStoragePort;
import vn.toam.listing.domain.repository.RoomImageRepository;
import vn.toam.listing.domain.repository.RoomRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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

        byte[] bigBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("files", "huge.jpg", "image/jpeg", bigBytes);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("Dung lượng file vượt quá giới hạn 5MB");
    }

    @Test
    @DisplayName("Upload exceeding 10 images total is rejected")
    void testUploadExceedingMaxImagesRejected() throws IOException {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        when(roomImageRepository.countByRoomId(101L)).thenReturn(10L); // Already 10 images

        byte[] jpegBytes = createTestJpegBytes(100, 100);
        MockMultipartFile file = new MockMultipartFile("files", "photo11.jpg", "image/jpeg", jpegBytes);

        ImageUploadResponse response = imageService.uploadImages(1L, 101L, new MultipartFile[]{file}, "u-landlord-01");

        assertThat(response.getTotalUploaded()).isEqualTo(0);
        assertThat(response.getTotalFailed()).isEqualTo(1);
        assertThat(response.getItems().get(0).getErrorMessage()).contains("Phòng đã đạt giới hạn tối đa 10");
    }

    @Test
    @DisplayName("Set cover image: resets cover for room and sets new cover")
    void testSetCoverImage() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(mockRoom));
        RoomImage image = RoomImage.builder().id(202L).roomId(101L).build();
        when(roomImageRepository.findByIdAndRoomId(202L, 101L)).thenReturn(Optional.of(image));

        imageService.setCoverImage(1L, 101L, 202L);

        verify(roomImageRepository, times(1)).resetCoverForRoom(101L);
        verify(roomImageRepository, times(1)).setCoverForRoom(101L, 202L);
    }

    @Test
    @DisplayName("Delete cover image: auto-promotes next image with lowest sortOrder as cover")
    void testDeleteCoverAutoPromotesNextImage() {
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
}

