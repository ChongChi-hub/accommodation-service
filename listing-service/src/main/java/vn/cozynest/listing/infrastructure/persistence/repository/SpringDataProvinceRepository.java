package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.ProvinceJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataProvinceRepository extends JpaRepository<ProvinceJpaEntity, Long> {
    List<ProvinceJpaEntity> findAllByOrderByNameAsc();
    Optional<ProvinceJpaEntity> findBySlugIgnoreCase(String slug);
    Optional<ProvinceJpaEntity> findByCodeIgnoreCase(String code);
}
