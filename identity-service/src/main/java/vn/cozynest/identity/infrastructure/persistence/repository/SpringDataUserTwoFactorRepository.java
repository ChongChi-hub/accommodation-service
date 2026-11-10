package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserTwoFactorJpaEntity;

public interface SpringDataUserTwoFactorRepository extends JpaRepository<UserTwoFactorJpaEntity, String> {
}

