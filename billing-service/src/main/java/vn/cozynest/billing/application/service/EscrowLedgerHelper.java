package vn.cozynest.billing.application.service;

import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.model.PostingDirection;

import java.util.List;

final class EscrowLedgerHelper {
    private EscrowLedgerHelper() {
    }

    static void postPartialRelease(LedgerService ledgerService, EscrowHold hold,
                                   long releasedPart, int releasePercent, int holdDays, String actorId) {
        if (ledgerService == null || releasedPart <= 0) return;
        String contractId = hold.getContractId();
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
        if (holdDays > 0) {
            ledgerService.getOrCreateLandlordPendingWallet(hold.getLandlordId());
            String pendingAcc = ledgerService.getLandlordPendingAccountNumber(hold.getLandlordId());
            ledgerService.postEntryIdempotent(
                    "ESC-PARTIAL-PEND-" + contractId,
                    "Giải phóng " + releasePercent + "% tiền cọc hợp đồng " + contractId + " vào ví chờ " + holdDays + " ngày",
                    "ESCROW_PARTIAL_PENDING", contractId, actorId,
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, releasedPart),
                            new LedgerDto.PostingItem(pendingAcc, PostingDirection.CREDIT, releasedPart)));
        } else {
            ledgerService.getOrCreateWallet(hold.getLandlordId(), "Chủ trọ");
            String availAcc = ledgerService.getLandlordAvailableAccountNumber(hold.getLandlordId());
            ledgerService.postEntryIdempotent(
                    "ESC-PARTIAL-AVAIL-" + contractId,
                    "Giải phóng " + releasePercent + "% tiền cọc hợp đồng " + contractId + " vào số dư khả dụng chủ trọ",
                    "ESCROW_PARTIAL_RELEASE", contractId, actorId,
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, releasedPart),
                            new LedgerDto.PostingItem(availAcc, PostingDirection.CREDIT, releasedPart)));
        }
    }

    static void postPendingSettlement(LedgerService ledgerService, EscrowHold hold) {
        if (ledgerService == null || hold.getReleasedAmount() <= 0) return;
        ledgerService.getOrCreateLandlordPendingWallet(hold.getLandlordId());
        ledgerService.getOrCreateWallet(hold.getLandlordId(), "Chủ trọ");
        String pendingAcc = ledgerService.getLandlordPendingAccountNumber(hold.getLandlordId());
        String availAcc = ledgerService.getLandlordAvailableAccountNumber(hold.getLandlordId());
        ledgerService.postEntryIdempotent(
                "ESC-PEND-SETTLE-" + hold.getContractId(),
                "Chuyển tiền cọc đã hết thời gian chờ sang số dư khả dụng hợp đồng " + hold.getContractId(),
                "ESCROW_PENDING_SETTLE", hold.getContractId(), "SYSTEM_ESCROW",
                List.of(
                        new LedgerDto.PostingItem(pendingAcc, PostingDirection.DEBIT, hold.getReleasedAmount()),
                        new LedgerDto.PostingItem(availAcc, PostingDirection.CREDIT, hold.getReleasedAmount())));
    }

    static void postDeductionToLandlord(LedgerService ledgerService, EscrowHold hold,
                                        long deducted, String adminId, String note) {
        if (ledgerService == null || deducted <= 0) return;
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
        ledgerService.getOrCreateWallet(hold.getLandlordId(), "Chủ trọ");
        ledgerService.postEntryIdempotent(
                "ESC-DEDUCT-" + hold.getContractId(),
                "Khấu trừ tiền cọc cuối hợp đồng " + hold.getContractId() + " cho chủ trọ: " + note,
                "ESCROW_DEDUCTION", hold.getContractId(), adminId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, deducted),
                        new LedgerDto.PostingItem(ledgerService.getLandlordAvailableAccountNumber(hold.getLandlordId()), PostingDirection.CREDIT, deducted)));
    }
}
