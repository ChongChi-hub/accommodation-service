package vn.cozynest.listing.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ListingNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(ListingNotificationClient.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient restClient;
    private final String internalServiceKey;

    public ListingNotificationClient(
            @Value("${services.communication.url:${COMMUNICATION_SERVICE_URL:http://localhost:8085}}") String communicationUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(communicationUrl)
                .requestFactory(factory)
                .build();
    }

    public void sendNotification(String userId, String type, String title, String content, String linkUrl) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "userId", userId,
                            "type", type != null ? type : "INFO",
                            "title", title,
                            "content", content,
                            "linkUrl", linkUrl != null ? linkUrl : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent notification to user {}: {}", userId, title);
        } catch (Exception ex) {
            log.warn("Failed to dispatch listing notification for user {}: {}", userId, ex.getMessage());
        }
    }

    public void notifyModeratorsAndAdmins(String type, String title, String content, String linkUrl) {
        List<String> privilegedUsers = List.of("u-admin-01", "u-mod-01");
        for (String uid : privilegedUsers) {
            sendNotification(uid, type, title, content, linkUrl);
        }
    }
}
