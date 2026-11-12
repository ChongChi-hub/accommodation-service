package vn.cozynest.listing.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.*;
import vn.cozynest.listing.infrastructure.persistence.entity.*;

@Component
public class ListingCatalogPersistenceMapper {

    public Province toDomain(ProvinceJpaEntity e) {
        if (e == null) return null;
        return Province.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).nameEn(e.getNameEn())
                .slug(e.getSlug()).fullName(e.getFullName()).fullNameEn(e.getFullNameEn()).imageUrl(e.getImageUrl())
                .build();
    }

    public District toDomain(DistrictJpaEntity e) {
        if (e == null) return null;
        return District.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).nameEn(e.getNameEn())
                .fullName(e.getFullName()).fullNameEn(e.getFullNameEn()).provinceCode(e.getProvinceCode())
                .build();
    }

    public Ward toDomain(WardJpaEntity e) {
        if (e == null) return null;
        return Ward.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).nameEn(e.getNameEn())
                .fullName(e.getFullName()).fullNameEn(e.getFullNameEn())
                .provinceCode(e.getProvinceCode()).districtCode(e.getDistrictCode())
                .build();
    }

    public RoomType toDomain(RoomTypeJpaEntity e) {
        if (e == null) return null;
        return RoomType.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).description(e.getDescription()).icon(e.getIcon())
                .build();
    }

    public Banner toDomain(BannerJpaEntity e) {
        if (e == null) return null;
        return Banner.builder()
                .id(e.getId()).title(e.getTitle()).subtitle(e.getSubtitle()).badgeText(e.getBadgeText())
                .imageUrl(e.getImageUrl()).linkUrl(e.getLinkUrl()).sortOrder(e.getSortOrder()).active(e.isActive())
                .startTime(e.getStartTime()).endTime(e.getEndTime()).createdAt(e.getCreatedAt())
                .build();
    }

    public BannerJpaEntity toEntity(Banner d) {
        if (d == null) return null;
        return BannerJpaEntity.builder()
                .id(d.getId()).title(d.getTitle()).subtitle(d.getSubtitle()).badgeText(d.getBadgeText())
                .imageUrl(d.getImageUrl()).linkUrl(d.getLinkUrl()).sortOrder(d.getSortOrder()).active(d.isActive())
                .startTime(d.getStartTime()).endTime(d.getEndTime()).createdAt(d.getCreatedAt())
                .build();
    }

    public Bookmark toDomain(BookmarkJpaEntity e) {
        if (e == null) return null;
        return Bookmark.builder()
                .id(e.getId()).userId(e.getUserId()).roomId(e.getRoomId()).createdAt(e.getCreatedAt())
                .build();
    }

    public BookmarkJpaEntity toEntity(Bookmark d) {
        if (d == null) return null;
        return BookmarkJpaEntity.builder()
                .id(d.getId()).userId(d.getUserId()).roomId(d.getRoomId()).createdAt(d.getCreatedAt())
                .build();
    }

    public CmsArticle toDomain(CmsArticleJpaEntity e) {
        if (e == null) return null;
        return CmsArticle.builder()
                .id(e.getId()).slug(e.getSlug()).titleVi(e.getTitleVi()).titleEn(e.getTitleEn())
                .summaryVi(e.getSummaryVi()).summaryEn(e.getSummaryEn()).contentVi(e.getContentVi()).contentEn(e.getContentEn())
                .category(e.getCategory()).readingMinutes(e.getReadingMinutes()).imageUrl(e.getImageUrl())
                .imageAltVi(e.getImageAltVi()).imageAltEn(e.getImageAltEn()).publishedAt(e.getPublishedAt())
                .sortOrder(e.getSortOrder()).active(e.isActive()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public CmsArticleJpaEntity toEntity(CmsArticle d) {
        if (d == null) return null;
        return CmsArticleJpaEntity.builder()
                .id(d.getId()).slug(d.getSlug()).titleVi(d.getTitleVi()).titleEn(d.getTitleEn())
                .summaryVi(d.getSummaryVi()).summaryEn(d.getSummaryEn()).contentVi(d.getContentVi()).contentEn(d.getContentEn())
                .category(d.getCategory()).readingMinutes(d.getReadingMinutes()).imageUrl(d.getImageUrl())
                .imageAltVi(d.getImageAltVi()).imageAltEn(d.getImageAltEn()).publishedAt(d.getPublishedAt())
                .sortOrder(d.getSortOrder()).active(d.isActive()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt())
                .build();
    }

    public LandlordSnapshot toDomain(LandlordSnapshotJpaEntity e) {
        if (e == null) return null;
        return LandlordSnapshot.builder()
                .id(e.getId()).landlordId(e.getLandlordId()).name(e.getName()).phone(e.getPhone())
                .avatarUrl(e.getAvatarUrl()).email(e.getEmail()).verified(e.isVerified()).ratingAvg(e.getRatingAvg())
                .reviewCount(e.getReviewCount()).responseRatePercent(e.getResponseRatePercent())
                .responseTimeMinutes(e.getResponseTimeMinutes()).roomCount(e.getRoomCount())
                .joinedAt(e.getJoinedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public LandlordSnapshotJpaEntity toEntity(LandlordSnapshot d) {
        if (d == null) return null;
        return LandlordSnapshotJpaEntity.builder()
                .id(d.getId()).landlordId(d.getLandlordId()).name(d.getName()).phone(d.getPhone())
                .avatarUrl(d.getAvatarUrl()).email(d.getEmail()).verified(d.isVerified()).ratingAvg(d.getRatingAvg())
                .reviewCount(d.getReviewCount()).responseRatePercent(d.getResponseRatePercent())
                .responseTimeMinutes(d.getResponseTimeMinutes()).roomCount(d.getRoomCount())
                .joinedAt(d.getJoinedAt()).updatedAt(d.getUpdatedAt())
                .build();
    }

    public PropertyOwnershipDocument toDomain(PropertyOwnershipDocumentJpaEntity e) {
        if (e == null) return null;
        return PropertyOwnershipDocument.builder()
                .id(e.getId()).roomId(e.getRoomId()).landlordId(e.getLandlordId()).docType(e.getDocType())
                .fileUrl(e.getFileUrl()).maskedNumber(e.getMaskedNumber()).status(e.getStatus())
                .rejectReason(e.getRejectReason()).reviewerId(e.getReviewerId()).reviewedAt(e.getReviewedAt())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public PropertyOwnershipDocumentJpaEntity toEntity(PropertyOwnershipDocument d) {
        if (d == null) return null;
        return PropertyOwnershipDocumentJpaEntity.builder()
                .id(d.getId()).roomId(d.getRoomId()).landlordId(d.getLandlordId()).docType(d.getDocType())
                .fileUrl(d.getFileUrl()).maskedNumber(d.getMaskedNumber()).status(d.getStatus())
                .rejectReason(d.getRejectReason()).reviewerId(d.getReviewerId()).reviewedAt(d.getReviewedAt())
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt())
                .build();
    }

    public Review toDomain(ReviewJpaEntity e) {
        if (e == null) return null;
        return Review.builder()
                .id(e.getId()).roomId(e.getRoomId()).tenantId(e.getTenantId()).tenantName(e.getTenantName())
                .tenantAvatar(e.getTenantAvatar()).rating(e.getRating()).verifiedTenant(e.isVerifiedTenant())
                .comment(e.getComment()).landlordReply(e.getLandlordReply()).createdAt(e.getCreatedAt())
                .build();
    }

    public ReviewJpaEntity toEntity(Review d) {
        if (d == null) return null;
        return ReviewJpaEntity.builder()
                .id(d.getId()).roomId(d.getRoomId()).tenantId(d.getTenantId()).tenantName(d.getTenantName())
                .tenantAvatar(d.getTenantAvatar()).rating(d.getRating()).verifiedTenant(d.isVerifiedTenant())
                .comment(d.getComment()).landlordReply(d.getLandlordReply()).createdAt(d.getCreatedAt())
                .build();
    }
}

