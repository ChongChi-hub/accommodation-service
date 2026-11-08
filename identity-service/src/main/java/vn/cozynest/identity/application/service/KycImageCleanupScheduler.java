package vn.cozynest.identity.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class KycImageCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(KycImageCleanupScheduler.class);

    private final UserVerificationRepository verificationRepository;
    private final KycPrivateStoragePort kycStorage;
    private final boolean cleanupEnabled;
    private final int retentionDays;

    public KycImageCleanupScheduler(
            UserVerificationRepository verificationRepository,
            KycPrivateStoragePort kycStorage,
            @Value("${app.kyc.cleanup-enabled:false}") boolean cleanupEnabled,
            @Value("${app.kyc.retention-days:30}") int retentionDays) {
        this.verificationRepository = verificationRepository;
        this.kycStorage = kycStorage;
        this.cleanupEnabled = cleanupEnabled;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.kyc.cleanup-cron:0 0 2 * * *}") // Run at 2 AM every day
    @Transactional
    public void cleanupRejectedKycImages() {
        if (!cleanupEnabled) {
            log.info("KYC image cleanup scheduler is disabled by configuration.");
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        log.info("Scanning for rejected KYC documents older than {} days (cutoff: {})...", retentionDays, cutoff);

        List<UserVerification> expiredVerifications = verificationRepository.findRejectedBefore(cutoff);
        int purgedCount = 0;

        for (UserVerification v : expiredVerifications) {
            if ("PURGED".equals(v.getFrontImageKey()) && "PURGED".equals(v.getBackImageKey())) {
                continue;
            }
            if (v.getFrontImageKey() != null && !"PURGED".equals(v.getFrontImageKey())) {
                kycStorage.delete(v.getFrontImageKey());
            }
            if (v.getBackImageKey() != null && !"PURGED".equals(v.getBackImageKey())) {
                kycStorage.delete(v.getBackImageKey());
            }
            v.purgeImages();
            verificationRepository.save(v);
            purgedCount++;
        }

        if (purgedCount > 0) {
            log.info("Successfully purged images for {} expired rejected KYC documents.", purgedCount);
        }
    }
}

