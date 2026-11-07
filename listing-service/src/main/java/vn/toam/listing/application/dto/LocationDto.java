package vn.toam.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LocationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvinceDto {
        private Long id;
        private String code;
        private String name;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictDto {
        private Long id;
        private String code;
        private String name;
        private String fullName;
        private String provinceCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WardDto {
        private Long id;
        private String code;
        private String name;
        private String fullName;
        private String districtCode;
    }
}
