package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.District;
import vn.cozynest.listing.domain.repository.DistrictRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataDistrictRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DistrictRepositoryAdapter implements DistrictRepository {

    private final SpringDataDistrictRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<District> findByProvinceCodeOrderByNameAsc(String provinceCode) {
        return springRepo.findByProvinceCodeOrderByNameAsc(provinceCode).stream().map(mapper::toDomain).toList();
    }
}

