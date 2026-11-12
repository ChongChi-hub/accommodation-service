package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.domain.model.DomainPage;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandlordRoomQueryService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String removeAccents(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        return withoutDiacritics.replace("đ", "d").replace("Đ", "D");
    }

    public Page<LandlordRoomListItemDto> getLandlordRooms(Long landlordId, String search, String status, String sortParam, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        String sortField = parseSortField(sortParam);
        boolean ascending = parseSortAscending(sortParam);

        String searchKeyword = null;
        if (search != null && !search.trim().isEmpty()) {
            searchKeyword = removeAccents(search.trim().toLowerCase());
        }

        String filterStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase()
                : null;

        DomainPage<Room> roomsPage = roomRepository.findLandlordRooms(landlordId, filterStatus, searchKeyword, safePage, safeSize, sortField, ascending);

        // Preload cover images for this page to prevent N+1 queries
        List<Long> roomIds = roomsPage.content().stream().map(Room::getId).toList();
        Map<Long, RoomImage> coverMap = new HashMap<>();
        for (Long rId : roomIds) {
            roomImageRepository.findByRoomIdAndCoverTrue(rId).ifPresent(img -> coverMap.put(rId, img));
        }

        List<LandlordRoomListItemDto> items = roomsPage.content().stream().map(room -> {
            RoomImage cover = coverMap.get(room.getId());
            String thumbUrl = (cover != null && cover.getThumbKey() != null)
                    ? "/api/v1/media/" + cover.getThumbKey()
                    : (cover != null ? cover.getImageUrl() : "/placeholder-room.webp");
            String mediumUrl = (cover != null && cover.getMediumKey() != null)
                    ? "/api/v1/media/" + cover.getMediumKey()
                    : thumbUrl;

            String shortAddress = room.getAddress();
            if (shortAddress != null && shortAddress.contains(",")) {
                String[] parts = shortAddress.split(",");
                if (parts.length >= 2) {
                    shortAddress = parts[parts.length - 2].trim() + ", " + parts[parts.length - 1].trim();
                }
            }

            return LandlordRoomListItemDto.builder()
                    .id(room.getId())
                    .roomUuid(room.getRoomUuid())
                    .title(room.getTitle())
                    .address(room.getAddress())
                    .shortAddress(shortAddress)
                    .price(room.getPrice())
                    .priceUnit(room.getPriceUnit() != null ? room.getPriceUnit() : "ROOM")
                    .deposit(room.getDeposit())
                    .area(room.getArea())
                    .status(room.getStatus())
                    .publishedAt(room.getPublishedAt())
                    .createdAt(room.getCreatedAt())
                    .viewCount(room.getViewCount() != null ? room.getViewCount() : 0)
                    .rejectedReason(room.getRejectedReason())
                    .hiddenReason(room.getHiddenReason())
                    .hiddenBy(room.getHiddenBy())
                    .coverThumbUrl(thumbUrl)
                    .coverMediumUrl(mediumUrl)
                    .ratingAvg(room.getRatingAvg() != null ? room.getRatingAvg() : 0.0)
                    .reviewCount(room.getReviewCount() != null ? room.getReviewCount() : 0)
                    .build();
        }).toList();
        return new PageImpl<>(items, PageRequest.of(safePage, safeSize), roomsPage.totalElements());
    }

    public RoomSummaryDto getRoomSummary(Long landlordId) {
        long total = roomRepository.countByLandlordIdAndDeletedAtIsNull(landlordId);
        long published = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "PUBLISHED");
        long hidden = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "HIDDEN");
        long pendingReview = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "PENDING_REVIEW");
        long draft = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "DRAFT");
        long rejected = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "REJECTED");
        long rented = roomRepository.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, "RENTED");

        return RoomSummaryDto.builder()
                .total(total)
                .published(published)
                .hidden(hidden)
                .pendingReview(pendingReview)
                .draft(draft)
                .rejected(rejected)
                .rented(rented)
                .build();
    }

    public LandlordRoomDetailDto getRoomDetail(Long landlordId, Long roomId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        List<RoomImage> images = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
        List<RoomImageDto> imageDtos = images.stream().map(img -> RoomImageDto.builder()
                .id(img.getId())
                .roomId(img.getRoomId())
                .imageUrl(img.getImageUrl())
                .storageKey(img.getStorageKey())
                .thumbKey(img.getThumbKey())
                .mediumKey(img.getMediumKey())
                .largeKey(img.getLargeKey())
                .originalFilename(img.getOriginalFilename())
                .contentType(img.getContentType())
                .sizeBytes(img.getSizeBytes())
                .width(img.getWidth())
                .height(img.getHeight())
                .cover(img.isCover())
                .sortOrder(img.getSortOrder())
                .altText(img.getAltText())
                .createdAt(img.getCreatedAt())
                .build()
        ).toList();

        return LandlordRoomDetailDto.builder()
                .id(room.getId())
                .roomUuid(room.getRoomUuid())
                .landlordId(room.getLandlordId())
                .landlordName(room.getLandlordName())
                .landlordPhone(room.getLandlordPhone())
                .roomTypeId(room.getRoomTypeId())
                .roomTypeCode(room.getRoomTypeCode())
                .title(room.getTitle())
                .slug(room.getSlug())
                .description(room.getDescription())
                .address(room.getAddress())
                .provinceCode(room.getProvinceCode())
                .districtCode(room.getDistrictCode())
                .wardCode(room.getWardCode())
                .wardId(room.getWardId())
                .latitude(room.getLatitude())
                .longitude(room.getLongitude())
                .area(room.getArea())
                .maxOccupants(room.getMaxOccupants())
                .price(room.getPrice())
                .priceUnit(room.getPriceUnit() != null ? room.getPriceUnit() : "ROOM")
                .deposit(room.getDeposit())
                .electricityCost(room.getElectricityCost())
                .waterCost(room.getWaterCost())
                .internetCost(room.getInternetCost())
                .serviceCost(room.getServiceCost())
                .status(room.getStatus())
                .approved(room.isApproved())
                .allowPets(room.isAllowPets())
                .allowCooking(room.isAllowCooking())
                .ratingAvg(room.getRatingAvg())
                .reviewCount(room.getReviewCount())
                .viewCount(room.getViewCount())
                .publishedAt(room.getPublishedAt())
                .hiddenAt(room.getHiddenAt())
                .hiddenBy(room.getHiddenBy())
                .hiddenReason(room.getHiddenReason())
                .rejectedReason(room.getRejectedReason())
                .version(room.getVersion())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .images(imageDtos)
                .build();
    }

    private String parseSortField(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return "createdAt";
        }
        String field = sortParam.split(",")[0].trim();
        return switch (field) {
            case "price" -> "price";
            case "views", "viewCount" -> "viewCount";
            case "publishedAt" -> "publishedAt";
            default -> "createdAt";
        };
    }

    private boolean parseSortAscending(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return false;
        }
        String[] parts = sortParam.split(",");
        return parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());
    }
}

