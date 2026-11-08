package vn.cozynest.listing.domain.exception;

public class RoomVersionConflictException extends RuntimeException {
    public RoomVersionConflictException(String message) {
        super(message);
    }
}
