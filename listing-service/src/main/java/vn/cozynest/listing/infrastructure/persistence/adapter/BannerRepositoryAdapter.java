package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Banner;
import vn.cozynest.listing.domain.repository.BannerRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataBannerRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BannerRepositoryAdapter implements BannerRepository {

    private final SpringDataBannerRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<Banner> findByActiveTrueOrderBySortOrderAsc() {
        return springRepo.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Banner> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Banner save(Banner banner) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(banner)));
    }
}

