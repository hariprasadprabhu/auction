package com.bid.auction.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Request body for PUT /tournaments/{id}/registration-config.
 * Each flag controls whether the corresponding player-registration field
 * is mandatory. Fields not sent in the request default to false (optional).
 *
 * Accepts both the full name (requireLastName) and the short name (lastName)
 * so the frontend can send either form.
 */
@Data
public class RegistrationConfigRequest {

    /** Make Last Name mandatory */
    @JsonAlias("lastName")
    private Boolean requireLastName;

    /** Make Date of Birth mandatory */
    @JsonAlias("dob")
    private Boolean requireDob;

    /** Make Photo mandatory */
    @JsonAlias("photo")
    private Boolean requirePhoto;

    /** Make Payment Proof mandatory */
    @JsonAlias("paymentProof")
    private Boolean requirePaymentProof;

    /** Make Mobile Number mandatory */
    @JsonAlias("mobileNumber")
    private Boolean requireMobileNumber;

    /** Make Handedness (right/left) mandatory */
    @JsonAlias("handedness")
    private Boolean requireHandedness;

    /** Make T-Shirt Size mandatory */
    @JsonAlias("tshirtSize")
    private Boolean requireTshirtSize;

    /** Make Trouser/Pant Size mandatory */
    @JsonAlias("trouserSize")
    private Boolean requireTrouserSize;

    /** Make Jersey Number mandatory */
    @JsonAlias("jerseyNumber")
    private Boolean requireJerseyNumber;

    /** Make Sleeve Type mandatory */
    @JsonAlias("sleeveType")
    private Boolean requireSleeveType;

    /** Make Player Location mandatory */
    @JsonAlias("playerLocation")
    private Boolean requirePlayerLocation;

    /** Make Last Season Played mandatory */
    @JsonAlias("lastSeasonPlayed")
    private Boolean requireLastSeasonPlayed;

    /** Make Last Season Team mandatory */
    @JsonAlias("lastSeasonTeam")
    private Boolean requireLastSeasonTeam;

    /** Make Bowling Style mandatory */
    @JsonAlias("bowlingStyle")
    private Boolean requireBowlingStyle;
}

