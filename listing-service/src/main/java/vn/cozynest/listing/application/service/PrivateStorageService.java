package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.SignedDownloadTokenService;
import vn.cozynest.listing.domain.model.StorageAccessLog;
import vn.cozynest.listing.domain.repository.StorageAccessLogRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivateStorageService {

    public static final String OWNERSHIP_BUCKET = "ownership-docs";

    private final PrivateObjectStoragePort storagePort;
    private final SignedDownloadTokenService tokenService;
    private final StorageAccessLogRepository auditRepository;

    @Transactional
    public UploadResult uploadPrivateDoc(String actorId, String actorRole, Long landlordId, String filename, byte[] bytes, String contentType) {
        String ext = extractSafeExtension(filename);
        String objectKey = "landlords/" + landlordId + "/" + UUID.randomUUID() + ext;
        PrivateObjectStoragePort.StoredObjectMetadata meta = storagePort.storePrivate(OWNERSHIP_BUCKET, objectKey, bytes, contentType);
        audit(actorId, actorRole, OWNERSHIP_BUCKET, objectKey, "UPLOAD", true, "sha256=" + meta.sha256Hex());
        return new UploadResult(objectKey, meta.sha256Hex(), meta.sizeBytes(), meta.contentType());
    }

    @Transactional
    public String createSignedUrl(String actorId, String actorRole, Long landlordId, String objectKey, boolean privilegedRole) {
        if (!privilegedRole && (landlordId == null || !objectKey.startsWith("landlords/" + landlordId + "/"))) {
            audit(actorId, actorRole, OWNERSHIP_BUCKET, objectKey, "SIGN_URL", false, "IDOR_DENIED");
            throw new AccessDeniedException("Khong co quyen tao lien ket tai tep cua nguoi khac");
        }
        String token = tokenService.createToken(OWNERSHIP_BUCKET, objectKey, actorId, 120);
        audit(actorId, actorRole, OWNERSHIP_BUCKET, objectKey, "SIGN_URL", true, "ttl=120s");
        return "/api/v1/storage/private/download?token=" + token;
    }

    @Transactional
    public DownloadedFile downloadByToken(String actorId, String actorRole, boolean privilegedRole, String token) {
        SignedDownloadTokenService.VerifiedToken verified = tokenService.verify(token);
        if (!privilegedRole && verified.authorizedActorId() != null && !verified.authorizedActorId().isBlank()
                && !verified.authorizedActorId().equals(actorId)) {
            audit(actorId, actorRole, verified.bucket(), verified.objectKey(), "DOWNLOAD", false, "ACTOR_MISMATCH");
            throw new AccessDeniedException("Token tai xuong khong thuoc phien nguoi dung hien tai");
        }
        byte[] content = storagePort.loadPrivate(verified.bucket(), verified.objectKey());
        if (content == null) {
            audit(actorId, actorRole, verified.bucket(), verified.objectKey(), "DOWNLOAD", false, "NOT_FOUND");
            throw new IllegalArgumentException("Tep khong ton tai trong kho luu tru rieng");
        }
        audit(actorId, actorRole, verified.bucket(), verified.objectKey(), "DOWNLOAD", true, "OK");
        return new DownloadedFile(verified.objectKey(), content);
    }

    private void audit(String actorId, String actorRole, String bucket, String key, String action, boolean allowed, String reason) {
        auditRepository.save(StorageAccessLog.builder()
                .actorId(actorId != null ? actorId : "anonymous")
                .actorRole(actorRole != null ? actorRole : "UNKNOWN")
                .bucket(bucket)
                .objectKey(key)
                .action(action)
                .allowed(allowed)
                .reason(reason)
                .build());
    }

    private String extractSafeExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".bin";
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        return switch (ext) {
            case ".pdf", ".png", ".jpg", ".jpeg", ".webp" -> ext;
            default -> ".bin";
        };
    }

    public record UploadResult(String objectKey, String sha256, long sizeBytes, String contentType) {}
    public record DownloadedFile(String objectKey, byte[] bytes) {}
}

