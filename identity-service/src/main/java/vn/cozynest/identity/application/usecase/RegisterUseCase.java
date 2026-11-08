package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RegisterCommand;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RegisterUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public RegisterUseCase(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoderPort passwordEncoder,
                           TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResult execute(RegisterCommand command) {
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IdentityDomainException("Email đã tồn tại trên hệ thống", "ERR_AUTH_EMAIL_EXISTS");
        }
        if (userRepository.existsByPhone(command.getPhone())) {
            throw new IdentityDomainException("Số điện thoại đã tồn tại trên hệ thống", "ERR_AUTH_PHONE_EXISTS");
        }

        String passwordHash = passwordEncoder.encode(command.getPassword());
        UserRoleType role = UserRoleType.fromString(command.getRole());
        if (role != UserRoleType.TENANT && role != UserRoleType.LANDLORD) {
            throw new IdentityDomainException("Không được phép tự đăng ký vai trò này", "ERR_AUTH_FORBIDDEN_ROLE");
        }
        User newUser = User.createNew(command.getEmail(), command.getPhone(), passwordHash, command.getFullName(), role);
        User savedUser = userRepository.save(newUser);

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.create(
                savedUser.getId(),
                tokenHash,
                "Web",
                "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(savedUser))
                .build();
    }
}

