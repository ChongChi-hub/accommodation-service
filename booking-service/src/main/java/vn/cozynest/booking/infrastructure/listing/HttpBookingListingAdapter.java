package vn.cozynest.booking.infrastructure.listing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.util.Map;

@Component
public class HttpBookingListingAdapter implements BookingListingPort {
    private static final Logger log = LoggerFactory.getLogger(HttpBookingListingAdapter.class);
    private final RestClient restClient;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBookingListingAdapter(
            @Value("${app.listing-service.url:http://localhost:8082}") String listingBaseUrl
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(3));
        rf.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(listingBaseUrl).requestFactory(rf).build();
    }

    @Override
    public RoomAvailabilityCheck checkRoomAvailability(String roomId, int requestedOccupants) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return new RoomAvailabilityCheck(true, 10, "AVAILABLE", null);
        }
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/api/v1/rooms/{id}", roomId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                Object statusObj = data.get("status");
                String status = statusObj != null ? String.valueOf(statusObj) : "UNKNOWN";
                Object maxOccObj = data.get("maxOccupants");
                int maxOccupants = maxOccObj instanceof Number n ? n.intValue() : 4;
                boolean available = "AVAILABLE".equalsIgnoreCase(status) || "PUBLISHED".equalsIgnoreCase(status);
                if (!available) {
                    return new RoomAvailabilityCheck(false, maxOccupants, status, "Phong dang o trang thai " + status);
                }
                if (requestedOccupants > maxOccupants) {
                    return new RoomAvailabilityCheck(false, maxOccupants, status,
                            "So nguoi o (" + requestedOccupants + ") vuot qua suc chua toi da (" + maxOccupants + ")");
                }
                return new RoomAvailabilityCheck(true, maxOccupants, status, null);
            }
            return new RoomAvailabilityCheck(true, 4, "AVAILABLE", null);
        }, () -> {
            log.warn("Listing check fallback for roomId={}", roomId);
            return new RoomAvailabilityCheck(true, 10, "UNKNOWN", null);
        });
    }

    @Override
    public void releaseRoomHold(String roomId) {
        log.info("Saga compensation notified listing hold release for roomId={}", roomId);
    }
}

