package vn.toam.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.toam.listing.application.dto.ImageUploadResponse;
import vn.toam.listing.domain.model.Room;
import vn.toam.listing.domain.model.RoomImage;
import vn.toam.listing.domain.port.ImageStoragePort;
import vn.toam.listing.domain.repository.RoomImageRepository;
import vn.toam.listing.domain.repository.RoomRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LandlordImageService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final ImageStoragePort imageStoragePort;

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB
    public static final int MAX_ROOM_IMAGES = 10;
    public static final int MAX_DIMENSION = 8000;
    public static final int MAX_UPLOADS_PER_MINUTE = 30;

    private final Map<Long, Queue<Instant>> uploadTimestampsByLandlord = new ConcurrentHashMap<>();

    public void clearRateLimits() {
        uploadTimestampsByLandlord.clear();
    }

    private void checkUploadRateLimit(Long landlordId, int incomingCount) {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);
        Queue<Instant> timestamps = uploadTimestampsByLandlord.computeIfAbsent(landlordId, k -> new ConcurrentLinkedQueue<>());

        while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
            timestamps.poll();
        }

        if (timestamps.size() + incomingCount > MAX_UPLOADS_PER_MINUTE) {
            throw new IllegalStateException("Bạn đã vượt quá giới hạn tải ảnh lên (tối đa " + MAX_UPLOADS_PER_MINUTE + " ảnh/phút). Vui lòng thử lại sau.");
        }
        for (int i = 0; i < incomingCount; i++) {
            timestamps.add(now);
        }
    }

    public ImageUploadResponse uploadImages(Long landlordId, Long roomId, MultipartFile[] files, String actorId) {
        // Enforce per-user upload rate limiting
        checkUploadRateLimit(landlordId, files != null ? files.length : 0);

        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        long currentCount = roomImageRepository.countByRoomId(roomId);
        List<ImageUploadResponse.ImageUploadItemResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.jpg";

            // 1. Check max room images limit
            if (currentCount + successful >= MAX_ROOM_IMAGES) {
                results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                        .originalFilename(filename)
                        .success(false)
                        .errorMessage("Phòng đã đạt giới hạn tối đa " + MAX_ROOM_IMAGES + " hình ảnh")
                        .build());
                failed++;
                continue;
            }

            // 2. Check individual file size (< 5MB)
            if (file.getSize() > MAX_FILE_SIZE) {
                results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                        .originalFilename(filename)
                        .success(false)
                        .errorMessage("Dung lượng file vượt quá giới hạn 5MB (" + (file.getSize() / 1024 / 1024) + "MB)")
                        .build());
                failed++;
                continue;
            }

            try {
                byte[] rawBytes = file.getBytes();

                // 3. Inspect Magic Bytes
                if (!isValidImageMagicBytes(rawBytes)) {
                    results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                            .originalFilename(filename)
                            .success(false)
                            .errorMessage("Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, PNG, WebP)")
                            .build());
                    failed++;
                    continue;
                }

                // 4. Decode image & verify dimension bounds
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(rawBytes));
                if (original == null) {
                    results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                            .originalFilename(filename)
                            .success(false)
                            .errorMessage("Không thể giải mã dữ liệu hình ảnh")
                            .build());
                    failed++;
                    continue;
                }

                if (original.getWidth() > MAX_DIMENSION || original.getHeight() > MAX_DIMENSION) {
                    results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                            .originalFilename(filename)
                            .success(false)
                            .errorMessage("Kích thước ảnh quá lớn (vượt quá 8000x8000px)")
                            .build());
                    failed++;
                    continue;
                }

                // 5. Redraw image onto a fresh canvas to strip EXIF, GPS, and Polyglot code
                BufferedImage sanitized = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = sanitized.createGraphics();
                g.drawImage(original, 0, 0, Color.WHITE, null);
                g.dispose();

                // 6. Generate 3 WebP sizes
                byte[] largeBytes = resizeImage(sanitized, 1600);
                byte[] mediumBytes = resizeImage(sanitized, 960);
                byte[] thumbBytes = resizeImage(sanitized, 320);

                String fileUuid = UUID.randomUUID().toString();
                String largeKey = "rooms/" + roomId + "/" + fileUuid + "_large.webp";
                String mediumKey = "rooms/" + roomId + "/" + fileUuid + "_medium.webp";
                String thumbKey = "rooms/" + roomId + "/" + fileUuid + "_thumb.webp";

                imageStoragePort.store(largeKey, largeBytes, "image/webp");
                imageStoragePort.store(mediumKey, mediumBytes, "image/webp");
                imageStoragePort.store(thumbKey, thumbBytes, "image/webp");

                // If room currently has 0 images, first uploaded image defaults to cover
                boolean isCover = (currentCount + successful == 0);
                int sortOrder = (int) (currentCount + successful);

                RoomImage roomImage = RoomImage.builder()
                        .roomId(roomId)
                        .imageUrl("/api/v1/media/" + largeKey)
                        .storageKey(largeKey)
                        .largeKey(largeKey)
                        .mediumKey(mediumKey)
                        .thumbKey(thumbKey)
                        .originalFilename(filename)
                        .contentType("image/webp")
                        .sizeBytes((long) largeBytes.length)
                        .width(sanitized.getWidth())
                        .height(sanitized.getHeight())
                        .cover(isCover)
                        .primary(isCover)
                        .sortOrder(sortOrder)
                        .displayOrder(sortOrder)
                        .altText(room.getTitle())
                        .uploadedBy(actorId)
                        .createdAt(LocalDateTime.now())
                        .build();

                RoomImage saved = roomImageRepository.save(roomImage);
                successful++;

                results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                        .imageId(saved.getId())
                        .originalFilename(filename)
                        .imageUrl("/api/v1/media/" + largeKey)
                        .largeUrl("/api/v1/media/" + largeKey)
                        .mediumUrl("/api/v1/media/" + mediumKey)
                        .thumbUrl("/api/v1/media/" + thumbKey)
                        .isCover(isCover)
                        .sortOrder(sortOrder)
                        .success(true)
                        .build());

            } catch (Exception e) {
                log.error("Failed to process image {}", filename, e);
                results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                        .originalFilename(filename)
                        .success(false)
                        .errorMessage("Lỗi xử lý file: " + e.getMessage())
                        .build());
                failed++;
            }
        }

        return ImageUploadResponse.builder()
                .totalUploaded(successful)
                .totalFailed(failed)
                .items(results)
                .build();
    }

    public void setCoverImage(Long landlordId, Long roomId, Long imageId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        RoomImage targetImage = roomImageRepository.findByIdAndRoomId(imageId, roomId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ảnh #" + imageId + " thuộc phòng #" + roomId));

        // Atomic reset and set
        roomImageRepository.resetCoverForRoom(roomId);
        roomImageRepository.setCoverForRoom(roomId, imageId);
        log.info("Landlord {} set cover image {} for room {}", landlordId, imageId, roomId);
    }

    public void deleteImage(Long landlordId, Long roomId, Long imageId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        RoomImage image = roomImageRepository.findByIdAndRoomId(imageId, roomId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ảnh #" + imageId + " thuộc phòng #" + roomId));

        boolean wasCover = image.isCover();

        // Delete physical files
        if (image.getLargeKey() != null) imageStoragePort.delete(image.getLargeKey());
        if (image.getMediumKey() != null) imageStoragePort.delete(image.getMediumKey());
        if (image.getThumbKey() != null) imageStoragePort.delete(image.getThumbKey());
        if (image.getStorageKey() != null) imageStoragePort.delete(image.getStorageKey());

        roomImageRepository.delete(image);

        // If cover was deleted, automatically designate the next image as cover
        if (wasCover) {
            List<RoomImage> remaining = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
            if (!remaining.isEmpty()) {
                RoomImage newCover = remaining.get(0);
                roomImageRepository.setCoverForRoom(roomId, newCover.getId());
                log.info("Auto-assigned image {} as new cover for room {}", newCover.getId(), roomId);
            }
        }
    }

    public void reorderImages(Long landlordId, Long roomId, List<Long> imageIds) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        List<RoomImage> images = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
        Map<Long, RoomImage> map = new HashMap<>();
        for (RoomImage img : images) {
            map.put(img.getId(), img);
        }

        for (int i = 0; i < imageIds.size(); i++) {
            Long imgId = imageIds.get(i);
            RoomImage img = map.get(imgId);
            if (img != null) {
                img.setSortOrder(i);
                img.setDisplayOrder(i);
                roomImageRepository.save(img);
            }
        }
        log.info("Landlord {} reordered images for room {}", landlordId, roomId);
    }

    private boolean isValidImageMagicBytes(byte[] bytes) {
        return vn.toam.common.image.ImageProcessingHelper.isValidImageMagicBytes(bytes);
    }

    private byte[] resizeImage(BufferedImage src, int targetWidth) throws IOException {
        int width = Math.min(src.getWidth(), targetWidth);
        int height = Math.max(1, (int) Math.round((double) src.getHeight() * width / src.getWidth()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Thumbnails.of(src)
                    .size(width, height)
                    .outputFormat("webp")
                    .outputQuality(0.85)
                    .toOutputStream(baos);
        } catch (Exception e) {
            baos.reset();
            Thumbnails.of(src)
                    .size(width, height)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(baos);
        }

        return baos.toByteArray();
    }
}

