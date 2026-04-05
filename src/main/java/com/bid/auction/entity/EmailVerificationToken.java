package com.bid.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Holds a short-lived OTP for email verification.
 * One row per user at most (replaced on every new send-otp request).
 * Never exposed through any API response.
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to the user who requested verification. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** The user's email at the time the OTP was issued (for reference / email send). */
    @Column(nullable = false)
    private String email;

    /** Plain-text OTP — kept so it can be resent if still within TTL. */
    @Column(name = "otp_plain", nullable = false, columnDefinition = "varchar(255) default ''")
    private String otpPlain;

    /** BCrypt hash of the plain-text OTP — used for constant-time verification. */
    @Column(name = "otp_hash", nullable = false, columnDefinition = "varchar(255) default ''")
    private String otpHash;

    /** When this OTP expires. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** When the record was created. */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

