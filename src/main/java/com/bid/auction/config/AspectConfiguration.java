package com.bid.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Aspect Configuration
 * 
 * Enables aspect-oriented programming for:
 * - Database operation retry logic
 * - Circuit breaker integration
 */
@Configuration
@EnableAspectJAutoProxy
public class AspectConfiguration {
    // Configuration enables AspectJ auto proxy for @Aspect components
}

