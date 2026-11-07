package vn.toam.listing.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.toam.listing.domain.port.KycVerificationPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpKycVerificationAdapter implements KycVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpKycVerificationAdapter.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final String internalServiceKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedStatus> cache = new ConcurrentHashMap<>();

    public HttpKycVerificationAdapter(
            @Value("${services.identity.url:http://localhost:8081}") String identityServiceUrl,
            @Value("${app.internal.service-key:ToAmInternalServiceKeySecret2026}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(identityServiceUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public KycVerificationStatus checkLandlordKyc(String actorId, Long landlordId) {
        String queryId = actorId != null && !actorId.isBlank() ? actorId : ("u-landlord-" + landlordId);

        // Check short TTL cache
        CachedStatus cached = cache.get(queryId);
        if (cached != null && !cached.isExpired()) {
            return cached.status;
        }

        // Inter-service call with fail-closed policy
        try {
            String json = restClient.get()
                    .uri("/internal/users/{id}/kyc-status", queryId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(String.class);

            if (json != null && !json.isBlank()) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode dataNode = root.has("data") ? root.get("data") : root;
                String kycStatus = dataNode.has("kycStatus") ? dataNode.get("kycStatus").asText() : "NONE";
                boolean isApproved = dataNode.has("isApproved") && dataNode.get("isApproved").asBoolean();

                KycVerificationStatus result = KycVerificationStatus.of(kycStatus, isApproved);
                cache.put(queryId, new CachedStatus(result, Instant.now()));
                return result;
            }
        } catch (Exception e) {
            log.error("FAIL-CLOSED: Failed to verify KYC status from identity-service for user {}: {}", queryId, e.getMessage());
        }

        // Fail-closed default: if service unreachable, treat as not approved
        return KycVerificationStatus.unverified("NONE");
    }

    public void invalidateCache(String actorId) {
        if (actorId != null) {
            cache.remove(actorId);
        }
    }

    private static class CachedStatus {
        final KycVerificationStatus status;
        final Instant cachedAt;

        CachedStatus(KycVerificationStatus status, Instant cachedAt) {
            this.status = status;
            this.cachedAt = cachedAt;
        }

        boolean isExpired() {
            return Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}

