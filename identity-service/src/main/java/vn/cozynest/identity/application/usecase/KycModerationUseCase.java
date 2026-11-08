package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.KycModerationDetailDto;
import vn.cozynest.identity.application.dto.KycModerationListItemDto;
import vn.cozynest.identity.application.dto.RejectKycRequest;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KycModerationUseCase {

    private final UserVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final KycStatusHistoryRepository historyRepository;
    private final KycAccessLogRepository accessLogRepository;
    private final KycEventOutboxRepository outboxRepository;
    private final KycPrivateStoragePort kycStorage;
    private final KycCryptoService cryptoService;

    public KycModerationUseCase(
            UserVerificationRepository verificationRepository,
            UserRepository userRepository,
            KycStatusHistoryRepository historyRepository,
            KycAccessLogRepository accessLogRepository,
            KycEventOutboxRepository outboxRepository,
            KycPrivateStoragePort kycStorage,
            KycCryptoService cryptoService) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.accessLogRepository = accessLogRepository;
        this.outboxRepository = outboxRepository;
        this.kycStorage = kycStorage;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public List<KycModerationListItemDto> getKycList(KycStatus status, String search, int page, int size) {
        int offset = page * size;
        return verificationRepository.findAllFiltered(status, search, offset, size).stream()
                .map(v -> {
                    Optional<User> uOpt = userRepository.findById(v.getUserId());
                    return KycModerationListItemDto.builder()
                            .id(v.getId())
                            .userId(v.getUserId())
                            .userEmail(uOpt.map(User::getEmail).orElse(null))
                            .userFullName(uOpt.map(User::getFullName).orElse(null))
                            .fullNameOnId(v.getFullNameOnId())
                            .idNumberMasked(cryptoService.maskIdNumber(v.getIdNumberLast4()))
                            .status(v.getStatus().name())
                            .submittedAt(v.getSubmittedAt())
                            .reviewedAt(v.getReviewedAt())
                            .reviewedBy(v.getReviewedBy())
                            .rejectReasonCode(v.getRejectReasonCode() != null ? v.getRejectReasonCode().name() : null)
                            .attemptCount(v.getAttemptCount())
                            .build();
                })
                .toList();
    }

    public KycModerationDetailDto getKycDetail(String reviewerId, String reviewerRole, String kycId, String clientIp) {
        UserVerification v = verificationRepository.findById(kycId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so xac minh", "ERR_KYC_NOT_FOUND"));

        accessLogRepository.save(KycAccessLog.record(v.getId(), reviewerId, reviewerRole, KycAccessAction.VIEW_DETAIL, clientIp));

        User user = userRepository.findById(v.getUserId()).orElse(null);
        String plainId = cryptoService.decryptIdNumber(v.getIdNumberEnc());

        List<KycModerationDetailDto.StatusHistoryItem> history = historyRepository.findByVerificationIdOrderByCreatedAtAsc(v.getId())
                .stream()
                .map(h -> KycModerationDetailDto.StatusHistoryItem.builder()
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .actorId(h.getActorId())
                        .reason(h.getReason())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        List<KycModerationDetailDto.AccessLogItem> logs = accessLogRepository.findByVerificationIdOrderByCreatedAtDesc(v.getId())
                .stream()
                .map(l -> KycModerationDetailDto.AccessLogItem.builder()
                        .actorId(l.getActorId())
                        .actorRole(l.getActorRole())
                        .action(l.getAction().name())
                        .ip(l.getIp())
                        .createdAt(l.getCreatedAt())
                        .build())
                .toList();

        return KycModerationDetailDto.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .fullNameOnId(v.getFullNameOnId())
                .dateOfBirth(v.getDateOfBirth())
                .idNumberPlain(plainId)
                .idNumberLast4(v.getIdNumberLast4())
                .issueDate(v.getIssueDate())
                .issuePlace(v.getIssuePlace())
                .status(v.getStatus().name())
                .consentAt(v.getConsentAt())
                .consentPolicyVersion(v.getConsentPolicyVersion())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .reviewedBy(v.getReviewedBy())
                .rejectReasonCode(v.getRejectReasonCode() != null ? v.getRejectReasonCode().name() : null)
                .rejectReasonNote(v.getRejectReasonNote())
                .attemptCount(v.getAttemptCount())
                .version(v.getVersion())
                .statusHistory(history)
                .accessLogs(logs)
                .build();
    }

    public byte[] streamKycImage(String reviewerId, String reviewerRole, String kycId, String side, String clientIp) {
        UserVerification v = verificationRepository.findById(kycId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so xac minh", "ERR_KYC_NOT_FOUND"));

        boolean isBack = "back".equalsIgnoreCase(side);
        KycAccessAction action = isBack ? KycAccessAction.VIEW_BACK : KycAccessAction.VIEW_FRONT;
        accessLogRepository.save(KycAccessLog.record(v.getId(), reviewerId, reviewerRole, action, clientIp));

        String key = isBack ? v.getBackImageKey() : v.getFrontImageKey();
        if (key == null || key.isBlank() || "PURGED".equals(key)) {
            throw new IdentityDomainException("Khong tim thay tep hinh anh", "ERR_KYC_IMAGE_NOT_FOUND");
        }

        byte[] bytes = kycStorage.load(key);
        if (bytes == null || bytes.length == 0) {
            throw new IdentityDomainException("Khong the doc du lieu tep anh", "ERR_KYC_IMAGE_NOT_FOUND");
        }
        return bytes;
    }

    public void approveKyc(String reviewerId, String reviewerRole, String kycId, Long expectedVersion, String clientIp) {
        UserVerification v = verificationRepository.findById(kycId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so xac minh", "ERR_KYC_NOT_FOUND"));

        if (expectedVersion != null && !expectedVersion.equals(v.getVersion())) {
            throw new IdentityDomainException("Ho so da duoc thay doi boi kiem duyet vien khac", "ERR_KYC_VERSION_CONFLICT");
        }

        v.approve(reviewerId);
        UserVerification saved = verificationRepository.save(v);

        historyRepository.save(KycStatusHistory.record(
                saved.getId(), KycStatus.PENDING.name(), KycStatus.APPROVED.name(), reviewerId, "Phe duyet ho so thanh cong"
        ));
        accessLogRepository.save(KycAccessLog.record(saved.getId(), reviewerId, reviewerRole, KycAccessAction.APPROVE, clientIp));

        String outboxPayload = "{\"verificationId\":\"" + saved.getId() + "\",\"userId\":\"" + saved.getUserId() + "\"}";
        outboxRepository.save(KycEventOutbox.create(saved.getId(), "KycApproved", outboxPayload));
    }

    public void rejectKyc(String reviewerId, String reviewerRole, String kycId, RejectKycRequest request, Long expectedVersion, String clientIp) {
        UserVerification v = verificationRepository.findById(kycId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so xac minh", "ERR_KYC_NOT_FOUND"));

        if (expectedVersion != null && !expectedVersion.equals(v.getVersion())) {
            throw new IdentityDomainException("Ho so da duoc thay doi boi kiem duyet vien khac", "ERR_KYC_VERSION_CONFLICT");
        }

        v.reject(reviewerId, request.getReasonCode(), request.getReasonNote());
        UserVerification saved = verificationRepository.save(v);

        historyRepository.save(KycStatusHistory.record(
                saved.getId(), KycStatus.PENDING.name(), KycStatus.REJECTED.name(), reviewerId,
                request.getReasonNote() != null ? request.getReasonNote() : request.getReasonCode().name()
        ));
        accessLogRepository.save(KycAccessLog.record(saved.getId(), reviewerId, reviewerRole, KycAccessAction.REJECT, clientIp));

        String outboxPayload = "{\"verificationId\":\"" + saved.getId() + "\",\"userId\":\"" + saved.getUserId() + "\"}";
        outboxRepository.save(KycEventOutbox.create(saved.getId(), "KycRejected", outboxPayload));
    }
}

