package vn.toam.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.identity.infrastructure.persistence.entity.LoginAttemptJpaEntity;

import java.time.Instant;

@Repository
public interface SpringDataLoginAttemptRepository extends JpaRepository<LoginAttemptJpaEntity, String> {
    long countByEmailAndSuccessFalseAndAttemptTimeAfter(String email, Instant after);
}
