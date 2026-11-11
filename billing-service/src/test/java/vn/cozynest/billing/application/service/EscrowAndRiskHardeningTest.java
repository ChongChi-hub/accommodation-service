package vn.cozynest.billing.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.model.RiskProfile;
import vn.cozynest.billing.domain.repository.EscrowHoldRepository;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.domain.repository.RiskProfileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EscrowAndRiskHardeningTest {

    @Test
    void enforcesLegal02LiveGateAndDisputeFreezeOnEscrow() {
        LedgerService ledger = mock(LedgerService.class);
        EscrowHoldRepository holdRepo = mock(EscrowHoldRepository.class);

        // LIVE mode with liveCustodyEnabled=false must block per LEGAL-02
        EscrowService blockedLiveService = new EscrowService(ledger, holdRepo, "LIVE", false);
        assertThatThrownBy(() -> blockedLiveService.openHold("cnt-1", "t-1", "l-1", 3_000_000L, 72))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEGAL-02");

        // Sandbox mode supports hold -> dispute freeze -> unfreeze -> inspection guard
        EscrowService sandboxService = new EscrowService(ledger, holdRepo, "SANDBOX_ONLY", false);
        EscrowHold hold = EscrowHold.createHold("cnt-1", "t-1", "l-1", 3_000_000L, true, 72);
        when(holdRepo.findByContractId("cnt-1")).thenReturn(Optional.of(hold));
        when(holdRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sandboxService.freezeForDispute("cnt-1", "Phong khong dung mo ta");
        assertThat(hold.getStatus()).isEqualTo("DISPUTE_FROZEN");
        assertThatThrownBy(() -> hold.release(true)).isInstanceOf(IllegalStateException.class);

        sandboxService.unfreezeDispute("cnt-1");
        assertThat(hold.getStatus()).isEqualTo("HELD");
        // Cannot auto-release before 72h inspection expires
        assertThatThrownBy(() -> hold.release(false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void enforces24hBankChangeLockBlacklistAndVelocityInRiskProfile() {
        RiskProfile profile = RiskProfile.createDefault("landlord-1");
        Instant now = Instant.now();

        profile.markBankChanged(now.minusSeconds(3600)); // 1 hour ago
        assertThatThrownBy(() -> profile.validateWithdrawal(1_000_000L, LocalDate.now(), now))
                .isInstanceOf(IllegalStateException.class);

        // Simulate past 25h ago + velocity cap (max 3/day)
        profile.markBankChanged(now.minusSeconds(25 * 3600L));
        LocalDate today = LocalDate.now();
        profile.recordWithdrawal(1_000_000L, today);
        profile.recordWithdrawal(1_000_000L, today);
        profile.recordWithdrawal(1_000_000L, today);
        assertThatThrownBy(() -> profile.validateWithdrawal(1_000_000L, today, now))
                .isInstanceOf(IllegalStateException.class);

        PayoutRequestRepository payoutRepo = mock(PayoutRequestRepository.class);
        RiskProfileRepository riskRepo = mock(RiskProfileRepository.class);
        PayoutService blockedLivePayout = new PayoutService(payoutRepo, riskRepo, mock(LedgerService.class), "LIVE", false);
        assertThatThrownBy(() -> blockedLivePayout.requestPayout("l-1",
                new vn.cozynest.billing.application.dto.PayoutDto.RequestPayoutCommand(500_000L, "MB", "123456", "Test", "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEGAL-02");
    }
}
