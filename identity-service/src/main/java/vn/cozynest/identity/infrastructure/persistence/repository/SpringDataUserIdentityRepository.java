package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserIdentityJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataUserIdentityRepository extends JpaRepository<UserIdentityJpaEntity, String> {
    Optional<UserIdentityJpaEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
    Optional<UserIdentityJpaEntity> findByUserIdAndProvider(String userId, String provider);
    List<UserIdentityJpaEntity> findAllByUserId(String userId);
    void deleteByUserIdAndProvider(String userId, String provider);
    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
    int countByUserId(String userId);
}

