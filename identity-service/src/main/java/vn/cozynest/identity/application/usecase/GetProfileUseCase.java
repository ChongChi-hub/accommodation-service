package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

@Service
public class GetProfileUseCase {
    private final UserRepository userRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final FaceVerificationRepository faceVerificationRepository;

    public GetProfileUseCase(
            UserRepository userRepository,
            LandlordProfileRepository landlordProfileRepository,
            UserVerificationRepository userVerificationRepository,
            FaceVerificationRepository faceVerificationRepository
    ) {
        this.userRepository = userRepository;
        this.landlordProfileRepository = landlordProfileRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.faceVerificationRepository = faceVerificationRepository;
    }

    public UserDto execute(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy người dùng", "ERR_AUTH_USER_NOT_FOUND"));
        LandlordProfile lp = landlordProfileRepository.findByUserId(userId).orElse(null);

        boolean kycVerified = userVerificationRepository.findByUserId(userId)
                .map(v -> v.getStatus() == KycStatus.APPROVED)
                .orElse(false);

        boolean faceVerified = faceVerificationRepository.findByUserId(userId)
                .map(f -> f.getStatus() == FaceVerificationStatus.PASSED)
                .orElse(false);

        return UserDto.fromDomain(user, lp, kycVerified, faceVerified);
    }
}
