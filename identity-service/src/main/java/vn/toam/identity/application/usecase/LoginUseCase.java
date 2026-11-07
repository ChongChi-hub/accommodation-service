package vn.toam.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.identity.application.dto.AuthResult;
import vn.toam.identity.application.dto.LoginCommand;
import vn.toam.identity.application.dto.UserDto;
import vn.toam.identity.application.port.LoginAttemptPort;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.application.port.TokenProviderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.RefreshToken;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserStatus;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final LoginAttemptPort loginAttemptPort;

    public LoginUseCase(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoderPort passwordEncoder,
                        TokenProviderPort tokenProvider,
                        LoginAttemptPort loginAttemptPort) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptPort = loginAttemptPort;
    }

    @Transactional
    public AuthResult execute(LoginCommand command) {
        if (loginAttemptPort.isBlocked(command.getEmail(), command.getIpAddress())) {
            throw new IdentityDomainException("Tài khoản hoặc địa chỉ IP đã bị tạm khóa do nhập sai mật khẩu nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.", "ERR_AUTH_TEMPORARY_LOCKED");
        }

        User user = userRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> {
                    loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
                    return new IdentityDomainException("Email hoặc mật khẩu không chính xác", "ERR_AUTH_INVALID_CREDENTIALS");
                });

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IdentityDomainException("Tài khoản của bạn đã bị khóa", "ERR_AUTH_ACCOUNT_LOCKED");
        }

        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            loginAttemptPort.recordAttempt(command.getEmail(), command.getIpAddress(), false);
            throw new IdentityDomainException("Email hoặc mật khẩu không chính xác", "ERR_AUTH_INVALID_CREDENTIALS");
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

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(user))
                .build();
    }
}

