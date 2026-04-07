package com.bid.auction.controller;

import com.bid.auction.dto.request.LoginRequest;
import com.bid.auction.dto.request.PasswordResetRequest;
import com.bid.auction.dto.request.RegisterRequest;
import com.bid.auction.dto.response.AuthResponse;
import com.bid.auction.dto.response.UserResponse;
import com.bid.auction.service.AuthService;
import com.bid.auction.service.EmailVerificationService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login to get a JWT token")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // ── Public endpoints (no JWT required) ────────────────────────────────────

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a new organizer account",
        description = "Creates a new ADMIN account. Returns the saved user (no token). Call /login next.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email already in use")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login and receive JWT token",
        description = "Returns a Bearer JWT token. Use it in the **Authorize** button (🔒) above.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — copy the `token` value",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token",
        description = "Send the current (possibly expired) Bearer token in the Authorization header to receive a new one.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New token issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing or malformed Authorization header"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refreshAccessToken(authHeader.substring(7)));
    }

    // ── Authenticated endpoints (JWT required) ────────────────────────────────

    /**
     * GET /auth/me — Returns the current user's profile including {@code emailVerified}.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile",
        description = "Returns profile of the authenticated user including the email-verified status.")
    @ApiResponse(responseCode = "200", description = "Profile returned",
        content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<UserResponse> me(Authentication auth) {
        return ResponseEntity.ok(authService.getUserResponseByEmail(auth.getName()));
    }

    /**
     * POST /auth/email/send-otp
     * <p>Generates a 6-digit OTP and emails it to the authenticated user.
     * <p>Optional body: {@code { "email": "new@example.com" }}
     * <ul>
     *   <li>If {@code email} is provided and differs from the current address,
     *       uniqueness is checked and the OTP is sent to the <em>new</em> email.
     *       The address is only persisted after the OTP is verified via
     *       {@code /auth/email/verify-otp}.</li>
     *   <li>If {@code email} is omitted the OTP is sent to the current email
     *       (which must not already be verified).</li>
     * </ul>
     */
    @PostMapping("/email/send-otp")
    @Operation(summary = "Send email verification OTP",
        description = "Sends a 6-digit OTP to the user's email. Optionally accepts `{ \"email\": \"new@example.com\" }` " +
                      "to change the address — uniqueness is validated and the OTP is sent to the new address. " +
                      "The change is committed only after a successful verify-otp call.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "400", description = "New email already in use"),
        @ApiResponse(responseCode = "409", description = "Current email already verified (and no new email given)")
    })
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String newEmail = (body != null) ? body.get("email") : null;
        emailVerificationService.sendVerificationOtp(auth.getName(), newEmail);
        String target = (newEmail != null && !newEmail.isBlank()) ? newEmail.trim() : "your email address";
        return ResponseEntity.ok(Map.of("message",
                "Verification OTP sent to " + target + ". It expires in 15 minutes."));
    }

    /**
     * POST /auth/email/verify-otp — Validates the submitted OTP.
     * On success sets {@code emailVerified = true} and clears the stored OTP.
     * Body: {@code { "otp": "123456" }}
     */
    @PostMapping("/email/verify-otp")
    @Operation(summary = "Verify email OTP",
        description = "Checks the submitted OTP. Marks the user's email as verified on success. Returns a fresh JWT token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully — new token issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @ApiResponse(responseCode = "409", description = "Email already verified")
    })
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody Map<String, String> body,
                                                   Authentication auth) {
        String otp = body.get("otp");
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("Field 'otp' is required.");
        }
        return ResponseEntity.ok(emailVerificationService.verifyOtp(auth.getName(), otp.trim()));
    }

    // ── Password Reset (public, no JWT required) ──────────────────────────────

    /**
     * POST /auth/password/send-otp
     * Sends a 6-digit password-reset OTP to the given email address.
     * Body: {@code { "email": "user@example.com" }}
     */
    @PostMapping("/password/send-otp")
    @SecurityRequirements
    @Operation(summary = "Send password-reset OTP",
        description = "Generates a 6-digit OTP valid for 15 minutes and emails it to the user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "404", description = "No account found for the given email")
    })
    public ResponseEntity<Map<String, String>> sendPasswordResetOtp(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Field 'email' is required.");
        }
        emailVerificationService.sendPasswordResetOtp(email.trim());
        return ResponseEntity.ok(Map.of("message",
                "Password reset OTP sent to your email address. It expires in 15 minutes."));
    }

    /**
     * POST /auth/password/verify-otp  (alias: /auth/password/reset)
     * Validates the OTP against the stored hash for the given email,
     * then sets the new password. No userId required.
     * Body: {@code { "email": "user@example.com", "newPassword": "...", "otp": "123456" }}
     */
    @PostMapping({"/password/verify-otp", "/password/reset"})
    @SecurityRequirements
    @Operation(summary = "Verify password-reset OTP and set new password",
        description = "Matches the OTP against the stored hash for the given email. " +
                      "On success sets the new password, marks emailVerified = true, and returns a fresh JWT token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully — new token issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> verifyPasswordResetOtp(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(emailVerificationService.resetPassword(request));
    }
}
