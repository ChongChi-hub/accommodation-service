package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.SaveBankAccountRequest;
import vn.cozynest.billing.application.dto.UserBankAccountDto;
import vn.cozynest.billing.infrastructure.persistence.entity.UserBankAccountJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUserBankAccountRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankAccountService {

    private final SpringDataUserBankAccountRepository repository;

    public BankAccountService(SpringDataUserBankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UserBankAccountDto> getByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUserId(userId).map(this::toDto);
    }

    @Transactional
    public UserBankAccountDto saveOrUpdate(String userId, SaveBankAccountRequest req) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID khong duoc de trong");
        }
        String bankCode = req.bankCode().trim().toUpperCase();
        String accountNumber = req.accountNumber().trim().replaceAll("[^0-9a-zA-Z]", "");
        String accountHolder = req.accountHolder().trim().toUpperCase();
        String bankName = req.bankName().trim();

        String qrUrl = req.qrCodeUrl();
        if (qrUrl == null || qrUrl.isBlank()) {
            String encodedName = URLEncoder.encode(accountHolder, StandardCharsets.UTF_8);
            qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + accountNumber + "-compact.png?accountName=" + encodedName;
        }

        Instant now = Instant.now();
        UserBankAccountJpaEntity entity = repository.findByUserId(userId)
                .orElse(UserBankAccountJpaEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .createdAt(now)
                        .build());

        entity.setBankCode(bankCode);
        entity.setBankName(bankName);
        entity.setAccountNumber(accountNumber);
        entity.setAccountHolder(accountHolder);
        entity.setQrCodeUrl(qrUrl);
        entity.setUpdatedAt(now);

        UserBankAccountJpaEntity saved = repository.save(entity);
        return toDto(saved);
    }

    private UserBankAccountDto toDto(UserBankAccountJpaEntity e) {
        return new UserBankAccountDto(
                e.getId(),
                e.getUserId(),
                e.getBankCode(),
                e.getBankName(),
                e.getAccountNumber(),
                e.getAccountHolder(),
                e.getQrCodeUrl(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

