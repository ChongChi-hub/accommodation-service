package vn.cozynest.billing.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpBillingContractAdapter implements BillingContractPort {

    private static final Logger log = LoggerFactory.getLogger(HttpBillingContractAdapter.class);
    private final RestClient restClient;
    private final String internalServiceKey;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBillingContractAdapter(
            @Value("${app.booking-service.url:http://localhost:8083}") String bookingBaseUrl,
            @Value("${app.internal.service-key:CozyNestInternalServiceKeySecret2026}") String internalServiceKey,
            Environment env
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(bookingBaseUrl).requestFactory(factory).build();
        this.internalServiceKey = internalServiceKey;
        String[] active = env != null ? env.getActiveProfiles() : null;
        boolean restricted = active != null && Arrays.stream(active)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        if (restricted && "CozyNestInternalServiceKeySecret2026".equals(internalServiceKey)) {
            throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
        }
    }

    @Override
    public Optional<ContractSnapshot> getContractSnapshot(String contractId, LocalDate billingDate) {
        if (contractId == null || contractId.isBlank()) {
            return Optional.empty();
        }
        LocalDate effectiveDate = billingDate != null ? billingDate : LocalDate.now();
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/internal/contracts/{id}/effective-rent?billingDate={date}", contractId, effectiveDate)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                return Optional.of(new ContractSnapshot(
                        asString(data.get("contractId")),
                        asString(data.get("roomId")),
                        asString(data.get("tenantId")),
                        asString(data.get("landlordId")),
                        asString(data.get("status")),
                        asLong(data.get("effectiveMonthlyRent")),
                        (int) asLong(data.get("paymentDueDay"))
                ));
            }
            return Optional.empty();
        }, () -> {
            log.warn("Booking contract lookup unavailable/open circuit for contractId={}", contractId);
            return Optional.empty();
        });
    }

    private String asString(Object v) {
        return v != null ? String.valueOf(v) : null;
    }

    private long asLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        try {
            return v != null ? Long.parseLong(String.valueOf(v)) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

