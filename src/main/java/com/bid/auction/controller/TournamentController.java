package com.bid.auction.controller;

import com.bid.auction.dto.request.TournamentRequest;
import com.bid.auction.dto.request.RegistrationConfigRequest;
import com.bid.auction.dto.response.TournamentResponse;
import com.bid.auction.dto.response.RegistrationConfigResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(tournamentService.getAll(currentUser(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getPublicDetails(id));
    }

    /**
     * Public endpoint for tournament details (no authentication required).
     * Used by unauthenticated users to view tournament info before registering.
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<TournamentResponse> getPublicDetails(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getPublicDetails(id));
    }

    /**
     * Public endpoint for the player registration page.
     * Returns tournament details including the {@code playerRegistrationOpen} flag so the
     * frontend can show "registration is closed" when {@code false}.
     * No authentication required.
     */
    @GetMapping("/{id}/registration")
    public ResponseEntity<TournamentResponse> getRegistrationInfo(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getPublicDetails(id));
    }

    /**
     * Toggle player self-registration open/closed for this tournament.
     * Only the tournament owner can call this endpoint.
     * Body: {@code { "playerRegistrationOpen": true | false }}
     */
    @PatchMapping("/{id}/registration")
    public ResponseEntity<TournamentResponse> setRegistrationOpen(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Boolean> body,
            Authentication auth) {
        Boolean open = body.get("playerRegistrationOpen");
        if (open == null) {
            throw new IllegalArgumentException("Field 'playerRegistrationOpen' is required.");
        }
        return ResponseEntity.ok(tournamentService.setPlayerRegistrationOpen(id, open, currentUser(auth)));
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(
            @Valid @RequestBody TournamentRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentService.create(request, currentUser(auth)));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<TournamentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(tournamentService.update(id, request, currentUser(auth)));
    }

    /**
     * PUBLIC — no authentication required.
     * Returns which player-registration fields are mandatory for this tournament.
     * Used by the registration form to know which fields to mark as required.
     *
     * GET /tournaments/{id}/registration-config
     */
    @GetMapping("/{id}/registration-config")
    public ResponseEntity<RegistrationConfigResponse> getRegistrationConfigPublic(
            @PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getRegistrationConfigPublic(id));
    }

    /**
     * Update which player-registration fields are mandatory for this tournament.
     * Send only the fields you want to change — omitted fields are left unchanged.
     * Only the tournament owner can call this.
     *
     * PATCH /tournaments/{id}/registration-config
     * Body: { "requireMobileNumber": true, "requireDob": true }
     */
    @PatchMapping("/{id}/registration-config")
    public ResponseEntity<RegistrationConfigResponse> updateRegistrationConfig(
            @PathVariable Long id,
            @RequestBody RegistrationConfigRequest request,
            Authentication auth) {
        return ResponseEntity.ok(tournamentService.updateRegistrationConfig(id, request, currentUser(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        tournamentService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }


    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}

