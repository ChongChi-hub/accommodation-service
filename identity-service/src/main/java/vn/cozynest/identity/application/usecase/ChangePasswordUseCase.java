package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.ChangePasswordCommand;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataPasswordResetTokenRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChangePasswordUseCase {

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailServicePort emailService;
    private final RateLimiterPort rateLimiter;
    private final SpringDataPasswordResetTokenRepository resetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordEncoderPort passwordEncoder,
                                 RefreshTokenRepository refreshTokenRepository,
                                 EmailServicePort emailService,
                                 RateLimiterPort rateLimiter) {
        this(userRepository, passwordEncoder, refreshTokenRepository, emailService, rateLimiter, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordEncoderPort passwordEncoder,
                                 RefreshTokenRepository refreshTokenRepository,
                                 EmailServicePort emailService,
                                 RateLimiterPort rateLimiter,
                                 SpringDataPasswordResetTokenRepository resetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.resetTokenRepository = resetTokenRepository;
    }

    @Transactional
    public void sendOtp(String userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy người dùng", "ERR_AUTH_USER_NOT_FOUND"));

        // Check 60s cooldown
        Optional<PasswordResetTokenJpaEntity> latestOpt = resetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId);
        if (latestOpt.isPresent()) {
            PasswordResetTokenJpaEntity latest = latestOpt.get();
            if (latest.getCreatedAt().isAfter(Instant.now().minus(60, ChronoUnit.SECONDS))) {
                throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu gửi lại mã xác thực", "ERR_RATE_LIMIT_EXCEEDED");
            }
        }

        resetTokenRepository.invalidateAllActiveTokens(userId);

        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otpCode);

        PasswordResetTokenJpaEntity token = PasswordResetTokenJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .otpHash(otpHash)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attempts(0)
                .requestIp(ipAddress != null ? ipAddress : "127.0.0.1")
                .createdAt(Instant.now())
                .build();

        resetTokenRepository.save(token);
        emailService.sendChangePasswordOtp(user.getEmail(), user.getFullName(), otpCode);
    }

    @Transactional
    public void execute(String userId, ChangePasswordCommand command) {
        String ip = command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1";
        rateLimiter.checkPasswordChangeAllowed(userId, ip);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay nguoi dung", "ERR_AUTH_USER_NOT_FOUND"));

        // Verify OTP if present / configured
        PasswordResetTokenJpaEntity token = null;
        if (resetTokenRepository != null && command.getOtp() != null && !command.getOtp().isBlank()) {
            token = resetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId)
                    .orElseThrow(() -> new IdentityDomainException("Mã xác thực không hợp lệ hoặc chưa được yêu cầu", "ERR_AUTH_INVALID_OTP"));

            if (token.getExpiresAt().isBefore(Instant.now())) {
                throw new IdentityDomainException("Mã xác thực đã hết hạn (chỉ có hiệu lực trong 10 phút)", "ERR_AUTH_OTP_EXPIRED");
            }

            if (token.getAttempts() >= 5) {
                token.setUsedAt(Instant.now());
                resetTokenRepository.save(token);
                throw new IdentityDomainException("Mã xác thực đã bị khóa do nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.", "ERR_AUTH_OTP_MAX_ATTEMPTS");
            }

            if (!passwordEncoder.matches(command.getOtp().trim(), token.getOtpHash())) {
                int newAttempts = token.getAttempts() + 1;
                token.setAttempts(newAttempts);
                resetTokenRepository.save(token);
                int remaining = 5 - newAttempts;
                if (remaining <= 0) {
                    token.setUsedAt(Instant.now());
                    resetTokenRepository.save(token);
                    throw new IdentityDomainException("Mã xác thực đã bị khóa do nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.", "ERR_AUTH_OTP_MAX_ATTEMPTS");
                }
                throw new IdentityDomainException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.", "ERR_AUTH_INVALID_OTP");
            }
        } else if (resetTokenRepository != null && (command.getOtp() == null || command.getOtp().isBlank())) {
            throw new IdentityDomainException("Vui lòng nhập mã xác thực OTP 6 số đã được gửi đến email của bạn", "ERR_AUTH_OTP_REQUIRED");
        }

        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPasswordHash())) {
            rateLimiter.recordFailedPasswordChange(userId, ip);
            throw new IdentityDomainException("Mat khau hien tai khong chinh xac", "ERR_AUTH_INVALID_PASSWORD");
        }

        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            throw new IdentityDomainException("Mat khau moi khong duoc trung voi mat khau hien tai", "ERR_PASSWORD_IDENTICAL");
        }

        if (!STRONG_PASSWORD_PATTERN.matcher(command.getNewPassword()).matches()) {
            throw new IdentityDomainException(
                    "Mat khau moi phai co it nhat 8 ky tu, gom chu hoa, chu thuong, so va ky tu dac biet",
                    "ERR_PASSWORD_TOO_WEAK"
            );
        }

        // Mark OTP token as used
        if (token != null && resetTokenRepository != null) {
            token.setUsedAt(Instant.now());
            resetTokenRepository.save(token);
        }

        user.changePassword(passwordEncoder.encode(command.getNewPassword()));
        userRepository.save(user);

        // Reset rate limit counter on success
        rateLimiter.clearPasswordChangeAttempts(userId, ip);

        // Revoke all other refresh token sessions while keeping current session
        refreshTokenRepository.revokeAllExceptByUserId(user.getId(), command.getCurrentTokenHash());

        // Notify user via email
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getFullName());
    }
}
