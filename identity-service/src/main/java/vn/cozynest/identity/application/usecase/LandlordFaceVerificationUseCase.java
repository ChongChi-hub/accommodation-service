package vn.cozynest.identity.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.identity.application.dto.FaceChallengeResponse;
import vn.cozynest.identity.application.dto.FaceStatusResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.port.FaceProviderResult;
import vn.cozynest.identity.domain.port.FaceVerificationProvider;
import vn.cozynest.identity.domain.repository.FaceVerificationFrameRepository;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.infrastructure.security.FaceCryptoService;
import vn.cozynest.identity.infrastructure.security.FaceImageValidator;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class LandlordFaceVerificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(LandlordFaceVerificationUseCase.class);
    private static final int MAX_ATTEMPTS_PER_DAY = 3;
    private static final String CONSENT_POLICY_VERSION = "v1.0";

    private final FaceVerificationRepository faceRepository;
    private final FaceVerificationFrameRepository frameRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final FacePrivateStoragePort storagePort;
    private final FaceCryptoService cryptoService;
    private final FaceImageValidator imageValidator;
    private final Map<String, FaceVerificationProvider> providers;
    private final String activeProviderName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LandlordFaceVerificationUseCase(
            FaceVerificationRepository faceRepository,
            FaceVerificationFrameRepository frameRepository,
            UserVerificationRepository userVerificationRepository,
            FacePrivateStoragePort storagePort,
            FaceCryptoService cryptoService,
            FaceImageValidator imageValidator,
            List<FaceVerificationProvider> providerList,
            @Value("${face.provider:manual}") String activeProviderName) {
        this.faceRepository = faceRepository;
        this.frameRepository = frameRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.storagePort = storagePort;
        this.cryptoService = cryptoService;
        this.imageValidator = imageValidator;
        this.activeProviderName = activeProviderName != null ? activeProviderName.trim() : "manual";
        this.providers = new HashMap<>();
        for (FaceVerificationProvider p : providerList) {
            this.providers.put(p.getProviderName(), p);
        }
    }

    public FaceChallengeResponse createChallenge(String userId, boolean consent) {
        if (!consent) {
            throw new IdentityDomainException("Bạn phải đồng ý riêng với điều khoản xử lý dữ liệu sinh trắc học khuôn mặt", "ERR_FACE_CONSENT_REQUIRED");
        }

        UserVerification kyc = userVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new IdentityDomainException("Bạn cần hoàn tất nộp giấy tờ tùy thân (KYC) trước khi xác thực khuôn mặt", "ERR_FACE_DOC_REQUIRED"));

        if (kyc.getStatus() == KycStatus.NONE || kyc.getStatus() == KycStatus.REJECTED) {
            throw new IdentityDomainException("Hồ sơ giấy tờ tùy thân chưa hợp lệ hoặc đã bị từ chối", "ERR_FACE_DOC_INVALID");
        }

        Instant oneDayAgo = Instant.now().minus(Duration.ofHours(24));
        int attemptsIn24h = faceRepository.countAttemptsInLast24Hours(userId, oneDayAgo);
        if (attemptsIn24h >= MAX_ATTEMPTS_PER_DAY) {
            throw new IdentityDomainException("Bạn đã vượt quá số lần thử xác thực khuôn mặt trong 24 giờ (tối đa 3 lần). Vui lòng thử lại sau.", "ERR_FACE_RATE_LIMIT_EXCEEDED");
        }

        List<FaceActionType> actions = FaceActionType.pickRandomTwo();
        List<String> actionCodes = actions.stream().map(Enum::name).toList();
        String challengeJson;
        try {
            challengeJson = objectMapper.writeValueAsString(actionCodes);
        } catch (Exception e) {
            challengeJson = "[\"" + actionCodes.get(0) + "\",\"" + actionCodes.get(1) + "\"]";
        }

        String nonce = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));

        Optional<FaceVerification> existingOpt = faceRepository.findByUserId(userId);
        FaceVerification verification;
        int nextAttemptNo = attemptsIn24h + 1;

        if (existingOpt.isPresent()) {
            verification = existingOpt.get();
            if (verification.getStatus() == FaceVerificationStatus.PASSED) {
                throw new IdentityDomainException("Khuôn mặt của bạn đã được xác thực thành công trước đó", "ERR_FACE_ALREADY_PASSED");
            }
            verification.updateChallenge(challengeJson, nonce, expiresAt, nextAttemptNo);
        } else {
            verification = FaceVerification.createChallenge(
                    userId, kyc.getId(), activeProviderName, challengeJson, nonce, expiresAt, nextAttemptNo, CONSENT_POLICY_VERSION
            );
        }
        faceRepository.save(verification);

        return FaceChallengeResponse.builder()
                .nonce(nonce)
                .expiresAt(expiresAt)
                .attemptNo(nextAttemptNo)
                .remainingAttempts(Math.max(0, MAX_ATTEMPTS_PER_DAY - nextAttemptNo))
                .actions(actions.stream().map(a -> FaceChallengeResponse.ActionItem.builder()
                        .code(a.name())
                        .description(a.getDescription())
                        .build()).toList())
                .build();
    }

    public FaceStatusResponse submitFrames(
            String userId, String nonce, boolean livenessClientFlag,
            MultipartFile neutralFrame, List<MultipartFile> actionFrames) {

        FaceVerification verification = faceRepository.findByUserId(userId)
                .orElseThrow(() -> new IdentityDomainException("Chưa có phiên thử thách xác thực khuôn mặt nào được khởi tạo", "ERR_FACE_CHALLENGE_NOT_FOUND"));

        if (verification.getChallengeNonce() == null || !verification.getChallengeNonce().equals(nonce)) {
            throw new IdentityDomainException("Mã xác thực thử thách (nonce) không hợp lệ hoặc đã qua sử dụng", "ERR_FACE_NONCE_INVALID");
        }
        if (verification.getChallengeExpiresAt() == null || Instant.now().isAfter(verification.getChallengeExpiresAt())) {
            verification.markExpired();
            faceRepository.save(verification);
            throw new IdentityDomainException("Phiên thử thách xác thực khuôn mặt đã hết hạn (2 phút). Vui lòng lấy thử thách mới.", "ERR_FACE_CHALLENGE_EXPIRED");
        }

        List<String> requiredActions = parseChallengeActions(verification.getChallengeJson());
        if (actionFrames == null || actionFrames.size() != requiredActions.size()) {
            throw new IdentityDomainException("Số lượng ảnh chụp hành động không khớp với thử thách yêu cầu", "ERR_FACE_FRAMES_COUNT_MISMATCH");
        }

        // Clean previous frames if re-attempting
        List<FaceVerificationFrame> oldFrames = frameRepository.findActiveByFaceVerificationId(verification.getId());
        for (FaceVerificationFrame old : oldFrames) {
            storagePort.delete(old.getStorageKey());
            old.markDeleted();
            frameRepository.save(old);
        }

        List<FaceVerificationFrame> newFrames = new ArrayList<>();
        storeFrame(verification.getId(), "NEUTRAL", neutralFrame, newFrames);
        for (int i = 0; i < actionFrames.size(); i++) {
            storeFrame(verification.getId(), requiredActions.get(i), actionFrames.get(i), newFrames);
        }

        frameRepository.saveAll(newFrames);
        verification.completeSubmission(livenessClientFlag);

        // Execute provider evaluation (vendor or manual)
        FaceVerificationProvider provider = providers.getOrDefault(verification.getProvider(), providers.get("manual"));
        if (provider != null) {
            FaceProviderResult result = provider.verify(verification, newFrames);
            if (result.isAutoDecided()) {
                if (result.isPassed()) {
                    verification.pass("SYSTEM_VENDOR");
                } else {
                    verification.fail("SYSTEM_VENDOR", result.getReasonCode(), result.getNote());
                }
            }
        }

        FaceVerification saved = faceRepository.save(verification);
        return toStatusResponse(saved);
    }

    @Transactional(readOnly = true)
    public FaceStatusResponse getFaceStatus(String userId) {
        Optional<FaceVerification> opt = faceRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return FaceStatusResponse.builder()
                    .status("NONE")
                    .provider(activeProviderName)
                    .remainingAttempts(MAX_ATTEMPTS_PER_DAY)
                    .canSubmit(true)
                    .build();
        }
        return toStatusResponse(opt.get());
    }

    public void revokeConsentAndPurge(String userId) {
        FaceVerification verification = faceRepository.findByUserId(userId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy dữ liệu xác thực khuôn mặt của bạn", "ERR_FACE_NOT_FOUND"));

        List<FaceVerificationFrame> frames = frameRepository.findActiveByFaceVerificationId(verification.getId());
        for (FaceVerificationFrame f : frames) {
            storagePort.delete(f.getStorageKey());
            f.markDeleted();
            frameRepository.save(f);
        }
        verification.markExpired();
        faceRepository.save(verification);
        log.info("User {} revoked biometric consent. All face frames purged securely.", userId);
    }

    private List<String> parseChallengeActions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private FaceStatusResponse toStatusResponse(FaceVerification v) {
        Instant oneDayAgo = Instant.now().minus(Duration.ofHours(24));
        int attemptsIn24h = faceRepository.countAttemptsInLast24Hours(v.getUserId(), oneDayAgo);
        int remaining = Math.max(0, MAX_ATTEMPTS_PER_DAY - attemptsIn24h);
        boolean canSubmit = v.getStatus() != FaceVerificationStatus.PASSED && remaining > 0;

        return FaceStatusResponse.builder()
                .status(v.getStatus().name())
                .provider(v.getProvider())
                .livenessClientFlag(v.isLivenessClientFlag())
                .attemptNo(v.getAttemptNo())
                .remainingAttempts(remaining)
                .canSubmit(canSubmit)
                .consentAt(v.getConsentAt())
                .consentVersion(v.getConsentVersion())
                .rejectReasonCode(v.getRejectReasonCode())
                .rejectReasonNote(v.getRejectReasonNote())
                .submittedAt(v.getUpdatedAt())
                .reviewedAt(v.getReviewedAt())
                .build();
    }

    private void storeFrame(String fvId, String purpose, MultipartFile file, List<FaceVerificationFrame> list) {
        byte[] bytes = imageValidator.validateAndSanitize(file);
        String key = "face/" + fvId + "/" + purpose.toLowerCase() + "_" + UUID.randomUUID() + ".enc";
        storagePort.store(key, cryptoService.encrypt(bytes));
        list.add(FaceVerificationFrame.create(fvId, purpose, key, cryptoService.computeSha256(bytes), bytes.length));
    }
}

