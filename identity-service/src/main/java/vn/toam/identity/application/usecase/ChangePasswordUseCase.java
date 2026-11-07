package vn.toam.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.identity.application.dto.ChangePasswordCommand;
import vn.toam.identity.application.port.EmailServicePort;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.application.port.RateLimiterPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;

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

    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordEncoderPort passwordEncoder,
                                 RefreshTokenRepository refreshTokenRepository,
                                 EmailServicePort emailService,
                                 RateLimiterPort rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void execute(String userId, ChangePasswordCommand command) {
        String ip = command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1";
        rateLimiter.checkPasswordChangeAllowed(userId, ip);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay nguoi dung", "ERR_AUTH_USER_NOT_FOUND"));

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
