package com.bid.auction.controller;

import com.bid.auction.dto.request.SponsorRequest;
import com.bid.auction.dto.response.SponsorResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.SponsorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Sponsors", description = "Manage sponsors for tournaments")
@SecurityRequirement(name = "bearerAuth")
public class SponsorController {

    private final SponsorService sponsorService;
    private final AuthService authService;

    /**
     * Add multiple sponsors to a tournament.
     * Only tournament owner can add sponsors.
     */
    @PostMapping("/tournaments/{tournamentId}/sponsors")
    @Operation(summary = "Add sponsors to a tournament",
               description = "Add a list of sponsors to the tournament. Only tournament owner can perform this action.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sponsors added successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SponsorResponse.class)))),
        @ApiResponse(responseCode = "403", description = "Not authorized - must be tournament owner"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<List<SponsorResponse>> addSponsors(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Valid @RequestBody List<SponsorRequest> requests,
            Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sponsorService.addSponsors(tournamentId, requests, user));
    }

    /**
     * Get all sponsors for a specific tournament.
     * Only tournament owner can view sponsors.
     */
    @GetMapping("/tournaments/{tournamentId}/sponsors")
    @Operation(summary = "Get sponsors for a tournament",
               description = "Retrieve all sponsors for the specified tournament. Only tournament owner can perform this action.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of sponsors",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SponsorResponse.class)))),
        @ApiResponse(responseCode = "403", description = "Not authorized - must be tournament owner"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<List<SponsorResponse>> getSponsors(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(sponsorService.getByTournament(tournamentId, user));
    }

    /**
     * Get all sponsors for a tournament (public endpoint).
     * No authentication required - used for displaying sponsors on tournament details page.
     */
    @GetMapping("/tournaments/{tournamentId}/sponsors/public")
    @Operation(summary = "Get sponsors for a tournament (public)",
               description = "Retrieve all sponsors for the specified tournament. No authentication required.")
    @ApiResponse(responseCode = "200", description = "List of sponsors",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = SponsorResponse.class))))
    public ResponseEntity<List<SponsorResponse>> getPublicSponsors(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return ResponseEntity.ok(sponsorService.getPublicSponsors(tournamentId));
    }

    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}


