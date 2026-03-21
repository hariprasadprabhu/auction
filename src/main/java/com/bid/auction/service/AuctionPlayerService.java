package com.bid.auction.service;

import com.bid.auction.dto.request.AddToAuctionRequest;
import com.bid.auction.dto.request.AuctionPlayerRequest;
import com.bid.auction.dto.request.SellPlayerRequest;
import com.bid.auction.dto.response.AuctionPlayerResponse;
import com.bid.auction.entity.AuctionPlayer;
import com.bid.auction.entity.Player;
import com.bid.auction.entity.Team;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.AuctionStatus;
import com.bid.auction.enums.PlayerStatus;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.AuctionPlayerRepository;
import com.bid.auction.repository.PlayerRepository;
import com.bid.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuctionPlayerService {

    private final AuctionPlayerRepository auctionPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TournamentService tournamentService;
    private final TeamPurseService teamPurseService;

    // ── List ──────────────────────────────────────────────────────────────────
    public List<AuctionPlayerResponse> getAllByTournament(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        return auctionPlayerRepository.findByTournamentIdOrderBySortOrder(tournamentId)
                .stream().map(this::toResponse).toList();
    }

    // ── Get single ────────────────────────────────────────────────────────────
    public AuctionPlayerResponse getById(Long id, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);
        return toResponse(ap);
    }

    // ── Create ────────────────────────────────────────────────────────────────
    public AuctionPlayerResponse create(Long tournamentId, AuctionPlayerRequest req, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);

        int nextOrder = auctionPlayerRepository.findMaxSortOrderByTournamentId(tournamentId) + 1;

        AuctionPlayer ap = AuctionPlayer.builder()
                .playerNumber(req.getPlayerNumber())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .age(req.getAge())
                .city(req.getCity())
                .battingStyle(req.getBattingStyle())
                .bowlingStyle(req.getBowlingStyle())
                .role(req.getRole())
                .basePrice(req.getBasePrice())
                .auctionStatus(AuctionStatus.UPCOMING)
                .tournament(tournament)
                .sortOrder(nextOrder)
                .build();

        setPhoto(ap, req.getPhoto());
        return toResponse(auctionPlayerRepository.save(ap));
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public AuctionPlayerResponse update(Long id, AuctionPlayerRequest req, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);

        ap.setPlayerNumber(req.getPlayerNumber());
        ap.setFirstName(req.getFirstName());
        ap.setLastName(req.getLastName());
        ap.setAge(req.getAge());
        ap.setCity(req.getCity());
        ap.setBattingStyle(req.getBattingStyle());
        ap.setBowlingStyle(req.getBowlingStyle());
        ap.setRole(req.getRole());
        ap.setBasePrice(req.getBasePrice());
        if (req.getPhoto() != null && !req.getPhoto().isEmpty()) setPhoto(ap, req.getPhoto());

        return toResponse(auctionPlayerRepository.save(ap));
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);
        
        // If this auction player is sold to a team, recalculate the team's purse
        if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
            teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), ap.getTournament(), ap.getSoldPrice());
        }
        
        auctionPlayerRepository.delete(ap);
    }

    // ── Remove from auction pool (called internally when a player is rejected or deleted) ──
    // When a SOLD player is deleted, this method:
    // 1. Refunds the sold price back to the team's purse
    // 2. Recalculates team's available purse
    // 3. Recalculates required players count (remaining slots)
    // 4. Recalculates maxBid and reserved count based on new remaining slots
    // 5. KEEPS the auction player record (clears player reference) for team auction history
    // This ensures team's purchased players remain visible in their auction/team data
    @Transactional
    public void removeFromAuctionIfPresent(Long playerId) {
        // Get all auction players linked to this player
        List<AuctionPlayer> linkedAuctionPlayers = auctionPlayerRepository.findByPlayerId(playerId);
        
        // For each linked auction player, if it's sold to a team, refund the team and recalculate values
        for (AuctionPlayer ap : linkedAuctionPlayers) {
            if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
                // Player was SOLD to a team - refund and recalculate all team values
                // This updates:
                // - currentPurse (adds back the sold price)
                // - purseUsed (deducts the sold price)
                // - playersBought (decrements by 1)
                // - remainingSlots (increments by 1)
                // - reservedFund (recalculated based on new remainingSlots)
                // - maxBidPerPlayer (recalculated as current purse - reserved fund)
                // - availableForBidding (recalculated as current purse - reserved fund)
                teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), ap.getTournament(), ap.getSoldPrice());
                
                // IMPORTANT: Clear player reference but KEEP auction record
                // This preserves team's auction history showing they purchased this player
                // The auctionStatus, soldToTeam, and soldPrice remain for audit trail
                ap.setPlayer(null);
                auctionPlayerRepository.save(ap);
            } else {
                // For non-SOLD auction players, also clear player reference but keep record
                ap.setPlayer(null);
                auctionPlayerRepository.save(ap);
            }
        }
        // NOTE: We do NOT delete auction player records anymore
        // This ensures team auction data remains intact even after player deletion
    }

    // ── Delete player with auction refunds and team purse recalculation ────────
    // (Alias for removeFromAuctionIfPresent - kept for backwards compatibility)
    @Transactional
    public void deletePlayerWithAuctionRefunds(Long playerId) {
        removeFromAuctionIfPresent(playerId);
    }

    // ── Sync Player changes to linked AuctionPlayer rows ─────────────────────
    @Transactional
    public void syncFromPlayer(Player player) {
        List<AuctionPlayer> linked = auctionPlayerRepository.findByPlayerId(player.getId());
        if (linked.isEmpty()) return;
        for (AuctionPlayer ap : linked) {
            ap.setPlayerNumber(player.getPlayerNumber());
            ap.setFirstName(player.getFirstName());
            ap.setLastName(player.getLastName());
            ap.setRole(player.getRole());
            if (player.getPhoto() != null) {
                ap.setPhoto(player.getPhoto());
                ap.setPhotoContentType(player.getPhotoContentType());
            }
        }
        auctionPlayerRepository.saveAll(linked);
    }

    // ── Auto-promote on approval (called internally when a player is approved) ─
    @Transactional
    public void autoPromoteToAuction(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        Tournament tournament = player.getTournament();

        // Idempotent: skip if already in auction pool
        if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(playerId, tournament.getId())) {
            return;
        }

        int nextOrder = auctionPlayerRepository.findMaxSortOrderByTournamentId(tournament.getId()) + 1;

        AuctionPlayer ap = AuctionPlayer.builder()
                .player(player)
                .playerNumber(player.getPlayerNumber())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .role(player.getRole())
                .basePrice(tournament.getBasePrice())   // default from tournament; admin can update later
                .photo(player.getPhoto())
                .photoContentType(player.getPhotoContentType())
                .auctionStatus(AuctionStatus.UPCOMING)
                .tournament(tournament)
                .sortOrder(nextOrder)
                .build();

        auctionPlayerRepository.save(ap);
    }

    // ── Promote approved registered player into auction pool ──────────────────
    @Transactional
    public AuctionPlayerResponse promoteToAuction(Long playerId, AddToAuctionRequest req, User user) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        // Ownership check — the tournament this player belongs to must be owned by this user
        Tournament tournament = tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);

        // Only APPROVED players can enter the auction pool
        if (player.getStatus() != PlayerStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Player '" + player.getFirstName() + " " + player.getLastName()
                            + "' is not APPROVED (current status: " + player.getStatus() + "). "
                            + "Approve the player before adding to auction.");
        }

        // Prevent duplicate entries in the same tournament's auction pool
        if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(playerId, tournament.getId())) {
            throw new IllegalArgumentException(
                    "Player '" + player.getFirstName() + " " + player.getLastName()
                            + "' is already in the auction pool for this tournament.");
        }

        int nextOrder = auctionPlayerRepository.findMaxSortOrderByTournamentId(tournament.getId()) + 1;

        AuctionPlayer ap = AuctionPlayer.builder()
                .player(player)
                .playerNumber(player.getPlayerNumber())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .role(player.getRole())
                .age(req.getAge())
                .city(req.getCity())
                .battingStyle(req.getBattingStyle())
                .bowlingStyle(req.getBowlingStyle())
                .basePrice(req.getBasePrice())
                .photo(player.getPhoto())
                .photoContentType(player.getPhotoContentType())
                .auctionStatus(AuctionStatus.UPCOMING)
                .tournament(tournament)
                .sortOrder(nextOrder)
                .build();

        return toResponse(auctionPlayerRepository.save(ap));
    }

    // ── Sell ─────────────────────────────────────────────────────────────────
    @Transactional
    public AuctionPlayerResponse sell(Long id, SellPlayerRequest req, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        Tournament tournament = ap.getTournament();
        tournamentService.findAndVerifyOwner(tournament.getId(), user);

        // Validate team belongs to same tournament
        Team team = teamRepository.findByIdAndTournamentId(req.getTeamId(), tournament.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Team " + req.getTeamId() + " does not belong to this tournament"));

        // Validate soldPrice >= basePrice (skip check when basePrice is not set)
        if (ap.getBasePrice() != null && req.getSoldPrice() < ap.getBasePrice()) {
            throw new IllegalArgumentException(
                    "Sold price must be >= base price (" + ap.getBasePrice() + ")");
        }

        // Get team purse for accurate validation
        var teamPurse = teamPurseService.findByTeamAndTournament(team.getId(), tournament.getId());

        // Validate team slots not exceeded
        if (teamPurse.getPlayersBought() >= tournament.getPlayersPerTeam()) {
            throw new IllegalArgumentException(
                    "Team has already reached the maximum of " + tournament.getPlayersPerTeam() + " players");
        }

        // Validate sold price does not exceed max bid per player
        if (req.getSoldPrice() > teamPurse.getMaxBidPerPlayer()) {
            throw new IllegalArgumentException(
                    "Sold price (" + req.getSoldPrice() + ") exceeds max bid per player (" + 
                    teamPurse.getMaxBidPerPlayer() + ")");
        }

        // Validate team's available purse (after reserving minimum squad fund)
        if (teamPurse.getAvailableForBidding() < req.getSoldPrice()) {
            throw new IllegalArgumentException(
                    "Team's available purse for bidding (" + teamPurse.getAvailableForBidding() + 
                    ") is less than the sold price (" + req.getSoldPrice() + ")");
        }

        ap.setAuctionStatus(AuctionStatus.SOLD);
        ap.setSoldToTeam(team);
        ap.setSoldPrice(req.getSoldPrice());
        auctionPlayerRepository.save(ap);

        // Update team purse after player sale
        teamPurseService.updatePurseOnPlayerSold(team, tournament, req.getSoldPrice());

        // Update linked player status to SOLD (since players are tournament-specific)
        // Use targeted query to avoid I/O issues with large binary fields (photo, payment_proof)
        if (ap.getPlayer() != null) {
            playerRepository.updateStatusById(ap.getPlayer().getId(), PlayerStatus.SOLD);
        }

        return toResponse(ap);
    }

    // ── Mark Unsold ───────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> markUnsold(Long id, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);

        // If player was sold, revert team purse
        if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
            teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), ap.getTournament(), ap.getSoldPrice());
        }

        ap.setAuctionStatus(AuctionStatus.UNSOLD);
        ap.setSoldToTeam(null);
        ap.setSoldPrice(null);
        auctionPlayerRepository.save(ap);

        // Update linked player status to UNSOLD
        // Use targeted query to avoid I/O issues with large binary fields (photo, payment_proof)
        if (ap.getPlayer() != null) {
            playerRepository.updateStatusById(ap.getPlayer().getId(), PlayerStatus.UNSOLD);
        }

        return Map.of("id", ap.getId(), "auctionStatus", ap.getAuctionStatus().name());
    }

    // ── Requeue Unsold ────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> requeueUnsold(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        List<AuctionPlayer> unsold = auctionPlayerRepository
                .findByTournamentIdAndAuctionStatus(tournamentId, AuctionStatus.UNSOLD);
        unsold.forEach(ap -> ap.setAuctionStatus(AuctionStatus.UPCOMING));
        auctionPlayerRepository.saveAll(unsold);
        return Map.of("requeuedCount", unsold.size());
    }

    // ── Photo bytes ───────────────────────────────────────────────────────────
    public byte[] getPhoto(Long id) {
        AuctionPlayer ap = findAuctionPlayer(id);
        if (ap.getPhoto() == null)
            throw new ResourceNotFoundException("Photo not found for auction player: " + id);
        return ap.getPhoto();
    }

    public String getPhotoContentType(Long id) {
        AuctionPlayer ap = findAuctionPlayer(id);
        return ap.getPhotoContentType() != null ? ap.getPhotoContentType() : "image/jpeg";
    }

    // ── Reset Auction Players ─────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> resetAuctionPlayers(Long tournamentId, List<Long> playerIds, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        
        int processedCount = 0;
        int skippedCount = 0;
        
        for (Long playerId : playerIds) {
            AuctionPlayer ap = auctionPlayerRepository.findById(playerId).orElse(null);
            
            if (ap == null || !ap.getTournament().getId().equals(tournamentId)) {
                skippedCount++;
                continue;
            }
            
            // If player was sold, refund the team
            if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
                teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), ap.getTournament(), ap.getSoldPrice());
            }
            
            // Get the linked registered player to check status
            Player player = ap.getPlayer();
            
            // Always set auctionStatus to UPCOMING and clear sold data
            ap.setAuctionStatus(AuctionStatus.UPCOMING);
            ap.setSoldToTeam(null);
            ap.setSoldPrice(null);
            
            // Reset player status from SOLD/UNSOLD back to APPROVED
            // Use targeted query to avoid I/O issues with large binary fields (photo, payment_proof)
            if (player != null && (player.getStatus() == PlayerStatus.SOLD || player.getStatus() == PlayerStatus.UNSOLD)) {
                playerRepository.updateStatusById(player.getId(), PlayerStatus.APPROVED);
            }
            
            auctionPlayerRepository.save(ap);
            processedCount++;
        }
        
        return Map.of(
            "processedCount", processedCount,
            "skippedCount", skippedCount,
            "totalRequested", playerIds.size()
        );
    }

    // ── Reset Entire Auction ─────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> resetEntireAuction(Long tournamentId, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);
        
        // Get all auction players in this tournament
        List<AuctionPlayer> auctionPlayers = auctionPlayerRepository.findByTournamentId(tournamentId);
        
        // Step 1: Refund all sold players back to their teams
        for (AuctionPlayer ap : auctionPlayers) {
            if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
                teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), tournament, ap.getSoldPrice());
            }
        }
        
        // Step 1b: Reset player status from SOLD/UNSOLD back to APPROVED for all players
        // Use targeted query to avoid I/O issues with large binary fields (photo, payment_proof)
        for (AuctionPlayer ap : auctionPlayers) {
            Player player = ap.getPlayer();
            if (player != null && (player.getStatus() == PlayerStatus.SOLD || player.getStatus() == PlayerStatus.UNSOLD)) {
                playerRepository.updateStatusById(player.getId(), PlayerStatus.APPROVED);
            }
        }
        
        // Step 2: Delete all auction players
        auctionPlayerRepository.deleteAll(auctionPlayers);
        
        // Step 2b: Delete ALL team purses for this tournament BEFORE reinitializing
        // This prevents unique constraint violations when reinitializing
        teamPurseService.deleteTeamPursesForTournament(tournamentId);
        
        // Step 3: Get all approved players in the tournament
        List<Player> approvedPlayers = playerRepository.findByTournamentAndStatus(tournament, PlayerStatus.APPROVED);
        
        // Step 4: Re-insert approved players into auction pool with current tournament settings
        int nextOrder = 1;
        for (Player player : approvedPlayers) {
            AuctionPlayer ap = AuctionPlayer.builder()
                    .player(player)
                    .playerNumber(player.getPlayerNumber())
                    .firstName(player.getFirstName())
                    .lastName(player.getLastName())
                    .role(player.getRole())
                    .basePrice(tournament.getBasePrice())  // Uses current tournament base price
                    .photo(player.getPhoto())
                    .photoContentType(player.getPhotoContentType())
                    .auctionStatus(AuctionStatus.UPCOMING)
                    .tournament(tournament)
                    .sortOrder(nextOrder++)
                    .build();
            auctionPlayerRepository.save(ap);
        }
        
        // Step 5: Reset all team purses using CURRENT tournament settings
        // This recalculates based on latest:
        //   - purseAmount per team
        //   - playersPerTeam
        //   - basePrice
        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        for (Team team : teams) {
            teamPurseService.initializePurse(team, tournament);
        }
        
        // Prepare response with current tournament settings for transparency
        Long currentPurseAmount = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 
                ? tournament.getPurseAmount() : 1000000L;
        Integer currentPlayersPerTeam = tournament.getPlayersPerTeam() != null ? tournament.getPlayersPerTeam() : 11;
        Long currentBasePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        
        return Map.of(
            "deletedAuctionPlayers", auctionPlayers.size(),
            "readdedApprovedPlayers", approvedPlayers.size(),
            "teamsReset", teams.size(),
            "appliedTournamentSettings", Map.of(
                "purseAmount", currentPurseAmount,
                "playersPerTeam", currentPlayersPerTeam,
                "basePrice", currentBasePrice
            )
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private AuctionPlayer findAuctionPlayer(Long id) {
        return auctionPlayerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction player not found: " + id));
    }

    private void setPhoto(AuctionPlayer ap, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                ap.setPhoto(file.getBytes());
                ap.setPhotoContentType(file.getContentType());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read photo file");
            }
        }
    }

    public AuctionPlayerResponse toResponse(AuctionPlayer ap) {
        return AuctionPlayerResponse.builder()
                .id(ap.getId())
                .playerId(ap.getPlayer() != null ? ap.getPlayer().getId() : null)
                .playerNumber(ap.getPlayerNumber())
                .firstName(ap.getFirstName())
                .lastName(ap.getLastName())
                .age(ap.getAge())
                .city(ap.getCity())
                .battingStyle(ap.getBattingStyle())
                .bowlingStyle(ap.getBowlingStyle())
                .role(ap.getRole())
                .basePrice(ap.getBasePrice())
                .auctionStatus(ap.getAuctionStatus() != null ? ap.getAuctionStatus().name() : null)
                .soldToTeamId(ap.getSoldToTeam() != null ? ap.getSoldToTeam().getId() : null)
                .soldToTeamName(ap.getSoldToTeam() != null ? ap.getSoldToTeam().getName() : null)
                .soldPrice(ap.getSoldPrice())
                .tournamentId(ap.getTournament().getId())
                .sortOrder(ap.getSortOrder())
                .photoUrl(ap.getPhoto() != null ? "/api/auction-players/" + ap.getId() + "/photo" : null)
                .build();
    }
}

