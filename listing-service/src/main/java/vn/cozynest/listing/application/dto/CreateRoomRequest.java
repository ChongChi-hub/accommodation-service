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
public class CreateRoomRequest {

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

    private String priceUnit;

    private BigDecimal deposit;
    private BigDecimal electricityCost;
    private BigDecimal waterCost;
    private BigDecimal internetCost;
    private BigDecimal serviceCost;

    @NotNull(message = "Chỉ số công tơ điện ban đầu không được để trống")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Chỉ số công tơ điện ban đầu phải từ 0 trở lên")
    private BigDecimal initialElectricityReading;

    @NotNull(message = "Chỉ số đồng hồ nước ban đầu không được để trống")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Chỉ số đồng hồ nước ban đầu phải từ 0 trở lên")
    private BigDecimal initialWaterReading;

    private Boolean allowPets;
    private Boolean allowCooking;

    private String houseRules;
    private java.time.LocalDate availableDate;

    @jakarta.validation.constraints.Min(value = 1, message = "Thời hạn thuê tối thiểu phải từ 1 tháng trở lên")
    private Integer minLeaseMonths;

    private Boolean submitForReview;
}
