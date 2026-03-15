package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerViewResponse {

    private TournamentSummary tournament;
    private PlayerStats playerStats;
    private List<TeamStats> teamStats;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TournamentSummary {
        private Long id;
        private String name;
        private Long purseAmount;
        private Integer playersPerTeam;
        private Long basePrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStats {
        private long total;
        private long sold;
        private long unsold;
        private long available;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamStats {
        private TeamInfo team;
        private int playersBought;
        private long purseUsed;
        private long purseRemaining;
        private long maxBid;
        private long minPurseToKeep;
        private List<SoldPlayerInfo> soldPlayers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamInfo {
        private Long id;
        private String teamNumber;
        private String name;
        private String ownerName;
        private String logoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldPlayerInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String role;
        private Long soldPrice;
        private String photoUrl;
    }
}

