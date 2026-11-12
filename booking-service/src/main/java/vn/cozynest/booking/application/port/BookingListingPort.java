package vn.cozynest.booking.application.port;

/**
 * Port to listing-service for live room availability, maxOccupants check, and hold release.
 */
public interface BookingListingPort {
    RoomAvailabilityCheck checkRoomAvailability(String roomId, int requestedOccupants);

    void releaseRoomHold(String roomId);

    record RoomAvailabilityCheck(boolean available, int maxOccupants, String status, String reason) {}
}

