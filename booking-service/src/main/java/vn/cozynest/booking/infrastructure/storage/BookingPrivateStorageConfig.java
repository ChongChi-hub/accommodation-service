package vn.cozynest.booking.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import vn.cozynest.common.storage.EncryptedLocalStorageService;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.SignedDownloadTokenService;

import java.util.Arrays;

@Configuration
public class BookingPrivateStorageConfig {

    @Bean
    public PrivateObjectStoragePort privateObjectStoragePort(
            @Value("${storage.private-dir:./storage/booking-private}") String privateDir,
            @Value("${storage.aes-key-base64:}") String aesKeyBase64,
            Environment env
    ) {
        boolean isProd = isProduction(env);
        return new EncryptedLocalStorageService(privateDir, aesKeyBase64, isProd);
    }

    @Bean
    public SignedDownloadTokenService signedDownloadTokenService(
            @Value("${storage.hmac-secret:cozynest-dev-hmac-secret-change-in-prod-32bytes}") String hmacSecret,
            Environment env
    ) {
        if (isProduction(env) && hmacSecret.contains("change-in-prod")) {
            throw new IllegalStateException("Production profile requires non-default storage.hmac-secret");
        }
        return new SignedDownloadTokenService(hmacSecret);
    }

    private boolean isProduction(Environment env) {
        String[] active = env.getActiveProfiles();
        return active != null && Arrays.stream(active)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
    }
}

