package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.PhoneVerification;
import vn.cozynest.identity.domain.repository.PhoneVerificationRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.PhoneVerificationJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataPhoneVerificationRepository;

import java.time.Instant;
import java.util.Optional;

@Component
public class PhoneVerificationRepositoryAdapter implements PhoneVerificationRepository {
    private final SpringDataPhoneVerificationRepository repository;

    public PhoneVerificationRepositoryAdapter(SpringDataPhoneVerificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PhoneVerification save(PhoneVerification domain) {
        PhoneVerificationJpaEntity entity = PhoneVerificationJpaEntity.builder()
                .id(domain.getId())
                .phoneNumber(domain.getPhoneNumber())
                .otpHash(domain.getOtpHash())
                .attemptCount(domain.getAttemptCount())
                .verified(domain.isVerified())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
        PhoneVerificationJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PhoneVerification> findLatestByPhone(String phoneNumber) {
        return repository.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber).map(this::toDomain);
    }

    @Override
    public long countRecentByPhone(String phoneNumber, long sinceSeconds) {
        Instant since = Instant.now().minusSeconds(sinceSeconds);
        return repository.countRecent(phoneNumber, since);
    }

    private PhoneVerification toDomain(PhoneVerificationJpaEntity entity) {
        return new PhoneVerification(
                entity.getId(),
                entity.getPhoneNumber(),
                entity.getOtpHash(),
                entity.getAttemptCount(),
                entity.isVerified(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}

