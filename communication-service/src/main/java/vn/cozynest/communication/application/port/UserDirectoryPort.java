package vn.cozynest.communication.application.port;

import java.util.Optional;

public interface UserDirectoryPort {
    record UserSummary(String id, String email, String fullName, String phone) {}

    Optional<UserSummary> findUserById(String userId);
}
