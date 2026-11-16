package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.AnonymizeAccountRequest;
import vn.cozynest.identity.application.dto.ConsentHistoryDto;
import vn.cozynest.identity.application.dto.DataSubjectExportDto;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class DataSubjectRightsUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final FaceVerificationRepository faceVerificationRepository;
    private final FaceVerificationFrameRepository faceVerificationFrameRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConsentHistoryRepository consentHistoryRepository;

    public DataSubjectRightsUseCase(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserVerificationRepository userVerificationRepository,
            FaceVerificationRepository faceVerificationRepository,
            FaceVerificationFrameRepository faceVerificationFrameRepository,
            RefreshTokenRepository refreshTokenRepository,
            ConsentHistoryRepository consentHistoryRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.faceVerificationRepository = faceVerificationRepository;
        this.faceVerificationFrameRepository = faceVerificationFrameRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.consentHistoryRepository = consentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public DataSubjectExportDto exportPersonalData(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin tài khoản: " + userId));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        Optional<UserVerification> kycOpt = userVerificationRepository.findByUserId(userId);
        Optional<FaceVerification> faceOpt = faceVerificationRepository.findByUserId(userId);
        List<ConsentHistory> consents = consentHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        DataSubjectExportDto.UserProfileDossier profileDossier = DataSubjectExportDto.UserProfileDossier.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone() != null ? maskPhone(user.getPhone()) : null)
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .permanentAddress(profileOpt.map(UserProfile::getPermanentAddress).orElse(null))
                .occupation(profileOpt.map(UserProfile::getOccupation).orElse(null))
                .bio(profileOpt.map(UserProfile::getBio).orElse(null))
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();

        DataSubjectExportDto.KycDossier kycDossier = DataSubjectExportDto.KycDossier.builder()
                .kycSubmitted(kycOpt.isPresent())
                .documentType(kycOpt.map(k -> "CCCD").orElse(null))
                .maskedDocumentNumber(kycOpt.map(k -> "********" + k.getIdNumberLast4()).orElse(null))
                .status(kycOpt.map(k -> k.getStatus().name()).orElse("NOT_SUBMITTED"))
                .submittedAt(kycOpt.map(UserVerification::getSubmittedAt).orElse(null))
                .verifiedAt(kycOpt.map(UserVerification::getReviewedAt).orElse(null))
                .build();

        DataSubjectExportDto.BiometricDossier biometricDossier = DataSubjectExportDto.BiometricDossier.builder()
                .faceVerificationAttempted(faceOpt.isPresent())
                .status(faceOpt.map(f -> f.getStatus().name()).orElse("NOT_ATTEMPTED"))
                .attempts(faceOpt.map(FaceVerification::getAttemptNo).orElse(0))
                .consentAt(faceOpt.map(FaceVerification::getConsentAt).orElse(null))
                .consentVersion(faceOpt.map(FaceVerification::getConsentVersion).orElse(null))
                .framesPurged(user.isAnonymized())
                .build();

        List<ConsentHistoryDto> consentDtos = consents.stream().map(c -> ConsentHistoryDto.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .consentType(c.getConsentType())
                .version(c.getVersion())
                .agreed(c.isAgreed())
                .ipAddress(c.getIpAddress())
                .userAgent(c.getUserAgent())
                .createdAt(c.getCreatedAt())
                .build()
        ).toList();

        return DataSubjectExportDto.builder()
                .complianceNote("Trích xuất gói dữ liệu cá nhân theo quy định tại Điều 14 & Điều 15 Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân.")
                .exportedAt(Instant.now())
                .profile(profileDossier)
                .kyc(kycDossier)
                .biometric(biometricDossier)
                .consentHistories(consentDtos)
                .build();
    }

    public void anonymizeAccount(String userId, AnonymizeAccountRequest req, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin tài khoản: " + userId));

        if (user.isAnonymized()) {
            throw new IllegalStateException("Tài khoản đã được ẩn danh và xóa trước đó.");
        }

        // 1. Anonymize user entity (PII scrubbed, status = DELETED)
        user.anonymize();
        userRepository.save(user);

        // 2. Anonymize user profile
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.anonymize();
            userProfileRepository.save(profile);
        });

        // 3. Redact KYC verification
        userVerificationRepository.findByUserId(userId).ifPresent(kyc -> {
            kyc.anonymize();
            userVerificationRepository.save(kyc);
        });

        // 4. Purge biometric frames
        faceVerificationRepository.findByUserId(userId).ifPresent(fv -> {
            faceVerificationFrameRepository.deleteByFaceVerificationId(fv.getId());
        });

        // 5. Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        // 6. Audit consent record
        String meta = "{\"reason\":\"" + (req.getReason() != null ? req.getReason() : "Chủ tài khoản yêu cầu xóa theo NĐ 13") + "\"}";
        consentHistoryRepository.save(ConsentHistory.of(
                userId,
                "DATA_SUBJECT_ERASURE_REQUEST",
                "NĐ13-Điều16",
                true,
                ipAddress,
                userAgent,
                meta
        ));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
