package com.bid.auction.service;

import com.bid.auction.dto.request.TeamRequest;
import com.bid.auction.dto.response.TeamResponse;
import com.bid.auction.entity.Team;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TournamentService tournamentService;

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

        long count = teamRepository.countByTournamentId(tournamentId);
        String teamNumber = String.format("T%03d", count + 1);

        Team team = Team.builder()
                .teamNumber(teamNumber)
                .name(req.getName())
                .ownerName(req.getOwnerName())
                .mobileNumber(req.getMobileNumber())
                .tournament(tournament)
                .build();

        setLogo(team, req.getLogo());
        return toResponse(teamRepository.save(team));
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public TeamResponse update(Long id, TeamRequest req, User user) {
        Team team = findTeam(id);
        tournamentService.findAndVerifyOwner(team.getTournament().getId(), user);

        team.setName(req.getName());
        team.setOwnerName(req.getOwnerName());
        team.setMobileNumber(req.getMobileNumber());
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            setLogo(team, req.getLogo());
        }

        return toResponse(teamRepository.save(team));
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void delete(Long id, User user) {
        Team team = findTeam(id);
        tournamentService.findAndVerifyOwner(team.getTournament().getId(), user);
        teamRepository.delete(team);
    }

    // ── Logo bytes ────────────────────────────────────────────────────────────
    public byte[] getLogo(Long id) {
        Team team = findTeam(id);
        if (team.getLogo() == null) throw new ResourceNotFoundException("Logo not found for team: " + id);
        return team.getLogo();
    }

    public String getLogoContentType(Long id) {
        Team team = findTeam(id);
        return team.getLogoContentType() != null ? team.getLogoContentType() : "image/jpeg";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private void setLogo(Team team, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                team.setLogo(file.getBytes());
                team.setLogoContentType(file.getContentType());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read logo file");
            }
        }
    }

    public TeamResponse toResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .teamNumber(team.getTeamNumber())
                .name(team.getName())
                .ownerName(team.getOwnerName())
                .mobileNumber(team.getMobileNumber())
                .tournamentId(team.getTournament().getId())
                .logoUrl(team.getLogo() != null ? "/api/teams/" + team.getId() + "/logo" : null)
                .build();
    }
}

