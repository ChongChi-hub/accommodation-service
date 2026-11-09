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
public class CmsArticleDto {
    private Long id;
    private String slug;
    private String titleVi;
    private String titleEn;
    private String summaryVi;
    private String summaryEn;
    private String contentVi;
    private String contentEn;
    private String category;
    private Integer readingMinutes;
    private String imageUrl;
    private String imageAltVi;
    private String imageAltEn;
    private LocalDateTime publishedAt;
    private Integer sortOrder;
}

