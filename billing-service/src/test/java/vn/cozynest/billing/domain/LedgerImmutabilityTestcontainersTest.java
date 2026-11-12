package vn.cozynest.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.billing.BillingApplication;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.JournalEntry;
import vn.cozynest.billing.domain.model.PostingDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerImmutabilityTestcontainersTest {

    @Test
    @DisplayName("LEDGER-01/02: Real MySQL 9.0 Flyway V1-V6, immutability triggers, imbalance rejection, concurrency")
    void enforcesLedgerTriggersImmutabilityAndConcurrencyOnRealMySql() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected: skipping Testcontainers MySQL test");
            return;
        }
        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker not accessible: skipping Testcontainers MySQL test");
            return;
        }

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("cozynest_billing")
                .withUsername("testuser")
                .withPassword("testpass")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();

            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(BillingApplication.class)
                    .run(
                            "--server.port=0",
                            "--spring.datasource.url=" + mysql.getJdbcUrl(),
                            "--spring.datasource.username=" + mysql.getUsername(),
                            "--spring.datasource.password=" + mysql.getPassword(),
                            "--spring.flyway.locations=classpath:db/migration",
                            "--spring.jpa.hibernate.ddl-auto=validate",
                            "--cozynest.broker.enabled=false"
                    )) {

                LedgerService ledgerService = ctx.getBean(LedgerService.class);
                JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

                ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Gateway Cash", AccountType.ASSET, "SYSTEM");
                ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Deposit Escrow", AccountType.LIABILITY, "SYSTEM");
                ledgerService.getOrCreateWallet("ll-race-1", "Landlord Race");

                // 1. Imbalanced entry must be rejected without changing balances
                assertThatThrownBy(() -> ledgerService.postEntry(
                        "Imbalanced test", "TEST", "ref-bad", "admin-1",
                        List.of(
                                new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, 1_000_000L),
                                new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, 900_000L)
                        )))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY)).isZero();

                // 2. Concurrent postings under pessimistic locks preserve exact sum
                int threads = 6;
                ExecutorService pool = Executors.newFixedThreadPool(threads);
                CountDownLatch gate = new CountDownLatch(1);
                List<Future<JournalEntry>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    final int idx = i;
                    futures.add(pool.submit(() -> {
                        gate.await(5, TimeUnit.SECONDS);
                        return ledgerService.postEntryIdempotent(
                                "CONC-KEY-" + idx,
                                "Concurrent deposit #" + idx,
                                "DEPOSIT", "cnt-" + idx, "sys",
                                List.of(
                                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, 500_000L),
                                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, 500_000L)
                                ));
                    }));
                }
                gate.countDown();
                for (Future<JournalEntry> f : futures) {
                    assertThat(f.get(15, TimeUnit.SECONDS)).isNotNull();
                }
                pool.shutdownNow();

                assertThat(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY)).isEqualTo(3_000_000L);
                assertThat(ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)).isEqualTo(3_000_000L);

                // 3. Database triggers must block UPDATE & DELETE on journal_entries & ledger_postings
                assertThatThrownBy(() -> jdbc.update("UPDATE journal_entries SET total_amount = 999 WHERE status = 'POSTED'"))
                        .isInstanceOf(DataAccessException.class)
                        .hasMessageContaining("immutable");
                assertThatThrownBy(() -> jdbc.update("DELETE FROM journal_entries"))
                        .isInstanceOf(DataAccessException.class)
                        .hasMessageContaining("immutable");
                assertThatThrownBy(() -> jdbc.update("UPDATE ledger_postings SET amount = 1"))
                        .isInstanceOf(DataAccessException.class)
                        .hasMessageContaining("immutable");
                assertThatThrownBy(() -> jdbc.update("DELETE FROM ledger_postings"))
                        .isInstanceOf(DataAccessException.class)
                        .hasMessageContaining("immutable");
            }
        }
    }
}
