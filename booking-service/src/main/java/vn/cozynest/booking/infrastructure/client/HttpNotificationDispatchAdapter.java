package vn.cozynest.booking.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.booking.application.port.NotificationDispatchPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;

import java.util.Map;

@Component
public class HttpNotificationDispatchAdapter implements NotificationDispatchPort {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationDispatchAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient restClient;
    private final String internalServiceKey;
    private final ObjectMapper objectMapper;

    public HttpNotificationDispatchAdapter(String communicationUrl, String internalServiceKey, ObjectMapper objectMapper) {
        this(communicationUrl, internalServiceKey, objectMapper, null);
    }

    @Autowired
    public HttpNotificationDispatchAdapter(
            @Value("${services.communication.url:${COMMUNICATION_SERVICE_URL:http://localhost:8085}}") String communicationUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey,
            ObjectMapper objectMapper,
            @Autowired(required = false) Environment env) {
        this.internalServiceKey = internalServiceKey;
        this.objectMapper = objectMapper;

        if (env != null && env.matchesProfiles("prod", "production")) {
            String envKey = System.getenv("INTERNAL_SERVICE_KEY");
            if (envKey == null || envKey.isBlank() || DEFAULT_INTERNAL_KEY.equals(internalServiceKey)) {
                throw new IllegalStateException("Production environment must explicitly configure INTERNAL_SERVICE_KEY without default dev key");
            }
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(communicationUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean dispatch(AppointmentEventOutbox event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload() != null ? event.getPayload() : "{}");
            String recipientId = payload.path("tenantId").asText("");
            if (recipientId.isBlank()) {
                recipientId = payload.path("landlordId").asText("");
            }
            if (recipientId.isBlank()) {
                log.warn("Outbox event {} missing recipient userId; skipping external dispatch", event.getId());
                return true;
            }

            String title = resolveTitle(event.getEventType());
            String content = "Lịch hẹn " + event.getAggregateId() + " đã chuyển sang trạng thái mới.";
            String linkUrl = "/dashboard/tenant/appointments";

            restClient.post()
                    .uri("/internal/notifications")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "userId", recipientId,
                            "type", "APPOINTMENT",
                            "title", title,
                            "content", content,
                            "linkUrl", linkUrl
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("Failed to dispatch outbox event {} to communication-service: {}", event.getId(), ex.getMessage());
            return false;
        }
    }

    private String resolveTitle(String eventType) {
        if (eventType == null) return "Cập nhật lịch hẹn xem phòng";
        return switch (eventType) {
            case "APPOINTMENT_CONFIRMED" -> "Lịch hẹn xem phòng đã được xác nhận";
            case "APPOINTMENT_CANCELLED" -> "Lịch hẹn xem phòng đã bị hủy";
            case "APPOINTMENT_DECLINED" -> "Lịch hẹn xem phòng bị từ chối";
            default -> "Cập nhật trạng thái lịch hẹn xem phòng";
        };
    }
}

