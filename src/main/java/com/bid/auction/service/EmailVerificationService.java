package com.bid.auction.service;

import com.bid.auction.dto.response.UserResponse;
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
     * @throws IllegalStateException if the email is already verified
     */
    @Transactional
    public void sendVerificationOtp(String userEmail) {
        User user = authService.getUserByEmail(userEmail);

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified.");
        }

        // Always delete any existing token (expired or not), then issue a fresh OTP
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush(); // force DELETE to hit the DB before the INSERT below

        String plainOtp = generateOtp();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otpPlain(plainOtp)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        tokenRepository.save(token);
        log.info("OTP generated and sent for {}", userEmail);

        sendOtpEmail(user.getEmail(), user.getName(), plainOtp);
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
    public UserResponse verifyOtp(String userEmail, String rawOtp) {
        User user = authService.getUserByEmail(userEmail);

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified.");
        }

        EmailVerificationToken token = tokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No verification OTP found. Please request a new one."));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            tokenRepository.deleteByUserId(user.getId());
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!passwordEncoder.matches(rawOtp, token.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP. Please check and try again.");
        }

        // Mark verified and clean up the token row
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());

        log.info("Email verified successfully for {}", userEmail);

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(true)
                .build();
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
}
