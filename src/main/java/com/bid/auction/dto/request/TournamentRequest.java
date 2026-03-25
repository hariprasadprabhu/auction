package com.bid.auction.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TournamentRequest {

    @NotBlank(message = "Tournament name is required")
    private String name;

    @NotNull(message = "Date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotBlank(message = "Sport is required")
    private String sport;

    @NotNull(message = "Total teams is required")
    @Min(value = 2, message = "Total teams must be at least 2")
    private Integer totalTeams;

    @NotNull(message = "Total players is required")
    @Min(value = 1, message = "Total players must be at least 1")
    private Integer totalPlayers;

    @NotNull(message = "Purse amount is required")
    @Min(value = 1, message = "Purse amount must be at least 1")
    private Long purseAmount;

    @NotNull(message = "Players per team is required")
    @Min(value = 1, message = "Players per team must be at least 1")
    private Integer playersPerTeam;

    @NotNull(message = "Base price is required")
    @Min(value = 1, message = "Base price must be at least 1")
    private Long basePrice;

    @Min(value = 1, message = "Initial increment must be at least 1")
    private Long initialIncrement;

    // UPCOMING | ONGOING | COMPLETED
    private String status;

    private String logo;

    private Boolean paymentProofRequired;
}

