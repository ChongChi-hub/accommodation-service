package vn.toam.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityDto {
    private Long id;
    private String code;
    private String name;
    private String icon;
    private String category;
}
