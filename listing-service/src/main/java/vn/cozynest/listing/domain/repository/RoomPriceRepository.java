package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.RoomPrice;

import java.util.List;

public interface RoomPriceRepository {
    List<RoomPrice> findByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
    RoomPrice save(RoomPrice price);
}
