package com.bid.auth.service;

import com.bid.auth.dto.request.PasswordResetRequest;
import com.bid.auth.dto.response.AuthResponse;
import com.bid.auth.entity.EmailVerificationToken;
import com.bid.auth.entity.User;
import com.bid.auth.repository.EmailVerificationTokenRepository;
import com.bid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ResendEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Value("${app.name:Bid Players}")
    private String appName;

    @Transactional
    public void sendVerificationOtp(String userEmail, String newEmail) {
        User user = authService.getUserByEmail(userEmail);
        String targetEmail;

        if (newEmail != null && !newEmail.isBlank()) {
            String trimmedNew = newEmail.trim().toLowerCase();
            userRepository.findByEmail(trimmedNew).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Email address is already in use: " + trimmedNew);
                }
            });
            targetEmail = trimmedNew;
        } else {
            if (user.isEmailVerified()) {
                throw new IllegalStateException("Email is already verified.");
            }
            targetEmail = user.getEmail();
        }

        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();
        String plainOtp = generateOtp();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .email(targetEmail)
                .otpPlain(plainOtp)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build());

        String name = user.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendOtpEmail(targetEmail, name, plainOtp);
                log.info("Email verification OTP sent to {} for user {}", targetEmail, user.getId());
            }
        });
    }

    @Transactional
    public AuthResponse verifyOtp(String userEmail, String rawOtp) {
        User user = authService.getUserByEmail(userEmail);
        EmailVerificationToken token = tokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No verification OTP found. Please request a new one."));

        boolean isEmailChange = !token.getEmail().equalsIgnoreCase(user.getEmail());
        if (!isEmailChange && user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified.");
        }
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            tokenRepository.deleteByUserId(user.getId());
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(rawOtp, token.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP. Please check and try again.");
        }

        if (isEmailChange) {
            user.setEmail(token.getEmail());
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        return authService.generateAuthResponse(user);
    }

    @Transactional
    public void sendPasswordResetOtp(String userEmail) {
        User user = authService.getUserByEmail(userEmail);
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();
        String plainOtp = generateOtp();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otpPlain(plainOtp)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build());

        String email = user.getEmail();
        String name = user.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendPasswordResetOtpEmail(email, name, plainOtp);
                log.info("Password reset OTP sent to {}", email);
            }
        });
    }

    @Transactional
    public AuthResponse resetPassword(PasswordResetRequest request) {
        User user = authService.getUserByEmail(request.getEmail());
        EmailVerificationToken token = tokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OTP found. Please request a new password-reset OTP."));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            tokenRepository.deleteByUserId(user.getId());
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), token.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP. Please check and try again.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        return authService.generateAuthResponse(user);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt(bound));
    }

    private void sendOtpEmail(String to, String name, String otp) {
        String text = "Hi " + name + ",\n\n"
                + "Your " + appName + " email verification code is:\n\n"
                + "    " + otp + "\n\n"
                + "This code is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "– The " + appName + " Team";
        emailService.sendText(to, appName + " – Email Verification Code", text);
    }

    private void sendPasswordResetOtpEmail(String to, String name, String otp) {
        String text = "Hi " + name + ",\n\n"
                + "Your " + appName + " password reset code is:\n\n"
                + "    " + otp + "\n\n"
                + "This code is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "– The " + appName + " Team";
        emailService.sendText(to, appName + " – Password Reset Code", text);
    }
}
