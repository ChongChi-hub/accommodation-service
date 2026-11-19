package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.LandlordApprovalStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

@Service
public class LandlordApplicationUseCase {

    private final LandlordProfileRepository landlordProfileRepository;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;

    public LandlordApplicationUseCase(LandlordProfileRepository landlordProfileRepository, UserRepository userRepository) {
        this(landlordProfileRepository, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LandlordApplicationUseCase(
            LandlordProfileRepository landlordProfileRepository,
            UserRepository userRepository,
            UserVerificationRepository userVerificationRepository) {
        this.landlordProfileRepository = landlordProfileRepository;
        this.userRepository = userRepository;
        this.userVerificationRepository = userVerificationRepository;
    }

    @Transactional
    public LandlordApplicationSummaryDto apply(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Người dùng không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));

        if (user.getEmailVerifiedAt() == null) {
            throw new IdentityDomainException("Email chưa được xác minh. Vui lòng xác minh email trước khi nộp hồ sơ chủ trọ.", "ERR_EMAIL_NOT_VERIFIED");
        }

        LandlordProfile profile = landlordProfileRepository.findByUserId(userId)
                .orElseGet(() -> landlordProfileRepository.save(LandlordProfile.createNew(userId)));

        return LandlordApplicationSummaryDto.fromDomain(profile);
    }

    @Transactional
    public LandlordApplicationSummaryDto upgradeToLandlord(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Người dùng không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));

        if (user.getEmailVerifiedAt() == null) {
            throw new IdentityDomainException("Email chưa được xác minh. Vui lòng xác minh email trước khi nâng cấp thành chủ trọ.", "ERR_EMAIL_NOT_VERIFIED");
        }

        boolean kycVerified = userVerificationRepository != null && userVerificationRepository.findByUserId(userId)
                .map(v -> v.getStatus() == KycStatus.APPROVED)
                .orElse(false);

        if (!kycVerified) {
            throw new IdentityDomainException("Bạn cần hoàn tất và được duyệt xác thực CCCD trước khi nâng cấp thành chủ trọ.", "ERR_KYC_NOT_VERIFIED");
        }

        // Upgrade user with LANDLORD role (keeping existing TENANT role)
        user.addRole(UserRoleType.LANDLORD);
        userRepository.save(user);

        LandlordProfile profile = landlordProfileRepository.findByUserId(userId)
                .orElseGet(() -> LandlordProfile.createNew(userId));
        if (profile.getApprovalStatus() == LandlordApprovalStatus.AWAITING_KYC || profile.getApprovalStatus() == LandlordApprovalStatus.REJECTED) {
            profile.submitKyc();
        }
        profile.approve("AUTO_UPGRADE");
        landlordProfileRepository.save(profile);

        return LandlordApplicationSummaryDto.fromDomain(profile);
    }

    @Transactional(readOnly = true)
    public LandlordApplicationSummaryDto getApplication(String userId) {
        return landlordProfileRepository.findByUserId(userId)
                .map(LandlordApplicationSummaryDto::fromDomain)
                .orElse(null);
    }
}

