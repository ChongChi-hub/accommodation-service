package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Ward;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {
    List<Ward> findByProvinceCodeOrderByNameAsc(String provinceCode);
    List<Ward> findByDistrictCodeOrderByNameAsc(String districtCode);
}

