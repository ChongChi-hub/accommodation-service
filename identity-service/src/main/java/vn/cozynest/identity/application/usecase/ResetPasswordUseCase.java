package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.ResetPasswordCommand;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataPasswordResetTokenRepository;

@Service
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final SpringDataPasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final EmailServicePort emailService;

    public ResetPasswordUseCase(
            UserRepository userRepository,
            SpringDataPasswordResetTokenRepository resetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoderPort passwordEncoder,
            EmailServicePort emailService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void execute(ResetPasswordCommand command) {
        User user = userRepository.findByEmail(command.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IdentityDomainException("Yêu cầu không hợp lệ hoặc mã OTP đã hết hạn", "ERR_AUTH_INVALID_OTP"));

        PasswordResetTokenJpaEntity token = resetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy yêu cầu đặt lại mật khẩu hoặc mã đã hết hạn", "ERR_AUTH_INVALID_OTP"));

        if (!token.isValid()) {
            throw new IdentityDomainException("Mã OTP đã hết hạn hoặc vượt quá số lần thử cho phép (tối đa 5 lần)", "ERR_AUTH_OTP_EXPIRED");
        }

        if (!passwordEncoder.matches(command.getOtp().trim(), token.getOtpHash())) {
            token.incrementAttempts();
            resetTokenRepository.save(token);
            if (token.getAttempts() >= 5) {
                throw new IdentityDomainException("Mã OTP đã bị vô hiệu hóa do nhập sai quá 5 lần liên tiếp. Vui lòng gửi lại yêu cầu khôi phục.", "ERR_AUTH_OTP_LOCKED");
            }
            throw new IdentityDomainException("Mã xác thực OTP không chính xác", "ERR_AUTH_INVALID_OTP");
        }

        // Validate not reusing old password
        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            throw new IdentityDomainException("Mật khẩu mới không được trùng với mật khẩu gần nhất", "ERR_AUTH_OLD_PASSWORD_REUSE");
        }

        vn.cozynest.identity.domain.service.PasswordPolicyValidator.validate(command.getNewPassword(), user.getEmail());

        // OTP is correct
        token.markUsed();
        resetTokenRepository.save(token);

        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        user.changePassword(newPasswordHash);
        user.unlock();
        userRepository.save(user);

        // Security: Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Send confirmation email
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getFullName());
    }
}
