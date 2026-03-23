package com.bid.auction.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker Pattern Implementation for Database Access
 * 
 * Prevents cascading failures by:
 * - Tracking database operation failures
 * - Opening circuit when failure threshold exceeded
 * - Preventing requests from hitting failing database
 * - Allowing circuit to half-open for recovery tests
 */
@Slf4j
@Component
public class DatabaseCircuitBreaker {

    private static final int FAILURE_THRESHOLD = 5;
    private static final int SUCCESS_THRESHOLD = 3;
    private static final long TIMEOUT_MILLIS = 30000; // 30 seconds

    enum State {
        CLOSED,      // Normal operation
        OPEN,        // Preventing requests
        HALF_OPEN    // Testing recovery
    }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    /**
     * Record a successful database operation
     */
    public void recordSuccess() {
        if (state == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= SUCCESS_THRESHOLD) {
                transitionToClosed();
            }
        } else if (state == State.CLOSED) {
            failureCount.set(0);
        }
    }

    /**
     * Record a failed database operation
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();
        
        if (failures >= FAILURE_THRESHOLD && state != State.OPEN) {
            transitionToOpen();
        }
    }

    /**
     * Check if requests should be allowed to proceed
     * 
     * @return true if circuit is CLOSED or HALF_OPEN, false if OPEN
     */
    public boolean isRequestAllowed() {
        if (state == State.CLOSED) {
            return true;
        }

        if (state == State.OPEN) {
            // Check if timeout has passed to allow half-open state
            long timeSinceFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceFailure >= TIMEOUT_MILLIS) {
                transitionToHalfOpen();
                return true;
            }
            return false;
        }

        // HALF_OPEN - allow requests for testing
        return true;
    }

    /**
     * Get current circuit state
     */
    public String getState() {
        return state.name();
    }

    /**
     * Get detailed status
     */
    public CircuitBreakerStatus getStatus() {
        return new CircuitBreakerStatus(
            state.name(),
            failureCount.get(),
            successCount.get(),
            System.currentTimeMillis() - lastFailureTime.get()
        );
    }

    /**
     * Manually reset circuit to closed state
     */
    public void reset() {
        transitionToClosed();
        log.info("✓ Circuit breaker manually reset");
    }

    // Private transition methods

    private void transitionToOpen() {
        state = State.OPEN;
        successCount.set(0);
        log.error("🚨 CIRCUIT BREAKER OPENED - Database appears to be unhealthy");
    }

    private void transitionToHalfOpen() {
        state = State.HALF_OPEN;
        failureCount.set(0);
        successCount.set(0);
        log.warn("⚠️  CIRCUIT BREAKER HALF-OPEN - Testing database recovery");
    }

    private void transitionToClosed() {
        state = State.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        log.info("✓ CIRCUIT BREAKER CLOSED - Database recovered");
    }

    /**
     * Circuit breaker status record
     */
    public static class CircuitBreakerStatus {
        public final String state;
        public final int failureCount;
        public final int successCount;
        public final long timeSinceLastFailure;

        public CircuitBreakerStatus(String state, int failureCount, int successCount, long timeSinceLastFailure) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.timeSinceLastFailure = timeSinceLastFailure;
        }

        @Override
        public String toString() {
            return "CircuitBreakerStatus{" +
                    "state='" + state + '\'' +
                    ", failureCount=" + failureCount +
                    ", successCount=" + successCount +
                    ", timeSinceLastFailure=" + timeSinceLastFailure + "ms" +
                    '}';
        }
    }
}

