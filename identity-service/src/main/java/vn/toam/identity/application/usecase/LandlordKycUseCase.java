package vn.toam.identity.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.toam.common.image.ImageProcessingHelper;
import vn.toam.identity.application.dto.LandlordKycStatusResponse;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.KycEventOutbox;
import vn.toam.identity.domain.model.KycStatus;
import vn.toam.identity.domain.model.KycStatusHistory;
import vn.toam.identity.domain.model.UserVerification;
import vn.toam.identity.domain.port.KycPrivateStoragePort;
import vn.toam.identity.domain.repository.KycEventOutboxRepository;
import vn.toam.identity.domain.repository.KycStatusHistoryRepository;
import vn.toam.identity.domain.repository.UserVerificationRepository;
import vn.toam.identity.infrastructure.security.KycCryptoService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LandlordKycUseCase {

    private static final Logger log = LoggerFactory.getLogger(LandlordKycUseCase.class);
    private static final int MAX_SUBMISSIONS_PER_DAY = 3;

    private final UserVerificationRepository verificationRepository;
    private final KycStatusHistoryRepository historyRepository;
    private final KycEventOutboxRepository outboxRepository;
    private final KycPrivateStoragePort kycStorage;
    private final KycCryptoService cryptoService;

    public LandlordKycUseCase(
            UserVerificationRepository verificationRepository,
            KycStatusHistoryRepository historyRepository,
            KycEventOutboxRepository outboxRepository,
            KycPrivateStoragePort kycStorage,
            KycCryptoService cryptoService) {
        this.verificationRepository = verificationRepository;
        this.historyRepository = historyRepository;
        this.outboxRepository = outboxRepository;
        this.kycStorage = kycStorage;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public LandlordKycStatusResponse getKycStatus(String landlordId) {
        Optional<UserVerification> opt = verificationRepository.findByUserId(landlordId);
        if (opt.isEmpty()) {
            return LandlordKycStatusResponse.builder()
                    .status(KycStatus.NONE.name())
                    .remainingAttempts(MAX_SUBMISSIONS_PER_DAY)
                    .canResubmit(true)
                    .hasFrontImage(false)
                    .hasBackImage(false)
                    .build();
        }
        return toResponse(opt.get());
    }

    private LandlordKycStatusResponse toResponse(UserVerification v) {
        boolean canResubmit = v.getStatus() == KycStatus.NONE || v.getStatus() == KycStatus.REJECTED;
        int remaining = Math.max(0, MAX_SUBMISSIONS_PER_DAY - (v.getAttemptCount() % MAX_SUBMISSIONS_PER_DAY));

        return LandlordKycStatusResponse.builder()
                .id(v.getId())
                .status(v.getStatus().name())
                .fullNameOnId(v.getFullNameOnId())
                .dateOfBirth(v.getDateOfBirth())
                .idNumberMasked(cryptoService.maskIdNumber(v.getIdNumberLast4()))
                .issueDate(v.getIssueDate())
                .issuePlace(v.getIssuePlace())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .rejectReasonCode(v.getRejectReasonCode() != null ? v.getRejectReasonCode().name() : null)
                .rejectReasonNote(v.getRejectReasonNote())
                .attemptCount(v.getAttemptCount())
                .remainingAttempts(remaining)
                .hasFrontImage(v.getFrontImageKey() != null && !"PURGED".equals(v.getFrontImageKey()))
                .hasBackImage(v.getBackImageKey() != null && !"PURGED".equals(v.getBackImageKey()))
                .canResubmit(canResubmit)
                .build();
    }

    public LandlordKycStatusResponse submitKyc(
            String landlordId, MultipartFile frontFile, MultipartFile backFile,
            String fullNameOnId, String idNumber, LocalDate dateOfBirth,
            LocalDate issueDate, String issuePlace, boolean consent) {

        if (!consent) {
            throw new IdentityDomainException("Ban phai dong y voi dieu khoan xu ly du lieu ca nhan", "ERR_KYC_CONSENT_REQUIRED");
        }
        if (idNumber == null || !idNumber.matches("^[0-9]{12}$")) {
            throw new IdentityDomainException("So Can cuoc cong dan phai dung 12 chu so", "ERR_KYC_INVALID_ID_NUMBER");
        }
        if (issueDate != null && issueDate.isAfter(LocalDate.now())) {
            throw new IdentityDomainException("Ngay cap CCCD khong the o tuong lai", "ERR_KYC_INVALID_ISSUE_DATE");
        }

        Optional<UserVerification> existingOpt = verificationRepository.findByUserId(landlordId);
        if (existingOpt.isPresent()) {
            KycStatus currentStatus = existingOpt.get().getStatus();
            if (currentStatus == KycStatus.PENDING || currentStatus == KycStatus.APPROVED) {
                throw new IdentityDomainException("Ho so dang cho duyet hoac da duoc xac minh", "ERR_KYC_ALREADY_SUBMITTED");
            }
        }

        String idHash = cryptoService.hashIdNumber(idNumber);
        if (verificationRepository.existsByActiveIdHashAndUserIdNot(idHash, landlordId)) {
            throw new IdentityDomainException("So Can cuoc cong dan nay da duoc su dung boi tai khoan khac", "ERR_KYC_ID_NUMBER_DUPLICATE");
        }

        String frontKey = processAndStorePrivateImage(landlordId, frontFile, "front");
        String backKey = processAndStorePrivateImage(landlordId, backFile, "back");

        String idEnc = cryptoService.encryptIdNumber(idNumber);
        String last4 = cryptoService.extractLast4(idNumber);

        UserVerification verification;
        String fromStatus;
        if (existingOpt.isPresent()) {
            verification = existingOpt.get();
            fromStatus = verification.getStatus().name();
            verification.resubmit(fullNameOnId, dateOfBirth, idEnc, idHash, last4, issueDate, issuePlace, frontKey, backKey, "v1.0");
        } else {
            verification = UserVerification.createInitialSubmission(
                    landlordId, fullNameOnId, dateOfBirth, idEnc, idHash, last4, issueDate, issuePlace, frontKey, backKey, "v1.0"
            );
            fromStatus = KycStatus.NONE.name();
        }

        UserVerification saved = verificationRepository.save(verification);

        // Audit Trail & Outbox
        historyRepository.save(KycStatusHistory.record(
                saved.getId(), fromStatus, KycStatus.PENDING.name(), landlordId, "Chu tro nop ho so xac minh danh tinh"
        ));

        String outboxPayload = "{\"verificationId\":\"" + saved.getId() + "\",\"userId\":\"" + landlordId + "\"}";
        outboxRepository.save(KycEventOutbox.create(saved.getId(), "KycSubmitted", outboxPayload));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public byte[] streamKycImage(String landlordId, String side) {
        UserVerification v = verificationRepository.findByUserId(landlordId)
                .orElseThrow(() -> new IdentityDomainException("Khong tim thay ho so KYC", "ERR_KYC_NOT_FOUND"));

        String imageKey = "back".equalsIgnoreCase(side) ? v.getBackImageKey() : v.getFrontImageKey();
        if (imageKey == null || imageKey.isBlank() || "PURGED".equals(imageKey)) {
            throw new IdentityDomainException("Khong tim thay tep hinh anh", "ERR_KYC_IMAGE_NOT_FOUND");
        }

        byte[] bytes = kycStorage.load(imageKey);
        if (bytes == null || bytes.length == 0) {
            throw new IdentityDomainException("Khong the tai tep hinh anh", "ERR_KYC_IMAGE_NOT_FOUND");
        }
        return bytes;
    }

    private String processAndStorePrivateImage(String landlordId, MultipartFile file, String side) {
        if (file == null || file.isEmpty()) {
            throw new IdentityDomainException("Vui long tai len day du anh mat truoc va mat sau", "ERR_KYC_IMAGE_REQUIRED");
        }
        if (file.getSize() > ImageProcessingHelper.MAX_FILE_SIZE_5MB) {
            throw new IdentityDomainException("Dung luong anh CCCD vuot qua 5MB", "ERR_KYC_IMAGE_TOO_LARGE");
        }

        try {
            byte[] rawBytes = file.getBytes();
            if (!ImageProcessingHelper.isValidImageMagicBytes(rawBytes)) {
                throw new IdentityDomainException("Dinh dang anh khong hop le (chi chap nhan JPG, PNG, WebP)", "ERR_KYC_IMAGE_INVALID_FORMAT");
            }

            BufferedImage sanitized = ImageProcessingHelper.decodeAndSanitize(rawBytes);
            byte[] webpBytes = ImageProcessingHelper.resizeBoundingBoxWebP(sanitized, 2000, 0.9f);

            String fileUuid = UUID.randomUUID().toString();
            String storageKey = "kyc/private/" + landlordId + "_" + fileUuid + "_" + side + ".webp";
            kycStorage.store(storageKey, webpBytes, "image/webp");

            return storageKey;
        } catch (IOException e) {
            log.error("Failed to process private KYC document for landlord {}", landlordId, e);
            throw new IdentityDomainException("Loi xu ly tep hinh anh xac minh", "ERR_KYC_IMAGE_PROCESS_FAILED");
        }
    }
}
