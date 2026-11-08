package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotAreaDto {
    private String code;
    private String name;
    private String fullName;
    private String slug;
    private String imageUrl;
    private Long roomCount;
}

