package vn.cozynest.identity.application.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.application.service.UserPresenceService;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProviderPort tokenProvider;
    private final UserPresenceService userPresenceService;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, TokenProviderPort tokenProvider) {
        this(refreshTokenRepository, tokenProvider, null);
    }

    @Autowired
    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository,
                         TokenProviderPort tokenProvider,
                         @Autowired(required = false) UserPresenceService userPresenceService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.userPresenceService = userPresenceService;
    }

    @Transactional
    public void execute(String userId, String rawRefreshToken) {
        String effectiveUserId = userId;
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken);
            var optToken = refreshTokenRepository.findByTokenHash(tokenHash);
            if (optToken.isPresent()) {
                var token = optToken.get();
                token.revoke("LOGOUT");
                refreshTokenRepository.save(token);
                if (effectiveUserId == null || effectiveUserId.isBlank()) {
                    effectiveUserId = token.getUserId();
                }
            }
        }
        if (effectiveUserId != null && !effectiveUserId.isBlank()) {
            refreshTokenRepository.revokeAllByUserId(effectiveUserId);
            if (userPresenceService != null) {
                userPresenceService.recordOffline(effectiveUserId);
            }
        }
    }
}
