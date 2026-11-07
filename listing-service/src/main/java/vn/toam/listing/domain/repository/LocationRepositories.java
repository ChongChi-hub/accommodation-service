package vn.toam.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.listing.domain.model.Province;
import vn.toam.listing.domain.model.District;
import vn.toam.listing.domain.model.Ward;

import java.util.List;

public interface LocationRepositories {

    @Repository
    interface ProvinceRepository extends JpaRepository<Province, Long> {
        List<Province> findAllByOrderByNameAsc();
    }

    @Repository
    interface DistrictRepository extends JpaRepository<District, Long> {
        List<District> findByProvinceCodeOrderByNameAsc(String provinceCode);
    }

    @Repository
    interface WardRepository extends JpaRepository<Ward, Long> {
        List<Ward> findByDistrictCodeOrderByNameAsc(String districtCode);
    }
}

