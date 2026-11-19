package vn.cozynest.billing.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.port.NotificationProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Component
public class ConfigurableBankNotificationProvider implements NotificationProvider {
    private final ObjectMapper objectMapper;
    private final String providerName;
    private final String webhookSecret;
    private final String previousSecret;
    private final long windowSeconds;
    private final String platformAccountNumber;

    @Autowired
    public ConfigurableBankNotificationProvider(
            ObjectMapper objectMapper,
            @Value("${fintech.webhook.provider:FAKE}") String providerName,
            @Value("${fintech.gateway.webhook-secret:cozynest-dev-fake-webhook-secret-2026}") String webhookSecret,
            @Value("${fintech.gateway.webhook-secret-previous:}") String previousSecret,
            @Value("${fintech.webhook.window-seconds:300}") long windowSeconds,
            @Value("${fintech.platform.account-number:1042701900}") String platformAccountNumber) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.providerName = providerName != null && !providerName.isBlank() ? providerName.trim().toUpperCase(Locale.ROOT) : "FAKE";
        this.webhookSecret = webhookSecret != null && !webhookSecret.isBlank() ? webhookSecret : "cozynest-dev-fake-webhook-secret-2026";
        this.previousSecret = previousSecret != null ? previousSecret : "";
        this.windowSeconds = windowSeconds > 0 ? windowSeconds : 300L;
        this.platformAccountNumber = platformAccountNumber != null && !platformAccountNumber.isBlank() ? platformAccountNumber.trim() : "1042701900";
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public boolean verifySignature(String rawBody, String signature, long timestampSeconds) {
        if (rawBody == null || signature == null || signature.isBlank()) {
            return false;
        }
        String cleanSig = signature.trim().toLowerCase(Locale.ROOT);
        String msgWithTs = rawBody + "." + timestampSeconds;
        if (constantTimeEquals(hmacSha256(msgWithTs, webhookSecret), cleanSig)
                || constantTimeEquals(hmacSha256(rawBody, webhookSecret), cleanSig)) {
            return true;
        }
        if (!previousSecret.isBlank()) {
            return constantTimeEquals(hmacSha256(msgWithTs, previousSecret), cleanSig)
                    || constantTimeEquals(hmacSha256(rawBody, previousSecret), cleanSig);
        }
        return false;
    }

    @Override
    public boolean isTimestampWithinWindow(long timestampSeconds) {
        long now = Instant.now().getEpochSecond();
        return Math.abs(now - timestampSeconds) <= windowSeconds;
    }

    @Override
    public ParsedBankNotification parseNotification(String rawBody, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;

            String txId = firstText(data, root, "providerTxId", "transactionId", "reference", "id", "bankTxCode", "orderCode");
            String explicitCode = firstText(data, root, "paymentCode");
            if ((txId == null || txId.isBlank()) && explicitCode != null) {
                txId = "WH-" + explicitCode + "-" + firstLong(data, root, 0L, "amount", "transferAmount");
            }
            String dir = firstText(data, root, "direction", "transferType", "type");
            if (dir == null || dir.isBlank()) {
                dir = "IN";
            } else if ("in".equalsIgnoreCase(dir) || "CREDIT".equalsIgnoreCase(dir) || "TIEN_VAO".equalsIgnoreCase(dir)) {
                dir = "IN";
            } else if ("out".equalsIgnoreCase(dir) || "DEBIT".equalsIgnoreCase(dir) || "TIEN_RA".equalsIgnoreCase(dir)) {
                dir = "OUT";
            }

            long amount = firstLong(data, root, 0L, "amount", "transferAmount", "value");
            String receiverAcc = firstText(data, root, "receiverAccount", "accountNumber", "subAccount", "virtualAccountNumber");
            if (receiverAcc == null || receiverAcc.isBlank()) {
                receiverAcc = platformAccountNumber;
            }
            String content = firstText(data, root, "content", "description", "transferContent", "addInfo", "paymentCode");
            long ts = firstLong(data, root, Instant.now().getEpochSecond(), "timestamp", "transactionDateTime");
            Instant occurredAt = ts > 10_000_000_000L ? Instant.ofEpochMilli(ts) : Instant.ofEpochSecond(ts);

            return new ParsedBankNotification(providerName, txId, dir.toUpperCase(Locale.ROOT),
                    amount, receiverAcc.trim(), content != null ? content : "", occurredAt, explicitCode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload webhook khong dung dinh dang JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String sanitizePayload(String rawBody) {
        if (rawBody == null) return "";
        return rawBody
                .replaceAll("(?i)(\"(?:accountNumber|receiverAccount|senderAccount|counterAccountNumber)\"\\s*:\\s*\")([0-9A-Za-z]{2,})(\")", "$1******$3")
                .replaceAll("(?i)(\"(?:secret|apiKey|checksumKey|token|authorization)\"\\s*:\\s*\")([^\"]+)(\")", "$1[REDACTED]$3")
                .replaceAll("\\b[0-9]{8,19}\\b", "******");
    }

    @Override
    public String computeTestSignature(String rawBody, long timestampSeconds) {
        return hmacSha256(rawBody + "." + timestampSeconds, webhookSecret);
    }

    private String firstText(JsonNode primary, JsonNode fallback, String... fields) {
        for (String f : fields) {
            if (primary != null && primary.hasNonNull(f) && !primary.get(f).asText().isBlank()) {
                return primary.get(f).asText().trim();
            }
            if (fallback != null && fallback.hasNonNull(f) && !fallback.get(f).asText().isBlank()) {
                return fallback.get(f).asText().trim();
            }
        }
        return null;
    }

    private long firstLong(JsonNode primary, JsonNode fallback, long defaultVal, String... fields) {
        for (String f : fields) {
            if (primary != null && primary.hasNonNull(f)) return primary.get(f).asLong(defaultVal);
            if (fallback != null && fallback.hasNonNull(f)) return fallback.get(f).asLong(defaultVal);
        }
        return defaultVal;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC calculation failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

