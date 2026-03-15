package com.bid.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncrementRuleRequest {

    @NotNull(message = "From amount is required")
    @Min(value = 0, message = "From amount must be >= 0")
    private Long fromAmount;

    /**
     * 0 or null means "and above" → stored as Long.MAX_VALUE
     */
    private Long toAmount;

    @NotNull(message = "Increment by is required")
    @Min(value = 1, message = "Increment by must be at least 1")
    private Long incrementBy;
}

