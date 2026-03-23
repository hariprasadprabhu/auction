package com.bid.auction.service;

import com.bid.auction.dto.request.TournamentRequest;
import com.bid.auction.dto.response.TournamentResponse;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.TournamentStatus;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.IncrementRuleRepository;
import com.bid.auction.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
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
                .createdBy(user)
                .build();

        return toResponse(tournamentRepository.save(t));
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public TournamentResponse update(Long id, TournamentRequest req, User user) {
        Tournament t = findAndVerifyOwner(id, user);

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

        Tournament updatedTournament = tournamentRepository.save(t);

        // Recalculate all team purses if financial details changed
        teamPurseService.recalculateAllTeamPurses(updatedTournament);

        return toResponse(updatedTournament);
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
                .build();
    }
}

