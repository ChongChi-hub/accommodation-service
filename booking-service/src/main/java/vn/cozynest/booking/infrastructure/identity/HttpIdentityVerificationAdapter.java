package vn.cozynest.booking.infrastructure.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.common.resilience.ServiceCircuitBreaker;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Component
public class HttpIdentityVerificationAdapter implements IdentityVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpIdentityVerificationAdapter.class);
    private final RestClient restClient;
    private final String internalServiceKey;
    private final boolean sandboxBypassAllowed;
    private final ServiceCircuitBreaker circuitBreaker = new ServiceCircuitBreaker(3, Duration.ofSeconds(30));

    @Autowired
    public HttpIdentityVerificationAdapter(
            @Value("${app.identity-service.url:http://localhost:8081}") String identityBaseUrl,
            @Value("${app.internal.service-key:CozyNestInternalServiceKeySecret2026}") String internalServiceKey,
            @Value("${app.booking.identity.allow-sandbox-bypass:false}") boolean allowSandboxBypass,
            Environment env
    ) {
        this.restClient = RestClient.builder().baseUrl(identityBaseUrl).build();
        this.internalServiceKey = internalServiceKey;
        String[] active = env != null ? env.getActiveProfiles() : null;
        boolean restricted = active != null && Arrays.stream(active)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        if (restricted && "CozyNestInternalServiceKeySecret2026".equals(internalServiceKey)) {
            throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
        }
        this.sandboxBypassAllowed = allowSandboxBypass && !restricted;
    }

    @Override
    public boolean isPhoneVerified(String userId) {
        return circuitBreaker.executeWithFallback(2, () -> {
            Map<String, Object> body = restClient.get()
                    .uri("/internal/users/{id}/phone-status", userId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof Map<?, ?> data) {
                return Boolean.TRUE.equals(data.get("phoneVerified"));
            }
            return false;
        }, () -> {
            if (sandboxBypassAllowed) {
                log.warn("Identity phone check fell back to sandbox bypass for userId={}", userId);
                return true;
            }
            log.error("Identity phone check failed/open circuit for userId={}; failing closed", userId);
            return false;
        });
    }

    @Override
    public boolean verifyContractOtp(String userId, String otpCode) {
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
                log.warn("Contract OTP fell back to sandbox bypass for userId={}", userId);
                return "123456".equals(otpCode);
            }
            log.error("Identity OTP verification failed/open circuit for userId={}; failing closed", userId);
            return false;
        });
    }
}

