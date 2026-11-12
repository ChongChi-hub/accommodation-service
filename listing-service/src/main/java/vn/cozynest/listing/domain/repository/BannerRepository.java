package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Banner;

import java.util.List;

public interface BannerRepository {
    List<Banner> findByActiveTrueOrderBySortOrderAsc();
    List<Banner> findAll();
    Banner save(Banner banner);
}
