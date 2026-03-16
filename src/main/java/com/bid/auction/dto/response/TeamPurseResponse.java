package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for team purse/financial details in a tournament
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPurseResponse {
    private Long id;
    private Long teamId;
    private String teamNumber;
    private String teamName;
    private Long tournamentId;
    
    /** Initial purse amount allocated to the team */
    private Long initialPurse;
    
    /** Current remaining purse available for bidding */
    private Long currentPurse;
    
    /** Total amount spent on players so far */
    private Long purseUsed;
    
    /** Maximum bid allowed for a single player */
    private Long maxBidPerPlayer;
    
    /** Reserved fund for ensuring minimum squad */
    private Long reservedFund;
    
    /** Available purse after reserving minimum squad fund */
    private Long availableForBidding;
    
    /** Number of players already purchased */
    private Integer playersBought;
    
    /** Number of remaining slots to be filled */
    private Integer remainingSlots;
}

