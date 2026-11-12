package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Province;
import vn.cozynest.listing.domain.repository.ProvinceRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataProvinceRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProvinceRepositoryAdapter implements ProvinceRepository {

    private final SpringDataProvinceRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<Province> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Province> findAllByOrderByNameAsc() {
        return springRepo.findAllByOrderByNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Province> findBySlugIgnoreCase(String slug) {
        return springRepo.findBySlugIgnoreCase(slug).map(mapper::toDomain);
    }

    @Override
    public Optional<Province> findByCodeIgnoreCase(String code) {
        return springRepo.findByCodeIgnoreCase(code).map(mapper::toDomain);
    }
}

