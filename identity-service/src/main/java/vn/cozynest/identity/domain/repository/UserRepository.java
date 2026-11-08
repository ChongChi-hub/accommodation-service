package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    User save(User user);
}

