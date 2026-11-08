package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.RoleJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, String> {
    Optional<RoleJpaEntity> findByName(String name);
}

