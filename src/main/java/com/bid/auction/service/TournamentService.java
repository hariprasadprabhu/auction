package com.bid.auction.service;

import com.bid.auction.dto.request.TournamentRequest;
import com.bid.auction.dto.request.RegistrationConfigRequest;
import com.bid.auction.dto.response.TournamentResponse;
import com.bid.auction.dto.response.RegistrationConfigResponse;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.TournamentStatus;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.IncrementRuleRepository;
import com.bid.auction.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TeamPurseService teamPurseService;
    private final IncrementRuleRepository incrementRuleRepository;

    /**
     * Injected lazily to break the circular dependency:
     * TournamentService → AuctionPlayerService → TournamentService
     */
    @Autowired @Lazy
    private AuctionPlayerService auctionPlayerService;

    // ── List ──────────────────────────────────────────────────────────────────
    public List<TournamentResponse> getAll(User user) {
        return tournamentRepository.findByCreatedBy(user)
                .stream().map(this::toResponse).toList();
    }

    // ── Get single ────────────────────────────────────────────────────────
    public TournamentResponse getById(Long id, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        return toResponse(t);
    }

    /**
     * Public method to get tournament details without authentication.
     * Used by unauthenticated users viewing tournament info for registration.
     */
    public TournamentResponse getPublicDetails(Long id) {
        Tournament t = findById(id);
        return toResponse(t);
    }

    // ── Create ────────────────────────────────────────────────────────────────
    public TournamentResponse create(TournamentRequest req, User user) {
        Tournament t = Tournament.builder()
                .name(req.getName())
                .date(req.getDate())
                .sport(req.getSport())
                .totalTeams(req.getTotalTeams())
                .totalPlayers(req.getTotalPlayers())
                .teamAllowed(2)  // Always set to 2 on tournament creation
                .purseAmount(req.getPurseAmount())
                .playersPerTeam(req.getPlayersPerTeam())
                .basePrice(req.getBasePrice())
                .initialIncrement(req.getInitialIncrement())
                .status(parseStatus(req.getStatus(), TournamentStatus.UPCOMING))
                .logo(req.getLogo())
                .paymentProofRequired(req.getPaymentProofRequired())
                .paymentMethod(req.getPaymentMethod())
                .paymentNumber(req.getPaymentNumber())
                .amountToPay(req.getAmountToPay())
                .playerRegistrationOpen(req.getPlayerRegistrationOpen() != null ? req.getPlayerRegistrationOpen() : true)
                .createdBy(user)
                .build();

        return toResponse(tournamentRepository.save(t));
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Transactional
    public TournamentResponse update(Long id, TournamentRequest req, User user) {
        Tournament t = findAndVerifyOwner(id, user);

        // Capture current values of fields that affect the auction before overwriting them
        Long oldBasePrice      = t.getBasePrice();
        Long oldInitIncrement  = t.getInitialIncrement();
        Integer oldPlayersPerTeam = t.getPlayersPerTeam();
        Long oldPurseAmount    = t.getPurseAmount();

        // ── Auction-date edit-limit enforcement ───────────────────────────────
        // The date field (tournament / auction date) may be changed at most once
        // after creation.  canEditAuctionDate starts as true (creation = edit #1).
        // The first post-creation change consumes that allowance (sets flag false).
        // Any further attempt to change the date is rejected.
        boolean auctionDateChanging = !java.util.Objects.equals(req.getDate(), t.getDate());
        if (auctionDateChanging) {
            if (Boolean.FALSE.equals(t.getCanEditAuctionDate())) {
                throw new IllegalStateException(
                        "Auction date has already been modified once and cannot be changed again.");
            }
            // Consume the one allowed post-creation edit
            t.setCanEditAuctionDate(false);
        }
        // ─────────────────────────────────────────────────────────────────────

        t.setName(req.getName());
        t.setDate(req.getDate());
        t.setSport(req.getSport());
        t.setTotalTeams(req.getTotalTeams());
        t.setTotalPlayers(req.getTotalPlayers());
        t.setPurseAmount(req.getPurseAmount());
        t.setPlayersPerTeam(req.getPlayersPerTeam());
        t.setBasePrice(req.getBasePrice());
        t.setInitialIncrement(req.getInitialIncrement());
        if (req.getStatus() != null) {
            t.setStatus(parseStatus(req.getStatus(), t.getStatus()));
        }
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            t.setLogo(req.getLogo());
        }
        if (req.getPaymentProofRequired() != null) {
            t.setPaymentProofRequired(req.getPaymentProofRequired());
        }
        if (req.getPaymentMethod() != null) {
            t.setPaymentMethod(req.getPaymentMethod());
        }
        if (req.getPaymentNumber() != null) {
            t.setPaymentNumber(req.getPaymentNumber());
        }
        if (req.getAmountToPay() != null) {
            t.setAmountToPay(req.getAmountToPay());
        }
        if (req.getPlayerRegistrationOpen() != null) {
            t.setPlayerRegistrationOpen(req.getPlayerRegistrationOpen());
        }

        Tournament updatedTournament = tournamentRepository.save(t);

        // Determine whether any auction-critical field actually changed BEFORE
        // touching team purses, so we can choose the right code path below.
        boolean auctionFieldChanged =
                !java.util.Objects.equals(req.getBasePrice(),       oldBasePrice)      ||
                !java.util.Objects.equals(req.getInitialIncrement(), oldInitIncrement) ||
                !java.util.Objects.equals(req.getPlayersPerTeam(),  oldPlayersPerTeam) ||
                !java.util.Objects.equals(req.getPurseAmount(),     oldPurseAmount);

        if (auctionFieldChanged) {
            // Full reset: wipes all auction players, resets player statuses, and
            // re-initialises every team purse from scratch with the new settings.
            // This makes a preceding recalculateAllTeamPurses call unnecessary —
            // calling both in the same transaction was the root cause of detached-
            // entity / stale-JPA-cache bugs in the previous implementation.
            auctionPlayerService.resetEntireAuctionInternal(updatedTournament);
        } else {
            // No auction reset needed — just re-crunch the purse figures so they
            // reflect any non-critical changes (e.g. name, date, sport).
            teamPurseService.recalculateAllTeamPurses(updatedTournament);
        }

        return toResponse(updatedTournament);
    }

    // ── Toggle player registration ────────────────────────────────────────────
    @Transactional
    public TournamentResponse setPlayerRegistrationOpen(Long id, boolean open, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        t.setPlayerRegistrationOpen(open);
        return toResponse(tournamentRepository.save(t));
    }

    // ── Registration field config ─────────────────────────────────────────────

    /**
     * Public (no auth) – used by the player registration form to know which
     * fields are mandatory before the player submits.
     */
    public RegistrationConfigResponse getRegistrationConfigPublic(Long id) {
        Tournament t = findById(id);
        return toConfigResponse(t);
    }

    /** GET /tournaments/{id}/registration-config  (owner-only) */
    public RegistrationConfigResponse getRegistrationConfig(Long id, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        return toConfigResponse(t);
    }

    /** PATCH /tournaments/{id}/registration-config  (owner-only) */
    @Transactional
    public RegistrationConfigResponse updateRegistrationConfig(Long id, RegistrationConfigRequest req, User user) {
        Tournament t = findAndVerifyOwner(id, user);

        if (req.getRequireLastName()         != null) t.setRegRequireLastName(req.getRequireLastName());
        if (req.getRequireDob()              != null) t.setRegRequireDob(req.getRequireDob());
        if (req.getRequirePhoto()            != null) t.setRegRequirePhoto(req.getRequirePhoto());
        if (req.getRequirePaymentProof()     != null) {
            t.setPaymentProofRequired(req.getRequirePaymentProof());
        }
        if (req.getRequireMobileNumber()     != null) t.setRegRequireMobileNumber(req.getRequireMobileNumber());
        if (req.getRequireHandedness()       != null) t.setRegRequireHandedness(req.getRequireHandedness());
        if (req.getRequireTshirtSize()       != null) t.setRegRequireTshirtSize(req.getRequireTshirtSize());
        if (req.getRequireTrouserSize()      != null) t.setRegRequireTrouserSize(req.getRequireTrouserSize());
        if (req.getRequireJerseyNumber()     != null) t.setRegRequireJerseyNumber(req.getRequireJerseyNumber());
        if (req.getRequireSleeveType()       != null) t.setRegRequireSleeveType(req.getRequireSleeveType());
        if (req.getRequirePlayerLocation()   != null) t.setRegRequirePlayerLocation(req.getRequirePlayerLocation());
        if (req.getRequireLastSeasonPlayed() != null) t.setRegRequireLastSeasonPlayed(req.getRequireLastSeasonPlayed());
        if (req.getRequireLastSeasonTeam()   != null) t.setRegRequireLastSeasonTeam(req.getRequireLastSeasonTeam());
        if (req.getRequireBowlingStyle()     != null) t.setRegRequireBowlingStyle(req.getRequireBowlingStyle());

        return toConfigResponse(tournamentRepository.save(t));
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        
        // Delete all increment rules for this tournament
        incrementRuleRepository.deleteByTournamentId(id);
        
        // Delete the tournament (cascade will handle teams, players, auction players, etc.)
        tournamentRepository.delete(t);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public Tournament findAndVerifyOwner(Long id, User user) {
        Tournament t = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
        if (!t.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this tournament");
        }
        return t;
    }

    /** Used by other services that only need the entity (no ownership check). */
    public Tournament findById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }

    private TournamentStatus parseStatus(String statusStr, TournamentStatus defaultVal) {
        if (statusStr == null || statusStr.isBlank()) return defaultVal;
        try {
            return TournamentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }

    public TournamentResponse toResponse(Tournament t) {
        return TournamentResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .date(t.getDate())
                .sport(t.getSport())
                .totalTeams(t.getTotalTeams())
                .totalPlayers(t.getTotalPlayers())
                .teamAllowed(t.getTeamAllowed())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .purseAmount(t.getPurseAmount())
                .playersPerTeam(t.getPlayersPerTeam())
                .basePrice(t.getBasePrice())
                .initialIncrement(t.getInitialIncrement())
                .logoUrl(t.getLogo())
                .paymentProofRequired(t.getPaymentProofRequired())
                .paymentMethod(t.getPaymentMethod())
                .paymentNumber(t.getPaymentNumber())
                .amountToPay(t.getAmountToPay())
                .canEditAuctionDate(t.getCanEditAuctionDate())
                .playerRegistrationOpen(t.getPlayerRegistrationOpen())
                .registrationConfig(toConfigResponse(t))
                .build();
    }

    private RegistrationConfigResponse toConfigResponse(Tournament t) {
        return RegistrationConfigResponse.builder()
                .requireLastName(Boolean.TRUE.equals(t.getRegRequireLastName()))
                .requireDob(Boolean.TRUE.equals(t.getRegRequireDob()))
                .requirePhoto(Boolean.TRUE.equals(t.getRegRequirePhoto()))
                .requirePaymentProof(Boolean.TRUE.equals(t.getPaymentProofRequired()))
                .requireMobileNumber(Boolean.TRUE.equals(t.getRegRequireMobileNumber()))
                .requireHandedness(Boolean.TRUE.equals(t.getRegRequireHandedness()))
                .requireTshirtSize(Boolean.TRUE.equals(t.getRegRequireTshirtSize()))
                .requireTrouserSize(Boolean.TRUE.equals(t.getRegRequireTrouserSize()))
                .requireJerseyNumber(Boolean.TRUE.equals(t.getRegRequireJerseyNumber()))
                .requireSleeveType(Boolean.TRUE.equals(t.getRegRequireSleeveType()))
                .requirePlayerLocation(Boolean.TRUE.equals(t.getRegRequirePlayerLocation()))
                .requireLastSeasonPlayed(Boolean.TRUE.equals(t.getRegRequireLastSeasonPlayed()))
                .requireLastSeasonTeam(Boolean.TRUE.equals(t.getRegRequireLastSeasonTeam()))
                .requireBowlingStyle(Boolean.TRUE.equals(t.getRegRequireBowlingStyle()))
                .build();
    }
}

