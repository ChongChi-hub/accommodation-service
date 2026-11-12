package vn.cozynest.common.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lightweight, framework-agnostic circuit breaker with retry support for downstream service calls.
 */
public class ServiceCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openStateDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private volatile Instant openedAt;

    public ServiceCircuitBreaker(int failureThreshold, Duration openStateDuration) {
        this(failureThreshold, openStateDuration, Clock.systemUTC());
    }

    public ServiceCircuitBreaker(int failureThreshold, Duration openStateDuration, Clock clock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        this.failureThreshold = failureThreshold;
        this.openStateDuration = openStateDuration;
        this.clock = clock;
    }

    public State getState() {
        if (state.get() == State.OPEN && openedAt != null) {
            if (Duration.between(openedAt, clock.instant()).compareTo(openStateDuration) >= 0) {
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
            }
        }
        return state.get();
    }

    public <T> T executeWithFallback(int maxAttempts, Supplier<T> action, Supplier<T> fallback) {
        State current = getState();
        if (current == State.OPEN) {
            return fallback.get();
        }
        int attempts = Math.max(1, maxAttempts);
        for (int i = 1; i <= attempts; i++) {
            try {
                T result = action.get();
                onSuccess();
                return result;
            } catch (RuntimeException ex) {
                if (i == attempts) {
                    onFailure();
                }
            }
        }
        return fallback.get();
    }

    public synchronized void onSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
        openedAt = null;
    }

    public synchronized void onFailure() {
        if (state.get() == State.HALF_OPEN) {
            tripOpen();
            return;
        }
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            tripOpen();
        }
    }

    private void tripOpen() {
        state.set(State.OPEN);
        openedAt = clock.instant();
    }
}

