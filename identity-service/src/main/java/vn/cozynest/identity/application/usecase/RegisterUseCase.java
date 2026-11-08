package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RegisterCommand;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RegisterUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final vn.cozynest.identity.application.port.RateLimiterPort rateLimiter;

    public RegisterUseCase(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           LandlordProfileRepository landlordProfileRepository,
                           PasswordEncoderPort passwordEncoder,
                           TokenProviderPort tokenProvider,
                           vn.cozynest.identity.application.port.RateLimiterPort rateLimiter) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.landlordProfileRepository = landlordProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public AuthResult execute(RegisterCommand command) {
        rateLimiter.checkRegisterRateLimit(command.getIpAddress());

        String normalizedEmail = command.getEmail() != null ? command.getEmail().trim().toLowerCase() : "";
        String normalizedPhone = normalizePhone(command.getPhone());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IdentityDomainException("Email đã tồn tại trên hệ thống", "ERR_AUTH_EMAIL_EXISTS");
        }
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new IdentityDomainException("Số điện thoại đã tồn tại trên hệ thống", "ERR_AUTH_PHONE_EXISTS");
        }

        vn.cozynest.identity.domain.service.PasswordPolicyValidator.validate(command.getPassword(), normalizedEmail);

        // Role whitelisting: only TENANT or LANDLORD self-registration choices are allowed
        String roleStr = command.getRole() != null ? command.getRole().trim().toUpperCase() : "TENANT";
        if (!"TENANT".equals(roleStr) && !"LANDLORD".equals(roleStr)) {
            throw new IdentityDomainException("Không được phép tự đăng ký vai trò này", "ERR_AUTH_FORBIDDEN_ROLE");
        }

        String passwordHash = passwordEncoder.encode(command.getPassword());

        // R1: All self-registered accounts receive TENANT role only
        User newUser = User.createNew(normalizedEmail, normalizedPhone, passwordHash, command.getFullName(), UserRoleType.TENANT);
        User savedUser = userRepository.save(newUser);

        // R2: Choosing LANDLORD creates an application in AWAITING_KYC
        LandlordProfile profile = null;
        if ("LANDLORD".equals(roleStr)) {
            profile = landlordProfileRepository.save(LandlordProfile.createNew(savedUser.getId()));
        }

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.create(
                savedUser.getId(),
                tokenHash,
                "Web",
                command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(savedUser, profile))
                .roles(List.of("TENANT"))
                .isNewUser(true)
                .emailVerified(false)
                .landlordApplication(vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto.fromDomain(profile))
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String cleaned = phone.replaceAll("[\\s\\-\\.]", "");
        if (cleaned.startsWith("0")) {
            return "+84" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            return "+84" + cleaned;
        }
        return cleaned;
    }
}
