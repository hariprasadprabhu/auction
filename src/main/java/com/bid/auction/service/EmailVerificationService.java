package com.bid.auction.service;

import com.bid.auction.dto.request.PasswordResetRequest;
import com.bid.auction.dto.response.AuthResponse;
import com.bid.auction.entity.EmailVerificationToken;
import com.bid.auction.entity.User;
import com.bid.auction.repository.EmailVerificationTokenRepository;
import com.bid.auction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private static final int OTP_LENGTH         = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final SecureRandom RANDOM    = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository                   userRepository;
    private final JavaMailSender                   mailSender;
    private final PasswordEncoder                  passwordEncoder;
    private final AuthService                      authService;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.name:Bid Players}")
    private String appName;

    // ──────────────────────────────────────────────────────────────────────────
    // Send OTP
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores a BCrypt hash + expiry in the
     * {@code email_verification_tokens} table (replacing any previous record),
     * and emails the plain OTP to the user.
     *
     * <p>If {@code newEmail} is supplied the method checks uniqueness and stores
     * the new address in the token row so the JWT (which still carries the old
     * email) remains valid until the OTP is verified.  The actual {@code user.email}
     * column is only updated inside {@link #verifyOtp} upon successful verification.
     *
     * @param userEmail currently authenticated user's email (from JWT)
     * @param newEmail  optional new email address to switch to; {@code null} means
     *                  "use the current address"
     * @throws IllegalStateException    if no new email was given and the current
     *                                  email is already verified
     * @throws IllegalArgumentException if {@code newEmail} is already taken by
     *                                  another account
     */
    @Transactional
    public void sendVerificationOtp(String userEmail, String newEmail) {
        User user = authService.getUserByEmail(userEmail);

        String targetEmail;  // the address to send the OTP to

        if (newEmail != null && !newEmail.isBlank()) {
            String trimmedNew = newEmail.trim().toLowerCase();

            // Block if the exact same email is already registered to a *different* user
            userRepository.findByEmail(trimmedNew).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new IllegalArgumentException(
                            "Email address is already in use: " + trimmedNew);
                }
            });

            targetEmail = trimmedNew;
            // NOTE: we do NOT update user.email here — that happens in verifyOtp()
            // so the existing JWT (which carries the old email) keeps working.

        } else {
            // No new email – enforce "not yet verified" guard for the current address
            if (user.isEmailVerified()) {
                throw new IllegalStateException("Email is already verified.");
            }
            targetEmail = user.getEmail();
        }

        // Always delete any existing token, then issue a fresh OTP
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String plainOtp = generateOtp();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .email(targetEmail)          // stores pending new email (or current email)
                .otpPlain(plainOtp)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        tokenRepository.save(token);
        log.info("OTP generated and will be sent to {} for user {}", targetEmail, user.getId());

        // Send email AFTER the transaction commits so the DB connection is
        // released before blocking on the SMTP call.
        final String finalTargetEmail = targetEmail;
        final String finalName = user.getName();
        final String finalOtp = plainOtp;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendOtpEmail(finalTargetEmail, finalName, finalOtp);
                log.info("OTP sent to {} for user {}", finalTargetEmail, user.getId());
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Verify OTP
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates the supplied OTP against the stored hash.
     * On success sets {@code emailVerified = true} on the User and
     * deletes the token row so it cannot be reused.
     *
     * @throws IllegalStateException    if the email is already verified
     * @throws IllegalArgumentException if no OTP exists, or it is invalid / expired
     */
    @Transactional
    public AuthResponse verifyOtp(String userEmail, String rawOtp) {
        User user = authService.getUserByEmail(userEmail);

        // Fetch the token first so we can detect a pending email-change before
        // deciding whether to block an "already verified" user.
        EmailVerificationToken token = tokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No verification OTP found. Please request a new one."));

        boolean isEmailChange = !token.getEmail().equalsIgnoreCase(user.getEmail());

        // Block "already verified" only when the user is verifying their *current* address
        // (not when they are in the middle of changing to a new one).
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

        // Apply pending email change (if any), then always mark email as verified.
        if (isEmailChange) {
            log.info("Applying pending email change: {} → {} for user {}",
                    user.getEmail(), token.getEmail(), user.getId());
            user.setEmail(token.getEmail());
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());

        log.info("Email verified successfully for {}", user.getEmail());

        return authService.generateAuthResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Password Reset
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores a BCrypt hash + expiry in the
     * {@code email_verification_tokens} table (replacing any previous record),
     * and emails the plain OTP to the user for password reset purposes.
     * Unlike {@link #sendVerificationOtp}, this does NOT require the email to be unverified.
     */
    @Transactional
    public void sendPasswordResetOtp(String userEmail) {
        User user = authService.getUserByEmail(userEmail);

        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String plainOtp = generateOtp();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otpPlain(plainOtp)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        tokenRepository.save(token);
        log.info("Password reset OTP generated for {}", userEmail);

        // Send email AFTER commit — avoids holding the DB connection during SMTP.
        final String finalEmail = user.getEmail();
        final String finalName = user.getName();
        final String finalOtp = plainOtp;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendPasswordResetOtpEmail(finalEmail, finalName, finalOtp);
                log.info("Password reset OTP sent for {}", finalEmail);
            }
        });
    }

    /**
     * Validates the supplied OTP against the stored hash for the given email.
     * On success updates the user's password (BCrypt-encoded), sets emailVerified=true,
     * and deletes the token.
     *
     * @throws IllegalArgumentException if the user is not found, no OTP exists,
     *                                  or the OTP is invalid / expired
     */
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

        log.info("Password reset successfully for {}. emailVerified set to true.", request.getEmail());

        return authService.generateAuthResponse(user);
    }


    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt(bound));
    }

    private void sendOtpEmail(String to, String name, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(appName + " – Email Verification Code");
        message.setText(
                "Hi " + name + ",\n\n" +
                "Your " + appName + " email verification code is:\n\n" +
                "    " + otp + "\n\n" +
                "This code is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "– The " + appName + " Team"
        );
        mailSender.send(message);
    }

    private void sendPasswordResetOtpEmail(String to, String name, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(appName + " – Password Reset Code");
        message.setText(
                "Hi " + name + ",\n\n" +
                "Your " + appName + " password reset code is:\n\n" +
                "    " + otp + "\n\n" +
                "This code is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "– The " + appName + " Team"
        );
        mailSender.send(message);
    }
}
