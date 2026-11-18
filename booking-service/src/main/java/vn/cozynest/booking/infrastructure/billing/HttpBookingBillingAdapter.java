package vn.cozynest.booking.infrastructure.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.booking.application.port.BookingBillingPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Component
public class HttpBookingBillingAdapter implements BookingBillingPort {
    private static final Logger log = LoggerFactory.getLogger(HttpBookingBillingAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";
    private final RestClient restClient;
    private final String internalServiceKey;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBookingBillingAdapter(
            @Value("${app.billing-service.url:http://localhost:8084}") String billingBaseUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey,
            @Autowired(required = false) Environment env
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(2));
        rf.setReadTimeout(Duration.ofSeconds(3));
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
    public void openEscrowHold(String contractId, String tenantId, String landlordId, long amount) {
        if (amount <= 0) {
            return;
        }
        log.info("Escrow hold registration requested for contractId={}, amount={}", contractId, amount);
        try {
            circuitBreaker.executeWithFallback(1, () -> {
                restClient.post()
                        .uri("/internal/billing/escrow/init")
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .body(Map.of(
                                "contractId", contractId,
                                "tenantId", tenantId,
                                "landlordId", landlordId,
                                "amount", amount
                        ))
                        .retrieve()
                        .toBodilessEntity();
                return null;
            }, () -> null);
        } catch (Exception ex) {
            log.warn("Non-blocking failure to register escrow hold with billing-service: {}", ex.getMessage());
        }
    }

    @Override
    public void refundOrReleaseOnCompensation(String contractId, String reason) {
        log.warn("Saga compensation signaled billing escrow release/refund for contractId={}, reason={}", contractId, reason);
        try {
            circuitBreaker.executeWithFallback(1, () -> {
                restClient.post()
                        .uri("/internal/billing/escrow/{contractId}/compensate", contractId)
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .body(Map.of("reason", reason != null ? reason : "Saga compensation"))
                        .retrieve()
                        .toBodilessEntity();
                return null;
            }, () -> null);
        } catch (Exception ex) {
            log.warn("Non-blocking failure to compensate escrow hold with billing-service: {}", ex.getMessage());
        }
    }
}

