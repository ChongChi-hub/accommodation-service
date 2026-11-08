package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.LoginAttemptJpaEntity;

import java.time.Instant;

@Repository
public interface SpringDataLoginAttemptRepository extends JpaRepository<LoginAttemptJpaEntity, String> {

    long countByEmailAndSuccessFalseAndAttemptTimeAfter(String email, Instant after);

    long countByIpAddressAndSuccessFalseAndAttemptTimeAfter(String ipAddress, Instant after);

    long countByEmailAndIpAddressAndSuccessFalseAndAttemptTimeAfter(String email, String ipAddress, Instant after);
}
