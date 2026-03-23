package com.bid.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Database Resilience Configuration
 * 
 * Enables:
 * - Retry mechanisms for database operations
 * - Scheduled monitoring and health checks
 * - Connection pool optimization
 */
@Configuration
@EnableRetry
@EnableScheduling
public class DatabaseResilienceConfig {
    
    public DatabaseResilienceConfig() {
        // Configuration enables @Retry and @Scheduled annotations
    }
}

