package vn.cozynest.identity.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.cozynest.identity.application.port.EmailServicePort;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class JavaMailEmailServiceAdapter implements EmailServicePort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailServiceAdapter.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public JavaMailEmailServiceAdapter(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:no-reply@cozynest.vn}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "no-reply@cozynest.vn";
    }

    @Override
    @Async
    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        try {
            String htmlContent = loadTemplate("templates/email/password_reset_otp.html")
                    .replace("{{fullName}}", fullName != null ? fullName : "Quý khách")
                    .replace("{{otpCode}}", otpCode);

            String textContent = loadTemplate("templates/email/password_reset_otp.txt")
                    .replace("{{fullName}}", fullName != null ? fullName : "Quý khách")
                    .replace("{{otpCode}}", otpCode);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP đặt lại mật khẩu - Tổ Ấm");
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendEmailVerificationOtp(String toEmail, String fullName, String otpCode) {
        try {
            String htmlContent = "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
                    + "<h2>Xác thực địa chỉ email - Tổ Ấm</h2>"
                    + "<p>Xin chào " + (fullName != null ? fullName : "Quý khách") + ",</p>"
                    + "<p>Mã xác thực (OTP) kích hoạt tài khoản của bạn là:</p>"
                    + "<div style='font-size:28px;font-weight:bold;letter-spacing:6px;color:#0f766e;padding:12px 0;'>" + otpCode + "</div>"
                    + "<p>Mã OTP này có hiệu lực trong vòng 10 phút. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>"
                    + "</div>";

            String textContent = "Mã xác thực email Tổ Ấm của bạn là: " + otpCode + ". Hiệu lực 10 phút.";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP kích hoạt tài khoản - Tổ Ấm");
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Email verification OTP sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send verification OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordChangedNotice(String toEmail, String fullName) {
        try {
            String htmlContent = loadTemplate("templates/email/password_changed.html")
                    .replace("{{fullName}}", fullName != null ? fullName : "Quý khách");

            String textContent = loadTemplate("templates/email/password_changed.txt")
                    .replace("{{fullName}}", fullName != null ? fullName : "Quý khách");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Mật khẩu tài khoản Tổ Ấm đã được thay đổi thành công");
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Password changed confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send password changed notice to {}: {}", toEmail, e.getMessage());
        }
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
