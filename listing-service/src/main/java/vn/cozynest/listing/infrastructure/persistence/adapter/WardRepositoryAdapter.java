package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Ward;
import vn.cozynest.listing.domain.repository.WardRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataWardRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WardRepositoryAdapter implements WardRepository {

    private final SpringDataWardRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<Ward> findByProvinceCodeOrderByNameAsc(String provinceCode) {
        return springRepo.findByProvinceCodeOrderByNameAsc(provinceCode).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ward> findByDistrictCodeOrderByNameAsc(String districtCode) {
        return springRepo.findByDistrictCodeOrderByNameAsc(districtCode).stream().map(mapper::toDomain).toList();
    }
}

