package vn.cozynest.common.storage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Pure Java AWS Signature Version 4 helper for Cloudflare R2 / AWS S3 / MinIO presigned URLs.
 */
public final class AwsSigV4Signer {

    private static final DateTimeFormatter ISO_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private AwsSigV4Signer() {
    }

    public static String createPresignedUrl(
            String method,
            String endpoint,
            String bucket,
            String key,
            String accessKey,
            String secretKey,
            String region,
            long ttlSeconds
    ) {
        Instant now = Instant.now();
        String dateStamp = DATE_ONLY.format(now);
        String amzDate = ISO_BASIC.format(now);
        long expires = Math.min(Math.max(ttlSeconds, 15), 604800); // 15s to 7 days

        String safeRegion = (region != null && !region.isBlank()) ? region.trim() : "auto";
        String credentialScope = dateStamp + "/" + safeRegion + "/s3/aws4_request";
        String cleanEndpoint = endpoint.replaceAll("/+$", "");
        URI baseUri = URI.create(cleanEndpoint);
        String host = baseUri.getHost() + (baseUri.getPort() > 0 && baseUri.getPort() != 80 && baseUri.getPort() != 443 ? ":" + baseUri.getPort() : "");

        String canonicalUri = "/" + bucket + "/" + key.replaceAll("^/+", "");
        String queryParams = "X-Amz-Algorithm=AWS4-HMAC-SHA256"
                + "&X-Amz-Credential=" + urlEncode(accessKey + "/" + credentialScope)
                + "&X-Amz-Date=" + amzDate
                + "&X-Amz-Expires=" + expires
                + "&X-Amz-SignedHeaders=host";

        String canonicalRequest = method.toUpperCase() + "\n"
                + canonicalUri + "\n"
                + queryParams + "\n"
                + "host:" + host + "\n\n"
                + "host\n"
                + "UNSIGNED-PAYLOAD";

        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] signingKey = deriveSigningKey(secretKey, dateStamp, safeRegion);
        String signature = hmacSha256Hex(signingKey, stringToSign);

        return cleanEndpoint + canonicalUri + "?" + queryParams + "&X-Amz-Signature=" + signature;
    }

    private static byte[] deriveSigningKey(String secretKey, String dateStamp, String region) {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, "s3");
        return hmacSha256(kService, "aws4_request");
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed computing HMAC-SHA256", e);
        }
    }

    private static String hmacSha256Hex(byte[] key, String data) {
        return HexFormat.of().formatHex(hmacSha256(key, data));
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed computing SHA-256", e);
        }
    }

    private static String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
