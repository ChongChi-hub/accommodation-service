package vn.toam.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.identity.application.dto.ChangePasswordCommand;
import vn.toam.identity.application.port.EmailServicePort;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;

@Service
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailServicePort emailService;

    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordEncoderPort passwordEncoder,
                                 RefreshTokenRepository refreshTokenRepository,
                                 EmailServicePort emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
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

        // Security: Revoke all existing refresh tokens (force re-login on all devices)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Notify user via email
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getFullName());
    }
}


