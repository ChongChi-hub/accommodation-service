package vn.cozynest.admin.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.admin.application.port.AdminBillingEscrowPort;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Component
public class HttpAdminBillingEscrowAdapter implements AdminBillingEscrowPort {

    private static final Logger log = LoggerFactory.getLogger(HttpAdminBillingEscrowAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient restClient;
    private final String internalServiceKey;

    @Autowired
    public HttpAdminBillingEscrowAdapter(
            @Value("${app.billing-service.url:http://localhost:8084}") String billingBaseUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey,
            @Autowired(required = false) Environment env
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(3));
        rf.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(billingBaseUrl).requestFactory(rf).build();
        this.internalServiceKey = internalServiceKey;
        String[] active = env != null ? env.getActiveProfiles() : null;
        boolean restricted = active != null && Arrays.stream(active)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        if (restricted && DEFAULT_INTERNAL_KEY.equals(internalServiceKey)) {
            throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
        }
    }

    @Override
    public void freezeEscrow(String contractId, String actorId, String reason) {
        postEscrowAction("/internal/billing/escrow/{contractId}/freeze", contractId, Map.of(
                "actorId", actorId != null ? actorId : "SYSTEM_ADMIN",
                "reason", reason != null ? reason : "Dong bang tranh chap"
        ));
    }

    @Override
    public void releaseEscrow(String contractId, String actorId) {
        postEscrowAction("/internal/billing/escrow/{contractId}/release", contractId, Map.of(
                "actorId", actorId != null ? actorId : "SYSTEM_ADMIN"
        ));
    }

    @Override
    public void refundEscrow(String contractId, String actorId, String reason) {
        postEscrowAction("/internal/billing/escrow/{contractId}/refund", contractId, Map.of(
                "actorId", actorId != null ? actorId : "SYSTEM_ADMIN",
                "reason", reason != null ? reason : "Hoan coc tranh chap"
        ));
    }

    private void postEscrowAction(String uri, String contractId, Map<String, String> body) {
        try {
            restClient.post()
                    .uri(uri, contractId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Escrow internal call {} for contractId={} failed: {}", uri, contractId, ex.getMessage());
            throw new IllegalStateException("Khong the cap nhat trang thai ky quy tai billing-service: " + ex.getMessage(), ex);
        }
    }
}
