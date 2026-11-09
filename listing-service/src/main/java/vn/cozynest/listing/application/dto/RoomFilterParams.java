package vn.cozynest.listing.application.dto;

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
    private String q;
    private Long provinceId;
    private String provinceCode;
    private String provinceSlug;
    private List<Long> wardIds;
    private String wardCode;
    private String districtCode;
    private List<Long> roomTypeIds;
    private String roomTypeCode;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String priceUnit;
    private Double areaMin;
    private Double areaMax;
    private Double minArea;
    private Double maxArea;
    private Integer bedrooms;
    private Integer occupants;
    private Integer minAvailableSpots;
    private List<Long> amenityIds;
    @Builder.Default
    private String amenityMode = "all"; // all | any
    private Boolean allowPets;
    private Boolean allowCooking;
    private String genderPreference;
    private String furnishing;
    @Builder.Default
    private Boolean includeUnavailable = false;
    private String sort; // newest | priceAsc | priceDesc | areaAsc | areaDesc | relevance
    private String sortBy;
    private Integer page;
    private Integer size;

    public BigDecimal getEffectivePriceMin() {
        return priceMin != null ? priceMin : minPrice;
    }

    public BigDecimal getEffectivePriceMax() {
        return priceMax != null ? priceMax : maxPrice;
    }

    public Double getEffectiveAreaMin() {
        return areaMin != null ? areaMin : minArea;
    }

    public Double getEffectiveAreaMax() {
        return areaMax != null ? areaMax : maxArea;
    }

    public String getEffectiveSort() {
        return sort != null && !sort.isBlank() ? sort : sortBy;
    }
}
