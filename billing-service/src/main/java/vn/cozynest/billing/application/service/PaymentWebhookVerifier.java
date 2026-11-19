package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;

@Component
public class PaymentWebhookVerifier {
    private final String webhookSecret;
    private final String previousWebhookSecret;
    private final boolean strictIpAllowlist;
    private final Set<String> allowedIps;

    public PaymentWebhookVerifier(
            @Value("${fintech.gateway.webhook-secret:}") String webhookSecret,
            @Value("${fintech.gateway.webhook-secret-previous:}") String previousWebhookSecret,
            @Value("${fintech.gateway.strict-ip-allowlist:false}") boolean strictIpAllowlist,
            @Value("${fintech.gateway.allowed-ips:127.0.0.1,::1}") String allowedIpsRaw) {
        this.webhookSecret = resolveSecretContent(webhookSecret, "PAYMENT_WEBHOOK_SECRET");
        this.previousWebhookSecret = resolveSecretContent(previousWebhookSecret, "PAYMENT_WEBHOOK_SECRET_PREVIOUS");
        this.strictIpAllowlist = strictIpAllowlist;
        this.allowedIps = parseIpList(allowedIpsRaw);
    }

    public boolean isValidSignature(String signedData, String providedSig) {
        if (providedSig == null || providedSig.isBlank()) return false;
        if (constantTimeMatches(computeHmacSha256(signedData, webhookSecret), providedSig)) return true;
        return previousWebhookSecret != null && !previousWebhookSecret.isBlank()
                && constantTimeMatches(computeHmacSha256(signedData, previousWebhookSecret), providedSig);
    }

    public boolean isIpAllowed(String sourceIp) {
        if (!strictIpAllowlist) return true;
        return sourceIp != null && allowedIps.contains(sourceIp.trim());
    }

    public boolean isTimestampValid(long timestamp, long maxSkewSeconds) {
        return Math.abs(Instant.now().getEpochSecond() - timestamp) <= maxSkewSeconds;
    }

    public String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Loi tinh HMAC-SHA256", e);
        }
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    private boolean constantTimeMatches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String resolveSecretContent(String rawInput, String envName) {
        String val = rawInput;
        if (val == null || val.isBlank()) val = System.getenv(envName);
        if (val == null || val.isBlank()) {
            String path = System.getenv(envName + "_PATH");
            if (path != null && !path.isBlank()) val = "file:" + path;
        }
        if (val == null || val.isBlank()) return "";
        if (val.startsWith("file:")) {
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(val.substring(5).trim())).trim();
            } catch (Exception ex) {
                return val;
            }
        }
        return val.trim();
    }

    private static Set<String> parseIpList(String raw) {
        Set<String> set = new HashSet<>();
        if (raw != null) {
            for (String s : raw.split(",")) {
                if (!s.isBlank()) set.add(s.trim());
            }
        }
        return set;
    }
}

