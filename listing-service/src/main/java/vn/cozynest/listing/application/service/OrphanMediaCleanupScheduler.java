package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.common.storage.ObjectStoragePort;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.domain.repository.MediaUploadRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Orphan Media Cleanup Scheduler (STORAGE-01).
 * Periodically deletes abandoned uploads older than 24h unattached to any room/chat/invoice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanMediaCleanupScheduler {

    private final ObjectStoragePort objectStoragePort;
    private final MediaUploadRepository uploadRepository;

    @Value("${storage.cleanup.enabled:true}")
    private boolean cleanupEnabled = true;

    @Scheduled(cron = "${storage.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public CleanupReport runDailyOrphanCleanup() {
        return cleanupOrphansOlderThan(Duration.ofHours(24));
    }

    @Transactional
    public CleanupReport cleanupOrphansOlderThan(Duration maxAge) {
        if (!cleanupEnabled) {
            log.info("Orphan media cleanup is disabled via configuration");
            return new CleanupReport(0, 0L);
        }

        Instant cutoff = Instant.now().minus(maxAge.toMillis(), ChronoUnit.MILLIS);
        List<MediaUpload> orphans = uploadRepository.findOrphanUploads(cutoff);
        log.info("Found {} candidate orphan uploads created before {}", orphans.size(), cutoff);

        int deletedCount = 0;
        long bytesReclaimed = 0L;

        for (MediaUpload orphan : orphans) {
            try {
                boolean isPrivate = isPrivateCategory(orphan.getCategory());
                objectStoragePort.deleteObject(orphan.getBucket(), orphan.getObjectKey(), isPrivate);
                orphan.markOrphanedDeleted();
                uploadRepository.save(orphan);

                deletedCount++;
                bytesReclaimed += orphan.getSizeBytes();
            } catch (Exception ex) {
                log.warn("Failed to cleanup orphan object key={}: {}", orphan.getObjectKey(), ex.getMessage());
            }
        }

        log.info("Orphan media cleanup completed: deleted {} files, reclaimed {} bytes", deletedCount, bytesReclaimed);
        return new CleanupReport(deletedCount, bytesReclaimed);
    }

    private boolean isPrivateCategory(String category) {
        if (category == null) return false;
        String upper = category.trim().toUpperCase();
        return upper.contains("CONTRACT") || upper.contains("OWNERSHIP") || upper.contains("KYC") || upper.contains("INVOICE");
    }

    public record CleanupReport(int deletedFilesCount, long reclaimedBytes) {
    }
}
