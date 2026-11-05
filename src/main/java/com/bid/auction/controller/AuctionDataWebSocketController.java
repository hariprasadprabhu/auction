package com.bid.auction.controller;

import com.bid.auction.dto.response.AuctionPlayerResponse;
import com.bid.auction.dto.response.TeamPurseResponse;
import com.bid.auction.service.AuctionPlayerService;
import com.bid.auction.service.TeamPurseService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuctionDataWebSocketController {

    private final AuctionPlayerService auctionPlayerService;
    private final TeamPurseService teamPurseService;

    @SubscribeMapping("/tournaments/{tournamentId}/auction-players")
    public List<AuctionPlayerResponse> subscribeToAuctionPlayers(
            @DestinationVariable Long tournamentId) {
        return auctionPlayerService.getAllByTournament(tournamentId, null);
    }

    @SubscribeMapping("/tournaments/{tournamentId}/team-purses")
    public List<TeamPurseResponse> subscribeToTeamPurses(
            @DestinationVariable Long tournamentId) {
        return teamPurseService.getAllTeamPurses(tournamentId);
    }
}
