package vn.toam.identity.domain.repository;

import vn.toam.identity.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken refreshToken);
    void revokeAllByUserId(String userId);
    void revokeAllExceptByUserId(String userId, String keepTokenHash);
}

