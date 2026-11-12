package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStatusHistory {

    private Long id;
    private Long roomId;
    private String fromStatus;
    private String toStatus;
    private String actorId;
    private String actorRole;
    private String reason;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
