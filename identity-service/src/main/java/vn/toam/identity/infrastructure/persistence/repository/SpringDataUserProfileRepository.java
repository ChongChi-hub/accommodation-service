package vn.toam.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.identity.infrastructure.persistence.entity.UserProfileJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileJpaEntity, String> {

    Optional<UserProfileJpaEntity> findByUserId(String userId);

    Optional<UserProfileJpaEntity> findByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, String userId);
}

