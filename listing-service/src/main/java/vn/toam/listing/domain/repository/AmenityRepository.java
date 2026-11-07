package vn.toam.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.listing.domain.model.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}

