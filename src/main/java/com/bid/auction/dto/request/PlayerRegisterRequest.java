package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PlayerRegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    @NotBlank(message = "Role is required")
    private String role;

    private String photo;
    private String paymentProof;
    private String mobileNumber;

    // ── Optional profile fields (all nullable) ───────────────────────────────
    /** RIGHT_HANDED or LEFT_HANDED */
    private String handedness;

    /** T-shirt size: XS, S, M, L, XL, XXL, etc. */
    private String tshirtSize;

    /** Trouser/pant size: 28, 30, 32, etc. */
    private String trouserSize;

    /** Jersey number preferred by the player */
    private String jerseyNumber;

    /** FULL_SLEEVES or HALF_SLEEVES */
    private String sleeveType;

    /** City / area the player is from */
    private String playerLocation;

    /** Whether the player participated in the last season */
    private Boolean lastSeasonPlayed;

    /** Team name the player played for in the last season (applicable if lastSeasonPlayed = true) */
    private String lastSeasonTeam;

    /** Bowling style e.g. "Right-arm fast", "Left-arm spin", etc. */
    private String bowlingStyle;
}

