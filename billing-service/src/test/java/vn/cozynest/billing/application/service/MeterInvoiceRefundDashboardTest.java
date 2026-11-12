package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.billing.application.dto.*;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.domain.service.UtilityTariffCalculator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeterInvoiceRefundDashboardTest {

    @Test
    void shouldCalculateThreeTariffsAndLockReadingBoundToIssuedInvoice() {
        UtilityReadingRepository repo = mock(UtilityReadingRepository.class);
        UtilityReadingAuditRepository auditRepo = mock(UtilityReadingAuditRepository.class);
        InvoiceRepository invRepo = mock(InvoiceRepository.class);
        UtilityReadingService service = new UtilityReadingService(repo, auditRepo, invRepo, new ObjectMapper());

        // EVN tiered check for 120 kWh: 50*1806 + 50*1866 + 20*2167 = 90300 + 93300 + 43340 = 226940
        long evn = UtilityTariffCalculator.calculateElectricityCost(
                new BigDecimal("120"), UtilityTariffCalculator.PricingModel.EVN_TIERED, null, null, 1);
        assertThat(evn).isEqualTo(226_940L);

        long fixed = UtilityTariffCalculator.calculateElectricityCost(
                new BigDecimal("100"), UtilityTariffCalculator.PricingModel.FIXED, 3500L, null, 1);
        assertThat(fixed).isEqualTo(350_000L);

        long perPerson = UtilityTariffCalculator.calculateWaterCost(
                BigDecimal.TEN, UtilityTariffCalculator.PricingModel.PER_PERSON, null, 100_000L, 3);
        assertThat(perPerson).isEqualTo(300_000L);

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        UtilityReadingDto.ReadingResponse res = service.recordReading(
                new UtilityReadingDto.RecordReadingCommand("c1", "r1", LocalDate.now(),
                        BigDecimal.ZERO, new BigDecimal("250"), BigDecimal.ZERO, new BigDecimal("5"),
                        List.of(), UtilityTariffCalculator.PricingModel.FIXED, 3000L, 20000L, null, null, 1),
                "l1");
        assertThat(res.hasSurgeAnomaly()).isTrue();
        assertThat(res.electricityCost()).isEqualTo(750_000L);

        UtilityReading stored = new UtilityReading(res.id(), "c1", "r1", LocalDate.now(),
                BigDecimal.ZERO, new BigDecimal("250"), BigDecimal.ZERO, new BigDecimal("5"), "[]", "l1", null);
        when(repo.findById(res.id())).thenReturn(Optional.of(stored));
        when(invRepo.findByUtilityReadingId(res.id())).thenReturn(List.of(
                Invoice.create("INV-1", "c1", res.id(), "t1", "l1", LocalDate.now(), 3_000_000L, 750_000L, 100_000L, 0L, 0L, LocalDate.now().plusDays(5))
        ));

        assertThatThrownBy(() -> service.editReading(res.id(),
                new UtilityReadingDto.EditReadingCommand(BigDecimal.ZERO, new BigDecimal("100"),
                        BigDecimal.ZERO, new BigDecimal("5"), null, null, null, null, null, 1, "fix"), "l1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ISSUED/PAID");
    }

    @Test
    void shouldGenerateIdempotentInvoiceCreditNoteAndMarkOverdue() {
        InvoiceRepository invRepo = mock(InvoiceRepository.class);
        LedgerService ledger = mock(LedgerService.class);
        InvoiceService service = new InvoiceService(invRepo, ledger);

        LocalDate period = LocalDate.of(2026, 11, 1);
        InvoiceDto.GenerateInvoiceCommand cmd = new InvoiceDto.GenerateInvoiceCommand(
                "c1", "u1", "t1", "l1", period, 3_000_000L, 200_000L, 100_000L, 0L, 0L, LocalDate.now().minusDays(2));

        Invoice saved = Invoice.create("INV-202611-AAAAAA", "c1", "u1", "t1", "l1", period,
                3_000_000L, 200_000L, 100_000L, 0L, 0L, LocalDate.now().minusDays(2));
        when(invRepo.findByContractIdAndPeriodMonth("c1", period))
                .thenReturn(List.of())
                .thenReturn(List.of(saved));
        when(invRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        InvoiceDto.InvoiceResponse first = service.generateInvoice(cmd);
        InvoiceDto.InvoiceResponse second = service.generateInvoice(cmd);
        assertThat(second.invoiceCode()).isEqualTo(saved.getInvoiceCode());

        assertThatThrownBy(() -> service.generateBatch(List.of(cmd), "other-landlord", false))
                .isInstanceOf(AccessDeniedException.class);

        when(invRepo.findById(first.id())).thenReturn(Optional.of(saved));
        InvoiceDto.InvoiceResponse cn = service.issueCreditNote(
                first.id(), new InvoiceDto.CreditNoteCommand(100_000L, "Giam tien nuoc"), "l1", false);
        assertThat(cn.invoiceType()).isEqualTo("CREDIT_NOTE");
        assertThat(cn.totalAmount()).isEqualTo(-100_000L);

        when(invRepo.findOverdueCandidates(any())).thenReturn(List.of(saved));
        assertThat(service.markOverdueInvoices()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("OVERDUE");
    }

    @Test
    void shouldRequireMakerCheckerAndKycMatchOnHighValueRefund() {
        RefundRequestRepository refRepo = mock(RefundRequestRepository.class);
        MakerCheckerRepository mcRepo = mock(MakerCheckerRepository.class);
        LedgerService ledger = mock(LedgerService.class);
        RefundService service = new RefundService(refRepo, mcRepo, ledger, "SANDBOX_ONLY", false);

        assertThatThrownBy(() -> service.requestRefund("t1", new RefundDto.CreateRefundCommand(
                "pi-1", "c1", "l1", 6_000_000L, "VCB", "0123456", "TRAN VAN B", "NGUYEN VAN A", "123456", "Huy coc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KYC");

        when(mcRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        RefundDto.RefundResponse res = service.requestRefund("t1", new RefundDto.CreateRefundCommand(
                "pi-1", "c1", "l1", 6_000_000L, "VCB", "0123456", "Nguyễn Văn A", "NGUYEN VAN A", "123456", "Hoan coc"));
        assertThat(res.requiresMakerChecker()).isTrue();
        assertThat(res.status()).isEqualTo("PENDING_APPROVAL");

        MakerCheckerRequest mc = MakerCheckerRequest.create("t1", "REFUND_APPROVAL", 6_000_000L, "Hoan coc", "{}");
        RefundRequest stored = RefundRequest.create(res.refundCode(), "pi-1", "c1", "t1", "l1",
                6_000_000L, "VCB", "0123456", "Nguyễn Văn A", true, true, true, mc.getId(), "Hoan coc");
        when(refRepo.findById(stored.getId())).thenReturn(Optional.of(stored));
        when(mcRepo.findById(mc.getId())).thenReturn(Optional.of(mc));

        RefundDto.RefundResponse approved = service.approveRefund(stored.getId(), "admin-checker-2");
        assertThat(approved.status()).isEqualTo("COMPLETED");
        verify(ledger).postEntryIdempotent(anyString(), anyString(), eq("REFUND"), eq(stored.getId()), eq("admin-checker-2"), anyList());
    }

    @Test
    void shouldExportTenantStatsWithBomAndCsvInjectionNeutralization() {
        LedgerService ledger = mock(LedgerService.class);
        InvoiceRepository invRepo = mock(InvoiceRepository.class);
        JournalEntryRepository jeRepo = mock(JournalEntryRepository.class);
        BillingDashboardService dash = new BillingDashboardService(ledger, invRepo, jeRepo);

        Invoice malicious = Invoice.create("=CMD()", "c1", null, "t1", "l1", LocalDate.of(2026, 11, 1),
                2_000_000L, 0L, 0L, 0L, 0L, LocalDate.of(2026, 12, 1));
        when(invRepo.findByTenantId("t1")).thenReturn(List.of(malicious));
        when(ledger.getAccountBalance(LedgerService.ACC_ESCROW)).thenReturn(2_000_000L);

        DashboardStatsDto.TenantFinancialStats stats = dash.getTenantFinancialStats("t1");
        assertThat(stats.unpaidInvoicesTotal()).isEqualTo(2_000_000L);

        String csv = new String(dash.exportInvoicesCsvWithBom("TENANT", "t1"), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("\"'=CMD()\"");
    }

    @Test
    void shouldEnforceMeterIdorGenerateContractInvoiceWithCreditsLateFeePdfAndAnalytics() {
        UtilityReadingRepository readingRepo = mock(UtilityReadingRepository.class);
        UtilityReadingAuditRepository auditRepo = mock(UtilityReadingAuditRepository.class);
        InvoiceRepository invRepo = mock(InvoiceRepository.class);
        BillingContractPort contractPort = mock(BillingContractPort.class);
        BillingEventOutboxRepository outboxRepo = mock(BillingEventOutboxRepository.class);
        LedgerService ledger = mock(LedgerService.class);

        LocalDate period = LocalDate.of(2026, 12, 1);
        when(contractPort.getContractSnapshot("c-wave4", period))
                .thenReturn(Optional.of(new BillingContractPort.ContractSnapshot(
                        "c-wave4", "r-4", "t-owner", "l-owner", "ACTIVE", 4_200_000L, 5)));
        when(contractPort.getContractSnapshot("c-wave4", LocalDate.now()))
                .thenReturn(Optional.of(new BillingContractPort.ContractSnapshot(
                        "c-wave4", "r-4", "t-owner", "l-owner", "ACTIVE", 4_200_000L, 5)));

        UtilityReadingService meterService = new UtilityReadingService(
                readingRepo, auditRepo, invRepo, new ObjectMapper(), Optional.of(contractPort));
        UtilityReading sampleReading = new UtilityReading(
                "ur-4", "c-wave4", "r-4", period,
                BigDecimal.ZERO, new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("2"),
                "[]", "l-owner", null, "ACTIVE", "FIXED", "{}", 150_000L, 40_000L, 1, null);
        when(readingRepo.findById("ur-4")).thenReturn(Optional.of(sampleReading));
        when(readingRepo.findByContractIdAndPeriodMonth("c-wave4", period)).thenReturn(Optional.of(sampleReading));
        when(readingRepo.findByContractId("c-wave4")).thenReturn(List.of(sampleReading));

        assertThatThrownBy(() -> meterService.getReadingsForContract("c-wave4", "stranger", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> meterService.getAuditHistory("ur-4", "stranger", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(meterService.getReadingsForContract("c-wave4", "t-owner", false)).hasSize(1);

        Invoice priorOverpaid = Invoice.create("INV-PRIOR", "c-wave4", null, "t-owner", "l-owner",
                LocalDate.of(2026, 11, 1), 1_000_000L, 0L, 0L, 0L, 0L, LocalDate.of(2026, 11, 5));
        priorOverpaid.recordPayment(1_200_000L);
        assertThat(priorOverpaid.getOverpaymentCreditAmount()).isEqualTo(200_000L);

        when(invRepo.findByContractId("c-wave4")).thenReturn(List.of(priorOverpaid));
        when(invRepo.findByContractIdAndPeriodMonth("c-wave4", period)).thenReturn(List.of());
        when(invRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        InvoiceService invService = new InvoiceService(
                invRepo, ledger, Optional.of(contractPort), Optional.of(readingRepo),
                Optional.of(outboxRepo), Optional.of(new InvoicePdfService()));
        InvoiceDto.InvoiceResponse generated = invService.generateFromContract(
                new InvoiceDto.GenerateFromContractCommand("c-wave4", period, 100_000L, 0L),
                "l-owner", false);
        // 4,200,000 rent + 150,000 elec + 40,000 water + 100,000 svc - 200,000 overpayment credit = 4,290,000
        assertThat(generated.totalAmount()).isEqualTo(4_290_000L);
        verify(outboxRepo, atLeastOnce()).save(argThat(e -> "INVOICE_GENERATED".equals(e.getEventType())));

        Invoice stored = Invoice.create(generated.invoiceCode(), "c-wave4", "ur-4", "t-owner", "l-owner",
                period, 4_290_000L, 0L, 0L, 0L, 0L, period.plusDays(4));
        when(invRepo.findById(stored.getId())).thenReturn(Optional.of(stored));
        InvoiceDto.InvoiceResponse withFee = invService.applyLateFee(stored.getId(), 100_000L, "l-owner", false);
        assertThat(withFee.totalAmount()).isEqualTo(4_390_000L);

        byte[] receipt = invService.generateReceiptPdf(stored.getId(), "t-owner", false);
        assertThat(new String(receipt, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        BillingDashboardService dash = new BillingDashboardService(ledger, invRepo, mock(JournalEntryRepository.class));
        when(invRepo.findByLandlordId("l-owner")).thenReturn(List.of(stored));
        DashboardStatsDto.LandlordAnalyticsResponse analytics = dash.getLandlordAnalytics("l-owner", 3, 4);
        assertThat(analytics.occupancyRatePercent()).isEqualTo(75.0);
        assertThat(analytics.revenueSeries()).hasSize(1);
        assertThatThrownBy(() -> dash.exportInvoicesCsvWithBom(
                "LANDLORD", "l-owner", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

