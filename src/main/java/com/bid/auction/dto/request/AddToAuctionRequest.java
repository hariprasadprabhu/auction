package com.bid.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Extra cricket-specific details an admin provides when promoting
 * an APPROVED registered player into the live auction pool.
 * Basic info (name, role, playerNumber, photo) is copied from the Player record.
 */
@Data
public class AddToAuctionRequest {

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    private Integer age;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Batting style is required")
    private String battingStyle;

    @NotBlank(message = "Bowling style is required")
    private String bowlingStyle;

    @NotNull(message = "Base price is required")
    @Min(value = 1, message = "Base price must be at least 1")
    private Long basePrice;
}

