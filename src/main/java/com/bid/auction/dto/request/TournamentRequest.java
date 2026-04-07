package com.bid.auction.dto.request;

import com.bid.auction.enums.PaymentMethod;
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

    /**
     * GPay / PhonePe number where players should send their entry fee.
     * Only relevant when paymentProofRequired = true.
     */
    private String paymentCollectionNumber;

    /**
     * Payment method(s) accepted: GPAY, PHONEPAY, or BOTH.
     * Only relevant when paymentProofRequired = true.
     */
    private PaymentMethod acceptedPaymentMethods;

    /**
     * Entry fee amount players must pay to participate.
     * Only relevant when paymentProofRequired = true.
     */
    private Long paymentAmount;

    /**
     * Whether player self-registration is open for this tournament.
     * Defaults to {@code true} on creation if not specified.
     */
    private Boolean playerRegistrationOpen;
}

