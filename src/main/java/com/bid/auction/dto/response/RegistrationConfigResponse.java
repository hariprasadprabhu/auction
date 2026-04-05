package com.bid.auction.dto.response;

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
}

