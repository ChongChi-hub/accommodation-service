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
            @Value("${app.booking-service.url:${BOOKING_SERVICE_URL:http://localhost:8083}}") String bookingUrl,
            @Value("${app.internal.service-key:${INTERNAL_SERVICE_KEY:CozyNestInternalServiceKeySecret2026}}") String internalServiceKey
    ) {
        String resolvedUrl = baseUrl;
        if (resolvedUrl.contains("localhost") && bookingUrl != null && bookingUrl.contains("booking-service")) {
            resolvedUrl = "http://communication-service:8085";
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(resolvedUrl).requestFactory(factory).build();
        this.internalServiceKey = internalServiceKey;
    }

    public HttpBillingNotificationAdapter(String baseUrl, String internalServiceKey) {
        this(baseUrl, "http://localhost:8083", internalServiceKey);
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
                            "title", "Đã nhận tiền cọc ký quỹ từ khách thuê",
                            "content", "Hệ thống Sàn Tổ Ấm đã nhận và ghi nhận khoản tiền cọc "
                                    + String.format("%,d", amount)
                                    + " VND của hợp đồng " + contractId
                                    + " vào Quỹ Ký Quỹ Escrow. Bạn đã có thể bấm nút Bàn giao phòng cho khách thuê.",
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

