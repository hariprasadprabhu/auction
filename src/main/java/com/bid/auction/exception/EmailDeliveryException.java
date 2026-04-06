package com.bid.auction.exception;

/**
 * Thrown when an outbound email cannot be delivered (e.g. HTTP API error).
 * Mapped to HTTP 503 by {@link GlobalExceptionHandler}.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

