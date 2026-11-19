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
                "Quản trị viên đã xác nhận nhận đủ tiền cọc " + fmt(amount) + " VND của hợp đồng " + contractId
                        + " tại tài khoản sàn. Quỹ Escrow đã kích hoạt, bạn có thể bấm nút Bàn giao phòng.",
                "/dashboard/landlord/deposits");
    }

    @Override
    public void notifyTenantDepositConfirmed(String tenantId, String contractId, long amount) {
        send(tenantId, "ESCROW_DEPOSIT_CONFIRMED",
                "Tiền cọc hợp đồng đã được Admin duyệt",
                "Admin đã xác nhận nhận đủ tiền cọc " + fmt(amount) + " VND của hợp đồng " + contractId
                        + ". Quỹ Escrow giữ 50% đến hết hợp đồng; bạn đã có thể bấm Xác nhận nhận phòng.",
                "/dashboard/tenant/deposits");
    }

    @Override
    public void notifyAdminPaymentSubmitted(String tenantDisplayName, String orderType, String orderId, String paymentCode, long amount) {
        String tenantLabel = (tenantDisplayName != null && !tenantDisplayName.isBlank()) ? tenantDisplayName : "Khách thuê";
        boolean isInvoice = "INVOICE".equalsIgnoreCase(orderType);
        String title = isInvoice ? "Yêu cầu duyệt thanh toán hóa đơn tháng" : "Yêu cầu duyệt giao dịch tiền cọc";
        String content = isInvoice
                ? "Khách hàng " + tenantLabel + " đã chuyển tiền hóa đơn " + orderId + " số tiền " + fmt(amount)
                        + " VND (mã GD " + paymentCode + "). Vui lòng kiểm tra tài khoản " + platformAccountHolder + " của sàn và duyệt giao dịch."
                : "Khách hàng " + tenantLabel + " của hợp đồng " + orderId + " đã chuyển tiền cọc " + fmt(amount)
                        + " VND (mã GD " + paymentCode + "). Vui lòng kiểm tra tài khoản " + platformAccountHolder + " của sàn và duyệt hoặc hủy giao dịch.";
        for (String adminId : adminUserIds) {
            send(adminId, "ADMIN_PAYMENT_APPROVAL_REQUIRED", title, content, "/dashboard/admin/transactions");
        }
    }

    @Override
    public void notifyTenantDepositRejected(String tenantId, String contractId, String paymentCode, String reason) {
        String safeReason = (reason != null && !reason.isBlank()) ? reason : "Chưa nhận được tiền vào tài khoản sàn";
        send(tenantId, "ESCROW_DEPOSIT_REJECTED",
                "Giao dịch thanh toán không hợp lệ và đã bị hủy",
                "Admin đã hủy giao dịch " + paymentCode + " của mã " + contractId + ". Lý do: " + safeReason + ".",
                "/dashboard/tenant/deposits");
    }

    @Override
    public void notifyTenantInvoiceCreated(String tenantId, String invoiceCode, String periodMonth, long totalAmount) {
        send(tenantId, "INVOICE_GENERATED",
                "Bạn có hóa đơn tiền phòng mới (" + invoiceCode + ")",
                "Chủ trọ đã gửi hóa đơn kỳ " + periodMonth + " với tổng số tiền " + fmt(totalAmount)
                        + " VND. Vui lòng kiểm tra chỉ số điện nước và thanh toán qua mã QR của sàn.",
                "/dashboard/tenant/invoices");
    }

    @Override
    public void notifyInvoicePaidWithCommission(String landlordId, String tenantId, String invoiceCode, long totalAmount, long commissionFee, long landlordNet) {
        send(landlordId, "INVOICE_PAID_WALLET_CREDITED",
                "Đã cộng tiền thuê tháng vào Ví chủ trọ (" + invoiceCode + ")",
                "Admin đã duyệt hóa đơn " + invoiceCode + " (tổng thu " + fmt(totalAmount)
                        + " VND, trừ 5% hoa hồng sàn " + fmt(commissionFee) + " VND). Số dư khả dụng đã được cộng +" + fmt(landlordNet) + " VND.",
                "/dashboard/landlord/deposits");
        send(tenantId, "INVOICE_PAID_CONFIRMED",
                "Hóa đơn " + invoiceCode + " đã được xác nhận thanh toán",
                "Admin đã xác nhận nhận đủ " + fmt(totalAmount) + " VND cho hóa đơn " + invoiceCode + ". Cảm ơn bạn đã thanh toán đúng hạn.",
                "/dashboard/tenant/invoices");
    }

    @Override
    public void notifyAdminPayoutRequested(String landlordId, String payoutCode, long amount, String bankCode, String maskedAcc) {
        String content = "Chủ trọ (" + landlordId + ") vừa tạo lệnh rút tiền " + payoutCode + " số tiền " + fmt(amount)
                + " VND về tài khoản " + bankCode + " " + maskedAcc + ". Vui lòng kiểm tra, chuyển khoản và tải biên lai xác nhận giải ngân.";
        for (String adminId : adminUserIds) {
            send(adminId, "ADMIN_PAYOUT_REQUESTED", "Yêu cầu giải ngân rút tiền từ chủ trọ", content, "/dashboard/admin/transactions");
        }
    }

    @Override
    public void notifyLandlordPayoutPaid(String landlordId, String payoutCode, long amount, String bankTxCode) {
        send(landlordId, "PAYOUT_PAID",
                "Lệnh rút tiền " + payoutCode + " đã giải ngân thành công",
                "Admin đã chuyển khoản " + fmt(amount) + " VND (mã GD ngân hàng: " + bankTxCode
                        + ") và trừ số dư ví tương ứng. Bạn có thể xem ảnh biên lai tại lịch sử lệnh rút tiền.",
                "/dashboard/landlord/deposits");
    }

    private String fmt(long amount) {
        return String.format("%,d", amount);
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
