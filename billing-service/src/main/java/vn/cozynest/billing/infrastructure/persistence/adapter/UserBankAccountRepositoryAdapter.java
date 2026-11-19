package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.UserBankAccount;
import vn.cozynest.billing.domain.repository.UserBankAccountRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UserBankAccountJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUserBankAccountRepository;

import java.util.Optional;

@Component
public class UserBankAccountRepositoryAdapter implements UserBankAccountRepository {
    private final SpringDataUserBankAccountRepository jpaRepository;

    public UserBankAccountRepositoryAdapter(SpringDataUserBankAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserBankAccount> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public UserBankAccount save(UserBankAccount d) {
        UserBankAccountJpaEntity e = UserBankAccountJpaEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .bankCode(d.getBankCode())
                .bankName(d.getBankName())
                .accountNumber(d.getAccountNumber())
                .accountHolder(d.getAccountHolder())
                .qrCodeUrl(d.getQrCodeUrl())
                .verificationStatus(d.getVerificationStatus() != null ? d.getVerificationStatus() : "MATCHED")
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
        return toDomain(jpaRepository.save(e));
    }

    private UserBankAccount toDomain(UserBankAccountJpaEntity e) {
        return new UserBankAccount(
                e.getId(),
                e.getUserId(),
                e.getBankCode(),
                e.getBankName(),
                e.getAccountNumber(),
                e.getAccountHolder(),
                e.getQrCodeUrl(),
                e.getVerificationStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

