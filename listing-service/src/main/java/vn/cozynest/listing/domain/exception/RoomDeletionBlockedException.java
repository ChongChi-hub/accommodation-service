package vn.cozynest.listing.domain.exception;

public class RoomDeletionBlockedException extends RuntimeException {
    public RoomDeletionBlockedException(String message) {
        super(message);
    }
}
