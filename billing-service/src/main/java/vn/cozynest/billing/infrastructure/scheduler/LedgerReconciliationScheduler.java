package vn.cozynest.billing.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.LedgerService;

@Component
public class LedgerReconciliationScheduler {
    private static final Logger log = LoggerFactory.getLogger(LedgerReconciliationScheduler.class);

    private final LedgerService ledgerService;

    public LedgerReconciliationScheduler(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * DoD: Job cân đối chạy mỗi đêm (mặc định 01:00 AM), lệch sẽ cảnh báo Telegram/Slack
     */
    @Scheduled(cron = "${app.ledger.reconcile-cron:0 0 1 * * ?}")
    public void runNightlyReconciliation() {
        log.info("⏰ [LEDGER-02] Bắt đầu chạy Job đối soát toàn vẹn sổ cái định kỳ mỗi đêm...");
        try {
            LedgerDto.ComprehensiveIntegrityReport report = ledgerService.runDailyReconciliation();
            if (report.isSystemBalanced()) {
                log.info("✅ [LEDGER-02] Job đối soát hoàn thành: Sổ cái hoàn toàn cân đối (Tổng Nợ: {}, Tổng Có: {}).",
                        report.totalDebit(), report.totalCredit());
            } else {
                log.warn("🚨 [LEDGER-02] Job đối soát phát hiện mất cân đối! Chênh lệch: {}, Tài khoản lệch: {}. Đã gửi cảnh báo: {}",
                        report.systemDifference(), report.totalMismatchedAccounts(), report.alertSent());
            }
        } catch (Exception e) {
            log.error("❌ [LEDGER-02] Lỗi khi chạy Job đối soát sổ cái: {}", e.getMessage(), e);
        }
    }
}

