package vn.cozynest.listing.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.common.image.ImageProcessingHelper;
import vn.cozynest.listing.application.dto.ImageUploadResponse;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.model.RoomStatusHistory;
import vn.cozynest.listing.domain.port.ImageStoragePort;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.domain.repository.RoomStatusHistoryRepository;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@Transactional
public class LandlordImageService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final ImageStoragePort imageStoragePort;
    private final UploadRateLimiter rateLimiter;
    private final Optional<RoomStatusHistoryRepository> roomStatusHistoryRepository;

    public LandlordImageService(
            RoomRepository roomRepository,
            RoomImageRepository roomImageRepository,
            ImageStoragePort imageStoragePort,
            UploadRateLimiter rateLimiter
    ) {
        this(roomRepository, roomImageRepository, imageStoragePort, rateLimiter, Optional.empty());
    }

    @Autowired
    public LandlordImageService(
            RoomRepository roomRepository,
            RoomImageRepository roomImageRepository,
            ImageStoragePort imageStoragePort,
            UploadRateLimiter rateLimiter,
            Optional<RoomStatusHistoryRepository> roomStatusHistoryRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomImageRepository = roomImageRepository;
        this.imageStoragePort = imageStoragePort;
        this.rateLimiter = rateLimiter;
        this.roomStatusHistoryRepository = roomStatusHistoryRepository != null
                ? roomStatusHistoryRepository
                : Optional.empty();
    }

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB
    public static final int MAX_ROOM_IMAGES = 10;
    public static final int MAX_DIMENSION = 8000;

    public ImageUploadResponse uploadImages(Long landlordId, Long roomId, MultipartFile[] files, String actorId) {
        // Enforce per-user upload rate limiting
        rateLimiter.checkRateLimit(landlordId, files != null ? files.length : 0);

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
                results.add(ImageUploadResponse.ImageUploadItemResult.builder().originalFilename(filename).success(false).errorMessage("Phòng đã đạt giới hạn tối đa " + MAX_ROOM_IMAGES + " hình ảnh").build());
                failed++;
                continue;
            }

            // 2. Check individual file size (< 5MB)
            if (file.getSize() > MAX_FILE_SIZE) {
                results.add(ImageUploadResponse.ImageUploadItemResult.builder().originalFilename(filename).success(false).errorMessage("Dung lượng file vượt quá giới hạn 5MB (" + (file.getSize() / 1024 / 1024) + "MB)").build());
                failed++;
                continue;
            }

            try {
                byte[] rawBytes = file.getBytes();

                // 3. Inspect Magic Bytes
                if (!ImageProcessingHelper.isValidImageMagicBytes(rawBytes)) {
                    results.add(ImageUploadResponse.ImageUploadItemResult.builder().originalFilename(filename).success(false).errorMessage("Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, PNG, WebP)").build());
                    failed++;
                    continue;
                }

                // 4. Decode and sanitize (EXIF strip, bounds check)
                BufferedImage sanitized = vn.cozynest.common.image.ImageProcessingHelper.decodeAndSanitize(rawBytes);
                if (sanitized.getWidth() > MAX_DIMENSION || sanitized.getHeight() > MAX_DIMENSION) {
                    results.add(ImageUploadResponse.ImageUploadItemResult.builder()
                            .originalFilename(filename).success(false)
                            .errorMessage("Kích thước ảnh quá lớn (vượt quá 8000x8000px)")
                            .build());
                    failed++;
                    continue;
                }

                // 5. Generate 3 WebP sizes
                byte[] largeBytes = vn.cozynest.common.image.ImageProcessingHelper.resizeBoundingBoxWebP(sanitized, 1600, 0.85f);
                byte[] mediumBytes = vn.cozynest.common.image.ImageProcessingHelper.resizeBoundingBoxWebP(sanitized, 960, 0.85f);
                byte[] thumbBytes = vn.cozynest.common.image.ImageProcessingHelper.resizeBoundingBoxWebP(sanitized, 320, 0.85f);

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
        resetRoomApprovalIfPublished(room, landlordId, "Thay doi anh bia phong dang xuat ban - can duyet lai");
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

        // If cover was deleted, automatically designate the next image as cover and require re-review if published
        if (wasCover) {
            List<RoomImage> remaining = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
            if (!remaining.isEmpty()) {
                RoomImage newCover = remaining.get(0);
                roomImageRepository.setCoverForRoom(roomId, newCover.getId());
                log.info("Auto-assigned image {} as new cover for room {}", newCover.getId(), roomId);
            }
            resetRoomApprovalIfPublished(room, landlordId, "Xoa anh bia phong dang xuat ban - can duyet lai");
        }
    }

    private void resetRoomApprovalIfPublished(Room room, Long landlordId, String reason) {
        String prevStatus = room.getStatus();
        if (room.isApproved() || "PUBLISHED".equalsIgnoreCase(prevStatus)) {
            room.setStatus("PENDING_REVIEW");
            room.setApproved(false);
            room.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(room);
            roomStatusHistoryRepository.ifPresent(repo -> repo.save(RoomStatusHistory.builder()
                    .roomId(room.getId())
                    .fromStatus(prevStatus)
                    .toStatus("PENDING_REVIEW")
                    .actorId(String.valueOf(landlordId))
                    .actorRole("LANDLORD")
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build()));
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
}

