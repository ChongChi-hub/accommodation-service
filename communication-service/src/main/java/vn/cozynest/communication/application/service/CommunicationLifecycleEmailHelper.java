package vn.cozynest.communication.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.communication.application.port.EmailServicePort;
import vn.cozynest.communication.application.port.UserDirectoryPort;

import java.util.Optional;

@Component
public class CommunicationLifecycleEmailHelper {

    private final EmailServicePort emailServicePort;
    private final UserDirectoryPort userDirectoryPort;
    private final String webBaseUrl;

    public CommunicationLifecycleEmailHelper(
            EmailServicePort emailServicePort,
            UserDirectoryPort userDirectoryPort,
            @Value("${app.web.base-url:http://localhost:3000}") String webBaseUrl
    ) {
        this.emailServicePort = emailServicePort;
        this.userDirectoryPort = userDirectoryPort;
        this.webBaseUrl = (webBaseUrl != null && !webBaseUrl.isBlank()) ? webBaseUrl : "http://localhost:3000";
    }

    public void sendAppointmentOutcomeEmail(String tenantId, String roomTitle, boolean completed) {
        String email = resolveEmail(tenantId);
        if (email == null) return;
        String room = (roomTitle != null && !roomTitle.isBlank()) ? roomTitle : "phòng trọ";
        String subject = completed
                ? "[Tổ Ấm] Lịch hẹn xem phòng đã hoàn thành - " + room
                : "[Tổ Ấm] Ghi nhận không đến lịch hẹn xem phòng - " + room;
        String text = completed
                ? "Buổi xem phòng " + room + " đã được chủ trọ xác nhận hoàn thành. Bạn có thể gửi yêu cầu thuê phòng tại: " + webBaseUrl + "/dashboard/tenant/appointments"
                : "Chủ trọ đã ghi nhận bạn không đến buổi xem phòng " + room + ". Xem chi tiết tại: " + webBaseUrl + "/dashboard/tenant/appointments";
        emailServicePort.sendEmail(email, subject, text, "<p>" + escapeHtml(text) + "</p>");
    }

    public void sendContractNeedsSignatureEmail(String tenantId, String contractCode) {
        String email = resolveEmail(tenantId);
        if (email == null) return;
        String code = (contractCode != null && !contractCode.isBlank()) ? contractCode : "hợp đồng mới";
        String subject = "[Tổ Ấm] Hợp đồng thuê phòng cần ký xác nhận - " + code;
        String text = "Chủ trọ đã khởi tạo hợp đồng " + code + ". Vui lòng đăng nhập và ký xác nhận bằng OTP tại: " + webBaseUrl + "/dashboard/tenant/contracts";
        emailServicePort.sendEmail(email, subject, text, "<p>" + escapeHtml(text) + "</p>");
    }

    public void sendContractActivatedEmail(String tenantId, String landlordId, String contractCode, String orderCode) {
        String code = (contractCode != null && !contractCode.isBlank()) ? contractCode : "hợp đồng";
        String orderLabel = (orderCode != null && !orderCode.isBlank()) ? " (Mã đơn: " + orderCode + ")" : "";
        String subject = "[Tổ Ấm] Hợp đồng thuê phòng đã có hiệu lực - " + code;

        String tenantEmail = resolveEmail(tenantId);
        if (tenantEmail != null) {
            String text = "Chúc mừng! Hợp đồng " + code + orderLabel + " đã chính thức có hiệu lực (ACTIVE). Xem địa chỉ đầy đủ và hướng dẫn nhận phòng tại: " + webBaseUrl + "/dashboard/tenant/my-room";
            emailServicePort.sendEmail(tenantEmail, subject, text, "<p>" + escapeHtml(text) + "</p>");
        }
        String landlordEmail = resolveEmail(landlordId);
        if (landlordEmail != null && !landlordEmail.equalsIgnoreCase(tenantEmail)) {
            String text = "Hợp đồng " + code + orderLabel + " đã chính thức có hiệu lực (ACTIVE). Quản lý khách thuê tại: " + webBaseUrl + "/dashboard/landlord/tenants";
            emailServicePort.sendEmail(landlordEmail, subject, text, "<p>" + escapeHtml(text) + "</p>");
        }
    }

    public void sendPaymentPaidEmail(String tenantId, String paymentCode, long amount, boolean sandboxMode) {
        String email = resolveEmail(tenantId);
        if (email == null) return;
        String sandboxNote = sandboxMode ? " [Chế độ thử nghiệm - SANDBOX_ONLY]" : "";
        String subject = "[Tổ Ấm] Xác nhận thanh toán thành công " + paymentCode + sandboxNote;
        String text = "Hệ thống đã ghi nhận khoản thanh toán " + paymentCode + " với số tiền " + amount + " VND" + sandboxNote
                + ". Xem chi tiết tại: " + webBaseUrl + "/dashboard/tenant/deposits";
        emailServicePort.sendEmail(email, subject, text, "<p>" + escapeHtml(text) + "</p>");
    }

    public void sendInvoiceGeneratedEmail(String tenantId, String invoiceCode, long totalAmount) {
        String email = resolveEmail(tenantId);
        if (email == null) return;
        String code = (invoiceCode != null && !invoiceCode.isBlank()) ? invoiceCode : "hóa đơn mới";
        String subject = "[Tổ Ấm] Hóa đơn tiền phòng kỳ mới - " + code;
        String text = "Chủ trọ vừa phát hành hóa đơn " + code + " với tổng số tiền " + totalAmount + " VND. Xem và thanh toán tại: " + webBaseUrl + "/dashboard/tenant/invoices";
        emailServicePort.sendEmail(email, subject, text, "<p>" + escapeHtml(text) + "</p>");
    }

    private String resolveEmail(String userId) {
        if (userId == null || userId.isBlank() || userDirectoryPort == null) return null;
        Optional<UserDirectoryPort.UserSummary> opt = userDirectoryPort.findUserById(userId);
        return opt.map(UserDirectoryPort.UserSummary::email).filter(e -> e != null && e.contains("@")).orElse(null);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
