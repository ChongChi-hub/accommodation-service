package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    private Long id;
    private String title;
    private String subtitle;
    private String badgeText;
    private String imageUrl;
    private String linkUrl;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
