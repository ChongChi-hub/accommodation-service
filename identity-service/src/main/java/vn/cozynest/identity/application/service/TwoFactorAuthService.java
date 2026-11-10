package vn.cozynest.identity.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.TwoFactorDto;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserTwoFactor;
import vn.cozynest.identity.domain.repository.UserTwoFactorRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.security.TotpHelper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class TwoFactorAuthService {
    private final UserTwoFactorRepository twoFactorRepository;
    private final UserRepository userRepository;
    private final TotpHelper totpHelper;
    private final ObjectMapper objectMapper;

    public TwoFactorAuthService(UserTwoFactorRepository twoFactorRepository,
                                UserRepository userRepository,
                                TotpHelper totpHelper,
                                ObjectMapper objectMapper) {
        this.twoFactorRepository = twoFactorRepository;
        this.userRepository = userRepository;
        this.totpHelper = totpHelper;
        this.objectMapper = objectMapper;
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
        String qrUri = totpHelper.buildOtpAuthUri(secret, userEmail, "ToAm");

        return new TwoFactorDto.SetupResponse(secret, qrUri, backupCodes);
    }

    @Transactional
    public boolean verifyAndEnable(String userId, String code) {
        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA chua duoc khoi tao"));

        boolean valid = totpHelper.verifyCode(twoFactor.getTotpSecret(), code);
        if (!valid) {
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
    public boolean disable(String userId, String code) {
        UserTwoFactor twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA chua duoc khoi tao"));

        boolean valid = totpHelper.verifyCode(twoFactor.getTotpSecret(), code);
        if (!valid) {
            return false;
        }

        twoFactor.disable();
        twoFactorRepository.save(twoFactor);

        userRepository.findById(userId).ifPresent(user -> {
            user.disableTwoFactor();
            userRepository.save(user);
        });

        return true;
    }

    @Transactional(readOnly = true)
    public TwoFactorDto.StatusResponse getStatus(String userId) {
        boolean enabled = twoFactorRepository.findByUserId(userId)
                .map(UserTwoFactor::isEnabled)
                .orElse(false);
        return new TwoFactorDto.StatusResponse(enabled);
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
            return objectMapper.writeValueAsString(codes);
        } catch (Exception e) {
            return "[]";
        }
    }
}

