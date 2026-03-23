package com.bid.auction.config;

import com.bid.auction.util.ConnectionPoolResilienceManager;
import com.bid.auction.util.DatabaseCircuitBreaker;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.management.MemoryMXBean;
import java.lang.management.ManagementFactory;

/**
 * Integrated Memory and Database Monitoring Component
 * 
 * Tracks:
 * - Heap memory usage (OutOfMemory prevention)
 * - Database connection pool health
 * - Circuit breaker status
 * - Overall application health
 * 
 * Runs periodically to log statistics and alert if critical thresholds are exceeded
 */
@Component
public class MemoryMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
    private static final long WARNING_THRESHOLD = 85; // Warn at 85% heap usage
    private static final long CRITICAL_THRESHOLD = 95; // Critical alert at 95%
    
    private final MemoryMXBean memoryMXBean;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private ConnectionPoolResilienceManager poolResilienceManager;

    @Autowired(required = false)
    private DatabaseCircuitBreaker circuitBreaker;
    
    public MemoryMonitor() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }
    
    /**
     * Monitor heap memory every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void monitorMemory() {
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        long heapPercent = (heapUsed * 100) / heapMax;
        
        // Log normal usage
        if (heapPercent < WARNING_THRESHOLD) {
            logger.debug("Heap Memory: {}MB / {}MB ({}%)", 
                toMB(heapUsed), toMB(heapMax), heapPercent);
        }
        // Warn at 85%
        else if (heapPercent < CRITICAL_THRESHOLD) {
            logger.warn("⚠️  HIGH HEAP USAGE: {}MB / {}MB ({}%) - Consider increasing -Xmx", 
                toMB(heapUsed), toMB(heapMax), heapPercent);
        }
        // Critical at 95%
        else {
            logger.error("🚨 CRITICAL HEAP USAGE: {}MB / {}MB ({}%) - OutOfMemoryError likely soon!", 
                toMB(heapUsed), toMB(heapMax), heapPercent);
        }
        
        logger.debug("Non-Heap Memory: {}MB", toMB(nonHeapUsed));

        // Monitor database connection pool health
        monitorConnectionPool();

        // Monitor circuit breaker status
        monitorCircuitBreaker();
    }

    /**
     * Monitor database connection pool health
     */
    private void monitorConnectionPool() {
        if (dataSource == null || !(dataSource instanceof HikariDataSource)) {
            return;
        }

        try {
            HikariDataSource hikariDs = (HikariDataSource) dataSource;
            var mxBean = hikariDs.getHikariPoolMXBean();
            if (mxBean == null) {
                return;
            }

            int active = mxBean.getActiveConnections();
            int idle = mxBean.getIdleConnections();
            int total = mxBean.getTotalConnections();
            int max = hikariDs.getMaximumPoolSize();
            int utilization = total > 0 ? (active * 100) / total : 0;

            // Log pool status
            if (utilization < 80) {
                logger.debug("DB Pool Healthy: {}/{} active, {} idle, {}% util", 
                    active, total, idle, utilization);
            } else if (utilization < 95) {
                logger.warn("⚠️  DB POOL WARNING: {}/{} active, {} idle, {}% util - Approaching capacity", 
                    active, total, idle, utilization);
            } else {
                logger.error("🚨 DB POOL CRITICAL: {}/{} active, {} idle, {}% util - May cause timeout errors!", 
                    active, total, idle, utilization);
            }

            // Check pool resilience status
            if (poolResilienceManager != null && poolResilienceManager.isPoolDegraded()) {
                logger.error("🚨 Database pool is in DEGRADED state - monitoring closely");
            }

        } catch (Exception e) {
            logger.debug("Error monitoring connection pool: {}", e.getMessage());
        }
    }

    /**
     * Monitor circuit breaker status
     */
    private void monitorCircuitBreaker() {
        if (circuitBreaker == null) {
            return;
        }

        DatabaseCircuitBreaker.CircuitBreakerStatus status = circuitBreaker.getStatus();
        
        if ("OPEN".equals(status.state)) {
            logger.error("🚨 CIRCUIT BREAKER OPEN - Database failures detected: {} failures", 
                status.failureCount);
        } else if ("HALF_OPEN".equals(status.state)) {
            logger.warn("⚠️  CIRCUIT BREAKER HALF-OPEN - Testing database recovery ({} successes)", 
                status.successCount);
        }
    }
    
    /**
     * Convert bytes to megabytes
     */
    private long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }
    
    /**
     * Get current memory stats as a string
     */
    public String getMemoryStats() {
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        long heapPercent = (heapUsed * 100) / heapMax;
        
        return String.format(
            "Heap: %dMB/%dMB (%d%%), Non-Heap: %dMB",
            toMB(heapUsed), toMB(heapMax), heapPercent, toMB(nonHeapUsed)
        );
    }

    /**
     * Get comprehensive system health report
     */
    public String getSystemHealthReport() {
        StringBuilder report = new StringBuilder();
        
        // Memory status
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long heapPercent = (heapUsed * 100) / heapMax;
        
        report.append("=== SYSTEM HEALTH REPORT ===\n");
        report.append(String.format("Memory: %dMB/%dMB (%d%%)\n", 
            toMB(heapUsed), toMB(heapMax), heapPercent));
        
        // Database pool status
        if (dataSource != null && dataSource instanceof HikariDataSource) {
            try {
                HikariDataSource hikariDs = (HikariDataSource) dataSource;
                var mxBean = hikariDs.getHikariPoolMXBean();
                if (mxBean != null) {
                    int active = mxBean.getActiveConnections();
                    int idle = mxBean.getIdleConnections();
                    int total = mxBean.getTotalConnections();
                    int utilization = total > 0 ? (active * 100) / total : 0;
                    
                    report.append(String.format("DB Pool: %d/%d active, %d idle (%d%% util)\n", 
                        active, total, idle, utilization));
                }
            } catch (Exception e) {
                report.append("DB Pool: [Error]\n");
            }
        }

        // Circuit breaker status
        if (circuitBreaker != null) {
            DatabaseCircuitBreaker.CircuitBreakerStatus status = circuitBreaker.getStatus();
            report.append(String.format("Circuit Breaker: %s\n", status.state));
        }

        report.append("=============================");
        return report.toString();
    }
}

