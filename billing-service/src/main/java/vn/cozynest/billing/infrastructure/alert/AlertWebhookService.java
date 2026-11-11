package vn.cozynest.billing.infrastructure.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.cozynest.billing.application.port.AlertNotificationPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AlertWebhookService implements AlertNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(AlertWebhookService.class);

    private final String webhookUrl;
    private final HttpClient httpClient;

    public AlertWebhookService(@Value("${app.ledger.alert-webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl != null ? webhookUrl.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean sendAlert(String title, String message) {
        log.warn("🚨 [LEDGER ALERT] {}: {}", title, message);

        if (webhookUrl.isEmpty()) {
            log.info("ℹ️ Không có cấu hình app.ledger.alert-webhook-url. Bỏ qua gửi HTTP Webhook.");
            return false;
        }

        try {
            String payload;
            if (webhookUrl.contains("slack.com")) {
                // Slack incoming webhook format
                payload = String.format("{\"text\":\"*%s*\\n%s\"}", escapeJson(title), escapeJson(message));
            } else {
                // Generic / Telegram format
                payload = String.format("{\"text\":\"%s: %s\"}", escapeJson(title), escapeJson(message));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("✅ Gửi cảnh báo sổ cái qua webhook thành công: HTTP {}", response.statusCode());
                return true;
            } else {
                log.error("❌ Gửi cảnh báo sổ cái qua webhook thất bại: HTTP {} - Body: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Lỗi ngoại lệ khi gửi cảnh báo webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
