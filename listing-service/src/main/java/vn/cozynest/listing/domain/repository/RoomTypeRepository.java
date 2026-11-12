package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.RoomType;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository {
    List<RoomType> findAll();
    List<RoomType> findAllByOrderByIdAsc();
    Optional<RoomType> findByCodeIgnoreCase(String code);
}
