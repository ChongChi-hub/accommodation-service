package vn.cozynest.listing.domain.port;

public interface CloudinaryStoragePort {
    String uploadRoomImage(byte[] imageBytes, String landlordName, String publicIdPrefix);
}
