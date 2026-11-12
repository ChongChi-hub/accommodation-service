package vn.cozynest.common.resilience;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCircuitBreakerTest {

    @Test
    void opensAfterThresholdAndRecoversAfterCooldown() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-11T00:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };

        ServiceCircuitBreaker breaker = new ServiceCircuitBreaker(2, Duration.ofSeconds(30), clock);
        AtomicInteger calls = new AtomicInteger(0);

        String r1 = breaker.executeWithFallback(1, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("timeout");
        }, () -> "FALLBACK");
        String r2 = breaker.executeWithFallback(1, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("timeout");
        }, () -> "FALLBACK");

        assertThat(r1).isEqualTo("FALLBACK");
        assertThat(r2).isEqualTo("FALLBACK");
        assertThat(breaker.getState()).isEqualTo(ServiceCircuitBreaker.State.OPEN);

        // Should short-circuit without calling supplier while OPEN
        String r3 = breaker.executeWithFallback(1, () -> {
            calls.incrementAndGet();
            return "OK";
        }, () -> "SHORT_CIRCUIT");
        assertThat(r3).isEqualTo("SHORT_CIRCUIT");
        assertThat(calls.get()).isEqualTo(2);

        // Advance beyond cooldown -> HALF_OPEN -> succeeds -> CLOSED
        now.set(now.get().plusSeconds(31));
        assertThat(breaker.getState()).isEqualTo(ServiceCircuitBreaker.State.HALF_OPEN);

        String r4 = breaker.executeWithFallback(1, () -> {
            calls.incrementAndGet();
            return "RECOVERED";
        }, () -> "FALLBACK");
        assertThat(r4).isEqualTo("RECOVERED");
        assertThat(breaker.getState()).isEqualTo(ServiceCircuitBreaker.State.CLOSED);
    }
}

