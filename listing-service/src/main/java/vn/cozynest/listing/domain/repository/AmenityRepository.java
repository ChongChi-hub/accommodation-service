package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Amenity;

import java.util.List;
import java.util.Optional;

public interface AmenityRepository {
    List<Amenity> findAll();
    List<Amenity> findAllById(Iterable<Long> ids);
    Optional<Amenity> findById(Long id);
    Amenity save(Amenity amenity);
}
