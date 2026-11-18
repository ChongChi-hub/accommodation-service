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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
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
    private final JdbcTemplate jdbcTemplate;

    public RoomUsageAdapter(RoomRepository roomRepository, String bookingServiceUrl, String internalServiceKey) {
        this(roomRepository, bookingServiceUrl, internalServiceKey, null, null);
    }

    public RoomUsageAdapter(
            RoomRepository roomRepository,
            String bookingServiceUrl,
            String internalServiceKey,
            Environment env) {
        this(roomRepository, bookingServiceUrl, internalServiceKey, env, null);
    }

    @Autowired
    public RoomUsageAdapter(
            RoomRepository roomRepository,
            @Value("${services.booking.url:${BOOKING_SERVICE_URL:http://localhost:8083}}") String bookingServiceUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey,
            @Autowired(required = false) Environment env,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.roomRepository = roomRepository;
        this.internalServiceKey = internalServiceKey;
        this.jdbcTemplate = jdbcTemplate;

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

        if ("DRAFT".equalsIgnoreCase(room.getStatus()) || "REJECTED".equalsIgnoreCase(room.getStatus())) {
            return false;
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/internal/rooms/{roomId}/usage");
            if (room.getRoomUuid() != null && !room.getRoomUuid().isBlank()) {
                uriBuilder.queryParam("roomUuid", room.getRoomUuid());
            }

            String raw = restClient.get()
                    .uri(uriBuilder.buildAndExpand(roomId).toUriString())
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(String.class);

            if (raw != null && !raw.isBlank()) {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode data = root.path("data");
                if (data.has("inActiveUse")) {
                    return data.path("inActiveUse").asBoolean(false);
                }
            }
            return false;
        } catch (HttpClientErrorException.NotFound nf) {
            return false;
        } catch (Exception ex) {
            log.warn("Failed to verify room usage via REST with booking-service for room {}: {}. Attempting fallback.", roomId, ex.getMessage());
            return checkUsageFallback(roomId, room.getRoomUuid(), ex);
        }
    }

    private boolean checkUsageFallback(Long roomId, String roomUuid, Exception originalEx) {
        if (jdbcTemplate != null) {
            try {
                String sqlAppts = """
                    SELECT COUNT(*) FROM cozynest_booking.viewing_appointments
                    WHERE (room_id = ? OR (? IS NOT NULL AND room_id = ?))
                      AND status IN ('PENDING', 'CONFIRMED')
                      AND scheduled_end >= NOW()
                """;
                Integer apptCount = jdbcTemplate.queryForObject(sqlAppts, Integer.class,
                        String.valueOf(roomId), roomUuid, roomUuid);

                if (apptCount != null && apptCount > 0) {
                    log.info("Database fallback detected {} upcoming appointment(s) for room {}. Blocking deletion.", apptCount, roomId);
                    return true;
                }

                String sqlContracts = """
                    SELECT COUNT(*) FROM cozynest_booking.contracts
                    WHERE (room_id = ? OR (? IS NOT NULL AND room_id = ?))
                      AND status = 'ACTIVE'
                """;
                Integer contractCount = jdbcTemplate.queryForObject(sqlContracts, Integer.class,
                        String.valueOf(roomId), roomUuid, roomUuid);

                if (contractCount != null && contractCount > 0) {
                    log.info("Database fallback detected {} active contract(s) for room {}. Blocking deletion.", contractCount, roomId);
                    return true;
                }

                return false;
            } catch (Exception ex) {
                log.warn("Database fallback query to cozynest_booking failed: {}.", ex.getMessage());
            }
        }
        if (originalEx instanceof HttpServerErrorException) {
            return true;
        }
        return false;
    }
}

