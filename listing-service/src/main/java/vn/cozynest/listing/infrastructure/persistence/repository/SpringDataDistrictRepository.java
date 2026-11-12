package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.DistrictJpaEntity;

import java.util.List;

public interface SpringDataDistrictRepository extends JpaRepository<DistrictJpaEntity, Long> {
    List<DistrictJpaEntity> findByProvinceCodeOrderByNameAsc(String provinceCode);
}
