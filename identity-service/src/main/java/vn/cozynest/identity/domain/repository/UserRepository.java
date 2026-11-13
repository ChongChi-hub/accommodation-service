package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    default List<User> findAll() {
        return java.util.Collections.emptyList();
    }
    User save(User user);
}

