package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.billing.infrastructure.persistence.entity.UserBankAccountJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataUserBankAccountRepository extends JpaRepository<UserBankAccountJpaEntity, String> {
    Optional<UserBankAccountJpaEntity> findByUserId(String userId);
}

