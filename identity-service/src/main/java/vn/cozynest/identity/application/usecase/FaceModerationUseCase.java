package vn.cozynest.identity.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.FaceDecisionRequest;
import vn.cozynest.identity.application.dto.FaceModerationDetailDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.FaceCryptoService;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FaceModerationUseCase {

    private final FaceVerificationRepository faceRepository;
    private final FaceVerificationFrameRepository frameRepository;
    private final FaceAccessLogRepository accessLogRepository;
    private final FaceEventOutboxRepository outboxRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UserRepository userRepository;
    private final FacePrivateStoragePort storagePort;
    private final FaceCryptoService faceCryptoService;
    private final KycCryptoService kycCryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FaceModerationUseCase(
            FaceVerificationRepository faceRepository,
            FaceVerificationFrameRepository frameRepository,
            FaceAccessLogRepository accessLogRepository,
            FaceEventOutboxRepository outboxRepository,
            UserVerificationRepository userVerificationRepository,
            UserRepository userRepository,
            FacePrivateStoragePort storagePort,
            FaceCryptoService faceCryptoService,
            KycCryptoService kycCryptoService) {
        this.faceRepository = faceRepository;
        this.frameRepository = frameRepository;
        this.accessLogRepository = accessLogRepository;
        this.outboxRepository = outboxRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.userRepository = userRepository;
        this.storagePort = storagePort;
        this.faceCryptoService = faceCryptoService;
        this.kycCryptoService = kycCryptoService;
    }

    @Transactional(readOnly = true)
    public FaceModerationDetailDto getFaceDetail(String reviewerId, String reviewerRole, String kycId, String clientIp) {
        FaceVerification v = findVerificationByKycOrId(kycId);
        checkNotSelfReview(reviewerId, v.getUserId());

        accessLogRepository.save(FaceAccessLog.record(
                reviewerId, reviewerRole, null, v.getId(), "VIEW_DETAIL", clientIp, null
        ));

        UserVerification docKyc = userVerificationRepository.findById(v.getKycId()).orElse(null);
        User user = userRepository.findById(v.getUserId()).orElse(null);
        List<FaceVerificationFrame> frames = frameRepository.findByFaceVerificationId(v.getId());
        List<FaceAccessLog> logs = accessLogRepository.findByFaceVerificationId(v.getId());

        List<String> actions = List.of();
        if (v.getChallengeJson() != null && !v.getChallengeJson().isBlank()) {
            try {
                actions = objectMapper.readValue(v.getChallengeJson(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }

        return FaceModerationDetailDto.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .kycId(v.getKycId())
                .userEmail(user != null ? user.getEmail() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .fullNameOnId(docKyc != null ? docKyc.getFullNameOnId() : null)
                .idNumberMasked(docKyc != null ? kycCryptoService.maskIdNumber(docKyc.getIdNumberLast4()) : null)
                .status(v.getStatus().name())
                .provider(v.getProvider())
                .livenessClientFlag(v.isLivenessClientFlag())
                .challengeActions(actions)
                .attemptNo(v.getAttemptNo())
                .consentAt(v.getConsentAt())
                .consentVersion(v.getConsentVersion())
                .reviewedBy(v.getReviewedBy())
                .reviewedAt(v.getReviewedAt())
                .rejectReasonCode(v.getRejectReasonCode())
                .rejectReasonNote(v.getRejectReasonNote())
                .version(v.getVersion())
                .frames(frames.stream().map(f -> FaceModerationDetailDto.FrameItem.builder()
                        .id(f.getId())
                        .purpose(f.getPurpose())
                        .size(f.getSize())
                        .checksum(f.getChecksum())
                        .createdAt(f.getCreatedAt())
                        .isDeleted(f.isDeleted())
                        .build()).toList())
                .accessLogs(logs.stream().map(l -> FaceModerationDetailDto.AccessLogItem.builder()
                        .actorId(l.getActorId())
                        .actorRole(l.getActorRole())
                        .purpose(l.getPurpose())
                        .ipAddress(l.getIpAddress())
                        .createdAt(l.getCreatedAt())
                        .build()).toList())
                .build();
    }

    public byte[] streamFrame(String reviewerId, String reviewerRole, String kycId, String frameId, String clientIp, String userAgent) {
        FaceVerification v = findVerificationByKycOrId(kycId);
        checkNotSelfReview(reviewerId, v.getUserId());

        FaceVerificationFrame frame = frameRepository.findById(frameId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy khung hình xác thực khuôn mặt", "ERR_FACE_FRAME_NOT_FOUND"));

        if (!frame.getFaceVerificationId().equals(v.getId())) {
            throw new IdentityDomainException("Khung hình không thuộc hồ sơ xác thực này", "ERR_FACE_FRAME_MISMATCH");
        }
        if (frame.isDeleted()) {
            throw new IdentityDomainException("Khung hình đã bị xóa theo chính sách lưu trữ quyền riêng tư", "ERR_FACE_FRAME_DELETED");
        }

        accessLogRepository.save(FaceAccessLog.record(
                reviewerId, reviewerRole, frame.getId(), v.getId(), "STREAM_FRAME", clientIp, userAgent
        ));

        byte[] encryptedBytes = storagePort.load(frame.getStorageKey());
        if (encryptedBytes == null || encryptedBytes.length == 0) {
            throw new IdentityDomainException("Không thể đọc tệp dữ liệu khung hình mã hóa", "ERR_FACE_STORAGE_READ");
        }
        return faceCryptoService.decrypt(encryptedBytes);
    }

    public void decide(String reviewerId, String reviewerRole, String kycId, FaceDecisionRequest request, Long expectedVersion, String clientIp) {
        FaceVerification v = findVerificationByKycOrId(kycId);
        checkNotSelfReview(reviewerId, v.getUserId());

        if (expectedVersion != null && !expectedVersion.equals(v.getVersion())) {
            throw new IdentityDomainException("Hồ sơ đã được cập nhật bởi kiểm duyệt viên khác", "ERR_FACE_VERSION_CONFLICT");
        }

        String decision = request.getDecision() != null ? request.getDecision().toUpperCase().trim() : "";
        if ("PASSED".equals(decision)) {
            v.pass(reviewerId);
            accessLogRepository.save(FaceAccessLog.record(
                    reviewerId, reviewerRole, null, v.getId(), "DECIDE_PASSED", clientIp, null
            ));
            outboxRepository.save(FaceEventOutbox.create(
                    v.getId(), "FaceVerificationPassed", "{\"verificationId\":\"" + v.getId() + "\",\"userId\":\"" + v.getUserId() + "\"}"
            ));
        } else if ("FAILED".equals(decision)) {
            v.fail(reviewerId, request.getReasonCode(), request.getNote());
            accessLogRepository.save(FaceAccessLog.record(
                    reviewerId, reviewerRole, null, v.getId(), "DECIDE_FAILED", clientIp, null
            ));
            outboxRepository.save(FaceEventOutbox.create(
                    v.getId(), "FaceVerificationFailed", "{\"verificationId\":\"" + v.getId() + "\",\"userId\":\"" + v.getUserId() + "\",\"reason\":\"" + request.getReasonCode() + "\"}"
            ));
        } else {
            throw new IdentityDomainException("Quyết định kiểm duyệt không hợp lệ (PASSED hoặc FAILED)", "ERR_FACE_INVALID_DECISION");
        }

        faceRepository.save(v);
    }

    private FaceVerification findVerificationByKycOrId(String kycOrVerificationId) {
        Optional<FaceVerification> byKyc = faceRepository.findByKycId(kycOrVerificationId);
        if (byKyc.isPresent()) return byKyc.get();
        return faceRepository.findById(kycOrVerificationId)
                .orElseThrow(() -> new IdentityDomainException("Không tìm thấy hồ sơ xác thực khuôn mặt", "ERR_FACE_NOT_FOUND"));
    }

    private void checkNotSelfReview(String reviewerId, String targetUserId) {
        if (reviewerId != null && reviewerId.equals(targetUserId)) {
            throw new IdentityDomainException("Kiểm duyệt viên không được phép tự duyệt hồ sơ khuôn mặt của chính mình", "ERR_SELF_REVIEW_FORBIDDEN");
        }
    }
}

