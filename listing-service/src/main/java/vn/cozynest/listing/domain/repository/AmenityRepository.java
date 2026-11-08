package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}

