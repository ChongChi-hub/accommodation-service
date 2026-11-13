package vn.cozynest.booking.application.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.ContractSignatureAudit;
import vn.cozynest.booking.domain.repository.ContractSignatureAuditRepository;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.SignedDownloadTokenService;

@Component
public class ContractSigningCoordinator {

    public static final String CONTRACTS_BUCKET = "contracts";
    private static final int DOWNLOAD_TTL_SECONDS = 120;

    private final IdentityVerificationPort identityVerificationPort;
    private final ContractPdfService pdfService;
    private final PrivateObjectStoragePort storagePort;
    private final SignedDownloadTokenService tokenService;
    private final ContractSignatureAuditRepository auditRepository;

    public ContractSigningCoordinator(
            IdentityVerificationPort identityVerificationPort,
            ContractPdfService pdfService,
            PrivateObjectStoragePort storagePort,
            SignedDownloadTokenService tokenService,
            ContractSignatureAuditRepository auditRepository
    ) {
        this.identityVerificationPort = identityVerificationPort;
        this.pdfService = pdfService;
        this.storagePort = storagePort;
        this.tokenService = tokenService;
        this.auditRepository = auditRepository;
    }

    public void verifyAndSign(
            Contract contract,
            String userId,
            String otpCode,
            boolean isLandlord,
            String ipAddress,
            String userAgent
    ) {
        String expectedSigner = isLandlord ? contract.getLandlordId() : contract.getTenantId();
        if (!expectedSigner.equals(userId)) {
            throw new AccessDeniedException("Ban khong co quyen ky hop dong nay");
        }
        if ("CANCELLED".equals(contract.getStatus()) || "TERMINATED".equals(contract.getStatus())) {
            throw new IllegalStateException("Hop dong da huy hoac cham dut, khong the ky");
        }
        verifyOtpForUser(userId, otpCode);

        if (isLandlord) {
            contract.signByLandlord();
        } else {
            contract.signByTenant();
        }

        String hash = contract.computeCanonicalHash();
        String objectKey = null;
        if ("ACTIVE".equals(contract.getStatus())) {
            ContractPdfService.GeneratedPdf pdf = pdfService.generateSignedContractPdf(contract);
            objectKey = "contracts/" + contract.getId() + "/" + contract.getContractCode() + ".pdf";
            PrivateObjectStoragePort.StoredObjectMetadata meta = storagePort.storePrivate(
                    CONTRACTS_BUCKET, objectKey, pdf.bytes(), "application/pdf");
            contract.recordPdfStorage(objectKey, "/api/v1/booking/contracts/" + contract.getId() + "/pdf", meta.sha256Hex());
            hash = meta.sha256Hex();
        }

        auditRepository.save(ContractSignatureAudit.record(
                contract.getId(),
                userId,
                isLandlord ? "LANDLORD" : "TENANT",
                true,
                hash,
                objectKey,
                ipAddress,
                userAgent
        ));
    }

    /**
     * Verifies a 6-digit contract signing OTP against identity-service (fail-closed).
     */
    public void verifyOtpForUser(String userId, String otpCode) {
        if (!identityVerificationPort.verifyContractOtp(userId, otpCode)) {
            throw new IllegalArgumentException("Ma OTP ky hop dong khong hop le hoac da het han");
        }
    }

    public String createSignedPdfUrl(Contract contract, String actorId) {
        if (contract.getPdfObjectKey() == null || contract.getPdfObjectKey().isBlank()) {
            throw new IllegalStateException("Hop dong chua co ban PDF ky so hoan chinh");
        }
        String token = tokenService.createToken(CONTRACTS_BUCKET, contract.getPdfObjectKey(), actorId, DOWNLOAD_TTL_SECONDS);
        return "/api/v1/booking/contracts/" + contract.getId() + "/pdf/download?token=" + token;
    }

    public byte[] getOrGeneratePdf(Contract contract) {
        if (contract.getPdfObjectKey() != null && !contract.getPdfObjectKey().isBlank()) {
            byte[] content = storagePort.loadPrivate(CONTRACTS_BUCKET, contract.getPdfObjectKey());
            if (content != null) {
                return content;
            }
        }
        ContractPdfService.GeneratedPdf pdf = pdfService.generateContractPdf(contract);
        return pdf.bytes();
    }

    public byte[] downloadSignedPdfByToken(String contractId, String actorId, boolean isAdmin, String token) {
        SignedDownloadTokenService.VerifiedToken verified = tokenService.verify(token);
        if (!CONTRACTS_BUCKET.equals(verified.bucket())
                || !verified.objectKey().startsWith("contracts/" + contractId + "/")) {
            throw new AccessDeniedException("Token tai hop dong khong khop ma hop dong");
        }
        if (!isAdmin && verified.authorizedActorId() != null && !verified.authorizedActorId().equals(actorId)) {
            throw new AccessDeniedException("Token tai hop dong khong thuoc phien nguoi dung hien tai");
        }
        byte[] content = storagePort.loadPrivate(verified.bucket(), verified.objectKey());
        if (content == null) {
            throw new IllegalArgumentException("Khong tim thay tep PDF hop dong trong kho luu tru rieng");
        }
        return content;
    }
}

