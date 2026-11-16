package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.MediaUpload;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MediaUploadRepository {
    MediaUpload save(MediaUpload upload);
    Optional<MediaUpload> findById(String id);
    Optional<MediaUpload> findByObjectKey(String objectKey);
    List<MediaUpload> findOrphanUploads(Instant cutoff);
    long countByStatus(String status);
}
