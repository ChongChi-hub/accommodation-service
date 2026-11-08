package vn.cozynest.listing.domain.service;

import vn.cozynest.listing.application.dto.RoomFilterParams;
import vn.cozynest.listing.domain.exception.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RoomSearchCriteriaValidator {

    public static void validate(RoomFilterParams params) {
        List<ValidationException.FieldError> errors = new ArrayList<>();

        if (params.getQ() != null && params.getQ().trim().length() > 100) {
            errors.add(new ValidationException.FieldError("q", "Từ khóa tìm kiếm không được vượt quá 100 ký tự"));
        }

        BigDecimal priceMin = params.getEffectivePriceMin();
        BigDecimal priceMax = params.getEffectivePriceMax();

        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new ValidationException.FieldError("priceMin", "Giá tối thiểu không được là số âm"));
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new ValidationException.FieldError("priceMax", "Giá tối đa không được là số âm"));
        }
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            errors.add(new ValidationException.FieldError("priceMin", "priceMin không được lớn hơn priceMax"));
        }

        Double areaMin = params.getEffectiveAreaMin();
        Double areaMax = params.getEffectiveAreaMax();

        if (areaMin != null && areaMin < 0) {
            errors.add(new ValidationException.FieldError("areaMin", "Diện tích tối thiểu không được là số âm"));
        }
        if (areaMax != null && areaMax < 0) {
            errors.add(new ValidationException.FieldError("areaMax", "Diện tích tối đa không được là số âm"));
        }
        if (areaMin != null && areaMax != null && areaMin > areaMax) {
            errors.add(new ValidationException.FieldError("areaMin", "areaMin không được lớn hơn areaMax"));
        }

        if (params.getSize() != null && params.getSize() > 50) {
            errors.add(new ValidationException.FieldError("size", "Số lượng kết quả mỗi trang tối đa là 50"));
        }
        if (params.getPage() != null && params.getPage() < 0) {
            errors.add(new ValidationException.FieldError("page", "Số trang không được là số âm"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Tham số tìm kiếm không hợp lệ", errors);
        }
    }
}

