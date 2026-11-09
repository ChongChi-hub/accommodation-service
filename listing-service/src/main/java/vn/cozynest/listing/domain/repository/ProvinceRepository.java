package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Province;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {
    List<Province> findAllByOrderByNameAsc();
    java.util.Optional<Province> findBySlugIgnoreCase(String slug);
    java.util.Optional<Province> findByCodeIgnoreCase(String code);
}

