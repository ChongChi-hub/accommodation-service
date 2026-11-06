package com.service.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRoomRequest {
    @NotBlank(message = "Thiếu loại phòng")
    private String roomTypeId;

    @NotBlank(message = "Thiếu phường/xã")
    private String wardId;

    @NotBlank(message = "Thiếu tiêu đề phòng")
    private String title;

    private String description;

    @NotBlank(message = "Thiếu địa chỉ")
    private String address;

    private Double latitude;
    private Double longitude;

    @NotNull(message = "Thiếu diện tích")
    private BigDecimal area;

    @NotNull(message = "Thiếu giá phòng")
    private BigDecimal price;

    private BigDecimal deposit;
    private Integer maxOccupants = 1;
    private Boolean allowPet = false;
    private Boolean allowCooking = false;
    private Boolean liveWithOwner = false;
    private String curfewTime;
    private String extraRules;
    private LocalDate availableFrom;

    private List<String> images;
    private List<String> amenityIds;
    private List<String> amenities; // alias for amenityIds
    private List<RoomPriceDto> prices;
}

