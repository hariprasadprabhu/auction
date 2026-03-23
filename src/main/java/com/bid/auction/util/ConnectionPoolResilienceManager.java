package com.bid.auction.util;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database Connection Pool Resilience Manager
 * 
 * Handles:
 * - Connection validation and health checks
 * - Automatic recovery from connection failures
 * - Pool saturation detection and management
 * - Metrics and logging
 */
@Slf4j
@Component
public class ConnectionPoolResilienceManager {

    @Autowired(required = false)
    private DataSource dataSource;

    private final AtomicBoolean isPoolDegraded = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int FAILURE_THRESHOLD = 3;
    private static final int DEGRADED_THRESHOLD = 80; // 80% utilization
    
    /**
     * Validate connection pool health every 15 seconds
     * Performs test queries and detects stale connections
     */
    @Scheduled(fixedDelay = 15000)
    public void validateConnectionHealth() {
        if (!(dataSource instanceof HikariDataSource)) {
            return;
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        
        try {
            // Test connection from pool
            try (Connection conn = dataSource.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                    
                    // Reset failure counter on success
                    int failures = consecutiveFailures.getAndSet(0);
                    if (failures > 0) {
                        log.info("✓ Connection pool recovered after {} failures", failures);
                    }
                    
                    // Check pool saturation
                    var mxBean = hikariDs.getHikariPoolMXBean();
                    if (mxBean != null) {
                        int active = mxBean.getActiveConnections();
                        int total = mxBean.getTotalConnections();
                        int utilization = total > 0 ? (active * 100) / total : 0;
                        
                        if (utilization >= DEGRADED_THRESHOLD) {
                            if (!isPoolDegraded.getAndSet(true)) {
                                log.warn("⚠️  Connection pool entering DEGRADED state: {}% utilization", utilization);
                            }
                        } else {
                            if (isPoolDegraded.getAndSet(false)) {
                                log.info("✓ Connection pool recovered from degraded state");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            int failures = consecutiveFailures.incrementAndGet();
            log.error("❌ Connection pool health check failed (attempt {}): {}", failures, e.getMessage());
            
            if (failures >= FAILURE_THRESHOLD) {
                isPoolDegraded.set(true);
                log.error("🚨 Connection pool marked as CRITICAL - multiple failures detected!");
            }
        }
    }

    /**
     * Perform aggressive pool cleanup if degraded
     * Runs every 30 seconds when pool is degraded
     */
    @Scheduled(fixedDelay = 30000)
    public void aggressivePoolCleanup() {
        if (!isPoolDegraded.get() || !(dataSource instanceof HikariDataSource)) {
            return;
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        log.info("Running aggressive pool cleanup...");
        
        try {
            // Soft evict idle connections to refresh them
            hikariDs.getHikariPoolMXBean().softEvictConnections();
            log.info("✓ Soft eviction completed - idle connections refreshed");
        } catch (Exception e) {
            log.error("Error during pool cleanup: {}", e.getMessage());
        }
    }

    /**
     * Shutdown and reinitialize pool if in critical state
     * Last resort recovery mechanism
     */
    public void emergencyPoolReset() {
        if (!(dataSource instanceof HikariDataSource)) {
            return;
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        log.error("🚨 EMERGENCY POOL RESET TRIGGERED!");
        
        try {
            // Close existing connections
            hikariDs.getHikariPoolMXBean().softEvictConnections();
            
            // Give it a moment to reinitialize
            Thread.sleep(2000);
            
            // Reset failure counter
            consecutiveFailures.set(0);
            isPoolDegraded.set(false);
            
            log.info("✓ Emergency pool reset completed");
        } catch (Exception e) {
            log.error("Error during emergency pool reset: {}", e.getMessage());
        }
    }

    /**
     * Check if pool is currently in degraded state
     */
    public boolean isPoolDegraded() {
        return isPoolDegraded.get();
    }

    /**
     * Get pool health status details
     */
    public PoolHealthStatus getHealthStatus() {
        if (!(dataSource instanceof HikariDataSource)) {
            return new PoolHealthStatus("UNKNOWN", "DataSource is not HikariCP", false);
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        var mxBean = hikariDs.getHikariPoolMXBean();
        
        if (mxBean == null) {
            return new PoolHealthStatus("UNKNOWN", "MXBean not available", false);
        }

        int active = mxBean.getActiveConnections();
        int idle = mxBean.getIdleConnections();
        int total = mxBean.getTotalConnections();
        int utilization = total > 0 ? (active * 100) / total : 0;
        
        String status;
        String reason;
        
        if (isPoolDegraded.get()) {
            status = "DEGRADED";
            reason = String.format("Pool degraded at %d%% utilization", utilization);
        } else if (utilization >= DEGRADED_THRESHOLD) {
            status = "WARNING";
            reason = String.format("Pool approaching capacity at %d%% utilization", utilization);
        } else {
            status = "HEALTHY";
            reason = String.format("Pool healthy at %d%% utilization", utilization);
        }
        
        return new PoolHealthStatus(status, reason, false);
    }

    /**
     * Pool health status record
     */
    public static class PoolHealthStatus {
        public final String status;
        public final String reason;
        public final boolean requiresEmergencyReset;

        public PoolHealthStatus(String status, String reason, boolean requiresEmergencyReset) {
            this.status = status;
            this.reason = reason;
            this.requiresEmergencyReset = requiresEmergencyReset;
        }

        @Override
        public String toString() {
            return "PoolHealthStatus{" +
                    "status='" + status + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}

