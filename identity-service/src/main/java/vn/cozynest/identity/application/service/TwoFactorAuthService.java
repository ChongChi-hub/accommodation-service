package vn.cozynest.identity.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.TwoFactorDto;
import vn.cozynest.identity.domain.model.TwoFactorRecoveryRequest;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserTwoFactor;
import vn.cozynest.identity.domain.repository.TwoFactorRecoveryRequestRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserTwoFactorRepository;
import vn.cozynest.identity.infrastructure.security.TotpHelper;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TwoFactorAuthService {
    private final UserTwoFactorRepository twoFactorRepository;
    private final UserRepository userRepository;
    private final TotpHelper totpHelper;
    private final TwoFactorRecoveryRequestRepository recoveryRepository;
    private final ObjectMapper objectMapper;

    public TwoFactorAuthService(UserTwoFactorRepository twoFactorRepository,
                                UserRepository userRepository,
                                TotpHelper totpHelper) {
        this(twoFactorRepository, userRepository, totpHelper, null);
    }

    @Autowired
    public TwoFactorAuthService(UserTwoFactorRepository twoFactorRepository,
                                UserRepository userRepository,
                                TotpHelper totpHelper,
                                TwoFactorRecoveryRequestRepository recoveryRepository) {
        this.twoFactorRepository = twoFactorRepository;
        this.userRepository = userRepository;
        this.totpHelper = totpHelper;
        this.recoveryRepository = recoveryRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public TwoFactorDto.SetupResponse setup(String userId, String userEmail) {
        String secret = totpHelper.generateSecret();
        List<String> backupCodes = generateBackupCodes();
        String backupJson = serializeBackupCodes(backupCodes);

        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .map(existing -> {
                    existing.updateSecret(secret, backupJson);
                    return existing;
                })
                .orElseGet(() -> UserTwoFactor.createPending(userId, secret, backupJson));

        twoFactorRepository.save(twoFactor);
        return new TwoFactorDto.SetupResponse(secret, totpHelper.buildOtpAuthUri(secret, userEmail, "ToAm"), backupCodes);
    }

    @Transactional
    public boolean verifyAndEnable(String userId, String code) {
        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA chua duoc khoi tao"));
        Instant now = Instant.now();
        if (twoFactor.isLocked(now)) {
            throw new IllegalStateException("2FA tam khoa do nhap sai qua nhieu lan. Vui long thu lai sau 15 phut.");
        }
        if (!totpHelper.verifyCode(twoFactor.getTotpSecret(), code)) {
            twoFactor.recordFailedAttempt(now);
            twoFactorRepository.save(twoFactor);
            return false;
        }
        twoFactor.enable();
        twoFactorRepository.save(twoFactor);
        userRepository.findById(userId).ifPresent(user -> {
            user.enableTwoFactor();
            userRepository.save(user);
        });
        return true;
    }

    @Transactional
    public boolean verifyChallenge(String userId, String code) {
        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA chua duoc khoi tao"));
        if (!twoFactor.isEnabled()) {
            return true;
        }
        Instant now = Instant.now();
        if (twoFactor.isLocked(now)) {
            throw new IllegalStateException("2FA tam khoa do nhap sai qua nhieu lan. Vui long thu lai sau 15 phut.");
        }
        boolean validTotp = totpHelper.verifyCode(twoFactor.getTotpSecret(), code);
        if (validTotp) {
            twoFactor.resetAttempts();
            twoFactorRepository.save(twoFactor);
            return true;
        }
        if (tryConsumeBackupCode(twoFactor, code)) {
            twoFactor.resetAttempts();
            twoFactorRepository.save(twoFactor);
            return true;
        }
        twoFactor.recordFailedAttempt(now);
        twoFactorRepository.save(twoFactor);
        return false;
    }

    @Transactional
    public boolean disable(String userId, String code) {
        if (!verifyChallenge(userId, code)) {
            return false;
        }
        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA chua duoc khoi tao"));
        twoFactor.disable();
        twoFactorRepository.save(twoFactor);
        userRepository.findById(userId).ifPresent(user -> {
            user.disableTwoFactor();
            userRepository.save(user);
        });
        return true;
    }

    @Transactional
    public TwoFactorRecoveryRequest requestAdminRecovery(String targetUserId, String requestedByAdminId, String reason) {
        if (recoveryRepository == null) {
            throw new IllegalStateException("Kho luu tru yeu cau khoi phuc 2FA chua duoc cau hinh");
        }
        return recoveryRepository.save(TwoFactorRecoveryRequest.request(targetUserId, requestedByAdminId, reason));
    }

    @Transactional
    public TwoFactorRecoveryRequest approveAdminRecovery(String recoveryId, String secondAdminId) {
        if (recoveryRepository == null) {
            throw new IllegalStateException("Kho luu tru yeu cau khoi phuc 2FA chua duoc cau hinh");
        }
        TwoFactorRecoveryRequest req = recoveryRepository.findById(recoveryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau khoi phuc 2FA"));
        req.approve(secondAdminId);
        twoFactorRepository.findByUserId(req.getTargetUserId()).ifPresent(tf -> {
            tf.disable();
            twoFactorRepository.save(tf);
        });
        userRepository.findById(req.getTargetUserId()).ifPresent(user -> {
            user.disableTwoFactor();
            userRepository.save(user);
        });
        return recoveryRepository.save(req);
    }

    @Transactional(readOnly = true)
    public TwoFactorDto.StatusResponse getStatus(String userId) {
        boolean enabled = twoFactorRepository.findByUserId(userId).map(UserTwoFactor::isEnabled).orElse(false);
        return new TwoFactorDto.StatusResponse(enabled);
    }

    private boolean tryConsumeBackupCode(UserTwoFactor twoFactor, String code) {
        if (code == null || twoFactor.getBackupCodesJson() == null) {
            return false;
        }
        try {
            List<String> storedHashes = objectMapper.readValue(twoFactor.getBackupCodesJson(), new TypeReference<>() {});
            String candidate = code.trim();
            String candidateHash = hashRecoveryCode(candidate);
            boolean matched = storedHashes.removeIf(entry ->
                    java.security.MessageDigest.isEqual(
                            entry.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            candidateHash.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                            || java.security.MessageDigest.isEqual(
                                    entry.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                    candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (matched) {
                twoFactor.replaceBackupCodesJson(objectMapper.writeValueAsString(storedHashes));
            }
            return matched;
        } catch (Exception ex) {
            return false;
        }
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            codes.add(String.format("%08d", random.nextInt(100_000_000)));
        }
        return codes;
    }

    private String serializeBackupCodes(List<String> codes) {
        try {
            List<String> hashed = codes.stream().map(TwoFactorAuthService::hashRecoveryCode).toList();
            return objectMapper.writeValueAsString(hashed);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static String hashRecoveryCode(String rawCode) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawCode.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "sha256:" + java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
