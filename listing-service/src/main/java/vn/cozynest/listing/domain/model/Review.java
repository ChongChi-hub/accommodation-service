package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    private Long id;
    private Long roomId;
    private Long tenantId;
    private String tenantName;
    private String tenantAvatar;
    private int rating;

    @Builder.Default
    private boolean verifiedTenant = true;

    private String comment;
    private String landlordReply;
    private LocalDateTime createdAt;
}
