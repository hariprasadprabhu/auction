package com.bid.auction.controller;

import com.bid.auction.util.ConnectionPoolMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints for monitoring connection pool health.
 * <p>
 * This controller provides diagnostic information about the HikariCP connection pool
 * to help troubleshoot timeout issues and capacity problems.
 * </p>
 * <p>
 * Endpoints:
 * - GET /api/health/db-connections - Get current pool statistics
 * - GET /api/health/db-diagnostic - Get detailed diagnostic report
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final ConnectionPoolMonitor poolMonitor;

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
}


