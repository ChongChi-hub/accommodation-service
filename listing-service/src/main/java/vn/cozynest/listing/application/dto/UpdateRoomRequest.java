package vn.cozynest.listing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private Long wardId;

    private Long roomTypeId;
    private String roomTypeCode;

    @NotNull(message = "Diện tích không được để trống")
    @Positive(message = "Diện tích phải lớn hơn 0")
    private Double area;

    private Integer maxOccupants;

    @NotNull(message = "Giá thuê không được để trống")
    @Positive(message = "Giá thuê phải lớn hơn 0")
    private BigDecimal price;

    private BigDecimal deposit;
    private BigDecimal electricityCost;
    private BigDecimal waterCost;
    private BigDecimal internetCost;
    private BigDecimal serviceCost;

    private Boolean allowPets;
    private Boolean allowCooking;
}

