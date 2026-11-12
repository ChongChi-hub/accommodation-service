package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.EscrowHoldRepository;

import java.time.Instant;
import java.util.List;

@Service
public class EscrowService {
    private final LedgerService ledgerService;
    private final EscrowHoldRepository escrowHoldRepository;
    private final String fintechMode;
    private final boolean liveCustodyEnabled;

    public EscrowService(LedgerService ledgerService) {
        this(ledgerService, null, "SANDBOX_ONLY", false);
    }

    @Autowired
    public EscrowService(LedgerService ledgerService,
                         EscrowHoldRepository escrowHoldRepository,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.escrow.live-custody-enabled:false}") boolean liveCustodyEnabled) {
        this.ledgerService = ledgerService;
        this.escrowHoldRepository = escrowHoldRepository;
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

    @Transactional
    public EscrowHold freezeForDispute(String contractId, String reason) {
        EscrowHold hold = requireHold(contractId);
        hold.freezeForDispute(reason);
        return escrowHoldRepository.save(hold);
    }

    @Transactional
    public EscrowHold unfreezeDispute(String contractId) {
        EscrowHold hold = requireHold(contractId);
        hold.unfreezeDispute();
        return escrowHoldRepository.save(hold);
    }

    @Transactional
    public void disburseDeposit(String contractId, String landlordId, long amount, String actorId) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository != null) {
            escrowHoldRepository.findByContractId(contractId).ifPresent(hold -> {
                hold.release(true);
                escrowHoldRepository.save(hold);
            });
        }
        ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");
        ledgerService.postEntryIdempotent("ESC-REL-" + contractId,
                "Giải ngân tiền cọc hợp đồng " + contractId + " cho chủ trọ",
                "ESCROW_DISBURSE", contractId, actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(landlordId), PostingDirection.CREDIT, amount)));
    }

    @Transactional
    public void refundDeposit(String contractId, String tenantId, long amount, String reason, String actorId) {
        assertSandboxOrSignedOff();
        if (escrowHoldRepository != null) {
            escrowHoldRepository.findByContractId(contractId).ifPresent(hold -> {
                hold.refund(reason);
                escrowHoldRepository.save(hold);
            });
        }
        ledgerService.postEntryIdempotent("ESC-REF-" + contractId,
                "Hoàn tiền cọc hợp đồng " + contractId + " cho người thuê: " + reason,
                "ESCROW_REFUND", contractId, actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, amount)));
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
