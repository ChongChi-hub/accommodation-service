package vn.cozynest.billing.infrastructure.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * HTTP adapter calling identity-service internal verification endpoints with fail-closed prod guards.
 */
@Component
public class HttpBillingIdentityVerificationAdapter implements BillingIdentityVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpBillingIdentityVerificationAdapter.class);
    private static final String DEFAULT_DEV_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient restClient;
    private final String internalServiceKey;
    private final boolean sandboxBypassAllowed;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpBillingIdentityVerificationAdapter(
            @Value("${app.identity-service.url:http://localhost:8081}") String identityBaseUrl,
            @Value("${app.internal.service-key:" + DEFAULT_DEV_KEY + "}") String internalServiceKey,
            @Value("${app.billing.identity.allow-sandbox-bypass:false}") boolean allowSandboxBypass,
            Environment env
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(3));
        rf.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(identityBaseUrl).requestFactory(rf).build();
        this.internalServiceKey = internalServiceKey;

        String[] active = env != null ? env.getActiveProfiles() : null;
        boolean restricted = active != null && Arrays.stream(active)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        if (restricted && (internalServiceKey == null || internalServiceKey.isBlank() || DEFAULT_DEV_KEY.equals(internalServiceKey))) {
            throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key without default dev key");
        }
        this.sandboxBypassAllowed = allowSandboxBypass && !restricted;
    }

    @Override
    public boolean verifyOtp(String userId, String otpCode) {
        if (otpCode == null || !otpCode.matches("^[0-9]{6}$")) {
            return false;
        }
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.post()
                    .uri("/internal/users/{id}/verify-otp", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(Map.of("otpCode", otpCode))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                return Boolean.TRUE.equals(data.get("verified"));
            }
            return false;
        }, () -> {
            if (sandboxBypassAllowed) {
                log.warn("Identity OTP check fell back to sandbox bypass for userId={}", userId);
                return "123456".equals(otpCode);
            }
            log.error("Identity OTP verification failed/circuit open for userId={}; failing closed", userId);
            return false;
        });
    }

    @Override
    public KycVerificationSummary getKycStatus(String userId) {
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/internal/users/{id}/kyc-status", userId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                Object statusObj = data.get("kycStatus");
                String status = statusObj != null ? String.valueOf(statusObj) : "NONE";
                boolean approved = Boolean.TRUE.equals(data.get("approved")) || Boolean.TRUE.equals(data.get("isApproved"));
                Object fullNameObj = data.get("verifiedFullName");
                String verifiedFullName = fullNameObj != null ? String.valueOf(fullNameObj) : null;
                return new KycVerificationSummary(userId, status, approved, verifiedFullName);
            }
            return new KycVerificationSummary(userId, "UNKNOWN", false, null);
        }, () -> {
            if (sandboxBypassAllowed) {
                log.warn("Identity KYC check fell back to sandbox unavailable for userId={}", userId);
            }
            log.error("Identity KYC check failed/circuit open for userId={}; failing closed", userId);
            return new KycVerificationSummary(userId, "UNAVAILABLE", false, null);
        });
    }
}
