package vn.cozynest.communication.infrastructure.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.communication.application.port.UserDirectoryPort;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpUserDirectoryAdapter implements UserDirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(HttpUserDirectoryAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient restClient;
    private final String internalServiceKey;

    @Autowired
    public HttpUserDirectoryAdapter(
            @Value("${services.identity.url:${IDENTITY_SERVICE_URL:http://localhost:8081}}") String identityBaseUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(identityBaseUrl)
                .requestFactory(factory)
                .build();
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public Optional<UserSummary> findUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        String normalizedId = normalizeUserId(userId);

        try {
            Map<String, Object> body = restClient.get()
                    .uri("/internal/users/{id}", normalizedId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                String id = (String) data.get("id");
                String email = (String) data.get("email");
                String fullName = (String) data.get("fullName");
                String phone = (String) data.get("phone");
                if (email != null && !email.isBlank()) {
                    return Optional.of(new UserSummary(
                            id != null ? id : normalizedId,
                            email,
                            fullName != null ? fullName : "Quý khách",
                            phone != null ? phone : ""
                    ));
                }
            }
        } catch (Exception ex) {
            log.debug("Could not fetch user profile for {} from identity-service: {}", userId, ex.getMessage());
        }

        return fallbackForDemoUser(normalizedId);
    }

    private String normalizeUserId(String userId) {
        String trimmed = userId.trim();
        switch (trimmed) {
            case "1": return "u-landlord-01";
            case "2": return "u-landlord-02";
            case "3": return "u-landlord-03";
            case "4": return "u-landlord-04";
            case "5": return "u-landlord-05";
            case "6": return "u-landlord-06";
            default: break;
        }
        if (trimmed.matches("^\\d+$")) {
            try {
                Map<String, Object> body = restClient.get()
                        .uri("/internal/users/resolve-landlord/{id}", trimmed)
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                Object dataObj = body != null ? body.get("data") : null;
                if (dataObj instanceof Map<?, ?> data && data.get("landlordUserId") != null) {
                    return String.valueOf(data.get("landlordUserId"));
                }
            } catch (Exception ex) {
                log.debug("Could not resolve numeric landlordId {}: {}", trimmed, ex.getMessage());
            }
        }
        return trimmed;
    }

    private Optional<UserSummary> fallbackForDemoUser(String userId) {
        return switch (userId) {
            case "u-landlord-01" -> Optional.of(new UserSummary("u-landlord-01", "landlord@cozynest.vn", "Trần Thị Chủ Trọ", "0902000002"));
            case "u-landlord-02" -> Optional.of(new UserSummary("u-landlord-02", "landlord2@cozynest.vn", "Hoàng Văn Chủ Trọ 2", "0902000003"));
            case "u-tenant-01" -> Optional.of(new UserSummary("u-tenant-01", "tenant@cozynest.vn", "Nguyễn Văn Thuê", "0901000001"));
            case "u-admin-01" -> Optional.of(new UserSummary("u-admin-01", "admin@cozynest.vn", "Lê Hoàng Quản Trị", "0903000003"));
            default -> {
                if (userId.startsWith("u-") || userId.contains("-")) {
                    yield Optional.of(new UserSummary(userId, userId + "@cozynest.vn", "Người dùng " + userId, ""));
                }
                yield Optional.empty();
            }
        };
    }
}
