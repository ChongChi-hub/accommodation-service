package vn.cozynest.identity.infrastructure.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class JavaMailEmailServiceAdapter implements EmailServicePort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailServiceAdapter.class);
    private static final int MAX_RETRIES = 3;

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;
    private final boolean mailEnabled;
    private final String mailHost;
    private final Environment environment;

    public JavaMailEmailServiceAdapter(
            JavaMailSender mailSender,
            @Value("${mail.from:no-reply@cozynest.vn}") String fromEmail,
            @Value("${mail.from-name:Tổ Ấm}") String fromName,
            @Value("${mail.enabled:true}") boolean mailEnabled,
            @Value("${spring.mail.host:}") String mailHost,
            Environment environment) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "no-reply@cozynest.vn";
        this.fromName = fromName != null && !fromName.isBlank() ? fromName : "Tổ Ấm";
        this.mailEnabled = mailEnabled;
        this.mailHost = mailHost != null ? mailHost.trim() : "";
        this.environment = environment;
    }

    @Override
    public void ensureAvailable() {
        if (!mailEnabled) {
            throw new IdentityDomainException(
                    "Dịch vụ gửi email hiện đang tắt (MAIL_ENABLED=false). Vui lòng liên hệ quản trị viên.",
                    "ERR_AUTH_SERVICE_UNAVAILABLE"
            );
        }
        boolean isProd = environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && (mailHost.isBlank() || "localhost".equalsIgnoreCase(mailHost))) {
            throw new IdentityDomainException(
                    "Cấu hình máy chủ email (MAIL_HOST) chưa được thiết lập cho môi trường production.",
                    "ERR_AUTH_SERVICE_UNAVAILABLE"
            );
        }
    }

    @Override
    @Async
    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        String htmlContent = loadTemplate("templates/email/password_reset_otp.html")
                .replace("{{fullName}}", fullName != null ? fullName : "Quý khách")
                .replace("{{otpCode}}", otpCode);

        String textContent = loadTemplate("templates/email/password_reset_otp.txt")
                .replace("{{fullName}}", fullName != null ? fullName : "Quý khách")
                .replace("{{otpCode}}", otpCode);

        sendWithRetry(toEmail, "Mã xác thực OTP đặt lại mật khẩu - Tổ Ấm", textContent, htmlContent, "password reset OTP");
    }

    @Override
    @Async
    public void sendEmailVerificationOtp(String toEmail, String fullName, String otpCode) {
        String htmlContent = "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
                + "<h2>Xác thực địa chỉ email - Tổ Ấm</h2>"
                + "<p>Xin chào " + (fullName != null ? fullName : "Quý khách") + ",</p>"
                + "<p>Mã xác thực (OTP) kích hoạt tài khoản của bạn là:</p>"
                + "<div style='font-size:28px;font-weight:bold;letter-spacing:6px;color:#0f766e;padding:12px 0;'>" + otpCode + "</div>"
                + "<p>Mã OTP này có hiệu lực trong vòng 10 phút. Nếu không phải bạn yêu cầu, vui lòng bỏ qua email này.</p>"
                + "</div>";

        String textContent = "Mã xác thực email Tổ Ấm của bạn là: " + otpCode + ". Hiệu lực 10 phút. Nếu không phải bạn yêu cầu, vui lòng bỏ qua email này.";

        sendWithRetry(toEmail, "Mã xác thực OTP kích hoạt tài khoản - Tổ Ấm", textContent, htmlContent, "email verification OTP");
    }

    @Override
    @Async
    public void sendPasswordChangedNotice(String toEmail, String fullName) {
        String htmlContent = loadTemplate("templates/email/password_changed.html")
                .replace("{{fullName}}", fullName != null ? fullName : "Quý khách");

        String textContent = loadTemplate("templates/email/password_changed.txt")
                .replace("{{fullName}}", fullName != null ? fullName : "Quý khách");

        sendWithRetry(toEmail, "Mật khẩu tài khoản Tổ Ấm đã được thay đổi thành công", textContent, htmlContent, "password changed notice");
    }

    private void sendWithRetry(String toEmail, String subject, String textContent, String htmlContent, String mailType) {
        long backoffMs = 1000L;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(textContent, htmlContent);

                mailSender.send(message);
                log.info("Sent {} email to recipient successfully on attempt {}", mailType, attempt);
                return;
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed to send {} email: {}", attempt, MAX_RETRIES, mailType, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        log.error("Exhausted {} retry attempts sending {} email.", MAX_RETRIES, mailType);
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Could not load email template from {}: {}", path, e.getMessage());
            return "";
        }
    }
}
