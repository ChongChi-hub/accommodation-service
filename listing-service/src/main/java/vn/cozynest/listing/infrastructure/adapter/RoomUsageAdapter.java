package vn.cozynest.listing.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.port.RoomUsagePort;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomUsageAdapter implements RoomUsagePort {

    private final RoomRepository roomRepository;

    @Override
    public boolean isRoomInActiveUse(Long roomId) {
        // TODO: In Phase 2, invoke booking-service feign client to check active contracts and upcoming appointments.
        // Currently, check if room status is 'RENTED' in listing database.
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        Room room = roomOpt.get();
        boolean isRented = "RENTED".equalsIgnoreCase(room.getStatus());
        if (isRented) {
            log.info("Room ID {} is actively marked as RENTED - deletion blocked", roomId);
        }
        return isRented;
    }
}

