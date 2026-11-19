package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;

import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentProofStorageService {
    private static final int DHASH_DUPLICATE_THRESHOLD = 10;

    private final PaymentProofRegistryRepository proofRepository;
    private final BankTransactionRegistryRepository bankTxRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final BillingContractPort contractPort;
    private final Path storageDir;
    private final SecretKeySpec aesKey;

    @Autowired
    public PaymentProofStorageService(
            PaymentProofRegistryRepository proofRepository,
            BankTransactionRegistryRepository bankTxRepository,
            @Autowired(required = false) EscrowHoldRepository escrowHoldRepository,
            @Autowired(required = false) PaymentIntentRepository paymentIntentRepository,
            @Autowired(required = false) BillingContractPort contractPort,
            @Value("${fintech.proof.storage-dir:./storage/private-proofs}") String storagePath,
            @Value("${fintech.proof.encryption-key:cozynest-private-proof-aes256-key-2026}") String secretKeyRaw) {
        this.proofRepository = proofRepository;
        this.bankTxRepository = bankTxRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.contractPort = contractPort;
        Path requestedDir = Paths.get(storagePath != null && !storagePath.isBlank() ? storagePath : "./storage/private-proofs");
        Path resolvedDir = requestedDir;
        try {
            Files.createDirectories(requestedDir);
        } catch (Exception e) {
            resolvedDir = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"), "cozynest-private-proofs");
            try {
                Files.createDirectories(resolvedDir);
            } catch (Exception ignored) {
            }
        }
        this.storageDir = resolvedDir;
        this.aesKey = ProofImageCipherHelper.deriveAesKey(
                secretKeyRaw != null && !secretKeyRaw.isBlank() ? secretKeyRaw : "cozynest-private-proof-aes256-key-2026");
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentProofRecord storeSupportingProof(String paymentCode, String orderType, String orderId,
                                                   String uploaderId, String rawBankTxCode,
                                                   String clientSha256Hint, String receiptPayload) {
        String normTx = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(rawBankTxCode);
        if (normTx.length() < 6 || normTx.length() > 36) {
            throw new IllegalArgumentException("Mã giao dịch ngân hàng không hợp lệ (cần 6-36 ký tự chữ/số)");
        }
        if (proofRepository.existsByNormalizedBankTxCode(normTx)
                || (bankTxRepository != null && bankTxRepository.existsByNormalizedTxCode(normTx))
                || (escrowHoldRepository != null && escrowHoldRepository.existsByBankTxCode(normTx))) {
            throw new IllegalStateException("Phát hiện trùng lặp: Mã giao dịch ngân hàng [" + normTx + "] đã được sử dụng trước đó!");
        }

        byte[] rawImageBytes = ProofImageCipherHelper.decodeReceiptBytes(receiptPayload);
        String detectedMime = ProofImageCipherHelper.verifyMagicBytes(rawImageBytes);
        String computedSha256 = "sha256-" + ProofImageCipherHelper.sha256Hex(rawImageBytes);
        String effectiveSha256 = (clientSha256Hint != null && clientSha256Hint.startsWith("sha256-") && clientSha256Hint.length() >= 20)
                ? clientSha256Hint.trim().toLowerCase() : computedSha256;

        if (proofRepository.existsBySha256Hash(effectiveSha256)
                || proofRepository.existsBySha256Hash(computedSha256)
                || (escrowHoldRepository != null && (escrowHoldRepository.existsByReceiptHash(effectiveSha256)
                || escrowHoldRepository.existsByReceiptHash(computedSha256)))) {
            throw new IllegalStateException("Phát hiện trùng lặp biên lai (SHA-256): Ảnh biên lai chuyển khoản này đã được sử dụng trước đó!");
        }

        BufferedImage decoded = ProofImageCipherHelper.readAndValidateImage(rawImageBytes);
        String dhash = ProofImageCipherHelper.computeDhash64(decoded);

        String duplicateOfId = null;
        for (PaymentProofRecord existing : proofRepository.findAll()) {
            int dist = PaymentProofRecord.computeDhashHammingDistance(dhash, existing.getDhash());
            if (dist <= DHASH_DUPLICATE_THRESHOLD) {
                duplicateOfId = existing.getId();
                break;
            }
        }
        if (duplicateOfId != null) {
            throw new IllegalStateException("Phát hiện ảnh biên lai chụp lại hoặc gần trùng (dHash=" + dhash
                    + " trùng khớp với biên lai #" + duplicateOfId + "): Hệ thống từ chối ảnh biên lai cũ chụp lại!");
        }

        byte[] cleanBytes = ProofImageCipherHelper.reencodeWithoutMetadata(decoded);
        String proofId = UUID.randomUUID().toString();
        String storageKey = proofId + ".enc";
        ProofImageCipherHelper.writeEncryptedFile(storageDir, storageKey, aesKey, cleanBytes);

        PaymentProofRecord record = new PaymentProofRecord(
                proofId, paymentCode, orderType, orderId, uploaderId,
                normTx, effectiveSha256, dhash, false, null,
                detectedMime, storageKey, Instant.now()
        );
        return proofRepository.save(record);
    }

    @Transactional
    public StreamedProof loadAuthorizedProofImage(String paymentCode, String actorId,
                                                  boolean isAdminOrMod, String actorRole, String ipAddress) {
        PaymentProofRecord record = proofRepository.findLatestByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy bằng chứng thanh toán"));
        if (!isAdminOrMod && !isAuthorizedParticipant(record, actorId)) {
            throw new WebhookSecurityException(404, "Không tìm thấy bằng chứng thanh toán");
        }
        try {
            byte[] decrypted = ProofImageCipherHelper.readEncryptedFile(storageDir, record.getStorageKey(), aesKey);
            proofRepository.saveAccessLog(PaymentProofAccessLog.create(
                    record.getId(), paymentCode, actorId, actorRole, "STREAM_PROOF_IMAGE", ipAddress));
            return new StreamedProof(record, decrypted);
        } catch (IllegalArgumentException e) {
            throw new WebhookSecurityException(404, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<PaymentProofRecord> findAuthorizedMetadata(String paymentCode, String actorId, boolean isAdminOrMod) {
        Optional<PaymentProofRecord> opt = proofRepository.findLatestByPaymentCode(paymentCode);
        if (opt.isEmpty()) return Optional.empty();
        if (!isAdminOrMod && !isAuthorizedParticipant(opt.get(), actorId)) {
            throw new WebhookSecurityException(404, "Không tìm thấy bằng chứng thanh toán");
        }
        return opt;
    }

    private boolean isAuthorizedParticipant(PaymentProofRecord record, String actorId) {
        if (actorId == null || actorId.isBlank()) return false;
        if (actorId.equals(record.getUploaderId())) return true;
        if (paymentIntentRepository != null) {
            Optional<PaymentIntent> intent = paymentIntentRepository.findByPaymentCode(record.getPaymentCode());
            if (intent.isPresent() && actorId.equals(intent.get().getPayerId())) return true;
        }
        if (escrowHoldRepository != null) {
            Optional<EscrowHold> hold = escrowHoldRepository.findByContractId(record.getOrderId());
            if (hold.isPresent() && (actorId.equals(hold.get().getLandlordId()) || actorId.equals(hold.get().getTenantId()))) {
                return true;
            }
        }
        if (contractPort != null && "DEPOSIT".equalsIgnoreCase(record.getOrderType())) {
            var snap = contractPort.getContractSnapshot(record.getOrderId(), LocalDate.now());
            return snap.isPresent() && (actorId.equals(snap.get().landlordId()) || actorId.equals(snap.get().tenantId()));
        }
        return false;
    }

    public record StreamedProof(PaymentProofRecord metadata, byte[] imageBytes) {}
}
