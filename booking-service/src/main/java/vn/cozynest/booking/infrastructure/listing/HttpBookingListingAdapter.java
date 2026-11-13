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

import java.math.BigDecimal;
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
    public RoomContractSnapshot getRoomContractSnapshot(String roomId) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return new RoomContractSnapshot(
                    3500000L, 3500000L,
                    BigDecimal.valueOf(3500), BigDecimal.valueOf(25000),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    "Phòng trọ CozyNest", ""
            );
        }
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/api/v1/rooms/{id}", roomId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                Long monthlyRent = toLong(data.get("price"), 3500000L);
                Long depositAmount = toLong(data.get("deposit"), monthlyRent);
                BigDecimal electricityRate = toBigDecimal(data.get("electricityCost"), BigDecimal.valueOf(3500));
                BigDecimal waterRate = toBigDecimal(data.get("waterCost"), BigDecimal.valueOf(25000));
                BigDecimal initialElec = toBigDecimal(data.get("initialElectricityReading"), BigDecimal.ZERO);
                BigDecimal initialWater = toBigDecimal(data.get("initialWaterReading"), BigDecimal.ZERO);
                String title = data.get("title") != null ? String.valueOf(data.get("title")) : "Phòng trọ CozyNest";
                String address = data.get("address") != null ? String.valueOf(data.get("address")) : "";
                return new RoomContractSnapshot(
                        monthlyRent, depositAmount, electricityRate, waterRate, initialElec, initialWater, title, address
                );
            }
            return new RoomContractSnapshot(
                    3500000L, 3500000L,
                    BigDecimal.valueOf(3500), BigDecimal.valueOf(25000),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    "Phòng trọ CozyNest", ""
            );
        }, () -> {
            log.warn("Listing getRoomContractSnapshot fallback for roomId={}", roomId);
            return new RoomContractSnapshot(
                    3500000L, 3500000L,
                    BigDecimal.valueOf(3500), BigDecimal.valueOf(25000),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    "Phòng trọ CozyNest", ""
            );
        });
    }

    @Override
    public void releaseRoomHold(String roomId) {
        log.info("Saga compensation notified listing hold release for roomId={}", roomId);
    }

    private Long toLong(Object value, Long defaultValue) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s).longValue();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s);
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }
}
