package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsArticle {

    private Long id;
    private String slug;
    private String titleVi;
    private String titleEn;
    private String summaryVi;
    private String summaryEn;
    private String contentVi;
    private String contentEn;

    @Builder.Default
    private String category = "GUIDE";

    @Builder.Default
    private Integer readingMinutes = 5;

    private String imageUrl;
    private String imageAltVi;
    private String imageAltEn;

    @Builder.Default
    private LocalDateTime publishedAt = LocalDateTime.now();

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
