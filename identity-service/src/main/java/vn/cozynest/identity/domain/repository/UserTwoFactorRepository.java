package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.UserTwoFactor;

import java.util.Optional;

public interface UserTwoFactorRepository {
    UserTwoFactor save(UserTwoFactor twoFactor);
    Optional<UserTwoFactor> findByUserId(String userId);
    void deleteByUserId(String userId);
}

