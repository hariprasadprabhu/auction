package com.bid.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SellPlayerRequest {

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotNull(message = "Sold price is required")
    @Min(value = 1, message = "Sold price must be at least 1")
    private Long soldPrice;
}

