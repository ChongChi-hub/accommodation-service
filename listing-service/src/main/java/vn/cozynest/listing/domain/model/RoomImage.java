package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "thumb_key", length = 255)
    private String thumbKey;

    @Column(name = "medium_key", length = 255)
    private String mediumKey;

    @Column(name = "large_key", length = 255)
    private String largeKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 50)
    @Builder.Default
    private String contentType = "image/webp";

    @Column(name = "size_bytes")
    @Builder.Default
    private Long sizeBytes = 102400L;

    @Column(name = "width")
    @Builder.Default
    private Integer width = 1280;

    @Column(name = "height")
    @Builder.Default
    private Integer height = 720;

    @Column(name = "is_cover", nullable = false)
    @Builder.Default
    private boolean cover = false;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
