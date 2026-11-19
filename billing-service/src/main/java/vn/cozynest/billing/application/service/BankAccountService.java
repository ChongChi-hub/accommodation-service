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

    @Transactional(readOnly = true)
    public boolean hasValidBankAccount(String userId) {
        return getByUserId(userId)
                .filter(a -> a.bankCode() != null && !a.bankCode().isBlank()
                        && a.accountNumber() != null && !a.accountNumber().isBlank()
                        && a.accountHolder() != null && !a.accountHolder().isBlank()
                        && a.qrCodeUrl() != null && !a.qrCodeUrl().isBlank())
                .isPresent();
    }

    @Transactional(readOnly = true)
    public UserBankAccountDto requireValidBankAccount(String userId, String roleLabel) {
        return getByUserId(userId)
                .filter(a -> a.accountNumber() != null && !a.accountNumber().isBlank()
                        && a.qrCodeUrl() != null && !a.qrCodeUrl().isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        roleLabel + " bắt buộc phải cung cấp Số tài khoản ngân hàng và Mã QR tại mục Tài khoản ngân hàng & QR để Sàn có thông tin giải ngân hoặc hoàn tiền."));
    }

    @Transactional
    public UserBankAccountDto saveOrUpdate(String userId, SaveBankAccountRequest req) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID không được để trống");
        }
        if (req == null || req.bankCode() == null || req.bankCode().isBlank()
                || req.accountNumber() == null || req.accountNumber().isBlank()
                || req.accountHolder() == null || req.accountHolder().isBlank()) {
            throw new IllegalArgumentException("Bắt buộc phải cung cấp đầy đủ Ngân hàng, Số tài khoản, Chủ tài khoản và Mã QR");
        }
        String bankCode = req.bankCode().trim().toUpperCase();
        String accountNumber = req.accountNumber().trim().replaceAll("[^0-9a-zA-Z]", "");
        if (accountNumber.length() < 6 || accountNumber.length() > 25) {
            throw new IllegalArgumentException("Số tài khoản ngân hàng phải từ 6 đến 25 ký tự số/chữ");
        }
        String accountHolder = req.accountHolder().trim().toUpperCase();
        if (accountHolder.length() < 2) {
            throw new IllegalArgumentException("Tên chủ tài khoản không hợp lệ");
        }
        String bankName = req.bankName() != null && !req.bankName().isBlank() ? req.bankName().trim() : bankCode;

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

