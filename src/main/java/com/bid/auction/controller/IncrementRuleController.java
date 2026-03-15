package com.bid.auction.controller;

import com.bid.auction.dto.request.IncrementRuleRequest;
import com.bid.auction.dto.response.IncrementRuleResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.IncrementRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncrementRuleController {

    private final IncrementRuleService incrementRuleService;
    private final AuthService authService;

    @GetMapping("/api/tournaments/{tournamentId}/increment-rules")
    public ResponseEntity<List<IncrementRuleResponse>> getAll(
            @PathVariable Long tournamentId, Authentication auth) {
        return ResponseEntity.ok(
                incrementRuleService.getAllByTournament(tournamentId, currentUser(auth)));
    }

    @PostMapping("/api/tournaments/{tournamentId}/increment-rules")
    public ResponseEntity<IncrementRuleResponse> create(
            @PathVariable Long tournamentId,
            @Valid @RequestBody IncrementRuleRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incrementRuleService.create(tournamentId, request, currentUser(auth)));
    }

    @PutMapping("/api/increment-rules/{id}")
    public ResponseEntity<IncrementRuleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody IncrementRuleRequest request,
            Authentication auth) {
        return ResponseEntity.ok(incrementRuleService.update(id, request, currentUser(auth)));
    }

    @DeleteMapping("/api/increment-rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        incrementRuleService.delete(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication auth) {
        return authService.getUserByEmail(auth.getName());
    }
}

