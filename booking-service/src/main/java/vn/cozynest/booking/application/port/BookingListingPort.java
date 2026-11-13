package vn.cozynest.booking.application.port;

import java.math.BigDecimal;

/**
 * Port to listing-service for live room availability, maxOccupants check, hold release,
 * and contract snapshot data (rent, deposit, utility rates, initial meter readings).
 */
public interface BookingListingPort {
    RoomAvailabilityCheck checkRoomAvailability(String roomId, int requestedOccupants);

    void releaseRoomHold(String roomId);

    RoomContractSnapshot getRoomContractSnapshot(String roomId);

    record RoomAvailabilityCheck(boolean available, int maxOccupants, String status, String reason) {}

    record RoomContractSnapshot(
            Long monthlyRent,
            Long depositAmount,
            BigDecimal electricityRate,
            BigDecimal waterRate,
            BigDecimal initialElectricityReading,
            BigDecimal initialWaterReading,
            String roomTitle,
            String roomAddress
    ) {}
}
