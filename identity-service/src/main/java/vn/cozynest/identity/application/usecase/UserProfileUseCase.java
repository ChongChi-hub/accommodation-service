package vn.cozynest.identity.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.common.image.ImageProcessingHelper;
import vn.cozynest.identity.application.dto.AvatarUploadResponse;
import vn.cozynest.identity.application.dto.UpdateUserProfileRequest;
import vn.cozynest.identity.application.dto.UserProfileResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserProfile;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.port.AvatarStoragePort;
import vn.cozynest.identity.domain.repository.UserProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserProfileUseCase.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final AvatarStoragePort avatarStorage;
    private final String mediaBaseUrl;

    public UserProfileUseCase(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserVerificationRepository userVerificationRepository,
            AvatarStoragePort avatarStorage,
            @Value("${app.media.base-url:/api/v1/media}") String mediaBaseUrl) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.avatarStorage = avatarStorage;
        this.mediaBaseUrl = mediaBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay nguoi dung", "ERR_AUTH_USER_NOT_FOUND"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.createInitial(userId, user.getFullName(), user.getPhone())));

        String kycStatus = KycStatus.NONE.name();
        boolean isLandlord = user.getRoles().contains(UserRoleType.LANDLORD);
        if (isLandlord) {
            kycStatus = userVerificationRepository.findByUserId(userId)
                    .map(v -> v.getStatus().name())
                    .orElse(KycStatus.NONE.name());
        }

        return toResponse(user, profile, kycStatus);
    }

    public UserProfileResponse updateProfile(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay nguoi dung", "ERR_AUTH_USER_NOT_FOUND"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.createInitial(userId, user.getFullName(), user.getPhone()));

        if (!request.getVersion().equals(profile.getVersion())) {
            throw new IdentityDomainException(
                    "Ho so da duoc thay doi boi mot phien lam viec khac. Vui long tai lai trang.",
                    "ERR_PROFILE_VERSION_CONFLICT"
            );
        }

        String normalizedPhone = request.getPhone().trim();
        if (userProfileRepository.existsByPhoneAndUserIdNot(normalizedPhone, userId)
                || userRepository.findByPhone(normalizedPhone).filter(u -> !u.getId().equals(userId)).isPresent()) {
            throw new IdentityDomainException("So dien thoai nay da duoc su dung boi tai khoan khac", "ERR_PROFILE_PHONE_DUPLICATE");
        }

        profile.updateDetails(
                request.getFullName(),
                normalizedPhone,
                request.getGender(),
                request.getDateOfBirth(),
                request.getAddressDetail(),
                request.getProvinceCode(),
                request.getWardId(),
                request.getBio(),
                request.getLocale()
        );

        user.updateProfile(request.getFullName().trim(), profile.getAvatarKey(), request.getGender(), request.getDateOfBirth());
        if (normalizedPhone != null && !normalizedPhone.isBlank()) {
            user.updatePhoneNumber(normalizedPhone);
        }
        userRepository.save(user);

        UserProfile savedProfile = userProfileRepository.save(profile);

        String kycStatus = userVerificationRepository.findByUserId(userId)
                .map(v -> v.getStatus().name())
                .orElse(KycStatus.NONE.name());

        return toResponse(user, savedProfile, kycStatus);
    }

    public AvatarUploadResponse uploadAvatar(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IdentityDomainException("Vui long chon tep hinh anh", "ERR_AVATAR_EMPTY");
        }
        if (file.getSize() > ImageProcessingHelper.MAX_FILE_SIZE_5MB) {
            throw new IdentityDomainException("Dung luong anh dai dien vuot qua gioi han 5MB", "ERR_AVATAR_SIZE_EXCEEDED");
        }

        try {
            byte[] rawBytes = file.getBytes();
            if (!ImageProcessingHelper.isValidImageMagicBytes(rawBytes)) {
                throw new IdentityDomainException("Dinh dang tep khong hop le (chi chap nhan JPG, PNG, WebP)", "ERR_AVATAR_FORMAT_INVALID");
            }

            BufferedImage sanitized = ImageProcessingHelper.decodeAndSanitize(rawBytes);
            BufferedImage square = ImageProcessingHelper.cropCenterSquare(sanitized);

            byte[] fullBytes = ImageProcessingHelper.resizeToWebP(square, 256, 256, 0.9f);
            byte[] thumbBytes = ImageProcessingHelper.resizeToWebP(square, 64, 64, 0.85f);
            byte[] medBytes = ImageProcessingHelper.resizeToWebP(square, 128, 128, 0.85f);

            String fileUuid = UUID.randomUUID().toString();
            String fullKey = "avatars/" + userId + "_" + fileUuid + ".webp";
            String thumbKey = "avatars/" + userId + "_" + fileUuid + "_thumb.webp";
            String medKey = "avatars/" + userId + "_" + fileUuid + "_medium.webp";

            avatarStorage.store(fullKey, fullBytes, "image/webp");
            avatarStorage.store(thumbKey, thumbBytes, "image/webp");
            avatarStorage.store(medKey, medBytes, "image/webp");

            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseGet(() -> UserProfile.createInitial(userId, "", ""));

            String oldKey = profile.getAvatarKey();
            String oldThumb = profile.getAvatarThumbKey();
            String oldMed = profile.getAvatarMediumKey();

            profile.setAvatarKeys(fullKey, thumbKey, medKey);
            userProfileRepository.save(profile);

            userRepository.findById(userId).ifPresent(u -> {
                u.updateProfile(u.getFullName(), fullKey, u.getGender(), u.getDateOfBirth());
                userRepository.save(u);
            });

            // Delete old avatar files if replaced
            if (oldKey != null && !oldKey.equals(fullKey)) {
                avatarStorage.delete(oldKey);
                avatarStorage.delete(oldThumb);
                avatarStorage.delete(oldMed);
            }

            return new AvatarUploadResponse(
                    fullKey,
                    buildMediaUrl(fullKey),
                    buildMediaUrl(thumbKey),
                    buildMediaUrl(medKey)
            );
        } catch (IOException e) {
            log.error("Failed to process uploaded avatar for user {}", userId, e);
            throw new IdentityDomainException("Khong the xu ly tep hinh anh", "ERR_AVATAR_PROCESS_FAILED");
        }
    }

    public void deleteAvatar(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so", "ERR_AUTH_USER_NOT_FOUND"));

        if (profile.getAvatarKey() != null) {
            avatarStorage.delete(profile.getAvatarKey());
            avatarStorage.delete(profile.getAvatarThumbKey());
            avatarStorage.delete(profile.getAvatarMediumKey());

            profile.removeAvatar();
            userProfileRepository.save(profile);

            userRepository.findById(userId).ifPresent(u -> {
                u.updateProfile(u.getFullName(), null, u.getGender(), u.getDateOfBirth());
                userRepository.save(u);
            });
        }
    }

    private String buildMediaUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return mediaBaseUrl + "/" + key.replace('\\', '/');
    }

    private UserProfileResponse toResponse(User user, UserProfile profile, String kycStatus) {
        Set<String> roles = user.getRoles().stream().map(UserRoleType::name).collect(Collectors.toSet());
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(profile.getFullName() != null ? profile.getFullName() : user.getFullName())
                .phone(profile.getPhone() != null ? profile.getPhone() : user.getPhone())
                .gender(profile.getGender() != null ? profile.getGender() : user.getGender())
                .dateOfBirth(profile.getDateOfBirth() != null ? profile.getDateOfBirth() : user.getDateOfBirth())
                .permanentAddress(profile.getPermanentAddress())
                .addressDetail(profile.getAddressDetail())
                .provinceCode(profile.getProvinceCode())
                .wardId(profile.getWardId())
                .occupation(profile.getOccupation())
                .bio(profile.getBio())
                .locale(profile.getLocale())
                .avatarKey(profile.getAvatarKey())
                .avatarUrl(buildMediaUrl(profile.getAvatarKey()))
                .avatarThumbUrl(buildMediaUrl(profile.getAvatarThumbKey()))
                .avatarMediumUrl(buildMediaUrl(profile.getAvatarMediumKey()))
                .kycStatus(kycStatus)
                .roles(roles)
                .version(profile.getVersion())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}

