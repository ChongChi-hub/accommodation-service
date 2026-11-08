package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

@Service
public class GetProfileUseCase {
    private final UserRepository userRepository;
    private final LandlordProfileRepository landlordProfileRepository;

    public GetProfileUseCase(UserRepository userRepository, LandlordProfileRepository landlordProfileRepository) {
        this.userRepository = userRepository;
        this.landlordProfileRepository = landlordProfileRepository;
    }

    public UserDto execute(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy người dùng", "ERR_AUTH_USER_NOT_FOUND"));
        LandlordProfile lp = landlordProfileRepository.findByUserId(userId).orElse(null);
        return UserDto.fromDomain(user, lp);
    }
}
