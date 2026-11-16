package vn.cozynest.listing.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import vn.cozynest.common.storage.ObjectStoragePort;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.S3CompatibleStorageAdapter;
import vn.cozynest.common.storage.SignedDownloadTokenService;

import java.util.Arrays;

@Configuration
public class PrivateStorageConfig {

    @Bean
    @org.springframework.context.annotation.Primary
    public S3CompatibleStorageAdapter s3CompatibleStorageAdapter(
            @Value("${storage.provider:${STORAGE_PROVIDER:local}}") String provider,
            @Value("${storage.s3.endpoint:${S3_ENDPOINT:http://localhost:9000}}") String endpoint,
            @Value("${storage.s3.region:${S3_REGION:auto}}") String region,
            @Value("${storage.s3.access-key:${S3_ACCESS_KEY:}}") String accessKey,
            @Value("${storage.s3.secret-key:${S3_SECRET_KEY:}}") String secretKey,
            @Value("${storage.s3.public-bucket:${S3_PUBLIC_BUCKET:cozynest-public}}") String publicBucket,
            @Value("${storage.s3.private-bucket:${S3_PRIVATE_BUCKET:cozynest-private}}") String privateBucket,
            @Value("${storage.s3.public-cdn-url:${S3_PUBLIC_CDN_URL:http://localhost:8080/api/v1/media}}") String publicCdnUrl,
            @Value("${storage.base-dir:${STORAGE_BASE_DIR:./storage}}") String baseDir,
            @Value("${storage.aes-key-base64:${STORAGE_AES_KEY_BASE64:}}") String aesKeyBase64
    ) {
        return new S3CompatibleStorageAdapter(
                provider, endpoint, region, accessKey, secretKey,
                publicBucket, privateBucket, publicCdnUrl, baseDir, aesKeyBase64
        );
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
