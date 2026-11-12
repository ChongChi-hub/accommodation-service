package vn.cozynest.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipDocAccessLog {
    private String id;
    private String docId;
    private Long roomId;
    private String actorId;
    private String actorRole;
    private String action;
    private LocalDateTime accessedAt;

    public static OwnershipDocAccessLog record(String docId, Long roomId, String actorId, String actorRole, String action) {
        return OwnershipDocAccessLog.builder()
                .id(UUID.randomUUID().toString())
                .docId(docId)
                .roomId(roomId)
                .actorId(actorId != null ? actorId : "UNKNOWN")
                .actorRole(actorRole != null ? actorRole : "MODERATOR")
                .action(action)
                .accessedAt(LocalDateTime.now())
                .build();
    }
}

