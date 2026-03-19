package com.bid.auction.controller;

import com.bid.auction.dto.request.AddToAuctionRequest;
import com.bid.auction.dto.request.BulkPlayerActionRequest;
import com.bid.auction.dto.request.PlayerRegisterRequest;
import com.bid.auction.dto.response.AuctionPlayerResponse;
import com.bid.auction.dto.response.PlayerResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuctionPlayerService;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final AuctionPlayerService auctionPlayerService;
    private final AuthService authService;

    // ── Tournament-scoped (AUTH) ───────────────────────────────────────────────

    @GetMapping("/tournaments/{tournamentId}/players")
    public ResponseEntity<List<PlayerResponse>> getAllByTournament(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return ResponseEntity.ok(playerService.getAllByTournament(tournamentId, status, currentUser(auth)));
    }

    /**
     * Get only APPROVED players who are eligible for the auction pool.
     * These are players ready to be added to AuctionPlayers.
     */
    @GetMapping("/tournaments/{tournamentId}/players/approved")
    public ResponseEntity<List<PlayerResponse>> getApprovedPlayers(
            @PathVariable Long tournamentId,
            Authentication auth) {
        return ResponseEntity.ok(playerService.getApprovedByTournament(tournamentId, currentUser(auth)));
    }

    /**
     * Get player registration statistics for the tournament.
     * Shows count of PENDING, APPROVED, and REJECTED players.
     */
    @GetMapping("/tournaments/{tournamentId}/players/stats")
    public ResponseEntity<Map<String, Object>> getPlayerStats(
            @PathVariable Long tournamentId,
            Authentication auth) {
        return ResponseEntity.ok(playerService.getPlayerStatsByTournament(tournamentId, currentUser(auth)));
    }

    // ── Public self-registration ───────────────────────────────────────────────

    @PostMapping(value = "/players/register/{tournamentId}",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlayerResponse> register(
            @PathVariable Long tournamentId,
            @ModelAttribute PlayerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playerService.register(tournamentId, request));
    }

    // ── Direct player endpoints (AUTH) ─────────────────────────────────────────

    @GetMapping("/players/{id}")
    public ResponseEntity<PlayerResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(playerService.getById(id, currentUser(auth)));
    }

    @PutMapping(value = "/players/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlayerResponse> update(
            @PathVariable Long id,
            @ModelAttribute PlayerRegisterRequest request,
            Authentication auth) {
        return ResponseEntity.ok(playerService.update(id, request, currentUser(auth)));
    }

    @DeleteMapping("/players/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        playerService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/players/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(playerService.approve(id, currentUser(auth)));
    }

    @PatchMapping("/players/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(playerService.reject(id, currentUser(auth)));
    }

    /**
     * Approve specified players for a tournament.
     * Only the tournament owner can perform this action.
     * Players in the provided list will be changed to APPROVED status.
     */
    @PatchMapping("/tournaments/{tournamentId}/players/approve-all")
    public ResponseEntity<Map<String, Object>> approveAllPlayers(
            @PathVariable Long tournamentId,
            @Valid @RequestBody BulkPlayerActionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(playerService.approveAll(tournamentId, request.getPlayerIds(), currentUser(auth)));
    }

    /**
     * Reject specified players for a tournament.
     * Only the tournament owner can perform this action.
     * Players in the provided list will be changed to REJECTED status.
     */
    @PatchMapping("/tournaments/{tournamentId}/players/reject-all")
    public ResponseEntity<Map<String, Object>> rejectAllPlayers(
            @PathVariable Long tournamentId,
            @Valid @RequestBody BulkPlayerActionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(playerService.rejectAll(tournamentId, request.getPlayerIds(), currentUser(auth)));
    }

    /**
     * Promote an APPROVED registered player into the live auction pool.
     * Copies name, playerNumber, role and photo from the Player record.
     * Admin provides the extra cricket stats (age, city, battingStyle, bowlingStyle, basePrice).
     */
    @PostMapping("/players/{id}/add-to-auction")
    public ResponseEntity<AuctionPlayerResponse> addToAuction(
            @PathVariable Long id,
            @Valid @RequestBody AddToAuctionRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionPlayerService.promoteToAuction(id, request, currentUser(auth)));
    }

    // ── Public image endpoints ─────────────────────────────────────────────────

    @GetMapping("/players/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        byte[] photo = playerService.getPhoto(id);
        String contentType = playerService.getPhotoContentType(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(photo);
    }

    // ── Auth-required payment proof ────────────────────────────────────────────

    @GetMapping("/players/{id}/payment-proof")
    public ResponseEntity<byte[]> getPaymentProof(@PathVariable Long id, Authentication auth) {
        // ownership will be verified inside the service
        playerService.getById(id, currentUser(auth)); // triggers ownership check
        byte[] proof = playerService.getPaymentProof(id);
        String contentType = playerService.getPaymentProofContentType(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(proof);
    }

    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}

