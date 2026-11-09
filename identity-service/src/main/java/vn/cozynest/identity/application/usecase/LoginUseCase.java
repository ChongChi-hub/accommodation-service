package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.application.dto.LoginCommand;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.port.LoginAttemptPort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserStatus;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final LoginAttemptPort loginAttemptPort;
    private final vn.cozynest.identity.application.port.RateLimiterPort rateLimiter;
    private final String dummyBcryptHash;

    public LoginUseCase(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        LandlordProfileRepository landlordProfileRepository,
                        PasswordEncoderPort passwordEncoder,
                        TokenProviderPort tokenProvider,
                        LoginAttemptPort loginAttemptPort,
                        vn.cozynest.identity.application.port.RateLimiterPort rateLimiter) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.landlordProfileRepository = landlordProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptPort = loginAttemptPort;
        this.rateLimiter = rateLimiter;
        this.dummyBcryptHash = passwordEncoder.encode("cozynest_dummy_constant_timing_password_protection");
    }

    @Transactional
    public AuthResult execute(LoginCommand command) {
        rateLimiter.checkLoginRateLimit(command.getIpAddress());

        if (command.getPassword() != null && command.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            passwordEncoder.matches("invalid", dummyBcryptHash);
            loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
            throw new IdentityDomainException("Email hoặc mật khẩu không chính xác", "ERR_AUTH_INVALID_CREDENTIALS");
        }

        if (loginAttemptPort.isBlocked(command.getEmail(), command.getIpAddress())) {
            throw new IdentityDomainException("Tài khoản hoặc IP của bạn đã bị tạm khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau 15 phút.", "ERR_AUTH_TEMPORARY_LOCKED");
        }

        String rawIdentifier = command.getEmail() != null ? command.getEmail().trim() : "";
        Optional<User> userOpt;
        if (rawIdentifier.contains("@")) {
            userOpt = userRepository.findByEmail(rawIdentifier.toLowerCase());
        } else {
            String normalizedPhone = normalizePhone(rawIdentifier);
            userOpt = normalizedPhone != null
                    ? userRepository.findByPhone(normalizedPhone).or(() -> userRepository.findByPhone(rawIdentifier))
                    : Optional.empty();
        }
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(command.getPassword(), dummyBcryptHash);
            loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
            throw new IdentityDomainException("Email hoặc mật khẩu không chính xác", "ERR_AUTH_INVALID_CREDENTIALS");
        }

        User user = userOpt.get();

        if (user.getPasswordHash() == null) {
            passwordEncoder.matches(command.getPassword(), dummyBcryptHash);
            loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
            throw new IdentityDomainException("Tài khoản này được đăng ký qua Google. Vui lòng đăng nhập bằng Google.", "ERR_AUTH_OAUTH_ACCOUNT");
        }

        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
            throw new IdentityDomainException("Email hoặc mật khẩu không chính xác", "ERR_AUTH_INVALID_CREDENTIALS");
        }

        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.changePassword(passwordEncoder.encode(command.getPassword()));
            userRepository.save(user);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IdentityDomainException("Tài khoản của bạn đã bị khóa bởi quản trị viên", "ERR_AUTH_USER_LOCKED");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IdentityDomainException("Tài khoản của bạn đang bị đình chỉ hoạt động", "ERR_USER_SUSPENDED");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IdentityDomainException("Tài khoản đã bị xóa khỏi hệ thống", "ERR_USER_DELETED");
        }

        loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), true);

        user.recordLogin();
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.create(
                user.getId(),
                tokenHash,
                command.getDeviceInfo() != null ? command.getDeviceInfo() : "Web",
                command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        LandlordProfile lp = landlordProfileRepository.findByUserId(user.getId()).orElse(null);

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(user, lp))
                .roles(user.getRoles().stream().map(Enum::name).toList())
                .emailVerified(user.getEmailVerifiedAt() != null)
                .landlordApplication(LandlordApplicationSummaryDto.fromDomain(lp))
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String cleaned = phone.replaceAll("[\\s\\-\\.]", "");
        if (cleaned.startsWith("0")) {
            return "+84" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            return "+84" + cleaned;
        }
        return cleaned;
    }
}
