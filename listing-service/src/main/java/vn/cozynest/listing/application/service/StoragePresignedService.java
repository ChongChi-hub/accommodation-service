package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.common.image.ImageProcessingHelper;
import vn.cozynest.common.storage.FileMagicBytesValidator;
import vn.cozynest.common.storage.ObjectStoragePort;
import vn.cozynest.listing.application.dto.PresignedUploadRequestDto;
import vn.cozynest.listing.application.dto.PresignedUploadResponseDto;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.domain.repository.MediaUploadRepository;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoragePresignedService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;   // 5 MB
    private static final long MAX_DOC_BYTES = 10L * 1024 * 1024;    // 10 MB
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    private final ObjectStoragePort objectStoragePort;
    private final MediaUploadRepository uploadRepository;

    @Transactional
    public PresignedUploadResponseDto requestPresignedUpload(PresignedUploadRequestDto dto, String uploaderId) {
        validateUploadRequest(dto);
        boolean isPrivate = isPrivateCategory(dto.category());
        String bucket = isPrivate ? ObjectStoragePort.PRIVATE_BUCKET_DEFAULT : ObjectStoragePort.PUBLIC_BUCKET_DEFAULT;

        String ext = resolveExtension(dto.filename(), dto.contentType());
        String target = (dto.targetId() != null && !dto.targetId().isBlank()) ? dto.targetId() : "misc";
        String objectKey = dto.category().toLowerCase() + "/" + target + "/" + UUID.randomUUID() + ext;

        long ttlSeconds = 900; // 15 mins
        ObjectStoragePort.PresignedUploadResponse presigned = objectStoragePort.generatePresignedUploadUrl(
                bucket, objectKey, dto.contentType(), dto.sizeBytes(), ttlSeconds, isPrivate
        );

        MediaUpload upload = MediaUpload.createPending(
                objectKey, bucket, dto.category().toUpperCase(), dto.contentType(), dto.sizeBytes(), uploaderId
        );
        MediaUpload saved = uploadRepository.save(upload);

        return new PresignedUploadResponseDto(
                saved.getId(), objectKey, bucket, presigned.method(),
                presigned.uploadUrl(), presigned.publicCdnUrl(), presigned.expiresAt(), isPrivate
        );
    }

    @Transactional
    public MediaUpload confirmUpload(String uploadId, String actorId, byte[] directBytes) {
        MediaUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy yêu cầu tải lên: " + uploadId));

        boolean isPrivate = isPrivateCategory(upload.getCategory());
        if (directBytes != null && directBytes.length > 0) {
            String detectedMime = FileMagicBytesValidator.detectAndValidate(directBytes, null, MAX_DOC_BYTES);
            byte[] finalBytes = directBytes;
            String finalMime = detectedMime;

            if (detectedMime.startsWith("image/") && !detectedMime.equals("image/webp")) {
                try {
                    BufferedImage bImg = javax.imageio.ImageIO.read(new ByteArrayInputStream(directBytes));
                    if (bImg != null) {
                        byte[] webp = ImageProcessingHelper.resizeToWebP(bImg, Math.min(bImg.getWidth(), 2560), Math.min(bImg.getHeight(), 2560), 0.85f);
                        if (webp != null && webp.length > 0) {
                            finalBytes = webp;
                            finalMime = "image/webp";
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            objectStoragePort.putObject(upload.getBucket(), upload.getObjectKey(), finalBytes, finalMime, isPrivate);
            upload.confirm(finalBytes.length, finalMime);
        } else {
            if (!objectStoragePort.exists(upload.getBucket(), upload.getObjectKey(), isPrivate)) {
                throw new IllegalStateException("Tệp chưa được tải lên Object Storage: " + upload.getObjectKey());
            }
            upload.confirm(upload.getSizeBytes(), upload.getContentType());
        }

        return uploadRepository.save(upload);
    }

    @Transactional
    public void attachEntity(String uploadId, String entityType, String entityId) {
        MediaUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy yêu cầu tải lên: " + uploadId));
        upload.attach(entityType, entityId);
        uploadRepository.save(upload);
    }

    public String generatePresignedDownloadUrl(String objectKey, long ttlSeconds) {
        MediaUpload upload = uploadRepository.findByObjectKey(objectKey)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tệp: " + objectKey));
        boolean isPrivate = isPrivateCategory(upload.getCategory());
        return objectStoragePort.generatePresignedDownloadUrl(upload.getBucket(), upload.getObjectKey(), ttlSeconds, isPrivate);
    }

    private void validateUploadRequest(PresignedUploadRequestDto dto) {
        if (!ALLOWED_MIME.contains(dto.contentType().toLowerCase())) {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ (chỉ chấp nhận JPEG, PNG, WEBP, PDF)");
        }
        long max = isPrivateCategory(dto.category()) ? MAX_DOC_BYTES : MAX_IMAGE_BYTES;
        if (dto.sizeBytes() > max) {
            throw new IllegalArgumentException("Kích thước tệp vượt quá giới hạn tối đa cho phép (" + (max / 1024 / 1024) + "MB)");
        }
    }

    private boolean isPrivateCategory(String category) {
        if (category == null) return false;
        String upper = category.trim().toUpperCase();
        return upper.contains("CONTRACT") || upper.contains("OWNERSHIP") || upper.contains("KYC") || upper.contains("INVOICE");
    }

    private String resolveExtension(String filename, String contentType) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) return ".pdf";
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            if (ext.matches("^\\.[a-z0-9]{3,4}$")) return ext;
        }
        return ".webp";
    }
}
