package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionPlayerResponse {
    private Long id;
    private Long playerId;          // id of the registered Player this was promoted from (null if admin-created directly)
    private String playerNumber;
    private String firstName;
    private String lastName;
    private Integer age;
    private String city;
    private String battingStyle;
    private String bowlingStyle;
    private String role;
    private Long basePrice;
    private String auctionStatus;
    private Long soldToTeamId;
    private String soldToTeamName;
    private Long soldPrice;
    private Long tournamentId;
    private Integer sortOrder;
    private String photoUrl;
}

