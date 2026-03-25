package com.bid.auction.controller;
import com.bid.auction.dto.response.TeamPurseResponse;
import com.bid.auction.service.TeamPurseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequiredArgsConstructor
@Tag(name = "Team Purse", description = "Manage team financial details (purse, bid limits, reserved funds)")
@SecurityRequirement(name = "bearerAuth")
public class TeamPurseController {
    private final TeamPurseService teamPurseService;
    @GetMapping("/tournaments/{tournamentId}/team-purses")
    @Operation(summary = "Get all team purses for a tournament",
            description = "Returns financial details for all teams in a tournament")
    public ResponseEntity<List<TeamPurseResponse>> getAllTeamPurses(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        List<TeamPurseResponse> purses = teamPurseService.getAllTeamPurses(tournamentId);
        return ResponseEntity.ok(purses);
    }
    @GetMapping("/tournaments/{tournamentId}/teams/{teamId}/purse")
    @Operation(summary = "Get team purse details",
            description = "Returns financial details for a specific team in a tournament")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<TeamPurseResponse> getTeamPurse(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "Team ID") @PathVariable Long teamId) {
        TeamPurseResponse purse = teamPurseService.getPurse(teamId, tournamentId);
        return ResponseEntity.ok(purse);
    }
    @GetMapping("/teams/{teamId}/purses")
    @Operation(summary = "Get team purses across all tournaments",
            description = "Returns financial details for a team across all tournaments it participates in")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<TeamPurseResponse>> getTeamPurseAcrossTournaments(
            @Parameter(description = "Team ID") @PathVariable Long teamId) {
        List<TeamPurseResponse> purses = teamPurseService.getTeamPurseAcrossTournaments(teamId);
        return ResponseEntity.ok(purses);
    }
}
