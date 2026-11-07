package vn.toam.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private int totalUploaded;
    private int totalFailed;
    private List<ImageUploadItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUploadItemResult {
        private Long imageId;
        private String originalFilename;
        private String imageUrl;
        private String thumbUrl;
        private String mediumUrl;
        private String largeUrl;
        private boolean isCover;
        private Integer sortOrder;
        private boolean success;
        private String errorMessage;
    }
}

