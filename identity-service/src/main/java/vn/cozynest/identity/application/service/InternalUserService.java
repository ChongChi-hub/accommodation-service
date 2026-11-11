package vn.cozynest.identity.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.InternalKycStatusResponse;
import vn.cozynest.identity.application.dto.InternalPhoneStatusResponse;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.domain.service.PhoneNumberPolicy;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InternalUserService {

    private final UserVerificationRepository verificationRepository;
    private final FaceVerificationRepository faceRepository;
    private final UserRepository userRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final boolean faceRequired;

    public InternalUserService(
            UserVerificationRepository verificationRepository,
            FaceVerificationRepository faceRepository,
            boolean faceRequired) {
        this(verificationRepository, faceRepository, null, null, faceRequired);
    }

    @Autowired
    public InternalUserService(
            UserVerificationRepository verificationRepository,
            FaceVerificationRepository faceRepository,
            UserRepository userRepository,
            PhoneVerificationService phoneVerificationService,
            @Value("${face.required:true}") boolean faceRequired) {
        this.verificationRepository = verificationRepository;
        this.faceRepository = faceRepository;
        this.userRepository = userRepository;
        this.phoneVerificationService = phoneVerificationService;
        this.faceRequired = faceRequired;
    }

    public InternalPhoneStatusResponse getPhoneStatus(String userId) {
        if (userRepository == null) {
            return new InternalPhoneStatusResponse(userId, false, "");
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new InternalPhoneStatusResponse(userId, false, "");
        }
        User user = userOpt.get();
        String masked = user.getPhone() != null && !user.getPhone().isBlank()
                ? PhoneNumberPolicy.mask(user.getPhone())
                : "";
        return new InternalPhoneStatusResponse(userId, user.isPhoneVerified(), masked);
    }

    @Transactional
    public boolean verifyPhoneOtpForUser(String userId, String otpCode) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isPhoneVerified() || user.getPhone() == null || user.getPhone().isBlank()) {
            return false;
        }
        return phoneVerificationService.verifyOtp(user.getPhone(), otpCode, null);
    }

    public InternalKycStatusResponse getKycStatus(String userId) {
        Optional<UserVerification> opt = verificationRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return new InternalKycStatusResponse(userId, KycStatus.NONE.name(), false);
        }
        UserVerification docKyc = opt.get();
        boolean docApproved = docKyc.getStatus() == KycStatus.APPROVED;

        if (!faceRequired) {
            return new InternalKycStatusResponse(userId, docKyc.getStatus().name(), docApproved);
        }

        // When faceRequired=true, APPROVED requires doc APPROVED and face PASSED
        Optional<FaceVerification> faceOpt = faceRepository.findByUserId(userId);
        boolean facePassed = faceOpt.map(f -> f.getStatus() == FaceVerificationStatus.PASSED).orElse(false);

        boolean overallApproved = docApproved && facePassed;
        String overallStatus;
        if (overallApproved) {
            overallStatus = KycStatus.APPROVED.name();
        } else if (docKyc.getStatus() == KycStatus.REJECTED) {
            overallStatus = KycStatus.REJECTED.name();
        } else if (faceOpt.isPresent() && faceOpt.get().getStatus() == FaceVerificationStatus.FAILED) {
            overallStatus = "FAILED_FACE";
        } else {
            overallStatus = KycStatus.PENDING.name();
        }

        return new InternalKycStatusResponse(userId, overallStatus, overallApproved);
    }
}

