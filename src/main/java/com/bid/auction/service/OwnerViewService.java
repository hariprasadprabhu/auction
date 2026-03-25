package com.bid.auction.service;

import com.bid.auction.dto.response.OwnerViewResponse;
import com.bid.auction.entity.AuctionPlayer;
import com.bid.auction.entity.Team;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.AuctionStatus;
import com.bid.auction.repository.AuctionPlayerRepository;
import com.bid.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerViewService {

    private final TeamRepository teamRepository;
    private final AuctionPlayerRepository auctionPlayerRepository;
    private final TournamentService tournamentService;

    public OwnerViewResponse getOwnerView(Long tournamentId, User user) {
        // For public access, skip ownership verification
        Tournament tournament = tournamentService.findById(tournamentId);

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        List<AuctionPlayer> allPlayers = auctionPlayerRepository.findByTournamentId(tournamentId);

        // ── Player stats ──────────────────────────────────────────────────────
        long total     = allPlayers.size();
        long sold      = allPlayers.stream().filter(p -> p.getAuctionStatus() == AuctionStatus.SOLD).count();
        long unsold    = allPlayers.stream().filter(p -> p.getAuctionStatus() == AuctionStatus.UNSOLD).count();
        long available = allPlayers.stream().filter(p -> p.getAuctionStatus() == AuctionStatus.UPCOMING).count();

        // ── Group sold players by team id ─────────────────────────────────────
        Map<Long, List<AuctionPlayer>> soldByTeam = allPlayers.stream()
                .filter(p -> p.getAuctionStatus() == AuctionStatus.SOLD && p.getSoldToTeam() != null)
                .collect(Collectors.groupingBy(p -> p.getSoldToTeam().getId()));

        // ── Build team stats ──────────────────────────────────────────────────
        List<OwnerViewResponse.TeamStats> teamStatsList = teams.stream().map(team -> {
            List<AuctionPlayer> teamSold = soldByTeam.getOrDefault(team.getId(), Collections.emptyList());

            int playersBought   = teamSold.size();
            long purseUsed      = teamSold.stream()
                    .mapToLong(p -> p.getSoldPrice() != null ? p.getSoldPrice() : 0L).sum();
            long purseRemaining = tournament.getPurseAmount() - purseUsed;
            int remainingSlots  = tournament.getPlayersPerTeam() - playersBought;

            long minPurseToKeep = remainingSlots > 0
                    ? (long) remainingSlots * tournament.getBasePrice() : 0L;
            long maxBid = remainingSlots > 1
                    ? purseRemaining - (long) (remainingSlots - 1) * tournament.getBasePrice()
                    : purseRemaining;
            maxBid = Math.max(0L, maxBid);

            List<OwnerViewResponse.SoldPlayerInfo> soldPlayerInfos = teamSold.stream()
                    .map(p -> OwnerViewResponse.SoldPlayerInfo.builder()
                            .id(p.getId())
                            .firstName(p.getFirstName())
                            .lastName(p.getLastName())
                            .role(p.getRole())
                            .soldPrice(p.getSoldPrice())
                            .photoUrl(p.getPhoto() != null
                                    ? "/api/auction-players/" + p.getId() + "/photo" : null)
                            .build())
                    .toList();

            OwnerViewResponse.TeamInfo teamInfo = OwnerViewResponse.TeamInfo.builder()
                    .id(team.getId())
                    .teamNumber(team.getTeamNumber())
                    .name(team.getName())
                    .ownerName(team.getOwnerName())
                    .logoUrl(team.getLogo() != null ? "/api/teams/" + team.getId() + "/logo" : null)
                    .build();

            return OwnerViewResponse.TeamStats.builder()
                    .team(teamInfo)
                    .playersBought(playersBought)
                    .purseUsed(purseUsed)
                    .purseRemaining(purseRemaining)
                    .maxBid(maxBid)
                    .minPurseToKeep(minPurseToKeep)
                    .soldPlayers(soldPlayerInfos)
                    .build();
        }).toList();

        // ── Tournament summary ────────────────────────────────────────────────
        OwnerViewResponse.TournamentSummary summary = OwnerViewResponse.TournamentSummary.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .purseAmount(tournament.getPurseAmount())
                .playersPerTeam(tournament.getPlayersPerTeam())
                .basePrice(tournament.getBasePrice())
                .build();

        return OwnerViewResponse.builder()
                .tournament(summary)
                .playerStats(OwnerViewResponse.PlayerStats.builder()
                        .total(total).sold(sold).unsold(unsold).available(available).build())
                .teamStats(teamStatsList)
                .build();
    }
}

