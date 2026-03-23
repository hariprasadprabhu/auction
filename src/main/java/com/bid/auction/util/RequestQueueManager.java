package com.bid.auction.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Request Queue Manager
 * 
 * When connection pool is exhausted:
 * - Queues incoming requests instead of rejecting them
 * - Processes from queue as connections become available
 * - Prevents application downtime due to connection starvation
 */
@Slf4j
@Component
public class RequestQueueManager {

    @Value("${app.db.request-queue-size:1000}")
    private int queueSize;

    @Value("${app.db.request-queue-timeout-seconds:30}")
    private int timeoutSeconds;

    private final BlockingQueue<RequestTask<?>> requestQueue;
    private volatile boolean isActive = false;

    public RequestQueueManager() {
        this.requestQueue = new LinkedBlockingQueue<>(1000);
    }

    /**
     * Submit a task to be executed when connection becomes available
     * 
     * @param task the task to execute
     * @return future that will contain the result
     */
    public <T> CompletableFuture<T> submitTask(RequestTask<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();

        // Check if queue is full
        if (requestQueue.size() >= queueSize * 0.9) {
            log.warn("⚠️  Request queue at {}% capacity", (requestQueue.size() * 100) / queueSize);
        }

        // Try to queue the request
        boolean queued = requestQueue.offer(new RequestTask<T>() {
            @Override
            public T execute() throws Exception {
                try {
                    T result = task.execute();
                    future.complete(result);
                    return result;
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    throw e;
                }
            }
        });

        if (!queued) {
            log.error("❌ Request queue is FULL - rejecting request");
            future.completeExceptionally(new RejectedExecutionException("Request queue is full"));
        }

        return future;
    }

    /**
     * Get current queue size
     */
    public int getQueueSize() {
        return requestQueue.size();
    }

    /**
     * Get queue utilization percentage
     */
    public double getQueueUtilization() {
        return (double) requestQueue.size() / queueSize * 100;
    }

    /**
     * Process queued requests
     * Should be called when connections become available
     */
    public int processQueuedRequests(int maxRequests) {
        int processed = 0;
        
        for (int i = 0; i < maxRequests && !requestQueue.isEmpty(); i++) {
            try {
                RequestTask<?> task = requestQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    try {
                        task.execute();
                        processed++;
                    } catch (Exception e) {
                        log.error("Error processing queued request: {}", e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (processed > 0) {
            log.debug("Processed {} queued requests", processed);
        }

        return processed;
    }

    /**
     * Clear all queued requests
     */
    public void clearQueue() {
        requestQueue.clear();
        log.info("Cleared request queue");
    }

    /**
     * Activate queue processing mode
     */
    public void activate() {
        isActive = true;
        log.warn("⚠️  Request queue activated - requests will be queued");
    }

    /**
     * Deactivate queue processing mode
     */
    public void deactivate() {
        isActive = false;
        log.info("✓ Request queue deactivated");
    }

    /**
     * Check if queue is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Request task interface
     */
    public interface RequestTask<T> {
        T execute() throws Exception;
    }

    /**
     * Get queue stats
     */
    public QueueStats getStats() {
        return new QueueStats(
            requestQueue.size(),
            queueSize,
            (double) requestQueue.size() / queueSize * 100,
            isActive
        );
    }

    /**
     * Queue statistics record
     */
    public static class QueueStats {
        public final int currentSize;
        public final int maxSize;
        public final double utilizationPercent;
        public final boolean isActive;

        public QueueStats(int currentSize, int maxSize, double utilizationPercent, boolean isActive) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.utilizationPercent = utilizationPercent;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            return String.format("Queue[%d/%d (%.1f%%)] %s", 
                currentSize, maxSize, utilizationPercent, isActive ? "ACTIVE" : "INACTIVE");
        }
    }
}

