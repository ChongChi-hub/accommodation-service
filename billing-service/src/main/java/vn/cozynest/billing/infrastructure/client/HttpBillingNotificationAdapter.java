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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HttpBillingNotificationAdapter implements BillingNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpBillingNotificationAdapter.class);
    private final RestClient restClient;
    private final String internalServiceKey;
    private final List<String> adminUserIds;
    private final String platformAccountHolder;

    @Autowired
    public HttpBillingNotificationAdapter(
            @Value("${app.communication-service.url:${COMMUNICATION_SERVICE_URL:http://localhost:8085}}") String baseUrl,
            @Value("${app.booking-service.url:${BOOKING_SERVICE_URL:http://localhost:8083}}") String bookingUrl,
            @Value("${app.internal.service-key:${INTERNAL_SERVICE_KEY:CozyNestInternalServiceKeySecret2026}}") String internalServiceKey,
            @Value("${fintech.platform.admin-user-ids:u-admin-01,u-admin-02}") String adminUserIdsRaw,
            @Value("${fintech.platform.account-holder:LE THANH CUONG}") String platformAccountHolder
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
        this.adminUserIds = Arrays.stream((adminUserIdsRaw != null ? adminUserIdsRaw : "u-admin-01").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.platformAccountHolder = platformAccountHolder != null && !platformAccountHolder.isBlank()
                ? platformAccountHolder.trim() : "LE THANH CUONG";
    }

    public HttpBillingNotificationAdapter(String baseUrl, String internalServiceKey) {
        this(baseUrl, "http://localhost:8083", internalServiceKey, "u-admin-01,u-admin-02", "LE THANH CUONG");
    }

    @Override
    public void notifyDepositConfirmed(String landlordId, String contractId, long amount) {
        send(landlordId, "ESCROW_DEPOSIT_CONFIRMED",
                "Admin đã duyệt tiền cọc ký quỹ từ khách thuê",
                "Quản trị viên đã xác nhận nhận đủ tiền cọc " + String.format("%,d", amount)
                        + " VND của hợp đồng " + contractId
                        + " tại tài khoản sàn. Quỹ Escrow đã kích hoạt, bạn có thể bấm nút Bàn giao phòng.",
                "/dashboard/landlord/deposits");
    }

    @Override
    public void notifyTenantDepositConfirmed(String tenantId, String contractId, long amount) {
        send(tenantId, "ESCROW_DEPOSIT_CONFIRMED",
                "Tiền cọc hợp đồng đã được Admin duyệt",
                "Admin đã xác nhận nhận đủ tiền cọc " + String.format("%,d", amount)
                        + " VND của hợp đồng " + contractId
                        + ". Quỹ Escrow đang giữ 50% đến hết hợp đồng; bạn đã có thể bấm Xác nhận nhận phòng.",
                "/dashboard/tenant/deposits");
    }

    @Override
    public void notifyAdminDepositSubmitted(String tenantDisplayName, String contractId, String paymentCode, long amount) {
        String tenantLabel = (tenantDisplayName != null && !tenantDisplayName.isBlank()) ? tenantDisplayName : "Khách thuê";
        String content = "Khách hàng " + tenantLabel + " của hợp đồng " + contractId
                + " đã chuyển tiền cọc " + String.format("%,d", amount) + " VND (mã GD " + paymentCode
                + "). Vui lòng kiểm tra tài khoản " + platformAccountHolder + " của sàn và duyệt hoặc hủy giao dịch.";
        for (String adminId : adminUserIds) {
            send(adminId, "ADMIN_DEPOSIT_APPROVAL_REQUIRED",
                    "Yêu cầu duyệt giao dịch tiền cọc",
                    content,
                    "/dashboard/admin/transactions");
        }
    }

    @Override
    public void notifyTenantDepositRejected(String tenantId, String contractId, String paymentCode, String reason) {
        String safeReason = (reason != null && !reason.isBlank()) ? reason : "Chưa nhận được tiền vào tài khoản sàn";
        send(tenantId, "ESCROW_DEPOSIT_REJECTED",
                "Giao dịch tiền cọc không hợp lệ và đã bị hủy",
                "Admin đã hủy giao dịch tiền cọc " + paymentCode + " của hợp đồng " + contractId
                        + ". Lý do: " + safeReason + ". Vui lòng kiểm tra lại và tạo giao dịch mới nếu cần.",
                "/dashboard/tenant/deposits");
    }

    private void send(String userId, String type, String title, String content, String linkUrl) {
        if (userId == null || userId.isBlank()) return;
        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(Map.of("userId", userId, "type", type, "title", title, "content", content, "linkUrl", linkUrl))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent notification type={} to userId={}", type, userId);
        } catch (Exception ex) {
            log.warn("Failed to send notification type={} to userId={}: {}", type, userId, ex.getMessage());
        }
    }
}
