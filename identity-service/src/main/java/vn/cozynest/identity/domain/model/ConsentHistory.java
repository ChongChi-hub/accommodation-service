package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ConsentHistory {

    private final String id;
    private final String userId;
    private final String consentType;
    private final String version;
    private final boolean agreed;
    private final String ipAddress;
    private final String userAgent;
    private final String metadataJson;
    private final Instant createdAt;

    public ConsentHistory(
            String id,
            String userId,
            String consentType,
            String version,
            boolean agreed,
            String ipAddress,
            String userAgent,
            String metadataJson,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.consentType = consentType != null ? consentType : "TERMS_OF_SERVICE";
        this.version = version != null ? version : "v1.0";
        this.agreed = agreed;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ConsentHistory of(
            String userId,
            String consentType,
            String version,
            boolean agreed,
            String ipAddress,
            String userAgent,
            String metadataJson
    ) {
        return new ConsentHistory(
                UUID.randomUUID().toString(),
                userId,
                consentType,
                version,
                agreed,
                ipAddress,
                userAgent,
                metadataJson,
                Instant.now()
        );
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getConsentType() { return consentType; }
    public String getVersion() { return version; }
    public boolean isAgreed() { return agreed; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getMetadataJson() { return metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
