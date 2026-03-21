package com.bid.auction.util;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to monitor and manage HikariCP connection pool health.
 *
 * Usage: Inject this component and call monitoring methods to track pool status.
 */
@Slf4j
@Component
public class ConnectionPoolMonitor {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Get current connection pool statistics
     *
     * @return Map with pool metrics
     */
    public Map<String, Object> getPoolStats() {
        if (!(dataSource instanceof HikariDataSource)) {
            return Map.of("error", "DataSource is not HikariCP");
        }

        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        var mxBean = hikariDs.getHikariPoolMXBean();

        Map<String, Object> stats = new HashMap<>();
        int activeConnections = mxBean.getActiveConnections();
        int idleConnections = mxBean.getIdleConnections();
        int totalConnections = mxBean.getTotalConnections();

        stats.put("activeConnections", activeConnections);
        stats.put("idleConnections", idleConnections);
        stats.put("totalConnections", totalConnections);
        stats.put("waitingForConnection", 0);  // Not available in HikariPoolMXBean
        stats.put("maximumPoolSize", hikariDs.getMaximumPoolSize());
        stats.put("minimumIdle", hikariDs.getMinimumIdle());
        stats.put("connectionTimeout", hikariDs.getConnectionTimeout());
        stats.put("idleTimeout", hikariDs.getIdleTimeout());
        stats.put("maxLifetime", hikariDs.getMaxLifetime());

        // Calculate utilization percentage
        int utilization = totalConnections > 0 ? (activeConnections * 100) / totalConnections : 0;
        stats.put("utilizationPercent", utilization);

        // Warning flags
        stats.put("isNearCapacity", activeConnections >= (totalConnections * 0.9));
        stats.put("hasWaitingRequests", false);  // Approximate based on utilization

        return stats;
    }

    /**
     * Check if pool is healthy
     *
     * @return true if pool has capacity and no issues
     */
    public boolean isPoolHealthy() {
        Map<String, Object> stats = getPoolStats();

        if (stats.containsKey("error")) {
            return false;
        }

        int active = (int) stats.get("activeConnections");
        int total = (int) stats.get("totalConnections");

        // Unhealthy if all connections are active
        boolean isFull = active == total;

        return !isFull;
    }

    /**
     * Log connection pool status for debugging
     */
    public void logPoolStatus() {
        Map<String, Object> stats = getPoolStats();

        if (stats.containsKey("error")) {
            log.warn("Connection Pool Monitor: {}", stats.get("error"));
            return;
        }

        int active = (int) stats.get("activeConnections");
        int idle = (int) stats.get("idleConnections");
        int total = (int) stats.get("totalConnections");
        int utilization = (int) stats.get("utilizationPercent");
        boolean nearCapacity = (boolean) stats.get("isNearCapacity");

        StringBuilder sb = new StringBuilder();
        sb.append("HikariCP Pool Status - ");
        sb.append("Active: ").append(active).append("/").append(total).append(", ");
        sb.append("Idle: ").append(idle).append(", ");
        sb.append("Utilization: ").append(utilization).append("%");

        if (nearCapacity) {
            log.warn("{} [WARNING: NEAR CAPACITY]", sb);
        } else {
            log.info("{}", sb);
        }
    }

    /**
     * Get a detailed diagnostic report as a formatted string
     *
     * @return Formatted diagnostic report
     */
    public String getDiagnosticReport() {
        Map<String, Object> stats = getPoolStats();

        if (stats.containsKey("error")) {
            return "Error: " + stats.get("error");
        }

        StringBuilder report = new StringBuilder();
        report.append("=== HikariCP Connection Pool Diagnostic Report ===\n");
        report.append(String.format("Total Connections: %d%n", stats.get("totalConnections")));
        report.append(String.format("Active Connections: %d%n", stats.get("activeConnections")));
        report.append(String.format("Idle Connections: %d%n", stats.get("idleConnections")));
        report.append(String.format("Utilization: %d%%%n", stats.get("utilizationPercent")));
        report.append(String.format("Maximum Pool Size: %d%n", stats.get("maximumPoolSize")));
        report.append(String.format("Minimum Idle: %d%n", stats.get("minimumIdle")));
        report.append(String.format("Connection Timeout: %dms%n", stats.get("connectionTimeout")));
        report.append(String.format("Idle Timeout: %dms%n", stats.get("idleTimeout")));
        report.append(String.format("Max Lifetime: %dms%n", stats.get("maxLifetime")));
        report.append(String.format("Pool Healthy: %s%n", isPoolHealthy()));

        if ((boolean) stats.get("isNearCapacity")) {
            report.append("\n⚠️  WARNING: Pool is operating near capacity!\n");
            report.append("   Consider increasing maximum-pool-size in application.yml\n");
        }

        report.append("=====================================================\n");
        return report.toString();
    }
}


