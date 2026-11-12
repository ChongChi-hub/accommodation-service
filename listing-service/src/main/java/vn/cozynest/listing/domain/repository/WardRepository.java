package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Ward;

import java.util.List;

public interface WardRepository {
    List<Ward> findByProvinceCodeOrderByNameAsc(String provinceCode);
    List<Ward> findByDistrictCodeOrderByNameAsc(String districtCode);
}
