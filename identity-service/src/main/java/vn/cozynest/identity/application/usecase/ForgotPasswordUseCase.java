package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.ForgotPasswordCommand;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataPasswordResetTokenRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final SpringDataPasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final EmailServicePort emailService;
    private final RateLimiterPort rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    public ForgotPasswordUseCase(
            UserRepository userRepository,
            SpringDataPasswordResetTokenRepository resetTokenRepository,
            PasswordEncoderPort passwordEncoder,
            EmailServicePort emailService,
            RateLimiterPort rateLimiter) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void execute(ForgotPasswordCommand command) {
        // Enforce rate limiting by email and IP address
        rateLimiter.checkForgotPasswordRateLimit(command.getEmail(), command.getIpAddress());

        // Fail fast if email service is disabled or unconfigured in production
        emailService.ensureAvailable();

        Optional<User> userOpt = userRepository.findByEmail(command.getEmail().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // Constant-time execution padding: execute dummy BCrypt hash so timing delta <= 15%
            passwordEncoder.encode("000000");
            // Anti-enumeration: Return silently without revealing user does not exist
            return;
        }

        User user = userOpt.get();
        resetTokenRepository.invalidateAllActiveTokens(user.getId());

        // Generate 6-digit OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otpCode);

        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .otpHash(otpHash)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attempts(0)
                .requestIp(command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1")
                .createdAt(Instant.now())
                .build();

        resetTokenRepository.save(entity);
        emailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), otpCode);
    }
}
