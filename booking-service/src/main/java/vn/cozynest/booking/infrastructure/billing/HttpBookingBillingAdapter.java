package vn.cozynest.booking.infrastructure.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.booking.application.port.BookingBillingPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.util.Map;

@Component
public class HttpBookingBillingAdapter implements BookingBillingPort {
    private static final Logger log = LoggerFactory.getLogger(HttpBookingBillingAdapter.class);
    private final RestClient restClient;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBookingBillingAdapter(
            @Value("${app.billing-service.url:http://localhost:8084}") String billingBaseUrl
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(2));
        rf.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(billingBaseUrl).requestFactory(rf).build();
    }

    @Override
    public void openEscrowHold(String contractId, String tenantId, String landlordId, long amount) {
        log.info("Escrow hold registration requested for contractId={}, amount={}", contractId, amount);
        try {
            circuitBreaker.executeWithFallback(1, () -> {
                restClient.post()
                        .uri("/api/v1/billing/escrow/init")
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
    }
}

