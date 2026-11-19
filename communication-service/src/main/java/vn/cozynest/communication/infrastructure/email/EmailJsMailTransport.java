package vn.cozynest.communication.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmailJsMailTransport {

    private static final Logger log = LoggerFactory.getLogger(EmailJsMailTransport.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final String endpointUrl;
    private final String serviceId;
    private final String templateId;
    private final String publicKey;
    private final String privateKey;
    private final String originHeader;
    private final long baseBackoffMs;

    public EmailJsMailTransport(
            String endpointUrl,
            String serviceId,
            String templateId,
            String publicKey,
            String privateKey,
            String originHeader,
            int connectTimeoutMs,
            int readTimeoutMs,
            long baseBackoffMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(connectTimeoutMs).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(readTimeoutMs).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.endpointUrl = endpointUrl;
        this.serviceId = serviceId;
        this.templateId = templateId;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.originHeader = (originHeader != null && !originHeader.isBlank()) ? originHeader : "http://localhost:3000";
        this.baseBackoffMs = Math.max(0L, baseBackoffMs);
    }

    public void send(String toEmail, String toName, String subject, String plainText, String htmlContent, String actionUrl) {
        Map<String, Object> templateParams = new LinkedHashMap<>();
        templateParams.put("to_email", toEmail);
        templateParams.put("to_name", (toName != null && !toName.isBlank()) ? toName : "Quý khách");
        templateParams.put("subject", subject != null ? subject : "Thông báo Tổ Ấm");
        templateParams.put("message", plainText != null ? plainText : "");
        templateParams.put("message_html", htmlContent != null ? htmlContent : (plainText != null ? plainText : ""));
        templateParams.put("action_url", actionUrl != null ? actionUrl : originHeader);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service_id", serviceId);
        payload.put("template_id", templateId);
        payload.put("user_id", publicKey);
        payload.put("accessToken", privateKey);
        payload.put("template_params", templateParams);

        long backoff = baseBackoffMs;
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(endpointUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", originHeader)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (HttpStatusCodeException ex) {
                int code = ex.getStatusCode().value();
                boolean retryable = code == 429 || code >= 500;
                log.warn("EmailJS HTTP {} on attempt {}/{} (retryable={})", code, attempt, MAX_ATTEMPTS, retryable);
                lastError = new IllegalStateException("EmailJS HTTP " + code, ex);
                if (!retryable) {
                    throw lastError;
                }
            } catch (RuntimeException ex) {
                log.warn("EmailJS transport error on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, ex.getClass().getSimpleName());
                lastError = ex;
            }
            if (attempt < MAX_ATTEMPTS && backoff > 0) {
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("EmailJS retry interrupted", ie);
                }
                backoff *= 2;
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("EmailJS send failed");
    }
}
