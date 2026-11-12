package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.WardJpaEntity;

import java.util.List;

public interface SpringDataWardRepository extends JpaRepository<WardJpaEntity, Long> {
    List<WardJpaEntity> findByProvinceCodeOrderByNameAsc(String provinceCode);
    List<WardJpaEntity> findByDistrictCodeOrderByNameAsc(String districtCode);
}

