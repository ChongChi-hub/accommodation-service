package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

@Service
public class LandlordApplicationUseCase {

    private final LandlordProfileRepository landlordProfileRepository;
    private final UserRepository userRepository;

    public LandlordApplicationUseCase(LandlordProfileRepository landlordProfileRepository, UserRepository userRepository) {
        this.landlordProfileRepository = landlordProfileRepository;
        this.userRepository = userRepository;
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

    @Transactional(readOnly = true)
    public LandlordApplicationSummaryDto getApplication(String userId) {
        return landlordProfileRepository.findByUserId(userId)
                .map(LandlordApplicationSummaryDto::fromDomain)
                .orElse(null);
    }
}

