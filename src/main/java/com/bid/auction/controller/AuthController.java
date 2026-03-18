package com.bid.auction.controller;

import com.bid.auction.dto.request.LoginRequest;
import com.bid.auction.dto.request.RegisterRequest;
import com.bid.auction.dto.response.AuthResponse;
import com.bid.auction.dto.response.UserResponse;
import com.bid.auction.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login to get a JWT token")
@SecurityRequirements   // override global JWT requirement — these endpoints are public
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new organizer account",
        description = "Creates a new ADMIN account. Returns the saved user (no token). Call /login next."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email already in use",
            content = @Content(schema = @Schema(example = """
                {"errors":{"email":"Email is already in use","password":"must match pattern"}}
            """)))
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login and receive JWT token",
        description = "Returns a Bearer JWT token. Use it in the **Authorize** button (🔒) above."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — copy the `token` value",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Bad credentials",
            content = @Content(schema = @Schema(example = """
                {"error":"UNAUTHORIZED","message":"Bad credentials","timestamp":"..."}
            """)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
