package vn.cozynest.identity.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.InternalKycStatusResponse;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InternalUserService {

    private final UserVerificationRepository verificationRepository;
    private final FaceVerificationRepository faceRepository;
    private final boolean faceRequired;

    public InternalUserService(
            UserVerificationRepository verificationRepository,
            FaceVerificationRepository faceRepository,
            @Value("${face.required:true}") boolean faceRequired) {
        this.verificationRepository = verificationRepository;
        this.faceRepository = faceRepository;
        this.faceRequired = faceRequired;
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

