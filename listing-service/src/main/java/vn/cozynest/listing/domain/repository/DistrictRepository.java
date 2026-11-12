package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.District;

import java.util.List;

public interface DistrictRepository {
    List<District> findByProvinceCodeOrderByNameAsc(String provinceCode);
}
