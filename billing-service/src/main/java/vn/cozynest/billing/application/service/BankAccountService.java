package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.SaveBankAccountRequest;
import vn.cozynest.billing.application.dto.UserBankAccountDto;
import vn.cozynest.billing.domain.model.UserBankAccount;
import vn.cozynest.billing.domain.repository.UserBankAccountRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class BankAccountService {

    private final UserBankAccountRepository repository;

    public BankAccountService(UserBankAccountRepository repository) {
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
    public Optional<UserBankAccount> findDomainByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasValidBankAccount(String userId) {
        return findDomainByUserId(userId)
                .filter(UserBankAccount::isMatched)
                .filter(a -> a.getQrCodeUrl() != null && !a.getQrCodeUrl().isBlank())
                .isPresent();
    }

    @Transactional(readOnly = true)
    public UserBankAccountDto requireValidBankAccount(String userId, String roleLabel) {
        return findDomainByUserId(userId)
                .filter(UserBankAccount::isMatched)
                .filter(a -> a.getQrCodeUrl() != null && !a.getQrCodeUrl().isBlank())
                .map(this::toDto)
                .orElseThrow(() -> new IllegalStateException(
                        roleLabel + " bắt buộc phải cung cấp Số tài khoản ngân hàng (MATCHED) và Mã QR tại mục Tài khoản ngân hàng & QR để Sàn có thông tin giải ngân hoặc hoàn tiền."));
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

        String rawQr = req.qrCodeUrl();
        final String finalQrUrl = (rawQr == null || rawQr.isBlank())
                ? "https://img.vietqr.io/image/" + bankCode + "-" + accountNumber + "-compact.png?accountName=" + URLEncoder.encode(accountHolder, StandardCharsets.UTF_8)
                : rawQr;

        UserBankAccount domain = repository.findByUserId(userId)
                .map(existing -> {
                    existing.update(bankCode, bankName, accountNumber, accountHolder, finalQrUrl, "MATCHED");
                    return existing;
                })
                .orElseGet(() -> UserBankAccount.create(userId, bankCode, bankName, accountNumber, accountHolder, finalQrUrl));

        UserBankAccount saved = repository.save(domain);
        return toDto(saved);
    }

    private UserBankAccountDto toDto(UserBankAccount e) {
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
