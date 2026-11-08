package vn.cozynest.identity.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.repository.FaceVerificationFrameRepository;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class FaceCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FaceCleanupScheduler.class);

    private final FaceVerificationRepository faceRepository;
    private final FaceVerificationFrameRepository frameRepository;
    private final FacePrivateStoragePort storagePort;
    private final boolean cleanupEnabled;
    private final int retentionDays;

    public FaceCleanupScheduler(
            FaceVerificationRepository faceRepository,
            FaceVerificationFrameRepository frameRepository,
            FacePrivateStoragePort storagePort,
            @Value("${face.cleanup.enabled:false}") boolean cleanupEnabled,
            @Value("${face.retention.days:30}") int retentionDays) {
        this.faceRepository = faceRepository;
        this.frameRepository = frameRepository;
        this.storagePort = storagePort;
        this.cleanupEnabled = cleanupEnabled;
        this.retentionDays = retentionDays > 0 ? retentionDays : 30;
    }

    @Scheduled(cron = "${face.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupExpiredFaceFrames() {
        if (!cleanupEnabled) {
            log.debug("Face frames automated privacy cleanup is disabled by configuration (face.cleanup.enabled=false)");
            return;
        }

        Instant purgeCutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        List<FaceVerificationStatus> decidedStatuses = List.of(FaceVerificationStatus.PASSED, FaceVerificationStatus.FAILED);

        List<FaceVerification> eligible = faceRepository.findEligibleForPurge(purgeCutoff, decidedStatuses);
        int purgedCount = 0;

        for (FaceVerification v : eligible) {
            List<FaceVerificationFrame> frames = frameRepository.findActiveByFaceVerificationId(v.getId());
            for (FaceVerificationFrame f : frames) {
                storagePort.delete(f.getStorageKey());
                f.markDeleted();
                frameRepository.save(f);
                purgedCount++;
            }
        }

        if (purgedCount > 0) {
            log.info("Face cleanup completed: Purged {} encrypted biometric frames older than {} days.", purgedCount, retentionDays);
        }
    }
}

