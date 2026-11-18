package vn.cozynest.listing.domain.port;

public interface ListingNotificationPort {
    void notifyModeratorsAndAdmins(String action, String title, String body, String link);
}
