package com.bid.auction.service;

import com.bid.auction.dto.request.TeamRequest;
import com.bid.auction.dto.response.TeamResponse;
import com.bid.auction.entity.Team;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.AuctionPlayerRepository;
import com.bid.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TournamentService tournamentService;
    private final TeamPurseService teamPurseService;
    private final AuctionPlayerRepository auctionPlayerRepository;

    // ── List ──────────────────────────────────────────────────────────────────
    public List<TeamResponse> getAllByTournament(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        return teamRepository.findByTournamentId(tournamentId)
                .stream().map(this::toResponse).toList();
    }

    // ── Get single ────────────────────────────────────────────────────────────
    public TeamResponse getById(Long id, User user) {
        Team team = findTeam(id);
        tournamentService.findAndVerifyOwner(team.getTournament().getId(), user);
        return toResponse(team);
    }

    // ── Create ────────────────────────────────────────────────────────────────
    public TeamResponse create(Long tournamentId, TeamRequest req, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);

        // Check if teamAllowed limit has been reached
        long currentTeamCount = teamRepository.countByTournamentId(tournamentId);
        if (tournament.getTeamAllowed() != null && currentTeamCount >= tournament.getTeamAllowed()) {
            throw new IllegalArgumentException("Reached maximum allowed teams: " + tournament.getTeamAllowed());
        }

        String teamNumber = String.format("T%03d", currentTeamCount + 1);

        Team team = Team.builder()
                .teamNumber(teamNumber)
                .name(req.getName())
                .ownerName(req.getOwnerName())
                .mobileNumber(req.getMobileNumber())
                .logo(req.getLogo())
                .tournament(tournament)
                .build();

        Team savedTeam = teamRepository.save(team);

        // Initialize team purse when team is created
        teamPurseService.initializePurse(savedTeam, tournament);

        return toResponse(savedTeam);
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public TeamResponse update(Long id, TeamRequest req, User user) {
        Team team = findTeam(id);
        tournamentService.findAndVerifyOwner(team.getTournament().getId(), user);

        team.setName(req.getName());
        team.setOwnerName(req.getOwnerName());
        team.setMobileNumber(req.getMobileNumber());
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            team.setLogo(req.getLogo());
        }

        return toResponse(teamRepository.save(team));
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, User user) {
        Team team = findTeam(id);
        Long tournamentId = team.getTournament().getId();
        tournamentService.findAndVerifyOwner(tournamentId, user);
        
        // Delete all AuctionPlayer records linked to this team (soldToTeam)
        // This includes all auction players bought by this team
        auctionPlayerRepository.deleteBySoldToTeamId(id);
        
        // Delete team purse records for this team in its tournament
        teamPurseService.deleteTeamPurseInTournament(id, tournamentId);
        
        // Delete the team (cascade delete via @OneToMany relationship)
        teamRepository.delete(team);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    public TeamResponse toResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .teamNumber(team.getTeamNumber())
                .name(team.getName())
                .ownerName(team.getOwnerName())
                .mobileNumber(team.getMobileNumber())
                .tournamentId(team.getTournament().getId())
                .logoUrl(team.getLogo())
                .build();
    }
}

