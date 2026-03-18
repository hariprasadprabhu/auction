package com.bid.auction.controller;

import com.bid.auction.dto.request.TeamRequest;
import com.bid.auction.dto.response.TeamResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Manage teams within a tournament")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;
    private final AuthService authService;

    // ── Tournament-scoped ─────────────────────────────────────────────────────

    @GetMapping("/tournaments/{tournamentId}/teams")
    @Operation(summary = "Get all teams for a tournament")
    @ApiResponse(responseCode = "200", description = "List of teams",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = TeamResponse.class))))
    public ResponseEntity<List<TeamResponse>> getAllByTournament(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            Authentication auth) {
        return ResponseEntity.ok(teamService.getAllByTournament(tournamentId, currentUser(auth)));
    }

    @PostMapping(value = "/tournaments/{tournamentId}/teams",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a team in a tournament",
               description = "Multipart form. `logo` is required (image/jpeg or image/png, max 2 MB).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Team created",
            content = @Content(schema = @Schema(implementation = TeamResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<TeamResponse> create(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Valid @ModelAttribute TeamRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.create(tournamentId, request, currentUser(auth)));
    }

    // ── Direct team endpoints ─────────────────────────────────────────────────

    @GetMapping("/teams/{id}")
    @Operation(summary = "Get team by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team found",
            content = @Content(schema = @Schema(implementation = TeamResponse.class))),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TeamResponse> getById(
            @Parameter(description = "Team ID") @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(teamService.getById(id, currentUser(auth)));
    }

    @PutMapping(value = "/teams/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update team",
               description = "Multipart form. If `logo` is omitted the existing logo is kept.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team updated",
            content = @Content(schema = @Schema(implementation = TeamResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TeamResponse> update(
            @Parameter(description = "Team ID") @PathVariable Long id,
            @Valid @ModelAttribute TeamRequest request,
            Authentication auth) {
        return ResponseEntity.ok(teamService.update(id, request, currentUser(auth)));
    }

    @DeleteMapping("/teams/{id}")
    @Operation(summary = "Delete team")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Team ID") @PathVariable Long id,
            Authentication auth) {
        teamService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{id}/logo")
    @SecurityRequirements  // public
    @Operation(summary = "Get team logo image (public)",
               description = "Returns raw image bytes. No authentication required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image bytes"),
        @ApiResponse(responseCode = "404", description = "No logo found")
    })
    public ResponseEntity<byte[]> getLogo(
            @Parameter(description = "Team ID") @PathVariable Long id) {
        byte[] logo = teamService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(teamService.getLogoContentType(id)))
                .body(logo);
    }

    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}
