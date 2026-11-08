package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RefreshTokenCommand;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProviderPort tokenProvider;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                              UserRepository userRepository,
                              TokenProviderPort tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResult execute(RefreshTokenCommand command) {
        String tokenHash = tokenProvider.hashToken(command.getRefreshToken());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IdentityDomainException("Refresh token không hợp lệ", "ERR_AUTH_INVALID_TOKEN"));

        if (!oldToken.isValid()) {
            refreshTokenRepository.revokeAllByUserId(oldToken.getUserId());
            throw new IdentityDomainException("Refresh token đã hết hạn hoặc bị thu hồi", "ERR_AUTH_TOKEN_EXPIRED");
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new IdentityDomainException("Người dùng không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));

        // Rotation
        String newRawRefreshToken = tokenProvider.generateRefreshToken();
        String newTokenHash = tokenProvider.hashToken(newRawRefreshToken);

        oldToken.revoke(newTokenHash);
        refreshTokenRepository.save(oldToken);

        RefreshToken newToken = RefreshToken.create(
                user.getId(),
                newTokenHash,
                command.getDeviceInfo() != null ? command.getDeviceInfo() : oldToken.getDeviceInfo(),
                command.getIpAddress() != null ? command.getIpAddress() : oldToken.getIpAddress(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(newToken);

        String newAccessToken = tokenProvider.generateAccessToken(user);

        return AuthResult.builder()
                .token(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .user(UserDto.fromDomain(user))
                .build();
    }
}

