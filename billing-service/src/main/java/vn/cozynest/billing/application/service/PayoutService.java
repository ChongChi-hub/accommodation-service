package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.domain.model.PayoutRequest;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.model.RiskProfile;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.domain.repository.RiskProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PayoutService {
    private final PayoutRequestRepository payoutRepository;
    private final RiskProfileRepository riskProfileRepository;
    private final LedgerService ledgerService;

    public PayoutService(PayoutRequestRepository payoutRepository,
                         RiskProfileRepository riskProfileRepository,
                         LedgerService ledgerService) {
        this.payoutRepository = payoutRepository;
        this.riskProfileRepository = riskProfileRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public PayoutDto.PayoutResponse requestPayout(String landlordId, PayoutDto.RequestPayoutCommand command) {
        LocalDate today = LocalDate.now();

        // 1. Enforce FIN-RISK-01 Limits
        RiskProfile riskProfile = riskProfileRepository.findByUserId(landlordId)
                .orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(landlordId)));
        riskProfile.validateWithdrawal(command.amount(), today);

        // 2. Validate sufficient wallet balance in Ledger
        String walletAcc = ledgerService.getWalletAccountNumber(landlordId);
        long availableBalance = ledgerService.getAccountBalance(walletAcc);
        if (availableBalance < command.amount()) {
            throw new IllegalStateException("Số dư ví khả dụng không đủ (Hiện có: " + availableBalance + " VND)");
        }

        // 3. Post Ledger Entry: DEBIT wallet, CREDIT gateway cash
        ledgerService.postEntry(
                "Rút tiền về tài khoản ngân hàng " + command.bankCode() + " " + command.accountNumber(),
                "PAYOUT",
                landlordId,
                landlordId,
                List.of(
                        new LedgerDto.PostingItem(walletAcc, PostingDirection.DEBIT, command.amount()),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, command.amount())
                )
        );

        // 4. Record risk profile usage
        riskProfile.recordWithdrawal(command.amount(), today);
        riskProfileRepository.save(riskProfile);

        // 5. Save payout request
        String payoutCode = "PO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        PayoutRequest payout = PayoutRequest.create(
                payoutCode, landlordId, command.amount(), command.bankCode(),
                command.accountNumber(), command.accountHolder(), true
        );
        payout.complete();
        PayoutRequest saved = payoutRepository.save(payout);

        return new PayoutDto.PayoutResponse(
                saved.getId(), saved.getPayoutCode(), saved.getAmount(), saved.getBankCode(),
                saved.getAccountNumber(), saved.getAccountHolder(), saved.getStatus(), saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PayoutDto.PayoutResponse> getLandlordPayouts(String landlordId) {
        return payoutRepository.findByLandlordId(landlordId).stream()
                .map(p -> new PayoutDto.PayoutResponse(
                        p.getId(), p.getPayoutCode(), p.getAmount(), p.getBankCode(),
                        p.getAccountNumber(), p.getAccountHolder(), p.getStatus(), p.getCreatedAt()
                ))
                .toList();
    }
}

