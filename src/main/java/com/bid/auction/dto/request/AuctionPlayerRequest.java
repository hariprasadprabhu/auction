package com.bid.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuctionPlayerRequest {

    @NotBlank(message = "Player number is required")
    private String playerNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    private Integer age;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Batting style is required")
    private String battingStyle;

    @NotBlank(message = "Bowling style is required")
    private String bowlingStyle;

    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "Base price is required")
    @Min(value = 1, message = "Base price must be at least 1")
    private Long basePrice;

    private String photo;
}

