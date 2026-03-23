package com.bid.auction.controller;

import com.bid.auction.dto.request.TournamentRequest;
import com.bid.auction.dto.response.TournamentResponse;
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
    public ResponseEntity<TournamentResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(tournamentService.getById(id, currentUser(auth)));
    }

    /**
     * Public endpoint for tournament details (no authentication required).
     * Used by unauthenticated users to view tournament info before registering.
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<TournamentResponse> getPublicDetails(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getPublicDetails(id));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        tournamentService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }


    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}

