package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long roomId;
    private Long tenantId;
    private String tenantName;
    private String tenantAvatar;
    private int rating;
    private String comment;
    private String landlordReply;
    private LocalDateTime createdAt;
}

