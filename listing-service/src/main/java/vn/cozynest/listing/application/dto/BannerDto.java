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
public class BannerDto {
    private Long id;
    private String title;
    private String subtitle;
    private String badgeText;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private boolean active;
}

