package com.bid.auth.controller;

import com.bid.auth.dto.response.UserResponse;
import com.bid.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

    private final AuthService authService;

    @Value("${app.internal-api-token:}")
    private String internalApiToken;

    @GetMapping("/by-email/{email}")
    public UserResponse getByEmail(
            @PathVariable String email,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalApiToken == null || internalApiToken.isBlank()
                || token == null
                || !MessageDigest.isEqual(
                        internalApiToken.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Internal service authentication is required.");
        }
        return authService.getUserResponseByEmail(email);
    }
}
