package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.UserProfile;

import java.util.Optional;

public interface UserProfileRepository {

    Optional<UserProfile> findById(String id);

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, String userId);

    UserProfile save(UserProfile userProfile);
}

