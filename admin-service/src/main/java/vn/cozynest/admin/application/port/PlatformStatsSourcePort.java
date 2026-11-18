package vn.cozynest.admin.application.port;

import java.time.LocalDate;
import java.util.Map;

public interface PlatformStatsSourcePort {

    Map<String, Object> fetchIdentityStats(LocalDate from, LocalDate to);

    Map<String, Object> fetchListingStats(LocalDate from, LocalDate to);

    Map<String, Object> fetchBookingStats(LocalDate from, LocalDate to);

    Map<String, Object> fetchBillingStats(LocalDate from, LocalDate to);
}
