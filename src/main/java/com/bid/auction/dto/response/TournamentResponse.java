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
}

