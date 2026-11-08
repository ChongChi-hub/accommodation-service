package vn.cozynest.identity.infrastructure.ekyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.port.FaceProviderResult;
import vn.cozynest.identity.domain.port.FaceVerificationProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component("vendorFaceVerificationProvider")
public class VendorFaceVerificationProvider implements FaceVerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(VendorFaceVerificationProvider.class);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public VendorFaceVerificationProvider(
            @Value("${face.vendor.base-url:}") String baseUrl,
            @Value("${face.vendor.api-key:}") String apiKey) {
        this.baseUrl = (baseUrl != null && !baseUrl.contains("REPLACE_IF")) ? baseUrl.trim() : "";
        this.apiKey = (apiKey != null && !apiKey.contains("REPLACE_IF")) ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        if (this.baseUrl.isBlank()) {
            log.warn("VendorFaceVerificationProvider: Vendor credentials not configured. Will safely fallback to manual review.");
        }
    }

    @Override
    public String getProviderName() {
        return "vendor";
    }

    @Override
    public FaceProviderResult verify(FaceVerification verification, List<FaceVerificationFrame> frames) {
        if (verification.getConsentAt() == null) {
            log.warn("Vendor verification rejected: explicit biometric consent is missing for user {}", verification.getUserId());
            return FaceProviderResult.pendingManualReview();
        }

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            log.warn("Vendor endpoint not configured. Routing submission to manual moderator review.");
            return FaceProviderResult.pendingManualReview();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/ekyc/verify-face"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"verificationId\":\"" + verification.getId() + "\"}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"status\":\"PASSED\"")) {
                return FaceProviderResult.autoPassed();
            } else {
                return FaceProviderResult.pendingManualReview();
            }
        } catch (Exception e) {
            log.warn("Vendor call failed ({}). Gracefully falling back to manual review.", e.getMessage());
            return FaceProviderResult.pendingManualReview();
        }
    }
}

