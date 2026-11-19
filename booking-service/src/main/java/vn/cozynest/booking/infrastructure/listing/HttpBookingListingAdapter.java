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
    private final String internalServiceKey;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBookingListingAdapter(
            @Value("${app.listing-service.url:http://localhost:8082}") String listingBaseUrl,
            @Value("${app.internal.service-key:CozyNestInternalServiceKeySecret2026}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(3));
        rf.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(listingBaseUrl).requestFactory(rf).build();
        this.internalServiceKey = internalServiceKey;
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
    public String getRoomOwnerId(String roomId) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return null;
        }
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/api/v1/rooms/{id}", roomId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data && data.get("landlordId") != null) {
                return String.valueOf(data.get("landlordId"));
            }
            return null;
        }, () -> null);
    }

    @Override
    public boolean markRoomRented(String roomId, String contractId) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return true;
        }
        try {
            restClient.post()
                    .uri("/internal/rooms/{id}/status", roomId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(Map.of(
                            "status", "RENTED",
                            "occupancyStatus", "OCCUPIED",
                            "reason", "Contract activated: " + contractId
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.error("Failed to mark room RENTED in listing-service for roomId={}, contractId={}: {}",
                    roomId, contractId, ex.getMessage());
            return false;
        }
    }

    @Override
    public RoomCheckinDetails getRoomCheckinDetails(String roomId) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return new RoomCheckinDetails("Phong tro CozyNest", "", "", "", "", "");
        }
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/api/v1/rooms/{id}", roomId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                String title = str(data.get("title"), "Phong tro CozyNest");
                String address = str(data.get("address"), "");
                String district = str(data.get("districtName"), str(data.get("districtCode"), ""));
                String province = str(data.get("provinceName"), str(data.get("provinceCode"), ""));
                String areaHint = (!district.isBlank() && !province.isBlank())
                        ? district + ", " + province : (!district.isBlank() ? district : province);
                String landlordName = str(data.get("landlordName"), "");
                String landlordPhone = str(data.get("landlordPhone"), "");
                String houseRules = str(data.get("houseRules"), str(data.get("curfew"), ""));
                return new RoomCheckinDetails(title, address, areaHint, landlordName, landlordPhone, houseRules);
            }
            return new RoomCheckinDetails("Phong tro CozyNest", "", "", "", "", "");
        }, () -> new RoomCheckinDetails("Phong tro CozyNest", "", "", "", "", ""));
    }

    @Override
    public void releaseRoomHold(String roomId) {
        if (roomId == null || !roomId.matches("\\d+")) {
            return;
        }
        try {
            restClient.post()
                    .uri("/internal/rooms/{id}/status", roomId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(Map.of(
                            "status", "PUBLISHED",
                            "occupancyStatus", "AVAILABLE",
                            "reason", "Contract terminated or saga compensated"
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Released room hold in listing-service for roomId={}", roomId);
        } catch (Exception ex) {
            log.warn("Failed to release room hold in listing-service for roomId={}: {}", roomId, ex.getMessage());
        }
    }

    private String str(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
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
