package vn.cozynest.listing.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.port.RoomUsagePort;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Optional;

@Component
public class RoomUsageAdapter implements RoomUsagePort {

    private static final Logger log = LoggerFactory.getLogger(RoomUsageAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RoomRepository roomRepository;
    private final RestClient restClient;
    private final String internalServiceKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoomUsageAdapter(RoomRepository roomRepository, String bookingServiceUrl, String internalServiceKey) {
        this(roomRepository, bookingServiceUrl, internalServiceKey, null);
    }

    @Autowired
    public RoomUsageAdapter(
            RoomRepository roomRepository,
            @Value("${services.booking.url:${BOOKING_SERVICE_URL:http://localhost:8083}}") String bookingServiceUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey,
            @Autowired(required = false) Environment env) {
        this.roomRepository = roomRepository;
        this.internalServiceKey = internalServiceKey;

        if (env != null && env.matchesProfiles("prod", "production")) {
            String envKey = System.getenv("INTERNAL_SERVICE_KEY");
            if (envKey == null || envKey.isBlank() || DEFAULT_INTERNAL_KEY.equals(internalServiceKey)) {
                throw new IllegalStateException("Production environment must explicitly configure INTERNAL_SERVICE_KEY without default dev key");
            }
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);

        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean isRoomInActiveUse(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        Room room = roomOpt.get();
        if ("RENTED".equalsIgnoreCase(room.getStatus())) {
            log.info("Room ID {} is actively marked as RENTED - deletion blocked", roomId);
            return true;
        }

        try {
            String raw = restClient.get()
                    .uri("/internal/rooms/{roomId}/usage", roomId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(String.class);

            if (raw != null && !raw.isBlank()) {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode data = root.path("data");
                if (data.has("inActiveUse")) {
                    return data.path("inActiveUse").asBoolean(true);
                }
            }
            return true; // Fail-closed on unexpected payload
        } catch (Exception ex) {
            log.warn("Failed to verify room usage with booking-service for room {}: {}. Failing closed.", roomId, ex.getMessage());
            return true;
        }
    }
}

