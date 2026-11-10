package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFacetsDto {

    @Builder.Default
    private List<FacetItemDto> provinces = new ArrayList<>();

    @Builder.Default
    private PriceRangeFacetDto priceRange = new PriceRangeFacetDto();

    @Builder.Default
    private List<AmenityFacetDto> amenities = new ArrayList<>();

    @Builder.Default
    private List<FacetItemDto> roomTypes = new ArrayList<>();

    @Builder.Default
    private List<FacetItemDto> categories = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetItemDto {
        private String code;
        private String name;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRangeFacetDto {
        private BigDecimal min;
        private BigDecimal max;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmenityFacetDto {
        private Long id;
        private String code;
        private String name;
        private Long count;
    }
}

