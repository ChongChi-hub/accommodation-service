package vn.cozynest.listing.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import vn.cozynest.common.storage.EncryptedLocalStorageService;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.SignedDownloadTokenService;

import java.util.Arrays;

@Configuration
public class PrivateStorageConfig {

    @Bean
    public PrivateObjectStoragePort privateObjectStoragePort(
            @Value("${storage.private-dir:./storage/private}") String privateDir,
            @Value("${storage.aes-key-base64:}") String aesKeyBase64,
            Environment env
    ) {
        boolean isProd = env.getActiveProfiles() != null
                && Arrays.stream(env.getActiveProfiles()).anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        return new EncryptedLocalStorageService(privateDir, aesKeyBase64, isProd);
    }

    @Bean
    public SignedDownloadTokenService signedDownloadTokenService(
            @Value("${storage.hmac-secret:cozynest-dev-hmac-secret-change-in-prod-32bytes}") String hmacSecret,
            Environment env
    ) {
        boolean isProd = env.getActiveProfiles() != null
                && Arrays.stream(env.getActiveProfiles()).anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        if (isProd && hmacSecret.contains("change-in-prod")) {
            throw new IllegalStateException("Production profile requires non-default storage.hmac-secret");
        }
        return new SignedDownloadTokenService(hmacSecret);
    }
}

