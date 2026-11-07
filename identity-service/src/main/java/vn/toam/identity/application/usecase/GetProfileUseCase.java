package vn.toam.identity.application.usecase;

import org.springframework.stereotype.Service;
import vn.toam.identity.application.dto.UserDto;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.repository.UserRepository;

@Service
public class GetProfileUseCase {
    private final UserRepository userRepository;

    public GetProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto execute(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy người dùng", "ERR_AUTH_USER_NOT_FOUND"));
        return UserDto.fromDomain(user);
    }
}

