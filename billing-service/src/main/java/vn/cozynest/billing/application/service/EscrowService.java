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
    private final String fintechMode;
    private final boolean liveCustodyEnabled;

    public EscrowService(LedgerService ledgerService) {
        this(ledgerService, null, Optional.empty(), "SANDBOX_ONLY", false);
    }

    public EscrowService(LedgerService ledgerService,
                         EscrowHoldRepository escrowHoldRepository,
                         String fintechMode,
                         boolean liveCustodyEnabled) {
        this(ledgerService, escrowHoldRepository, Optional.empty(), fintechMode, liveCustodyEnabled);
    }

    @Autowired
    public EscrowService(LedgerService ledgerService,
                         EscrowHoldRepository escrowHoldRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.escrow.live-custody-enabled:false}") boolean liveCustodyEnabled) {
        this.ledgerService = ledgerService;
        this.escrowHoldRepository = escrowHoldRepository;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
        this.fintechMode = fintechMode;
        this.liveCustodyEnabled = liveCustodyEnabled;
    }

    @Transactional
    public EscrowHold openHold(String contractId, String tenantId, String landlordId, long amount, long inspectionHours) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository == null) {
            return EscrowHold.createHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours);
        }
        return escrowHoldRepository.findByContractId(contractId)
                .orElseGet(() -> escrowHoldRepository.save(
                        EscrowHold.createHold(contractId, tenantId, landlordId, amount, !liveCustodyEnabled, inspectionHours)));
    }

    @Transactional(readOnly = true)
    public EscrowHold getHoldForParticipant(String contractId, String actorId, boolean isAdmin) {
        EscrowHold hold = requireHold(contractId);
        if (!isAdmin && !hold.getTenantId().equals(actorId) && !hold.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Khong co quyen truy cap thong tin ky quy hop dong nay");
        }
        return hold;
    }

    @Transactional
    public EscrowHold confirmMoveIn(String contractId, String actorId, boolean isAdmin) {
        assertSandboxOrSignedOff();
        EscrowHold hold = requireHold(contractId);
        boolean isTenant = hold.getTenantId().equals(actorId);
        boolean isLandlord = hold.getLandlordId().equals(actorId);
        if (!isAdmin && !isTenant && !isLandlord) {
            throw new AccessDeniedException("Chi nguoi thue hoac chu tro cua hop dong moi duoc xac nhan nhan phong");
        }
        hold.confirmMoveIn(isTenant || isAdmin, isLandlord || isAdmin);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_MOVE_IN_CONFIRMED", saved);
        return saved;
    }

    @Transactional
    public EscrowHold freezeForDispute(String contractId, String actorId, boolean isAdmin, String reason) {
        EscrowHold hold = requireHold(contractId);
        if (!isAdmin && !hold.getTenantId().equals(actorId) && !hold.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Khong co quyen dong bang ky quy hop dong nay");
        }
        hold.freezeForDispute(reason);
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DISPUTE_FROZEN", saved);
        return saved;
    }

    @Transactional
    public EscrowHold freezeForDispute(String contractId, String reason) {
        return freezeForDispute(contractId, "SYSTEM_ADMIN", true, reason);
    }

    @Transactional
    public EscrowHold unfreezeDispute(String contractId) {
        EscrowHold hold = requireHold(contractId);
        hold.unfreezeDispute();
        EscrowHold saved = escrowHoldRepository.save(hold);
        emitOutbox("ESCROW_DISPUTE_UNFROZEN", saved);
        return saved;
    }

    @Transactional
    public void disburseDeposit(String contractId, String landlordId, long amount, String actorId) {
        assertSandboxOrSignedOff();
        String targetLandlord = landlordId;
        long targetAmount = amount;
        if (escrowHoldRepository != null) {
            Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
            if (opt.isPresent()) {
                EscrowHold hold = opt.get();
                hold.release(true);
                escrowHoldRepository.save(hold);
                targetLandlord = hold.getLandlordId();
                targetAmount = hold.getAmount();
                emitOutbox("ESCROW_RELEASED", hold);
            }
        }
        ledgerService.getOrCreateWallet(targetLandlord, "Chủ trọ");
        ledgerService.postEntryIdempotent("ESC-REL-" + contractId,
                "Giải ngân tiền cọc hợp đồng " + contractId + " cho chủ trọ",
                "ESCROW_DISBURSE", contractId, actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, targetAmount),
                        new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(targetLandlord), PostingDirection.CREDIT, targetAmount)));
    }

    @Transactional
    public void refundDeposit(String contractId, String tenantId, long amount, String reason, String actorId) {
        assertSandboxOrSignedOff();
        long targetAmount = amount;
        if (escrowHoldRepository != null) {
            Optional<EscrowHold> opt = escrowHoldRepository.findByContractId(contractId);
            if (opt.isPresent()) {
                EscrowHold hold = opt.get();
                hold.refund(reason);
                escrowHoldRepository.save(hold);
                targetAmount = hold.getAmount();
                emitOutbox("ESCROW_REFUNDED", hold);
            }
        }
        ledgerService.postEntryIdempotent("ESC-REF-" + contractId,
                "Hoàn tiền cọc hợp đồng " + contractId + " cho người thuê: " + reason,
                "ESCROW_REFUND", contractId, actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, targetAmount),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, targetAmount)));
    }

    private void emitOutbox(String eventType, EscrowHold hold) {
        if (outboxRepository == null) {
            return;
        }
        String payload = String.format(
                "{\"contractId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"status\":\"%s\",\"amount\":%d,\"message\":\"Trang thai ky quy %s: %s\"}",
                hold.getContractId(), hold.getTenantId(), hold.getLandlordId(),
                hold.getStatus(), hold.getAmount(), hold.getContractId(), hold.getStatus()
        );
        outboxRepository.save(BillingEventOutbox.create(eventType, "ESCROW", hold.getId(), payload));
    }

    @Scheduled(cron = "${fintech.escrow.auto-release-cron:0 15 * * * ?}")
    @Transactional
    public int autoReleaseMaturedHolds() {
        if (escrowHoldRepository == null) return 0;
        int released = 0;
        for (EscrowHold hold : escrowHoldRepository.findReleasableBefore(Instant.now())) {
            hold.release(false);
            escrowHoldRepository.save(hold);
            ledgerService.getOrCreateWallet(hold.getLandlordId(), "Chủ trọ");
            ledgerService.postEntryIdempotent("ESC-AUTO-" + hold.getContractId(),
                    "Tự động giải ngân cọc sau thời gian kiểm tra " + hold.getContractId(),
                    "ESCROW_DISBURSE", hold.getContractId(), "SYSTEM_ESCROW",
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, hold.getAmount()),
                            new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(hold.getLandlordId()), PostingDirection.CREDIT, hold.getAmount())));
            released++;
        }
        return released;
    }

    private EscrowHold requireHold(String contractId) {
        if (escrowHoldRepository == null) {
            throw new IllegalStateException("Escrow repository unavailable");
        }
        return escrowHoldRepository.findByContractId(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản ký quỹ cho hợp đồng: " + contractId));
    }

    private void assertSandboxOrSignedOff() {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveCustodyEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live escrow custody is disabled until legal & licensed partner sign-off.");
        }
    }
}
