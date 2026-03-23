package com.bid.auction.service;

import com.bid.auction.dto.request.PlayerRegisterRequest;
import com.bid.auction.dto.response.PlayerResponse;
import com.bid.auction.entity.Player;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.PlayerStatus;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TournamentService tournamentService;
    private final AuctionPlayerService auctionPlayerService;

    // ── List (auth) ───────────────────────────────────────────────────────────
    public List<PlayerResponse> getAllByTournament(Long tournamentId, String status, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        List<Player> players;
        if (status != null && !status.isBlank()) {
            PlayerStatus ps = PlayerStatus.valueOf(status.toUpperCase());
            Tournament t = tournamentService.findById(tournamentId);
            players = playerRepository.findByTournamentAndStatus(t, ps);
        } else {
            players = playerRepository.findByTournamentId(tournamentId);
        }
        return players.stream().map(this::toResponse).toList();
    }

    // ── Get single (auth) ─────────────────────────────────────────────────────
    public PlayerResponse getById(Long id, User user) {
        Player player = findPlayer(id);
        tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);
        return toResponse(player);
    }

    // ── Public self-registration ──────────────────────────────────────────────
    public PlayerResponse register(Long tournamentId, PlayerRegisterRequest req) {
        Tournament tournament = tournamentService.findById(tournamentId);

        long count = playerRepository.countByTournamentId(tournamentId);
        String playerNumber = String.format("P%03d", count + 1);

        Player player = Player.builder()
                .playerNumber(playerNumber)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .dob(req.getDob())
                .role(req.getRole())
                .photo(req.getPhoto())
                .paymentProof(req.getPaymentProof())
                .status(PlayerStatus.PENDING)
                .tournament(tournament)
                .build();


        return toResponse(playerRepository.save(player));
    }

    // ── Update (auth) ─────────────────────────────────────────────────────────
    @Transactional
    public PlayerResponse update(Long id, PlayerRegisterRequest req, User user) {
        Player player = findPlayer(id);
        tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);

        player.setFirstName(req.getFirstName());
        if (req.getLastName() != null) player.setLastName(req.getLastName());
        if (req.getDob() != null) player.setDob(req.getDob());
        player.setRole(req.getRole());
        if (req.getPhoto() != null && !req.getPhoto().isEmpty()) player.setPhoto(req.getPhoto());
        if (req.getPaymentProof() != null && !req.getPaymentProof().isEmpty()) 
            player.setPaymentProof(req.getPaymentProof());

        Player saved = playerRepository.save(player);
        auctionPlayerService.syncFromPlayer(saved);   // propagate to AuctionPlayer
        return toResponse(saved);
    }

    // ── Delete (auth) ─────────────────────────────────────────────────────────
    // When a SOLD player is deleted:
    // 1. Refund the sold price back to the team's purse
    // 2. Recalculate all team values:
    //    - Available purse (currentPurse increases)
    //    - Required players count (remainingSlots increases)
    //    - Max bid per player (recalculated based on new available purse)
    //    - Reserved fund (recalculated based on new remainingSlots)
    // 3. Delete all linked auction player records
    // 4. Delete the player record
    @Transactional(timeout = 30)
    public void delete(Long id, User user) {
        Player player = findPlayer(id);
        Long tournamentId = player.getTournament().getId();
        tournamentService.findAndVerifyOwner(tournamentId, user);
        
        // Handle auction players linked to this player:
        // - If player status is SOLD, refund the team and recalculate all team values
        // - Delete all linked auction player records
        auctionPlayerService.deletePlayerWithAuctionRefunds(id);
        
        // Delete the player
        playerRepository.delete(player);
    }

    // ── Approve / Reject ──────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> approve(Long id, User user) {
        Player player = findPlayer(id);
        tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);
        player.setStatus(PlayerStatus.APPROVED);
        playerRepository.save(player);
        auctionPlayerService.autoPromoteToAuction(player.getId());
        return Map.of("id", player.getId(), "status", player.getStatus().name());
    }

    @Transactional
    public Map<String, Object> reject(Long id, User user) {
        Player player = findPlayer(id);
        tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);
        player.setStatus(PlayerStatus.REJECTED);
        playerRepository.save(player);
        auctionPlayerService.removeFromAuctionIfPresent(player.getId());
        return Map.of("id", player.getId(), "status", player.getStatus().name());
    }

    // ── Approve All / Reject All ──────────────────────────────────────────────
    @Transactional(timeout = 60)
    public Map<String, Object> approveAll(Long tournamentId, List<Long> playerIds, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Tournament tournament = tournamentService.findById(tournamentId);
        
        List<Player> playersToApprove = playerRepository.findAll().stream()
                .filter(p -> playerIds.contains(p.getId()))
                .filter(p -> p.getTournament().getId().equals(tournamentId))
                .toList();
        
        if (playersToApprove.isEmpty()) {
            return Map.of(
                "message", "No players found to approve",
                "approvedCount", 0,
                "status", "SUCCESS"
            );
        }
        
        for (Player player : playersToApprove) {
            player.setStatus(PlayerStatus.APPROVED);
            playerRepository.save(player);
            auctionPlayerService.autoPromoteToAuction(player.getId());
        }
        
        return Map.of(
            "message", "Players approved successfully",
            "approvedCount", playersToApprove.size(),
            "status", "SUCCESS"
        );
    }

    @Transactional(timeout = 60)
    public Map<String, Object> rejectAll(Long tournamentId, List<Long> playerIds, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Tournament tournament = tournamentService.findById(tournamentId);
        
        List<Player> playersToReject = playerRepository.findAll().stream()
                .filter(p -> playerIds.contains(p.getId()))
                .filter(p -> p.getTournament().getId().equals(tournamentId))
                .toList();
        
        if (playersToReject.isEmpty()) {
            return Map.of(
                "message", "No players found to reject",
                "rejectedCount", 0,
                "status", "SUCCESS"
            );
        }
        
        for (Player player : playersToReject) {
            player.setStatus(PlayerStatus.REJECTED);
            playerRepository.save(player);
            auctionPlayerService.removeFromAuctionIfPresent(player.getId());
        }
        
        return Map.of(
            "message", "Players rejected successfully",
            "rejectedCount", playersToReject.size(),
            "status", "SUCCESS"
        );
    }

    // ── Get Approved Players (only these can enter auction) ──────────────────
    public List<PlayerResponse> getApprovedByTournament(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Tournament tournament = tournamentService.findById(tournamentId);
        List<Player> approvedPlayers = playerRepository.findByTournamentAndStatus(tournament, PlayerStatus.APPROVED);
        return approvedPlayers.stream().map(this::toResponse).toList();
    }

    // ── Get Players by Status (for workflow monitoring) ──────────────────────
    public Map<String, Object> getPlayerStatsByTournament(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Tournament tournament = tournamentService.findById(tournamentId);
        
        List<Player> allPlayers = playerRepository.findByTournament(tournament);
        long pending = allPlayers.stream().filter(p -> p.getStatus() == PlayerStatus.PENDING).count();
        long approved = allPlayers.stream().filter(p -> p.getStatus() == PlayerStatus.APPROVED).count();
        long rejected = allPlayers.stream().filter(p -> p.getStatus() == PlayerStatus.REJECTED).count();
        
        return Map.of(
            "totalPlayers", allPlayers.size(),
            "pending", pending,
            "approved", approved,
            "rejected", rejected
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Player findPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    public PlayerResponse toResponse(Player p) {
        return PlayerResponse.builder()
                .id(p.getId())
                .playerNumber(p.getPlayerNumber())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dob(p.getDob())
                .role(p.getRole())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .tournamentId(p.getTournament().getId())
                .photoUrl(p.getPhoto())
                .paymentProofUrl(p.getPaymentProof())
                .build();
    }
}

