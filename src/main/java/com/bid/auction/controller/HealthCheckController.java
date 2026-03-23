package com.bid.auction.controller;

import com.bid.auction.util.ConnectionPoolMonitor;
import com.bid.auction.util.ConnectionPoolResilienceManager;
import com.bid.auction.util.DatabaseCircuitBreaker;
import com.bid.auction.util.RequestQueueManager;
import com.bid.auction.config.MemoryMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Health Monitoring Endpoints
 * 
 * Monitors:
 * - Database connection pool health
 * - Circuit breaker status
 * - Request queue status
 * - Memory and system health
 * 
 * Endpoints:
 * - GET /api/health/db-connections - Pool statistics
 * - GET /api/health/db-diagnostic - Detailed diagnostics
 * - GET /api/health/status - Simple status check
 * - GET /api/health/full-report - Comprehensive health report
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final ConnectionPoolMonitor poolMonitor;
    private final ConnectionPoolResilienceManager poolResilienceManager;
    private final DatabaseCircuitBreaker circuitBreaker;
    private final RequestQueueManager requestQueueManager;
    private final MemoryMonitor memoryMonitor;

    /**
     * Get current database connection pool statistics
     * 
     * @return JSON with connection pool metrics
     */
    @GetMapping("/db-connections")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        try {
            Map<String, Object> stats = poolMonitor.getPoolStats();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", poolMonitor.isPoolHealthy() ? "HEALTHY" : "DEGRADED");
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching connection pool stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get detailed diagnostic report about connection pool
     * Useful for troubleshooting timeout issues
     * 
     * @return Text report with pool diagnostics and warnings
     */
    @GetMapping("/db-diagnostic")
    public ResponseEntity<String> getDiagnosticReport() {
        try {
            String report = poolMonitor.getDiagnosticReport();
            log.debug("Diagnostic Report:\n{}", report);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(report);
        } catch (Exception e) {
            log.error("Error generating diagnostic report", e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body("Error generating diagnostic report: " + e.getMessage());
        }
    }

    /**
     * Simple health check endpoint
     * Returns 200 if DB connection pool is healthy
     * 
     * @return Health status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getHealthStatus() {
        boolean isHealthy = poolMonitor.isPoolHealthy();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", isHealthy ? "UP" : "DEGRADED");
        response.put("component", "database");
        response.put("timestamp", System.currentTimeMillis() + "");
        
        poolMonitor.logPoolStatus();
        
        return isHealthy 
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(503).body(response);
    }

    /**
     * Get circuit breaker status
     * 
     * @return Circuit breaker state and metrics
     */
    @GetMapping("/circuit-breaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        try {
            DatabaseCircuitBreaker.CircuitBreakerStatus status = circuitBreaker.getStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("state", status.state);
            response.put("failureCount", status.failureCount);
            response.put("successCount", status.successCount);
            response.put("timeSinceLastFailureMs", status.timeSinceLastFailure);
            response.put("timestamp", System.currentTimeMillis());
            
            boolean isHealthy = "CLOSED".equals(status.state);
            return isHealthy 
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Error fetching circuit breaker status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get pool resilience status
     * 
     * @return Pool health and degradation status
     */
    @GetMapping("/pool-resilience")
    public ResponseEntity<Map<String, Object>> getPoolResilienceStatus() {
        try {
            var healthStatus = poolResilienceManager.getHealthStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", healthStatus.status);
            response.put("reason", healthStatus.reason);
            response.put("isDegraded", poolResilienceManager.isPoolDegraded());
            response.put("requiresEmergencyReset", healthStatus.requiresEmergencyReset);
            response.put("timestamp", System.currentTimeMillis());
            
            boolean isHealthy = "HEALTHY".equals(healthStatus.status);
            return isHealthy 
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Error fetching pool resilience status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get request queue status
     * 
     * @return Queue capacity and utilization
     */
    @GetMapping("/queue-status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        try {
            var queueStats = requestQueueManager.getStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("queueSize", queueStats.currentSize);
            response.put("maxQueueSize", queueStats.maxSize);
            response.put("utilizationPercent", queueStats.utilizationPercent);
            response.put("isActive", queueStats.isActive);
            response.put("timestamp", System.currentTimeMillis());
            
            boolean isHealthy = !queueStats.isActive || queueStats.utilizationPercent < 80;
            return isHealthy 
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Error fetching queue status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get comprehensive system health report
     * Includes memory, database, circuit breaker, and queue status
     * 
     * @return Full health report as text
     */
    @GetMapping("/full-report")
    public ResponseEntity<String> getFullHealthReport() {
        try {
            StringBuilder report = new StringBuilder();
            
            // System health
            report.append(memoryMonitor.getSystemHealthReport()).append("\n\n");
            
            // Pool diagnostics
            report.append(poolMonitor.getDiagnosticReport()).append("\n");
            
            // Circuit breaker status
            DatabaseCircuitBreaker.CircuitBreakerStatus cbStatus = circuitBreaker.getStatus();
            report.append("=== CIRCUIT BREAKER STATUS ===\n");
            report.append("State: ").append(cbStatus.state).append("\n");
            report.append("Failures: ").append(cbStatus.failureCount).append("\n");
            report.append("Successes: ").append(cbStatus.successCount).append("\n");
            report.append("Time Since Last Failure: ").append(cbStatus.timeSinceLastFailure).append("ms\n\n");
            
            // Queue status
            var queueStats = requestQueueManager.getStats();
            report.append("=== REQUEST QUEUE STATUS ===\n");
            report.append("Size: ").append(queueStats.currentSize).append("/").append(queueStats.maxSize).append("\n");
            report.append("Utilization: ").append(String.format("%.1f%%", queueStats.utilizationPercent)).append("\n");
            report.append("Active: ").append(queueStats.isActive).append("\n");
            
            log.debug("Full health report generated");
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(report.toString());
                
        } catch (Exception e) {
            log.error("Error generating full health report", e);
            return ResponseEntity.status(500)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body("Error generating health report: " + e.getMessage());
        }
    }
}


