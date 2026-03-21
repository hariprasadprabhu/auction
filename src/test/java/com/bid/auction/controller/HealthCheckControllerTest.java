package com.bid.auction.controller;

import com.bid.auction.util.ConnectionPoolMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthCheckController
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckControllerTest {

    @Mock
    private ConnectionPoolMonitor poolMonitor;

    @InjectMocks
    private HealthCheckController healthCheckController;

    @BeforeEach
    void setUp() {
        // Any additional setup if needed
    }

    @Test
    void testGetConnectionStats_Success() {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("activeConnections", 5);
        mockStats.put("idleConnections", 15);
        mockStats.put("totalConnections", 20);
        mockStats.put("utilizationPercent", 25);

        when(poolMonitor.getPoolStats()).thenReturn(mockStats);
        when(poolMonitor.isPoolHealthy()).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = healthCheckController.getConnectionStats();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((boolean) response.getBody().get("success"));
        assertEquals("HEALTHY", response.getBody().get("status"));
        assertNotNull(response.getBody().get("data"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void testGetConnectionStats_Exception() {
        // Arrange
        when(poolMonitor.getPoolStats()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = healthCheckController.getConnectionStats();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("error").toString().contains("Database error"));
    }

    @Test
    void testGetDiagnosticReport_Success() {
        // Arrange
        String mockReport = "=== HikariCP Connection Pool Diagnostic Report ===\n" +
                           "Total Connections: 20\n" +
                           "Active Connections: 5\n" +
                           "Pool Healthy: true\n" +
                           "=====================================================\n";

        when(poolMonitor.getDiagnosticReport()).thenReturn(mockReport);

        // Act
        ResponseEntity<String> response = healthCheckController.getDiagnosticReport();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReport, response.getBody());
        assertEquals("text/plain; charset=UTF-8", response.getHeaders().getContentType().toString());
    }

    @Test
    void testGetDiagnosticReport_Exception() {
        // Arrange
        when(poolMonitor.getDiagnosticReport()).thenThrow(new RuntimeException("Report generation failed"));

        // Act
        ResponseEntity<String> response = healthCheckController.getDiagnosticReport();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error generating diagnostic report"));
    }

    @Test
    void testGetHealthStatus_Healthy() {
        // Arrange
        when(poolMonitor.isPoolHealthy()).thenReturn(true);

        // Act
        ResponseEntity<Map<String, String>> response = healthCheckController.getHealthStatus();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("database", response.getBody().get("component"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void testGetHealthStatus_Degraded() {
        // Arrange
        when(poolMonitor.isPoolHealthy()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, String>> response = healthCheckController.getHealthStatus();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEGRADED", response.getBody().get("status"));
        assertEquals("database", response.getBody().get("component"));
    }

    @Test
    void testGetConnectionStats_PoolDegraded() {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("activeConnections", 20);
        mockStats.put("idleConnections", 0);
        mockStats.put("totalConnections", 20);
        mockStats.put("utilizationPercent", 100);

        when(poolMonitor.getPoolStats()).thenReturn(mockStats);
        when(poolMonitor.isPoolHealthy()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = healthCheckController.getConnectionStats();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DEGRADED", response.getBody().get("status"));
    }

    @Test
    void testPoolMonitorCalledCorrectly() {
        // Arrange
        when(poolMonitor.getPoolStats()).thenReturn(new HashMap<>());
        when(poolMonitor.isPoolHealthy()).thenReturn(true);

        // Act
        healthCheckController.getConnectionStats();

        // Assert
        verify(poolMonitor, times(1)).getPoolStats();
        verify(poolMonitor, times(1)).isPoolHealthy();
    }

    @Test
    void testGetDiagnosticReport_LogsOutput() {
        // Arrange
        String mockReport = "Diagnostic Report";
        when(poolMonitor.getDiagnosticReport()).thenReturn(mockReport);

        // Act
        ResponseEntity<String> response = healthCheckController.getDiagnosticReport();

        // Assert
        verify(poolMonitor, times(1)).getDiagnosticReport();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetHealthStatus_LogsPoolStatus() {
        // Arrange
        when(poolMonitor.isPoolHealthy()).thenReturn(true);

        // Act
        healthCheckController.getHealthStatus();

        // Assert
        verify(poolMonitor, times(1)).isPoolHealthy();
        verify(poolMonitor, times(1)).logPoolStatus();
    }
}

