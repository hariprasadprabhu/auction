package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentResponse {
    private Long id;
    private String name;
    private LocalDate date;
    private String sport;
    private Integer totalTeams;
    private Integer totalPlayers;
    /**
     * Read-only field: Auto-calculated based on tournament configuration.
     * Cannot be modified via API.
     */
    private Integer teamAllowed;
    private String status;
    private Long purseAmount;
    private Integer playersPerTeam;
    private Long basePrice;
    private Long initialIncrement;
    private String logoUrl;
    private Boolean paymentProofRequired;

    /** Payment method players should use to pay the registration fee (e.g. "GPay", "PhonePe"). */
    private String paymentMethod;

    /** UPI ID / account number players should pay the registration fee to. */
    private String paymentNumber;

    /** Registration fee amount (in whole currency units) that players are required to pay. */
    private Long amountToPay;
    /**
     * Indicates whether the auction date can still be edited.
     * {@code true} = one more update is allowed; {@code false} = date is permanently locked.
     * Read-only — cannot be modified via the API.
     */
    private Boolean canEditAuctionDate;

    /**
     * Whether player self-registration is currently open.
     * {@code true} = players can register; {@code false} = registration is closed.
     */
    private Boolean playerRegistrationOpen;

    /**
     * Per-field mandatory configuration for player registration.
     * Tells the frontend which fields to mark as required on the registration form.
     */
    private RegistrationConfigResponse registrationConfig;
}

