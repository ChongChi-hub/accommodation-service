package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImage {

    private Long id;
    private Long roomId;
    private String imageUrl;
    private String storageKey;
    private String thumbKey;
    private String mediumKey;
    private String largeKey;
    private String originalFilename;

    @Builder.Default
    private String contentType = "image/webp";

    @Builder.Default
    private Long sizeBytes = 102400L;

    @Builder.Default
    private Integer width = 1280;

    @Builder.Default
    private Integer height = 720;

    @Builder.Default
    private boolean cover = false;

    @Builder.Default
    private boolean primary = false;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Integer displayOrder = 0;

    private String altText;
    private String uploadedBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
