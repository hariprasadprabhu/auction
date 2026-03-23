package com.bid.auction.aspect;

import com.bid.auction.util.DatabaseCircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

/**
 * Database Retry Aspect
 * 
 * Provides automatic retry logic for database operations:
 * - Retries failed operations up to 3 times
 * - Exponential backoff between retries
 * - Circuit breaker integration
 * - Detailed logging
 */
@Slf4j
@Aspect
@Component
public class DatabaseRetryAspect {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 100;
    
    private final DatabaseCircuitBreaker circuitBreaker;

    public DatabaseRetryAspect(DatabaseCircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Intercept repository and service methods that interact with database
     * Apply retry logic with exponential backoff
     */
    @Around("execution(* com.bid.auction.repository..*(..)) || execution(* com.bid.auction.service..*(..))")
    public Object retryOnDatabaseFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Check circuit breaker before attempting
        if (!circuitBreaker.isRequestAllowed()) {
            String message = "Circuit breaker is OPEN - database is unavailable";
            log.error("❌ {} - {}.{}", message, className, methodName);
            throw new ServiceUnavailableException(message);
        }

        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                // Execute the actual method
                Object result = joinPoint.proceed();
                
                // Record success with circuit breaker
                circuitBreaker.recordSuccess();
                
                if (attempt > 0) {
                    log.info("✓ {}.{} succeeded on attempt {}", className, methodName, attempt + 1);
                }
                
                return result;
                
            } catch (SQLException | TimeoutException | RuntimeException e) {
                attempt++;
                circuitBreaker.recordFailure();
                
                // Check if it's a connection pool exhaustion error
                boolean isConnectionError = isConnectionPoolError(e);
                
                if (attempt < MAX_RETRIES && isConnectionError) {
                    log.warn("⚠️  {}.{} failed (attempt {}/{}) - Retrying in {}ms: {}",
                        className, methodName, attempt, MAX_RETRIES, delay, e.getMessage());
                    
                    // Exponential backoff
                    Thread.sleep(delay);
                    delay *= 2; // Double delay for next retry
                    
                } else {
                    if (isConnectionError) {
                        log.error("❌ {}.{} exhausted retries after {} attempts", 
                            className, methodName, MAX_RETRIES);
                    }
                    throw e;
                }
                
            } catch (Throwable e) {
                // Non-database errors pass through
                throw e;
            }
        }

        throw new RuntimeException("Failed after " + MAX_RETRIES + " retries");
    }

    /**
     * Determine if error is related to connection pool exhaustion
     */
    private boolean isConnectionPoolError(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        message = message.toLowerCase();
        
        return message.contains("timeout")
            || message.contains("connection")
            || message.contains("pool")
            || message.contains("hikari")
            || message.contains("busy")
            || message.contains("exhausted");
    }

    /**
     * Custom exception for service unavailable
     */
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}

