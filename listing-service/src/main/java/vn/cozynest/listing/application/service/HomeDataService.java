package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.BannerDto;
import vn.cozynest.listing.application.dto.CmsArticleDto;
import vn.cozynest.listing.application.dto.HotAreaDto;
import vn.cozynest.listing.application.dto.PlatformStatsDto;
import vn.cozynest.listing.domain.model.Banner;
import vn.cozynest.listing.domain.model.CmsArticle;
import vn.cozynest.listing.domain.repository.BannerRepository;
import vn.cozynest.listing.domain.repository.CmsArticleRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDataService {

    private final RoomRepository roomRepository;
    private final BannerRepository bannerRepository;
    private final ReviewRepository reviewRepository;
    private final CmsArticleRepository cmsArticleRepository;

    public List<HotAreaDto> getHotAreas() {
        return getHotAreas(8, false);
    }

    public List<HotAreaDto> getHotAreas(int limit, boolean nonEmptyOnly) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Object[]> rows = roomRepository.findHotAreas();
        return rows.stream().map(row -> {
            String code = (String) row[0];
            String name = (String) row[1];
            String fullName = (String) row[2];
            String slug = (String) row[3];
            String imageUrl = (String) row[4];
            Long count = ((Number) row[5]).longValue();

            if (slug == null || slug.isBlank()) {
                slug = code.toLowerCase();
            }
            if (imageUrl == null || imageUrl.isBlank()) {
                imageUrl = "/uploads/locations/fallback.webp";
            }

            return HotAreaDto.builder()
                    .code(code)
                    .name(name)
                    .fullName(fullName)
                    .slug(slug)
                    .imageUrl(imageUrl)
                    .roomCount(count)
                    .build();
        }).filter(dto -> !nonEmptyOnly || dto.getRoomCount() > 0)
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    public List<BannerDto> getBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(b -> BannerDto.builder()
                        .id(b.getId())
                        .title(b.getTitle())
                        .subtitle(b.getSubtitle())
                        .badgeText(b.getBadgeText())
                        .imageUrl(b.getImageUrl())
                        .linkUrl(b.getLinkUrl())
                        .sortOrder(b.getSortOrder())
                        .active(b.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    public List<CmsArticleDto> getArticles() {
        return cmsArticleRepository.findByActiveTrueOrderBySortOrderAscPublishedAtDesc().stream()
                .map(this::toArticleDto)
                .collect(Collectors.toList());
    }

    public CmsArticleDto getArticleBySlug(String slug) {
        return cmsArticleRepository.findBySlugAndActiveTrue(slug)
                .map(this::toArticleDto)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bài viết: " + slug));
    }

    public PlatformStatsDto getPlatformStats() {
        long activeRooms = roomRepository.countActiveRooms();
        long provincesWithRooms = roomRepository.countActiveProvinces();
        long verifiedLandlords = roomRepository.countVerifiedLandlords();
        long totalReviews = reviewRepository.count();
        long happyTenants = activeRooms * 2 + totalReviews;

        return PlatformStatsDto.builder()
                .activeRooms(activeRooms)
                .provincesWithRooms(provincesWithRooms)
                .verifiedLandlords(verifiedLandlords > 0 ? verifiedLandlords : 1)
                .totalReviews(totalReviews)
                .happyTenants(happyTenants)
                .build();
    }

    private CmsArticleDto toArticleDto(CmsArticle a) {
        return CmsArticleDto.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .titleVi(a.getTitleVi())
                .titleEn(a.getTitleEn())
                .summaryVi(a.getSummaryVi())
                .summaryEn(a.getSummaryEn())
                .contentVi(a.getContentVi())
                .contentEn(a.getContentEn())
                .category(a.getCategory())
                .readingMinutes(a.getReadingMinutes())
                .imageUrl(a.getImageUrl())
                .imageAltVi(a.getImageAltVi())
                .imageAltEn(a.getImageAltEn())
                .publishedAt(a.getPublishedAt())
                .sortOrder(a.getSortOrder())
                .build();
    }
}
