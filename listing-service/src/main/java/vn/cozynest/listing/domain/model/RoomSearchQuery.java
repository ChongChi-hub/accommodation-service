package vn.cozynest.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure domain query criteria for searching public room listings.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSearchQuery {
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
    private String priceUnit;
    private Double areaMin;
    private Double areaMax;
    private Integer bedrooms;
    private Integer occupants;
    private Integer minAvailableSpots;
    private List<Long> amenityIds;
    @Builder.Default
    private String amenityMode = "all";
    private Boolean allowPets;
    private Boolean allowCooking;
    private String genderPreference;
    private String furnishing;
    @Builder.Default
    private Boolean includeUnavailable = false;
    private String sort;
    private Integer page;
    private Integer size;
}
