package com.bid.auth.service;

import com.bid.auth.dto.request.LoginRequest;
import com.bid.auth.dto.request.RegisterRequest;
import com.bid.auth.dto.response.AuthResponse;
import com.bid.auth.dto.response.UserResponse;
import com.bid.auth.entity.User;
import com.bid.auth.enums.Role;
import com.bid.auth.exception.ResourceNotFoundException;
import com.bid.auth.repository.UserRepository;
import com.bid.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .organisation(request.getOrganisation())
                .sport(request.getSport())
                .numberOfTeams(request.getNumberOfTeams())
                .role(Role.ADMIN)
                .build();
        return toUserResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = getUserByEmail(request.getEmail());
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return toAuthResponse(user, jwtUtils.generateToken(userDetails));
    }

    public AuthResponse refreshAccessToken(String expiredToken) {
        String email = jwtUtils.extractUsernameIgnoreExpiry(expiredToken);
        User user = getUserByEmail(email);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return toAuthResponse(user, jwtUtils.generateToken(userDetails));
    }

    public AuthResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return toAuthResponse(user, jwtUtils.generateToken(userDetails));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public UserResponse getUserResponseByEmail(String email) {
        return toUserResponse(getUserByEmail(email));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
