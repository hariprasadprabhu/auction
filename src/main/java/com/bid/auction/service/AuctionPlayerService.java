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
    public void delete(Long id, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);
        auctionPlayerRepository.delete(ap);
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

        // Validate soldPrice >= basePrice
        if (req.getSoldPrice() < ap.getBasePrice()) {
            throw new IllegalArgumentException(
                    "Sold price must be >= base price (" + ap.getBasePrice() + ")");
        }

        // Validate team slots not exceeded
        long playersBought = auctionPlayerRepository.countBySoldToTeamId(team.getId());
        if (playersBought >= tournament.getPlayersPerTeam()) {
            throw new IllegalArgumentException(
                    "Team has already reached the maximum of " + tournament.getPlayersPerTeam() + " players");
        }

        // Validate remaining purse
        List<AuctionPlayer> soldPlayers = auctionPlayerRepository.findBySoldToTeamId(team.getId());
        long purseUsed = soldPlayers.stream()
                .mapToLong(p -> p.getSoldPrice() != null ? p.getSoldPrice() : 0L).sum();
        long purseRemaining = tournament.getPurseAmount() - purseUsed;
        if (purseRemaining < req.getSoldPrice()) {
            throw new IllegalArgumentException(
                    "Team's remaining purse (" + purseRemaining + ") is less than the sold price");
        }

        ap.setAuctionStatus(AuctionStatus.SOLD);
        ap.setSoldToTeam(team);
        ap.setSoldPrice(req.getSoldPrice());
        auctionPlayerRepository.save(ap);
        return toResponse(ap);
    }

    // ── Mark Unsold ───────────────────────────────────────────────────────────
    public Map<String, Object> markUnsold(Long id, User user) {
        AuctionPlayer ap = findAuctionPlayer(id);
        tournamentService.findAndVerifyOwner(ap.getTournament().getId(), user);
        ap.setAuctionStatus(AuctionStatus.UNSOLD);
        ap.setSoldToTeam(null);
        ap.setSoldPrice(null);
        auctionPlayerRepository.save(ap);
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

