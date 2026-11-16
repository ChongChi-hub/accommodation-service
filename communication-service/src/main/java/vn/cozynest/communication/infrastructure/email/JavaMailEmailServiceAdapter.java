package vn.cozynest.communication.infrastructure.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.cozynest.communication.application.port.EmailServicePort;

import java.util.Optional;

@Component
public class JavaMailEmailServiceAdapter implements EmailServicePort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailServiceAdapter.class);
    private static final int MAX_RETRIES = 3;

    private final Optional<JavaMailSender> mailSender;
    private final String fromEmail;
    private final String fromName;
    private final boolean mailEnabled;

    @Autowired
    public JavaMailEmailServiceAdapter(
            Optional<JavaMailSender> mailSender,
            @Value("${mail.from:no-reply@cozynest.vn}") String fromEmail,
            @Value("${mail.from-name:Tổ Ấm}") String fromName,
            @Value("${mail.enabled:true}") boolean mailEnabled
    ) {
        this.mailSender = mailSender != null ? mailSender : Optional.empty();
        this.fromEmail = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "no-reply@cozynest.vn";
        this.fromName = (fromName != null && !fromName.isBlank()) ? fromName : "Tổ Ấm";
        this.mailEnabled = mailEnabled;
    }

    @Override
    public void sendEmail(String toEmail, String subject, String textContent, String htmlContent) {
        if (!mailEnabled) {
            log.info("Email dispatch disabled (mail.enabled=false). Skipping email to {}", toEmail);
            return;
        }
        if (mailSender.isEmpty()) {
            log.warn("JavaMailSender bean is not configured. Skipping email to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("Invalid recipient email address: '{}'. Skipping send.", toEmail);
            return;
        }

        long backoffMs = 500L;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                JavaMailSender sender = mailSender.get();
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(
                        textContent != null ? textContent : "",
                        htmlContent != null ? htmlContent : (textContent != null ? textContent : "")
                );

                sender.send(message);
                log.info("Successfully sent email '{}' to {} on attempt {}", subject, toEmail, attempt);
                return;
            } catch (Exception ex) {
                log.warn("Attempt {}/{} failed to send email '{}' to {}: {}", attempt, MAX_RETRIES, subject, toEmail, ex.getMessage());
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
        log.error("Failed to send email '{}' to {} after {} attempts", subject, toEmail, MAX_RETRIES);
    }
}
