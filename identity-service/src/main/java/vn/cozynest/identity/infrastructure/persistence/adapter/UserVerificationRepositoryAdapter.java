package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserVerificationJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserVerificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class UserVerificationRepositoryAdapter implements UserVerificationRepository {

    private final SpringDataUserVerificationRepository springRepo;

    public UserVerificationRepositoryAdapter(SpringDataUserVerificationRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<UserVerification> findById(String id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<UserVerification> findByUserId(String userId) {
        return springRepo.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public boolean existsByActiveIdHash(String idNumberHash) {
        return springRepo.existsByStatusInAndIdNumberHash(Set.of(KycStatus.PENDING, KycStatus.APPROVED), idNumberHash);
    }

    @Override
    public boolean existsByActiveIdHashAndUserIdNot(String idNumberHash, String userId) {
        return springRepo.existsByStatusInAndIdNumberHashAndUserIdNot(Set.of(KycStatus.PENDING, KycStatus.APPROVED), idNumberHash, userId);
    }

    @Override
    public List<UserVerification> findAllFiltered(KycStatus status, String search, int offset, int limit) {
        int page = offset / Math.max(1, limit);
        PageRequest pageRequest = PageRequest.of(page, limit);
        return springRepo.findFiltered(status, search, pageRequest).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countAllFiltered(KycStatus status, String search) {
        return springRepo.countFiltered(status, search);
    }

    @Override
    public List<UserVerification> findRejectedBefore(Instant cutoff) {
        return springRepo.findByStatusAndUpdatedAtBefore(KycStatus.REJECTED, cutoff).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public UserVerification save(UserVerification verification) {
        UserVerificationJpaEntity entity = toEntity(verification);
        UserVerificationJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private UserVerification toDomain(UserVerificationJpaEntity entity) {
        return new UserVerification(
                entity.getId(),
                entity.getUserId(),
                entity.getFullNameOnId(),
                entity.getDateOfBirth(),
                entity.getIdNumberEnc(),
                entity.getIdNumberHash(),
                entity.getIdNumberLast4(),
                entity.getIssueDate(),
                entity.getIssuePlace(),
                entity.getFrontImageKey(),
                entity.getBackImageKey(),
                entity.getSelfieKey(),
                entity.getStatus(),
                entity.getConsentAt(),
                entity.getConsentPolicyVersion(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy(),
                entity.getRejectReasonCode(),
                entity.getRejectReasonNote(),
                entity.getAttemptCount(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserVerificationJpaEntity toEntity(UserVerification domain) {
        UserVerificationJpaEntity entity = new UserVerificationJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setFullNameOnId(domain.getFullNameOnId());
        entity.setDateOfBirth(domain.getDateOfBirth());
        entity.setIdNumberEnc(domain.getIdNumberEnc());
        entity.setIdNumberHash(domain.getIdNumberHash());
        entity.setIdNumberLast4(domain.getIdNumberLast4());
        entity.setIssueDate(domain.getIssueDate());
        entity.setIssuePlace(domain.getIssuePlace());
        entity.setFrontImageKey(domain.getFrontImageKey());
        entity.setBackImageKey(domain.getBackImageKey());
        entity.setSelfieKey(domain.getSelfieKey());
        entity.setStatus(domain.getStatus());
        entity.setConsentAt(domain.getConsentAt());
        entity.setConsentPolicyVersion(domain.getConsentPolicyVersion());
        entity.setSubmittedAt(domain.getSubmittedAt());
        entity.setReviewedAt(domain.getReviewedAt());
        entity.setReviewedBy(domain.getReviewedBy());
        entity.setRejectReasonCode(domain.getRejectReasonCode());
        entity.setRejectReasonNote(domain.getRejectReasonNote());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

