package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cms_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsArticleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "summary_vi", nullable = false, columnDefinition = "TEXT")
    private String summaryVi;

    @Column(name = "summary_en", nullable = false, columnDefinition = "TEXT")
    private String summaryEn;

    @Column(name = "content_vi", nullable = false, columnDefinition = "LONGTEXT")
    private String contentVi;

    @Column(name = "content_en", nullable = false, columnDefinition = "LONGTEXT")
    private String contentEn;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String category = "GUIDE";

    @Column(name = "reading_minutes", nullable = false)
    @Builder.Default
    private Integer readingMinutes = 5;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_alt_vi", nullable = false)
    private String imageAltVi;

    @Column(name = "image_alt_en", nullable = false)
    private String imageAltEn;

    @Column(name = "published_at", nullable = false)
    @Builder.Default
    private LocalDateTime publishedAt = LocalDateTime.now();

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

