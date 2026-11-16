package vn.cozynest.listing.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.common.storage.ObjectStoragePort;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.domain.repository.MediaUploadRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrphanMediaCleanupSchedulerTest {

    @Test
    @DisplayName("Deletes orphan uploads older than 24h unattached to any entity and reclaims bytes")
    void cleansUpOrphanUploadsOlderThan24Hours() {
        ObjectStoragePort storagePort = mock(ObjectStoragePort.class);
        MediaUploadRepository repo = mock(MediaUploadRepository.class);

        MediaUpload orphan1 = new MediaUpload(
                "up-1", "rooms/misc/abandoned1.webp", "cozynest-public", "ROOM_IMAGE",
                "image/webp", 200_000L, "PENDING_UPLOAD", "usr-1",
                null, null, Instant.now().minus(25, ChronoUnit.HOURS), null, null
        );

        MediaUpload orphan2 = new MediaUpload(
                "up-2", "contracts/misc/abandoned2.pdf", "cozynest-private", "CONTRACT",
                "application/pdf", 500_000L, "CONFIRMED", "usr-2",
                null, null, Instant.now().minus(30, ChronoUnit.HOURS), null, null
        );

        when(repo.findOrphanUploads(any(Instant.class))).thenReturn(List.of(orphan1, orphan2));

        OrphanMediaCleanupScheduler scheduler = new OrphanMediaCleanupScheduler(storagePort, repo);
        OrphanMediaCleanupScheduler.CleanupReport report = scheduler.runDailyOrphanCleanup();

        assertThat(report.deletedFilesCount()).isEqualTo(2);
        assertThat(report.reclaimedBytes()).isEqualTo(700_000L);

        verify(storagePort).deleteObject(eq("cozynest-public"), eq("rooms/misc/abandoned1.webp"), eq(false));
        verify(storagePort).deleteObject(eq("cozynest-private"), eq("contracts/misc/abandoned2.pdf"), eq(true));

        assertThat(orphan1.getStatus()).isEqualTo("ORPHANED_DELETED");
        assertThat(orphan2.getStatus()).isEqualTo("ORPHANED_DELETED");

        verify(repo).save(orphan1);
        verify(repo).save(orphan2);
    }
}
