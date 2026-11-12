package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Province;

import java.util.List;
import java.util.Optional;

public interface ProvinceRepository {
    List<Province> findAll();
    List<Province> findAllByOrderByNameAsc();
    Optional<Province> findBySlugIgnoreCase(String slug);
    Optional<Province> findByCodeIgnoreCase(String code);
}
