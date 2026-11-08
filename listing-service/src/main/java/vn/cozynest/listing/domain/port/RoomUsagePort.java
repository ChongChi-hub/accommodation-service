package vn.cozynest.listing.domain.port;

public interface RoomUsagePort {

    /**
     * Checks if a room is actively rented or has active appointments/contracts.
     */
    boolean isRoomInActiveUse(Long roomId);
}

