package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.PostingDirection;

import java.util.List;

@Service
public class EscrowService {
    private final LedgerService ledgerService;

    public EscrowService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Transactional
    public void disburseDeposit(String contractId, String landlordId, long amount, String actorId) {
        // Move deposit from Escrow account to Landlord's available wallet balance
        ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");

        ledgerService.postEntry(
                "Giải ngân tiền cọc hợp đồng " + contractId + " cho chủ trọ",
                "ESCROW_DISBURSE",
                contractId,
                actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(landlordId), PostingDirection.CREDIT, amount)
                )
        );
    }

    @Transactional
    public void refundDeposit(String contractId, String tenantId, long amount, String reason, String actorId) {
        // Refund deposit from Escrow account back to Gateway payout
        ledgerService.postEntry(
                "Hoàn tiền cọc hợp đồng " + contractId + " cho người thuê: " + reason,
                "ESCROW_REFUND",
                contractId,
                actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, amount)
                )
        );
    }
}

