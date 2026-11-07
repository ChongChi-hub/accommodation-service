package vn.toam.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.identity.application.dto.ChangePasswordCommand;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.repository.UserRepository;

@Service
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void execute(String userId, ChangePasswordCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy người dùng", "ERR_AUTH_USER_NOT_FOUND"));

        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPasswordHash())) {
            throw new IdentityDomainException("Mật khẩu hiện tại không chính xác", "ERR_AUTH_INVALID_PASSWORD");
        }

        user.changePassword(passwordEncoder.encode(command.getNewPassword()));
        userRepository.save(user);
    }
}

