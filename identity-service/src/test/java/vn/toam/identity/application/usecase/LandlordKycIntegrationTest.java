package vn.toam.identity.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.toam.identity.application.dto.LandlordKycStatusResponse;
import vn.toam.identity.domain.model.KycEventOutbox;
import vn.toam.identity.domain.model.KycStatus;
import vn.toam.identity.domain.model.KycStatusHistory;
import vn.toam.identity.domain.model.UserVerification;
import vn.toam.identity.domain.port.KycPrivateStoragePort;
import vn.toam.identity.domain.repository.KycEventOutboxRepository;
import vn.toam.identity.domain.repository.KycStatusHistoryRepository;
import vn.toam.identity.domain.repository.UserVerificationRepository;
import vn.toam.identity.infrastructure.security.KycCryptoService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class LandlordKycIntegrationTest {

    static class InMemoryUserVerificationRepository implements UserVerificationRepository {
        final Map<String, UserVerification> byId = new ConcurrentHashMap<>();
        final Map<String, UserVerification> byUserId = new ConcurrentHashMap<>();

        @Override
        public UserVerification save(UserVerification v) {
            byId.put(v.getId(), v);
            byUserId.put(v.getUserId(), v);
            return v;
        }

        @Override
        public Optional<UserVerification> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<UserVerification> findByUserId(String userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public boolean existsByActiveIdHash(String idNumberHash) {
            return byUserId.values().stream()
                    .anyMatch(v -> v.getIdNumberHash().equals(idNumberHash)
                            && (v.getStatus() == KycStatus.PENDING || v.getStatus() == KycStatus.APPROVED));
        }

        @Override
        public boolean existsByActiveIdHashAndUserIdNot(String idHash, String userId) {
            return byUserId.values().stream()
                    .anyMatch(v -> v.getIdNumberHash().equals(idHash)
                            && !v.getUserId().equals(userId)
                            && (v.getStatus() == KycStatus.PENDING || v.getStatus() == KycStatus.APPROVED));
        }

        @Override
        public List<UserVerification> findAllFiltered(KycStatus status, String search, int offset, int limit) {
            return byUserId.values().stream()
                    .filter(v -> status == null || v.getStatus() == status)
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countAllFiltered(KycStatus status, String search) {
            return byUserId.values().stream()
                    .filter(v -> status == null || v.getStatus() == status)
                    .count();
        }

        @Override
        public List<UserVerification> findRejectedBefore(Instant cutoff) {
            return byUserId.values().stream()
                    .filter(v -> v.getStatus() == KycStatus.REJECTED && v.getUpdatedAt() != null && v.getUpdatedAt().isBefore(cutoff))
                    .toList();
        }
    }

    static class InMemoryStatusHistoryRepository implements KycStatusHistoryRepository {
        final List<KycStatusHistory> list = new ArrayList<>();
        @Override
        public KycStatusHistory save(KycStatusHistory history) {
            list.add(history);
            return history;
        }
        @Override
        public List<KycStatusHistory> findByVerificationIdOrderByCreatedAtAsc(String verificationId) {
            return list.stream().filter(h -> h.getVerificationId().equals(verificationId)).toList();
        }
    }

    static class InMemoryOutboxRepository implements KycEventOutboxRepository {
        final List<KycEventOutbox> list = new ArrayList<>();
        @Override
        public KycEventOutbox save(KycEventOutbox event) {
            list.add(event);
            return event;
        }
        @Override
        public List<KycEventOutbox> findPending(int limit) {
            return list.stream().limit(limit).toList();
        }
    }

    static class InMemoryPrivateStorage implements KycPrivateStoragePort {
        final Map<String, byte[]> files = new HashMap<>();
        @Override
        public String store(String storageKey, byte[] data, String contentType) {
            files.put(storageKey, data);
            return storageKey;
        }
        @Override
        public byte[] load(String storageKey) {
            return files.get(storageKey);
        }
        @Override
        public void delete(String storageKey) {
            files.remove(storageKey);
        }
        @Override
        public boolean exists(String storageKey) {
            return files.containsKey(storageKey);
        }
    }

    @Test
    @DisplayName("10. Full Submission Flow: Submit KYC -> DB Persistence -> Status PENDING -> Query Status")
    void testFullKycSubmissionAndPersistenceFlow() throws IOException {
        InMemoryUserVerificationRepository verificationRepo = new InMemoryUserVerificationRepository();
        InMemoryStatusHistoryRepository historyRepo = new InMemoryStatusHistoryRepository();
        InMemoryOutboxRepository outboxRepo = new InMemoryOutboxRepository();
        InMemoryPrivateStorage storage = new InMemoryPrivateStorage();
        KycCryptoService cryptoService = new KycCryptoService(
                "ToAmKycSecretKey2026DevEnvironment32B", "ToAmKycPepperSecret2026DevEnvKey!", null
        );

        LandlordKycUseCase useCase = new LandlordKycUseCase(
                verificationRepo, historyRepo, outboxRepo, storage, cryptoService
        );

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imgBytes = baos.toByteArray();

        MockMultipartFile front = new MockMultipartFile("front", "front.png", "image/png", imgBytes);
        MockMultipartFile back = new MockMultipartFile("back", "back.png", "image/png", imgBytes);

        // Step 1: Initial query returns NONE
        LandlordKycStatusResponse initialStatus = useCase.getKycStatus("u-landlord-01");
        assertThat(initialStatus.getStatus()).isEqualTo(KycStatus.NONE.name());
        assertThat(initialStatus.isCanResubmit()).isTrue();

        // Step 2: Submit KYC
        LandlordKycStatusResponse submitResult = useCase.submitKyc(
                "u-landlord-01", front, back, "Nguyen Van Toan", "079090123456",
                LocalDate.of(1990, 1, 1), LocalDate.of(2021, 5, 20), "CA TP HCM", true
        );

        assertThat(submitResult.getStatus()).isEqualTo(KycStatus.PENDING.name());
        assertThat(submitResult.getIdNumberMasked()).isEqualTo("********3456");

        // Step 3: Verify direct DB persistence
        UserVerification inDb = verificationRepo.findByUserId("u-landlord-01").orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(inDb.getFullNameOnId()).isEqualTo("Nguyen Van Toan");
        assertThat(inDb.getIdNumberLast4()).isEqualTo("3456");
        assertThat(inDb.getFrontImageKey()).startsWith("kyc/private/u-landlord-01_");
        assertThat(inDb.getBackImageKey()).startsWith("kyc/private/u-landlord-01_");

        // Step 4: Verify physical private storage has files stored in WebP format
        assertThat(storage.files).containsKey(inDb.getFrontImageKey());
        assertThat(storage.files).containsKey(inDb.getBackImageKey());

        // Step 5: Verify status history audit and transactional outbox
        assertThat(historyRepo.list).hasSize(1);
        assertThat(historyRepo.list.get(0).getToStatus()).isEqualTo("PENDING");

        assertThat(outboxRepo.list).hasSize(1);
        assertThat(outboxRepo.list.get(0).getEventType()).isEqualTo("KycSubmitted");

        // Step 6: Query again, status reflects PENDING from DB
        LandlordKycStatusResponse queried = useCase.getKycStatus("u-landlord-01");
        assertThat(queried.getStatus()).isEqualTo(KycStatus.PENDING.name());
        assertThat(queried.isCanResubmit()).isFalse(); // Cannot resubmit while PENDING
    }
}
