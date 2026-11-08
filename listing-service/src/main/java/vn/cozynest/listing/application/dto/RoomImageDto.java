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
public class RoomImageDto {

    private Long id;
    private Long roomId;
    private String imageUrl;
    private String storageKey;
    private String thumbKey;
    private String mediumKey;
    private String largeKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Integer width;
    private Integer height;
    private boolean cover;
    private Integer sortOrder;
    private String altText;
    private LocalDateTime createdAt;
}

