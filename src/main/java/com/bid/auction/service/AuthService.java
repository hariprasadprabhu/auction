package com.bid.auction.service;

import com.bid.auction.dto.response.UserResponse;
import com.bid.auction.entity.User;
import com.bid.auction.enums.Role;
import com.bid.auction.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RestClient authServiceClient;

    @Value("${app.internal-api-token:}")
    private String internalApiToken;

    public User getUserByEmail(String email) {
        try {
            UserResponse account = authServiceClient.get()
                    .uri("/internal/users/by-email/{email}", email)
                    .header("X-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(UserResponse.class);

            if (account == null) {
                throw new IllegalStateException("Auth Service returned an empty account response.");
            }

            return User.builder()
                    .id(account.getId())
                    .name(account.getName())
                    .email(account.getEmail())
                    .role(Role.valueOf(account.getRole()))
                    .emailVerified(account.isEmailVerified())
                    .build();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User not found: " + email);
        }
    }

    public UserResponse getUserResponseByEmail(String email) {
        User user = getUserByEmail(email);
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
