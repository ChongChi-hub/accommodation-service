package vn.cozynest.identity.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.port.SmsSenderPort;
import vn.cozynest.identity.domain.model.PhoneVerification;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.PhoneVerificationRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.service.PhoneNumberPolicy;

import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class PhoneVerificationService {
    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);
    private static final long OTP_TTL_SECONDS = 300;
    private static final long RATE_LIMIT_SECONDS = 600;
    private static final long DAILY_QUOTA_SECONDS = 86_400;
    private static final int MAX_ATTEMPTS_PER_10_MIN = 3;
    private static final int MAX_ATTEMPTS_PER_DAY = 5;

    private final PhoneVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final SmsSenderPort smsSenderPort;
    private final boolean sandboxFixedOtpAllowed;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(PhoneVerificationRepository verificationRepository,
                                    UserRepository userRepository,
                                    SmsSenderPort smsSenderPort) {
        this(verificationRepository, userRepository, smsSenderPort, false, null);
    }

    @Autowired
    public PhoneVerificationService(PhoneVerificationRepository verificationRepository,
                                    UserRepository userRepository,
                                    SmsSenderPort smsSenderPort,
                                    @Value("${app.auth.allow-sandbox-phone-otp:false}") boolean allowSandboxFlag,
                                    Environment environment) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.smsSenderPort = smsSenderPort;
        String[] activeProfiles = environment != null ? environment.getActiveProfiles() : null;
        boolean restrictedProfile = activeProfiles != null
                && Arrays.stream(activeProfiles)
                        .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        this.sandboxFixedOtpAllowed = allowSandboxFlag && !restrictedProfile;
    }

    @Transactional
    public String sendOtp(String rawPhoneNumber) {
        String phoneNumber = PhoneNumberPolicy.normalizeAndValidate(rawPhoneNumber);
        long recentCount = verificationRepository.countRecentByPhone(phoneNumber, RATE_LIMIT_SECONDS);
        if (recentCount >= MAX_ATTEMPTS_PER_10_MIN) {
            throw new IllegalStateException("Ban da yeu cau qua nhieu ma OTP. Vui long thu lai sau 10 phut.");
        }
        long dailyCount = verificationRepository.countRecentByPhone(phoneNumber, DAILY_QUOTA_SECONDS);
        if (dailyCount >= MAX_ATTEMPTS_PER_DAY) {
            throw new IllegalStateException("So dien thoai da dat han muc gui SMS trong ngay.");
        }

        String plainOtp = (sandboxFixedOtpAllowed && "0912345678".equals(phoneNumber))
                ? "123456"
                : String.format("%06d", secureRandom.nextInt(1_000_000));

        PhoneVerification verification = PhoneVerification.create(phoneNumber, plainOtp, OTP_TTL_SECONDS);
        verificationRepository.save(verification);
        smsSenderPort.sendOtpSms(phoneNumber, plainOtp);
        log.info("Dispatched phone OTP to {}", PhoneNumberPolicy.mask(phoneNumber));
        return PhoneNumberPolicy.mask(phoneNumber);
    }

    @Transactional
    public boolean verifyOtp(String rawPhoneNumber, String rawOtp, String userId) {
        String phoneNumber = PhoneNumberPolicy.normalizeAndValidate(rawPhoneNumber);
        PhoneVerification verification = verificationRepository.findLatestByPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau xac thuc"));

        boolean success = verification.verify(rawOtp);
        verificationRepository.save(verification);

        if (success && userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.isPhoneVerified() && user.getPhone() != null && !user.getPhone().equals(phoneNumber)) {
                    throw new IllegalStateException("Doi so dien thoai da xac minh yeu cau dung luong /change kem OTP so cu");
                }
                user.updatePhoneNumber(phoneNumber);
                userRepository.save(user);
            });
            log.info("Verified phone {} for user {}", PhoneNumberPolicy.mask(phoneNumber), userId);
        }
        return success;
    }

    @Transactional
    public String changePhoneNumber(String userId, String rawNewPhone, String newOtp, String oldOtp) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Yeu cau dang nhap de doi so dien thoai");
        }
        String newPhone = PhoneNumberPolicy.normalizeAndValidate(rawNewPhone);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nguoi dung khong ton tai"));

        if (user.isPhoneVerified() && user.getPhone() != null && !user.getPhone().isBlank()) {
            String currentPhone = PhoneNumberPolicy.normalizeAndValidate(user.getPhone());
            if (currentPhone.equals(newPhone)) {
                throw new IllegalArgumentException("So dien thoai moi trung voi so hien tai");
            }
            if (oldOtp == null || oldOtp.isBlank()) {
                throw new IllegalArgumentException("Can xac thuc OTP tu so dien thoai cu");
            }
            PhoneVerification oldVerification = verificationRepository.findLatestByPhone(currentPhone)
                    .orElseThrow(() -> new IllegalArgumentException("Chua gui OTP cho so dien thoai cu"));
            boolean oldOk = oldVerification.verify(oldOtp);
            verificationRepository.save(oldVerification);
            if (!oldOk) {
                throw new IllegalStateException("Ma OTP cua so dien thoai cu khong chinh xac");
            }
        }

        PhoneVerification newVerification = verificationRepository.findLatestByPhone(newPhone)
                .orElseThrow(() -> new IllegalArgumentException("Chua gui OTP cho so dien thoai moi"));
        boolean newOk = newVerification.verify(newOtp);
        verificationRepository.save(newVerification);
        if (!newOk) {
            throw new IllegalStateException("Ma OTP cua so dien thoai moi khong chinh xac");
        }

        user.updatePhoneNumber(newPhone);
        userRepository.save(user);
        log.info("Changed verified phone to {} for user {}", PhoneNumberPolicy.mask(newPhone), userId);
        return PhoneNumberPolicy.mask(newPhone);
    }
}
