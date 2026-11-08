package vn.cozynest.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.InternalKycStatusResponse;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InternalUserService {

    private final UserVerificationRepository verificationRepository;

    public InternalUserService(UserVerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    public InternalKycStatusResponse getKycStatus(String userId) {
        Optional<UserVerification> opt = verificationRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return new InternalKycStatusResponse(userId, KycStatus.NONE.name(), false);
        }
        UserVerification v = opt.get();
        boolean isApproved = v.getStatus() == KycStatus.APPROVED;
        return new InternalKycStatusResponse(userId, v.getStatus().name(), isApproved);
    }
}

