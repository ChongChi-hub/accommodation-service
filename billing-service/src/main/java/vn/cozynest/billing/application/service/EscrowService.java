package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.EscrowHoldRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EscrowService {
    private final LedgerService ledgerService;
    private final EscrowHoldRepository escrowHoldRepository;
    private final BillingEventOutboxRepository outboxRepository;
    private final BankAccountService bankAccountService;
    private final String fintechMode;
    private final boolean liveCustodyEnabled;
    private final int depositReleasePercent;
    private final int withdrawalHoldDays;

    public EscrowService(LedgerService ledgerService) { this(ledgerService, null, Optional.empty(), Optional.empty(), "SANDBOX_ONLY", false, 50, 0); }
    public EscrowService(LedgerService ledgerService, EscrowHoldRepository escrowHoldRepository, String fintechMode, boolean liveCustodyEnabled) {
        this(ledgerService, escrowHoldRepository, Optional.empty(), Optional.empty(), fintechMode, liveCustodyEnabled, 50, 0);
    }
    public EscrowService(LedgerService ledgerService, EscrowHoldRepository escrowHoldRepository, Optional<BillingEventOutboxRepository> outboxRepository, String fintechMode, boolean liveCustodyEnabled) {
        this(ledgerService, escrowHoldRepository, outboxRepository, Optional.empty(), fintechMode, liveCustodyEnabled, 50, 0);
    }
    @Autowired
    public EscrowService(LedgerService ledgerService, EscrowHoldRepository escrowHoldRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository, Optional<BankAccountService> bankAccountService,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.escrow.live-custody-enabled:false}") boolean liveCustodyEnabled,
                         @Value("${fintech.deposit.release-percent:${deposit.release.percent:50}}") int depositReleasePercent,
                         @Value("${fintech.withdrawal.hold-days:${withdrawal.hold.days:0}}") int withdrawalHoldDays) {
        this.ledgerService = ledgerService;
        this.escrowHoldRepository = escrowHoldRepository;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
        this.bankAccountService = bankAccountService != null ? bankAccountService.orElse(null) : null;
        this.fintechMode = fintechMode;
        this.liveCustodyEnabled = liveCustodyEnabled;
        this.depositReleasePercent = Math.max(0, Math.min(100, depositReleasePercent));
        this.withdrawalHoldDays = Math.max(0, withdrawalHoldDays);
    }

    @Transactional public EscrowHold openHold(String contractId, String tenantId, String landlordId, long amount, long inspectionHours) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository == null) return EscrowHold.createPendingHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours);
        return escrowHoldRepository.findByContractId(contractId)
                .orElseGet(() -> escrowHoldRepository.save(EscrowHold.createPendingHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours)));
    }
    @Transactional public EscrowHold recordDepositPaid(String contractId, String tenantId, String landlordId, long amount, long inspectionHours) {
        return recordDepositPaidWithProof(contractId, tenantId, landlordId, amount, inspectionHours, null, null, null, null);
    }
    @Transactional(rollbackFor = Exception.class)
    public EscrowHold recordDepositPaidWithProof(String contractId, String tenantId, String landlordId, long amount,
                                                 long inspectionHours, String paymentCode, String bankTxCode, String receiptHash, String receiptUrl) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository == null) return EscrowHold.createHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours);
        if (bankTxCode != null && !bankTxCode.isBlank() && escrowHoldRepository.existsByBankTxCode(bankTxCode)) {
            throw new IllegalStateException("Phát hiện trùng lặp: Mã giao dịch ngân hàng [" + bankTxCode + "] đã được sử dụng cho một giao dịch trước đó! Không được dùng lại mã giao dịch cũ.");
        }
        if (receiptHash != null && !receiptHash.isBlank() && escrowHoldRepository.existsByReceiptHash(receiptHash)) {
            throw new IllegalStateException("Phát hiện trùng lặp biên lai: Ảnh biên lai chuyển khoản này đã được sử dụng cho một giao dịch trước đó! Vui lòng tải lên ảnh biên lai mới của lần chuyển khoản này.");
        }
        EscrowHold hold = escrowHoldRepository.findByContractId(contractId)
                .map(existing -> { existing.activateFromDeposit(amount, inspectionHours); return existing; })
                .orElseGet(() -> EscrowHold.createHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours));
        if (paymentCode != null || bankTxCode != null || receiptHash != null || receiptUrl != null) hold.attachPaymentProof(paymentCode, bankTxCode, receiptHash, receiptUrl);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DEPOSIT_PAID", saved);
        return saved;
    }
    @Transactional(rollbackFor = Exception.class) public EscrowHold rejectDepositProof(String contractId, String actorId, boolean isAdmin, String reason) {
        EscrowHold hold = requireHold(contractId);
        if (!isAdmin && !hold.getLandlordId().equals(actorId)) throw new AccessDeniedException("Chỉ chủ trọ hoặc Admin mới có quyền từ chối biên lai chuyển khoản không hợp lệ");
        hold.rejectInvalidProof(reason != null && !reason.isBlank() ? reason : "Biên lai chuyển khoản hoặc mã giao dịch không hợp lệ");
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_PROOF_REJECTED", saved);
        return saved;
    }
    @Transactional public void compensateHold(String contractId, String reason, String actorId) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository == null) return;
        Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
        if (opt.isEmpty()) return;
        EscrowHold hold = opt.get();
        if ("AWAITING_DEPOSIT".equals(hold.getStatus())) { hold.cancelPending(reason != null ? reason : "Saga compensation truoc khi dat coc"); escrowHoldRepository.save(hold); emitOutbox("ESCROW_CANCELLED", hold); return; }
        if ("HELD".equals(hold.getStatus()) || "PARTIALLY_RELEASED".equals(hold.getStatus()) || "DISPUTE_FROZEN".equals(hold.getStatus())) {
            refundDeposit(contractId, hold.getTenantId(), hold.getEffectiveRetainedAmount(), reason != null ? reason : "Saga compensation hoan coc", actorId);
        }
    }
    @Transactional(readOnly = true) public Optional<EscrowHold> findHoldForParticipant(String contractId, String actorId, boolean isAdmin) {
        if (escrowHoldRepository == null) return Optional.empty();
        Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
        if (opt.isPresent() && !isAdmin && !opt.get().getTenantId().equals(actorId) && !opt.get().getLandlordId().equals(actorId)) throw new AccessDeniedException("Khong co quyen truy cap thong tin ky quy hop dong nay");
        return opt;
    }

    @Transactional(readOnly = true) public List<EscrowHold> getHoldsForTenant(String tenantId) { return escrowHoldRepository == null ? List.of() : escrowHoldRepository.findByTenantId(tenantId); }
    @Transactional(readOnly = true) public List<EscrowHold> getHoldsForLandlord(String landlordId) { return escrowHoldRepository == null ? List.of() : escrowHoldRepository.findByLandlordId(landlordId); }
    @Transactional(readOnly = true) public List<EscrowHold> getAllHolds() { return escrowHoldRepository == null ? List.of() : escrowHoldRepository.findAll(); }
    @Transactional(readOnly = true) public EscrowHold getHoldForParticipant(String contractId, String actorId, boolean isAdmin) {
        EscrowHold hold = requireHold(contractId);
        if (!isAdmin && !hold.getTenantId().equals(actorId) && !hold.getLandlordId().equals(actorId)) throw new AccessDeniedException("Khong co quyen truy cap thong tin ky quy hop dong nay");
        return hold;
    }

    @Transactional(rollbackFor = Exception.class)
    public EscrowHold confirmMoveIn(String contractId, String actorId, boolean isAdmin) {
        assertSandboxOrSignedOff();
        EscrowHold hold = requireHold(contractId);
        boolean isTenant = hold.getTenantId().equals(actorId);
        boolean isLandlord = hold.getLandlordId().equals(actorId);
        if (!isAdmin && !isTenant && !isLandlord) throw new AccessDeniedException("Chi nguoi thue hoac chu tro cua hop dong moi duoc xac nhan nhan phong");
        if (!isAdmin && bankAccountService != null) bankAccountService.requireValidBankAccount(actorId, isLandlord ? "Chủ trọ" : "Khách thuê");
        boolean wasAlreadyConfirmed = hold.isBothConfirmedMoveIn() && hold.getReleasedAmount() > 0;
        hold.confirmMoveIn(isTenant || isAdmin, isLandlord || isAdmin);
        if (hold.isBothConfirmedMoveIn() && !wasAlreadyConfirmed && ledgerService != null) {
            long releasedPart = hold.applyPartialReleaseOnMoveIn(depositReleasePercent, withdrawalHoldDays);
            EscrowLedgerHelper.postPartialRelease(ledgerService, hold, releasedPart, depositReleasePercent, withdrawalHoldDays, actorId);
        }
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_MOVE_IN_CONFIRMED", saved);
        return saved;
    }

    @Scheduled(cron = "${fintech.escrow.auto-release-cron:0 15 * * * ?}")
    @Transactional public int autoReleaseMaturedHolds() { return settleMaturedPendingReleases(Instant.now()); }

    @Transactional
    public int settleMaturedPendingReleases(Instant asOf) {
        if (escrowHoldRepository == null || ledgerService == null) return 0;
        int settled = 0;
        for (EscrowHold hold : escrowHoldRepository.findAll()) {
            if (hold.getReleasedAmount() > 0 && !hold.isPendingSettled() && hold.getPendingReleaseUntil() != null && !hold.getPendingReleaseUntil().isAfter(asOf)) {
                EscrowLedgerHelper.postPendingSettlement(ledgerService, hold);
                hold.markPendingSettled();
                escrowHoldRepository.save(hold);
                settled++;
            }
        }
        return settled;
    }

    @Transactional
    public EscrowHold submitDeductionClaim(String contractId, String landlordId, long claimAmount, String reason, String proofUrl) {
        EscrowHold hold = requireHold(contractId);
        if (!hold.getLandlordId().equals(landlordId)) throw new AccessDeniedException("Chỉ chủ trọ của hợp đồng mới được gửi yêu cầu khấu trừ cọc");
        hold.submitDeductionClaim(claimAmount, reason, proofUrl);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DEDUCTION_CLAIMED", saved);
        return saved;
    }

    @Transactional
    public EscrowHold decideDeductionClaim(String contractId, String adminId, boolean approved, String decisionNote) {
        EscrowHold hold = requireHold(contractId);
        long deducted = hold.decideDeductionClaim(approved, decisionNote);
        if (approved) EscrowLedgerHelper.postDeductionToLandlord(ledgerService, hold, deducted, adminId, decisionNote);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DEDUCTION_DECIDED", saved);
        return saved;
    }

    @Transactional public EscrowHold freezeForDispute(String contractId, String actorId, boolean isAdmin, String reason) {
        EscrowHold hold = requireHold(contractId);
        if (!isAdmin && !hold.getTenantId().equals(actorId) && !hold.getLandlordId().equals(actorId)) throw new AccessDeniedException("Khong co quyen dong bang ky quy hop dong nay");
        hold.freezeForDispute(reason);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DISPUTE_FROZEN", saved);
        return saved;
    }
    @Transactional public EscrowHold freezeForDispute(String contractId, String reason) { return freezeForDispute(contractId, "SYSTEM_ADMIN", true, reason); }
    @Transactional public EscrowHold unfreezeDispute(String contractId) {
        EscrowHold hold = requireHold(contractId);
        hold.unfreezeDispute();
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DISPUTE_UNFROZEN", saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void disburseDeposit(String contractId, String landlordId, long amount, String actorId) {
        assertSandboxOrSignedOff();
        String targetLandlord = landlordId;
        long targetAmount = amount;
        if (escrowHoldRepository != null) {
            Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
            if (opt.isPresent()) {
                EscrowHold hold = opt.get();
                targetLandlord = hold.getLandlordId();
                targetAmount = hold.getEffectiveRetainedAmount() > 0 ? hold.getEffectiveRetainedAmount() : hold.getAmount();
                if (bankAccountService != null) bankAccountService.requireValidBankAccount(targetLandlord, "Chủ trọ");
                hold.release(true);
                escrowHoldRepository.save(hold);
                emitOutbox("ESCROW_RELEASED", hold);
            }
        }
        ledgerService.getOrCreateWallet(targetLandlord, "Chủ trọ");
        ledgerService.postEntryIdempotent("ESC-REL-" + contractId, "Giải ngân tiền cọc hợp đồng " + contractId + " cho chủ trọ", "ESCROW_DISBURSE", contractId, actorId,
                List.of(new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, targetAmount),
                        new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(targetLandlord), PostingDirection.CREDIT, targetAmount)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundDeposit(String contractId, String tenantId, long amount, String reason, String actorId) {
        assertSandboxOrSignedOff();
        long targetAmount = amount;
        if (escrowHoldRepository != null) {
            Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
            if (opt.isPresent()) {
                EscrowHold hold = opt.get();
                if (bankAccountService != null) bankAccountService.requireValidBankAccount(hold.getTenantId(), "Khách thuê");
                long retained = hold.getEffectiveRetainedAmount();
                targetAmount = retained > 0 ? retained : hold.getAmount();
                hold.refund(reason);
                escrowHoldRepository.save(hold);
                emitOutbox("ESCROW_REFUNDED", hold);
            }
        }
        ledgerService.postEntryIdempotent("ESC-REF-" + contractId, "Hoàn tiền cọc hợp đồng " + contractId + " cho người thuê: " + reason, "ESCROW_REFUND", contractId, actorId,
                List.of(new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, targetAmount),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, targetAmount)));
    }

    private void emitOutbox(String eventType, EscrowHold hold) {
        if (outboxRepository == null) return;
        String payload = String.format("{\"contractId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"status\":\"%s\",\"amount\":%d,\"releasedAmount\":%d,\"retainedAmount\":%d}",
                hold.getContractId(), hold.getTenantId(), hold.getLandlordId(), hold.getStatus(), hold.getAmount(), hold.getReleasedAmount(), hold.getRetainedAmount());
        outboxRepository.save(BillingEventOutbox.create(eventType, "ESCROW", hold.getId(), payload));
    }

    private EscrowHold requireHold(String contractId) {
        if (escrowHoldRepository == null) throw new IllegalStateException("Escrow repository unavailable");
        return escrowHoldRepository.findByContractId(contractId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản ký quỹ cho hợp đồng: " + contractId));
    }
    private void assertSandboxOrSignedOff() {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveCustodyEnabled) throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live escrow custody is disabled until legal & licensed partner sign-off.");
    }
}
