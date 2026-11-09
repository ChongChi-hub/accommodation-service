package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.ResendVerifyEmailCommand;
import vn.cozynest.identity.application.dto.VerifyEmailCommand;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataEmailVerificationTokenRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final SpringDataEmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final EmailServicePort emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerifyEmailUseCase(
            UserRepository userRepository,
            SpringDataEmailVerificationTokenRepository verificationTokenRepository,
            PasswordEncoderPort passwordEncoder,
            EmailServicePort emailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationOtp(String email) {
        emailService.ensureAvailable();
        if (email == null || email.isBlank()) {
            passwordEncoder.encode("000000");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            passwordEncoder.encode("000000");
            return; // Anti-enumeration + timing equalization
        }

        User user = userOpt.get();
        if (user.getEmailVerifiedAt() != null) {
            return; // Already verified
        }

        // Check 60s cooldown
        Optional<EmailVerificationTokenJpaEntity> latestOpt = verificationTokenRepository.findLatestActiveTokenByUserId(user.getId());
        if (latestOpt.isPresent()) {
            EmailVerificationTokenJpaEntity latest = latestOpt.get();
            if (latest.getCreatedAt().isAfter(Instant.now().minus(60, ChronoUnit.SECONDS))) {
                throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu gửi lại mã xác thực", "ERR_RATE_LIMIT_EXCEEDED");
            }
        }

        verificationTokenRepository.invalidateAllActiveTokens(user.getId());

        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otpCode);

        EmailVerificationTokenJpaEntity token = EmailVerificationTokenJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .tokenHash(otpHash)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build();
        verificationTokenRepository.save(token);

        emailService.sendEmailVerificationOtp(user.getEmail(), user.getFullName(), otpCode);
    }

    @Transactional
    public void verifyEmail(VerifyEmailCommand command) {
        String normalizedEmail = command.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IdentityDomainException("Mã xác thực không hợp lệ hoặc đã hết hạn", "ERR_AUTH_INVALID_OTP"));

        if (user.getEmailVerifiedAt() != null) {
            return; // Already verified
        }

        EmailVerificationTokenJpaEntity token = verificationTokenRepository.findLatestActiveTokenByUserId(user.getId())
                .orElseThrow(() -> new IdentityDomainException("Mã xác thực không hợp lệ hoặc đã hết hạn", "ERR_AUTH_INVALID_OTP"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IdentityDomainException("Mã xác thực đã hết hạn (chỉ có hiệu lực trong 10 phút)", "ERR_AUTH_OTP_EXPIRED");
        }

        if (!passwordEncoder.matches(command.getOtp(), token.getTokenHash())) {
            throw new IdentityDomainException("Mã xác thực OTP không chính xác", "ERR_AUTH_INVALID_OTP");
        }

        token.setVerifiedAt(Instant.now());
        verificationTokenRepository.save(token);

        user.verifyEmailNow();
        userRepository.save(user);
    }
}

