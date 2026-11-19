package vn.cozynest.billing.application.service;

import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.domain.model.*;

import java.util.List;

final class PayoutLedgerHelper {
    private PayoutLedgerHelper() {
    }

    static void postHoldOnRequest(LedgerService ledger, String landlordId, String payoutCode,
                                  String bankCode, String maskedAccount, long amount) {
        String walletAcc = ledger.getLandlordAvailableAccountNumber(landlordId);
        ledger.getOrCreateWithdrawalHoldAccount(landlordId);
        String holdAcc = ledger.getWithdrawalHoldAccountNumber(landlordId);
        ledger.postEntryIdempotent("PAYOUT-HOLD-" + payoutCode,
                "Tạm giữ chờ rút tiền " + payoutCode + " về " + bankCode + " " + maskedAccount,
                "PAYOUT_HOLD", payoutCode, landlordId,
                List.of(new LedgerDto.PostingItem(walletAcc, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(holdAcc, PostingDirection.CREDIT, amount)));
    }

    static void postPaidOutflow(LedgerService ledger, PayoutRequest payout, String normTx, String adminId, String maskedAccount) {
        String holdAcc = ledger.getWithdrawalHoldAccountNumber(payout.getLandlordId());
        ledger.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Tài khoản tiền về cổng thanh toán", AccountType.ASSET, null);
        ledger.postEntryIdempotent("PAYOUT-PAID-" + payout.getPayoutCode(),
                "Admin xác nhận đã chuyển rút tiền " + payout.getPayoutCode() + " [" + normTx + "] về " + payout.getBankCode() + " " + maskedAccount,
                "PAYOUT", payout.getLandlordId(), adminId,
                List.of(new LedgerDto.PostingItem(holdAcc, PostingDirection.DEBIT, payout.getAmount()),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, payout.getAmount())));
    }

    static void postReversalToAvailable(LedgerService ledger, PayoutRequest payout, String idemKey, String desc, String actorId) {
        String holdAcc = ledger.getWithdrawalHoldAccountNumber(payout.getLandlordId());
        String walletAcc = ledger.getLandlordAvailableAccountNumber(payout.getLandlordId());
        ledger.postEntryIdempotent(idemKey, desc, "PAYOUT_REVERSAL", payout.getPayoutCode(), actorId,
                List.of(new LedgerDto.PostingItem(holdAcc, PostingDirection.DEBIT, payout.getAmount()),
                        new LedgerDto.PostingItem(walletAcc, PostingDirection.CREDIT, payout.getAmount())));
    }

    static PayoutDto.PayoutResponse toResponse(PayoutRequest p) {
        String masked = maskAccount(p.getAccountNumber());
        UserBankAccount sysBank = new UserBankAccount(null, p.getLandlordId(), p.getBankCode(), p.getBankCode(),
                p.getAccountNumber(), p.getAccountHolder(), null, "MATCHED", p.getCreatedAt(), p.getUpdatedAt());
        String systemQr = sysBank.buildSystemVietQrUrl(p.getAmount(), p.getPayoutCode());
        return new PayoutDto.PayoutResponse(p.getId(), p.getPayoutCode(), p.getAmount(), p.getBankCode(),
                masked, p.getAccountHolder(), p.getStatus(), p.getCreatedAt(),
                p.getLandlordId(), p.getFailureReason(), p.getBankTxCode(), p.getProofUrl(),
                systemQr, p.getApprovedBy(), p.getPaidBy(), p.getPaidAt());
    }

    static String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return accountNumber;
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
