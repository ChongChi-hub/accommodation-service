package vn.cozynest.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageAccessLog {

    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String actorId;
    private String actorRole;
    private String bucket;
    private String objectKey;
    private String action;
    private boolean allowed;
    private String reason;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
