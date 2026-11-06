package com.bid.auction.service;

import com.bid.auction.dto.response.AuctionPlayerResponse;
import com.bid.auction.dto.response.TeamPurseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionDataUpdateListener {

    private final AuctionPlayerService auctionPlayerService;
    private final TeamPurseService teamPurseService;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionDataChanged(AuctionDataChangedEvent event) {
        try {
            Long tournamentId = event.tournamentId();
            List<AuctionPlayerResponse> auctionPlayers =
                    auctionPlayerService.getAllByTournament(tournamentId, null);
            List<TeamPurseResponse> teamPurses =
                    teamPurseService.getAllTeamPurses(tournamentId);

            messagingTemplate.convertAndSend(
                    "/topic/tournaments/" + tournamentId + "/auction-players",
                    auctionPlayers);
            messagingTemplate.convertAndSend(
                    "/topic/tournaments/" + tournamentId + "/team-purses",
                    teamPurses);
        } catch (RuntimeException exception) {
            log.error("Failed to refresh and publish auction data for tournament {}",
                    event.tournamentId(), exception);
        }
    }
}
