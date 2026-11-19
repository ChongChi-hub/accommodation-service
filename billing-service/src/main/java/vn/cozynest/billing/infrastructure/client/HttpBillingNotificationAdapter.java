package vn.cozynest.billing.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.billing.application.port.BillingNotificationPort;

import java.time.Duration;
import java.util.Map;

@Component
public class HttpBillingNotificationAdapter implements BillingNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpBillingNotificationAdapter.class);
    private final RestClient restClient;
    private final String internalServiceKey;

    @Autowired
    public HttpBillingNotificationAdapter(
            @Value("${app.communication-service.url:${COMMUNICATION_SERVICE_URL:http://localhost:8085}}") String baseUrl,
            @Value("${app.internal.service-key:${INTERNAL_SERVICE_KEY:CozyNestInternalServiceKeySecret2026}}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public void notifyDepositConfirmed(String landlordId, String contractId, long amount) {
        if (landlordId == null || landlordId.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(Map.of(
                            "userId", landlordId,
                            "type", "ESCROW_DEPOSIT_CONFIRMED",
                            "title", "Khách thuê đã thanh toán đủ tiền cọc hợp đồng",
                            "content", "Khoản cọc hợp đồng " + contractId + " đã được nạp an toàn vào Quỹ Ký Quỹ Escrow. Vui lòng tiến hành bàn giao phòng cho khách thuê.",
                            "linkUrl", "/dashboard/landlord/deposits"
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent deposit confirmation notification to landlord={}", landlordId);
        } catch (Exception ex) {
            log.warn("Failed to send deposit confirmation notification to landlord={}: {}", landlordId, ex.getMessage());
        }
    }
}

