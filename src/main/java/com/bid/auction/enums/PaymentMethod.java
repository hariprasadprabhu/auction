package com.bid.auction.enums;

/**
 * Represents the payment method used/accepted.
 * <p>
 * GPAY     – Google Pay only
 * PHONEPAY – PhonePe only
 * BOTH     – Both GPay and PhonePe are accepted (used at Tournament level)
 */
public enum PaymentMethod {
    GPAY,
    PHONEPAY,
    BOTH
}

