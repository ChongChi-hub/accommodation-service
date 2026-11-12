package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProviderPort tokenProvider;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, TokenProviderPort tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public void execute(String userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke("LOGOUT");
                refreshTokenRepository.save(token);
            });
            return;
        }
        if (userId != null && !userId.isBlank()) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }
}

