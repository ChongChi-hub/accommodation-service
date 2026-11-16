package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSubjectExportDto {

    private String complianceNote;
    private Instant exportedAt;
    private UserProfileDossier profile;
    private KycDossier kyc;
    private BiometricDossier biometric;
    private List<ConsentHistoryDto> consentHistories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileDossier {
        private String userId;
        private String fullName;
        private String email;
        private String phone;
        private String gender;
        private String dateOfBirth;
        private String permanentAddress;
        private String occupation;
        private String bio;
        private String status;
        private Instant createdAt;
        private Instant lastLoginAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDossier {
        private boolean kycSubmitted;
        private String documentType;
        private String maskedDocumentNumber;
        private String status;
        private Instant submittedAt;
        private Instant verifiedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiometricDossier {
        private boolean faceVerificationAttempted;
        private String status;
        private Integer attempts;
        private Instant consentAt;
        private String consentVersion;
        private boolean framesPurged;
    }
}
