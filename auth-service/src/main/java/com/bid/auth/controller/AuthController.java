package com.bid.auth.controller;

import com.bid.auth.dto.request.LoginRequest;
import com.bid.auth.dto.request.PasswordResetRequest;
import com.bid.auth.dto.request.RegisterRequest;
import com.bid.auth.dto.response.AuthResponse;
import com.bid.auth.dto.response.UserResponse;
import com.bid.auth.service.AuthService;
import com.bid.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, sign in, and manage account verification")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register an organizer account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Sign in and receive a JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh an access token")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refreshAccessToken(authHeader.substring(7)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current account profile")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        return ResponseEntity.ok(authService.getUserResponseByEmail(auth.getName()));
    }

    @PostMapping("/email/send-otp")
    @Operation(summary = "Send an email verification OTP")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        String newEmail = body != null ? body.get("email") : null;
        emailVerificationService.sendVerificationOtp(auth.getName(), newEmail);
        String target = newEmail != null && !newEmail.isBlank() ? newEmail.trim() : "your email address";
        return ResponseEntity.ok(Map.of(
                "message", "Verification OTP sent to " + target + ". It expires in 15 minutes."));
    }

    @PostMapping("/email/verify-otp")
    @Operation(summary = "Verify an email OTP")
    public ResponseEntity<AuthResponse> verifyOtp(
            @RequestBody Map<String, String> body, Authentication auth) {
        String otp = body.get("otp");
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("Field 'otp' is required.");
        }
        return ResponseEntity.ok(emailVerificationService.verifyOtp(auth.getName(), otp.trim()));
    }

    @PostMapping("/password/send-otp")
    @SecurityRequirements
    @Operation(summary = "Send a password reset OTP")
    public ResponseEntity<Map<String, String>> sendPasswordResetOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Field 'email' is required.");
        }
        emailVerificationService.sendPasswordResetOtp(email.trim());
        return ResponseEntity.ok(Map.of(
                "message", "Password reset OTP sent to your email address. It expires in 15 minutes."));
    }

    @PostMapping({"/password/verify-otp", "/password/reset"})
    @SecurityRequirements
    @Operation(summary = "Verify a password reset OTP and set a new password")
    public ResponseEntity<AuthResponse> verifyPasswordResetOtp(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(emailVerificationService.resetPassword(request));
    }
}
