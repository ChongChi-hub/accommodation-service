package vn.cozynest.listing.application.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.application.dto.RoomFilterParams;
import vn.cozynest.listing.domain.model.Province;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.ProvinceRepository;

import java.util.*;

@Component
public class RoomSpecificationBuilder {

    public Specification<Room> buildSpecification(RoomFilterParams params, ProvinceRepository provinceRepository) {
        String resolvedProvinceCode = resolveProvinceCode(params.getProvinceCode(), params.getProvinceSlug(), provinceRepository);
        List<String> resolvedRoomTypes = expandRoomTypeCodes(params.getRoomTypeCode());
        String sanitizedQuery = sanitizeQuery(params.getQ());

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("approved"), true));
            predicates.add(cb.or(cb.equal(root.get("status"), "AVAILABLE"), cb.equal(root.get("status"), "PUBLISHED")));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNull(root.get("hiddenAt")));

            if (sanitizedQuery != null) {
                String pattern = "%" + sanitizedQuery.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("titleSearch")), pattern, '\\'),
                        cb.like(cb.lower(root.get("address")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description").as(String.class)), pattern, '\\')
                ));
            }
            if (resolvedProvinceCode != null) {
                predicates.add(cb.equal(root.get("provinceCode"), resolvedProvinceCode));
            }
            if (params.getDistrictCode() != null && !params.getDistrictCode().isBlank()) {
                predicates.add(cb.equal(root.get("districtCode"), params.getDistrictCode().trim()));
            }
            if (params.getWardCode() != null && !params.getWardCode().isBlank()) {
                predicates.add(cb.equal(root.get("wardCode"), params.getWardCode().trim()));
            }
            if (!resolvedRoomTypes.isEmpty()) {
                predicates.add(root.get("roomTypeCode").in(resolvedRoomTypes));
            }
            if (params.getEffectivePriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), params.getEffectivePriceMin()));
            }
            if (params.getEffectivePriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), params.getEffectivePriceMax()));
            }
            if (params.getEffectiveAreaMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), params.getEffectiveAreaMin()));
            }
            if (params.getEffectiveAreaMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), params.getEffectiveAreaMax()));
            }
            if (params.getOccupants() != null && params.getOccupants() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxOccupants"), params.getOccupants()));
            }
            if (params.getAllowPets() != null) {
                predicates.add(cb.equal(root.get("allowPets"), params.getAllowPets()));
            }
            if (params.getAllowCooking() != null) {
                predicates.add(cb.equal(root.get("allowCooking"), params.getAllowCooking()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public String resolveProvinceCode(String provinceCode, String provinceSlug, ProvinceRepository provinceRepository) {
        if (provinceCode != null && !provinceCode.isBlank()) {
            return provinceCode.trim().toUpperCase(Locale.ROOT);
        }
        if (provinceSlug == null || provinceSlug.isBlank()) {
            return null;
        }
        String candidate = provinceSlug.trim();
        if (provinceRepository != null) {
            return provinceRepository.findBySlugIgnoreCase(candidate)
                    .or(() -> provinceRepository.findByCodeIgnoreCase(candidate))
                    .map(Province::getCode)
                    .orElse(candidate.toUpperCase(Locale.ROOT));
        }
        return candidate.toUpperCase(Locale.ROOT);
    }

    public List<String> expandRoomTypeCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            switch (trimmed.toLowerCase(Locale.ROOT)) {
                case "phong-tro" -> {
                    codes.add("SINGLE");
                    codes.add("HOMESTAY");
                }
                case "nha-nguyen-can" -> codes.add("FULL_HOUSE");
                case "can-ho" -> {
                    codes.add("MINI_APARTMENT");
                    codes.add("STUDIO");
                    codes.add("APARTMENT");
                }
                case "o-ghep" -> codes.add("SHARED");
                default -> codes.add(trimmed.toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(codes);
    }

    public String sanitizeQuery(String q) {
        if (q == null) return null;
        String trimmed = q.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

