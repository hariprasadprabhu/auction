package com.bid.auction.dto.response;

import com.bid.auction.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the current registration field configuration for a tournament.
 * Returned by GET/PUT /tournaments/{id}/registration-config
 * and embedded inside TournamentResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationConfigResponse {

    private Boolean requireLastName;
    private Boolean requireDob;
    private Boolean requirePhoto;
    private Boolean requirePaymentProof;
    private Boolean requireMobileNumber;
    private Boolean requireHandedness;
    private Boolean requireTshirtSize;
    private Boolean requireTrouserSize;
    private Boolean requireJerseyNumber;
    private Boolean requireSleeveType;
    private Boolean requirePlayerLocation;
    private Boolean requireLastSeasonPlayed;
    private Boolean requireLastSeasonTeam;
    private Boolean requireBowlingStyle;

    /**
     * The GPay / PhonePe number players should send their entry fee to.
     * Populated only when requirePaymentProof = true.
     */
    private String paymentCollectionNumber;

    /**
     * Payment method(s) accepted by the organiser: GPAY, PHONEPAY, or BOTH.
     * Populated only when requirePaymentProof = true.
     */
    private PaymentMethod acceptedPaymentMethods;

    /**
     * Entry fee amount players must pay to participate.
     * Populated only when requirePaymentProof = true.
     */
    private Long paymentAmount;
}

