package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.LinkGoogleIdentityCommand;
import vn.cozynest.identity.application.dto.UserIdentityDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.IdentityAuditLog;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserIdentity;
import vn.cozynest.identity.domain.repository.IdentityAuditLogRepository;
import vn.cozynest.identity.domain.repository.UserIdentityRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.security.GoogleNonceManager;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;

import java.util.List;
import java.util.Optional;

@Service
public class ManageUserIdentitiesUseCase {

    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;
    private final IdentityAuditLogRepository auditLogRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final GoogleNonceManager nonceManager;

    public ManageUserIdentitiesUseCase(
            UserIdentityRepository userIdentityRepository,
            UserRepository userRepository,
            IdentityAuditLogRepository auditLogRepository,
            GoogleTokenVerifier googleTokenVerifier,
            GoogleNonceManager nonceManager) {
        this.userIdentityRepository = userIdentityRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.googleTokenVerifier = googleTokenVerifier;
        this.nonceManager = nonceManager;
    }

    @Transactional(readOnly = true)
    public List<UserIdentityDto> getIdentities(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IdentityDomainException("Người dùng không hợp lệ", "ERR_INVALID_REQUEST");
        }
        return userIdentityRepository.findAllByUserId(userId).stream()
                .map(UserIdentityDto::fromDomain)
                .toList();
    }

    @Transactional
    public UserIdentityDto linkGoogle(String userId, LinkGoogleIdentityCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Người dùng không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));

        nonceManager.consumeNonce(command.getNonce());
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(command.getCredential(), command.getNonce());

        Optional<UserIdentity> existingSub = userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", claims.sub());
        if (existingSub.isPresent()) {
            if (!existingSub.get().getUserId().equals(userId)) {
                throw new IdentityDomainException("Tài khoản Google này đã được liên kết với người dùng khác", "ERR_GOOGLE_ALREADY_LINKED");
            }
            return UserIdentityDto.fromDomain(existingSub.get());
        }

        Optional<UserIdentity> existingSelf = userIdentityRepository.findByUserIdAndProvider(userId, "GOOGLE");
        if (existingSelf.isPresent()) {
            throw new IdentityDomainException("Tài khoản của bạn đã được liên kết với một tài khoản Google", "ERR_GOOGLE_ALREADY_LINKED");
        }

        UserIdentity newIdentity = UserIdentity.createGoogleIdentity(
                userId, claims.sub(), claims.email(), claims.name(), claims.pictureUrl(), claims.locale());
        UserIdentity saved = userIdentityRepository.save(newIdentity);

        auditLogRepository.save(IdentityAuditLog.create(
                userId, userId, "GOOGLE_LINKED", "GOOGLE", command.getIpAddress(), command.getUserAgent(),
                "Linked Google account sub=" + claims.sub()));

        return UserIdentityDto.fromDomain(saved);
    }

    @Transactional
    public void unlinkGoogle(String userId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityDomainException("Người dùng không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));

        boolean hasPassword = user.hasPassword();
        int identitiesCount = userIdentityRepository.countByUserId(userId);

        if (!hasPassword && identitiesCount <= 1) {
            throw new IdentityDomainException(
                    "Không thể hủy liên kết phương thức đăng nhập duy nhất. Vui lòng thiết lập mật khẩu trước khi hủy liên kết Google.",
                    "ERR_AUTH_LAST_LOGIN_METHOD");
        }

        Optional<UserIdentity> identityOpt = userIdentityRepository.findByUserIdAndProvider(userId, "GOOGLE");
        if (identityOpt.isEmpty()) {
            throw new IdentityDomainException("Tài khoản chưa được liên kết với Google", "ERR_NOT_FOUND");
        }

        userIdentityRepository.deleteByUserIdAndProvider(userId, "GOOGLE");

        auditLogRepository.save(IdentityAuditLog.create(
                userId, userId, "GOOGLE_UNLINKED", "GOOGLE", ipAddress, userAgent, "Unlinked Google account"));
    }
}
