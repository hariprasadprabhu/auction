package com.bid.auction.util;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectionPoolMonitor
 */
class ConnectionPoolMonitorTest {

    private ConnectionPoolMonitor monitor;

    @Mock
    private HikariDataSource mockDataSource;

    @Mock
    private HikariDataSource.HikariPoolMXBean mockMXBean;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        monitor = new ConnectionPoolMonitor();
        ReflectionTestUtils.setField(monitor, "dataSource", mockDataSource);
    }

    @Test
    void testGetPoolStats_WithValidDataSource() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(5);
        when(mockMXBean.getIdleConnections()).thenReturn(15);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        Map<String, Object> stats = monitor.getPoolStats();

        // Assert
        assertNotNull(stats);
        assertEquals(5, stats.get("activeConnections"));
        assertEquals(15, stats.get("idleConnections"));
        assertEquals(20, stats.get("totalConnections"));
        assertEquals(20, stats.get("maximumPoolSize"));
        assertEquals(5, stats.get("minimumIdle"));
        assertEquals(25, stats.get("utilizationPercent"));
        assertFalse((boolean) stats.get("isNearCapacity"));
    }

    @Test
    void testGetPoolStats_CalculatesUtilizationCorrectly() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(18);  // 90% of 20
        when(mockMXBean.getIdleConnections()).thenReturn(2);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        Map<String, Object> stats = monitor.getPoolStats();

        // Assert
        assertEquals(90, stats.get("utilizationPercent"));
        assertTrue((boolean) stats.get("isNearCapacity"));
    }

    @Test
    void testGetPoolStats_WithNonHikariDataSource() {
        // Arrange
        ReflectionTestUtils.setField(monitor, "dataSource", mock(DataSource.class));

        // Act
        Map<String, Object> stats = monitor.getPoolStats();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("error"));
        assertEquals("DataSource is not HikariCP", stats.get("error"));
    }

    @Test
    void testIsPoolHealthy_WithAvailableConnections() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(5);
        when(mockMXBean.getIdleConnections()).thenReturn(15);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        boolean healthy = monitor.isPoolHealthy();

        // Assert
        assertTrue(healthy);
    }

    @Test
    void testIsPoolHealthy_WithExhaustedPool() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(20);  // All active
        when(mockMXBean.getIdleConnections()).thenReturn(0);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        boolean healthy = monitor.isPoolHealthy();

        // Assert
        assertFalse(healthy);
    }

    @Test
    void testGetDiagnosticReport_GeneratesValidReport() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(5);
        when(mockMXBean.getIdleConnections()).thenReturn(15);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        String report = monitor.getDiagnosticReport();

        // Assert
        assertNotNull(report);
        assertTrue(report.contains("HikariCP Connection Pool Diagnostic Report"));
        assertTrue(report.contains("Total Connections: 20"));
        assertTrue(report.contains("Active Connections: 5"));
        assertTrue(report.contains("Idle Connections: 15"));
        assertTrue(report.contains("Utilization: 25%"));
    }

    @Test
    void testGetDiagnosticReport_WarnsWhenNearCapacity() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(19);  // 95% utilization
        when(mockMXBean.getIdleConnections()).thenReturn(1);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        String report = monitor.getDiagnosticReport();

        // Assert
        assertNotNull(report);
        assertTrue(report.contains("WARNING: Pool is operating near capacity"));
    }

    @Test
    void testLogPoolStatus_DoesNotThrowException() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(5);
        when(mockMXBean.getIdleConnections()).thenReturn(15);
        when(mockMXBean.getTotalConnections()).thenReturn(20);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> monitor.logPoolStatus());
    }

    @Test
    void testGetPoolStats_HandlesZeroTotalConnections() {
        // Arrange
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockMXBean);
        when(mockMXBean.getActiveConnections()).thenReturn(0);
        when(mockMXBean.getIdleConnections()).thenReturn(0);
        when(mockMXBean.getTotalConnections()).thenReturn(0);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(20);
        when(mockDataSource.getMinimumIdle()).thenReturn(5);
        when(mockDataSource.getConnectionTimeout()).thenReturn(60000L);
        when(mockDataSource.getIdleTimeout()).thenReturn(300000L);
        when(mockDataSource.getMaxLifetime()).thenReturn(1800000L);

        // Act
        Map<String, Object> stats = monitor.getPoolStats();

        // Assert
        assertEquals(0, stats.get("utilizationPercent"));  // Should be 0, not NaN
        assertFalse((boolean) stats.get("isNearCapacity"));
    }
}

