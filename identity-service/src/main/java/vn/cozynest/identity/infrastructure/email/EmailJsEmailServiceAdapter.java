package vn.cozynest.identity.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.cozynest.identity.application.port.EmailServicePort;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class EmailJsEmailServiceAdapter implements EmailServicePort {

    private static final Logger log = LoggerFactory.getLogger(EmailJsEmailServiceAdapter.class);
    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";

    // Account 1: Register and Verify Email
    private static final String ACC1_SERVICE_ID = "service_cozynest";
    private static final String ACC1_PUBLIC_KEY = "Re_5YClSOUJvPTVQF";
    private static final String ACC1_PRIVATE_KEY = "qNyRWL93ZyFJgqNcgbMQ9";
    private static final String ACC1_TEMPLATE_VERIFY_EMAIL = "template_59nc5mf";
    private static final String ACC1_TEMPLATE_REGISTER = "template_91vlzrx";

    // Account 2: Change Password
    private static final String ACC2_SERVICE_ID = "service_pp9ykfq";
    private static final String ACC2_PUBLIC_KEY = "mhVhUeI-C6HkEfVNy";
    private static final String ACC2_PRIVATE_KEY = "lhAfrXJWJpwyWGhFxSHnY";
    private static final String ACC2_TEMPLATE_CHANGE_PASSWORD = "template_30c269c";

    private final RestClient restClient;

    public EmailJsEmailServiceAdapter() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ORIGIN, "http://localhost:3000")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void ensureAvailable() {
        // EmailJS API is cloud-hosted and always available
    }

    @Override
    @Async
    public void sendEmailVerificationOtp(String toEmail, String fullName, String otpCode) {
        Map<String, String> params = new HashMap<>();
        params.put("to_name", fullName != null ? fullName : "Quý khách");
        params.put("to_email", toEmail);
        params.put("otp_code", otpCode);
        params.put("expiry_minutes", "10");

        sendViaEmailJs(ACC1_SERVICE_ID, ACC1_TEMPLATE_VERIFY_EMAIL, ACC1_PUBLIC_KEY, ACC1_PRIVATE_KEY, params, "Xác minh email");
    }

    @Override
    @Async
    public void sendChangePasswordOtp(String toEmail, String fullName, String otpCode) {
        Map<String, String> params = new HashMap<>();
        params.put("to_name", fullName != null ? fullName : "Quý khách");
        params.put("to_email", toEmail);
        params.put("otp_code", otpCode);
        params.put("expiry_minutes", "10");

        sendViaEmailJs(ACC2_SERVICE_ID, ACC2_TEMPLATE_CHANGE_PASSWORD, ACC2_PUBLIC_KEY, ACC2_PRIVATE_KEY, params, "Đổi mật khẩu OTP");
    }

    @Override
    @Async
    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        // Fallback or use Account 2
        sendChangePasswordOtp(toEmail, fullName, otpCode);
    }

    @Override
    @Async
    public void sendPasswordChangedNotice(String toEmail, String fullName) {
        log.info("Password changed notice recorded for email: {}", toEmail);
    }

    private void sendViaEmailJs(
            String serviceId,
            String templateId,
            String publicKey,
            String privateKey,
            Map<String, String> params,
            String actionName
    ) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("service_id", serviceId);
            body.put("template_id", templateId);
            body.put("user_id", publicKey);
            body.put("accessToken", privateKey);
            body.put("template_params", params);

            String response = restClient.post()
                    .uri(EMAILJS_URL)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("EmailJS [{}] sent successfully to {}. Response: {}", actionName, params.get("to_email"), response);
        } catch (Exception ex) {
            log.error("Failed to send EmailJS [{}] to {}: {}", actionName, params.get("to_email"), ex.getMessage());
        }
    }
}
