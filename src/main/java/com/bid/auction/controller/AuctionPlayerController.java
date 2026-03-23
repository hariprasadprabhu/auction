package com.bid.auction.controller;

import com.bid.auction.dto.request.AuctionPlayerRequest;
import com.bid.auction.dto.request.ResetAuctionPlayersRequest;
import com.bid.auction.dto.request.SellPlayerRequest;
import com.bid.auction.dto.response.AuctionPlayerResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuctionPlayerService;
import com.bid.auction.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuctionPlayerController {

    private final AuctionPlayerService auctionPlayerService;
    private final AuthService authService;

    // ── Tournament-scoped ──────────────────────────────────────────────────────

    @GetMapping("/tournaments/{tournamentId}/auction-players")
    public ResponseEntity<List<AuctionPlayerResponse>> getAllByTournament(
            @PathVariable Long tournamentId, Authentication auth) {
        return ResponseEntity.ok(
                auctionPlayerService.getAllByTournament(tournamentId, currentUser(auth)));
    }

    @PostMapping(value = "/tournaments/{tournamentId}/auction-players")
    public ResponseEntity<AuctionPlayerResponse> create(
            @PathVariable Long tournamentId,
            @Valid @RequestBody AuctionPlayerRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionPlayerService.create(tournamentId, request, currentUser(auth)));
    }

    @PatchMapping("/tournaments/{tournamentId}/auction-players/requeue-unsold")
    public ResponseEntity<Map<String, Object>> requeueUnsold(
            @PathVariable Long tournamentId, Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.requeueUnsold(tournamentId, currentUser(auth)));
    }

    @PostMapping("/tournaments/{tournamentId}/auction-players/reset")
    public ResponseEntity<Map<String, Object>> resetAuctionPlayers(
            @PathVariable Long tournamentId,
            @Valid @RequestBody ResetAuctionPlayersRequest request,
            Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.resetAuctionPlayers(
                tournamentId, request.getPlayerIds(), currentUser(auth)));
    }

    @PostMapping("/tournaments/{tournamentId}/auction/reset-entire")
    public ResponseEntity<Map<String, Object>> resetEntireAuction(
            @PathVariable Long tournamentId,
            Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.resetEntireAuction(tournamentId, currentUser(auth)));
    }

    // ── Direct auction-player endpoints ───────────────────────────────────────

    @GetMapping("/auction-players/{id}")
    public ResponseEntity<AuctionPlayerResponse> getById(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.getById(id, currentUser(auth)));
    }

    @PutMapping(value = "/auction-players/{id}")
    public ResponseEntity<AuctionPlayerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AuctionPlayerRequest request,
            Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.update(id, request, currentUser(auth)));
    }

    @DeleteMapping("/auction-players/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        auctionPlayerService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/auction-players/{id}/sell")
    public ResponseEntity<AuctionPlayerResponse> sell(
            @PathVariable Long id,
            @Valid @RequestBody SellPlayerRequest request,
            Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.sell(id, request, currentUser(auth)));
    }

    @PatchMapping("/auction-players/{id}/unsold")
    public ResponseEntity<Map<String, Object>> markUnsold(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(auctionPlayerService.markUnsold(id, currentUser(auth)));
    }


    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}
