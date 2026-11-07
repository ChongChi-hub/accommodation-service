package vn.toam.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFilterParams {
    private String provinceCode;
    private String districtCode;
    private String roomTypeCode;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minArea;
    private Double maxArea;
    private Integer occupants;
    private Boolean allowPets;
    private Boolean allowCooking;
    private List<Long> amenityIds;
    private String sortBy; // "price_asc", "price_desc", "rating", "newest"
    private Integer page;
    private Integer size;
}

