package com.bid.auction.controller;

import com.bid.auction.dto.response.OwnerViewResponse;
import com.bid.auction.entity.User;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.OwnerViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
@Tag(name = "Owner View", description = "Tournament owner dashboard view (public)")
public class OwnerViewController {

    private final OwnerViewService ownerViewService;
    private final AuthService authService;

    @GetMapping("/{tournamentId}/owner-view")
    @SecurityRequirements  // No authentication required - public endpoint
    @Operation(summary = "Get owner dashboard view",
            description = "Returns tournament summary, player stats, and team stats (public)")
    public ResponseEntity<OwnerViewResponse> getOwnerView(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId, 
            Authentication auth) {
        User user = null;
        if (auth != null && auth.isAuthenticated()) {
            user = authService.getUserByEmail(auth.getName());
        }
        return ResponseEntity.ok(ownerViewService.getOwnerView(tournamentId, user));
    }
}

