package com.bid.auction.entity;

import com.bid.auction.enums.PlayerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerNumber;

    @Column(nullable = false)
    private String firstName;

    private String lastName;
    private LocalDate dob;

    @Column(nullable = false)
    private String role;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "photo")
    private String photo;

    @Column(name = "payment_proof")
    private String paymentProof;

    // ── Optional profile fields ──────────────────────────────────────────────
    /** e.g. "RIGHT_HANDED" or "LEFT_HANDED" */
    @Column(name = "handedness")
    private String handedness;

    /** e.g. "XS", "S", "M", "L", "XL", "XXL" */
    @Column(name = "tshirt_size")
    private String tshirtSize;

    /** e.g. "28", "30", "32" */
    @Column(name = "trouser_size")
    private String trouserSize;

    /** Jersey number chosen by the player */
    @Column(name = "jersey_number")
    private String jerseyNumber;

    /** e.g. "FULL_SLEEVES" or "HALF_SLEEVES" */
    @Column(name = "sleeve_type")
    private String sleeveType;

    /** City / area the player is from */
    @Column(name = "player_location")
    private String playerLocation;

    /** Whether the player participated in the last season */
    @Column(name = "last_season_played")
    private Boolean lastSeasonPlayed;

    /** Team name the player played for in the last season */
    @Column(name = "last_season_team")
    private String lastSeasonTeam;

    /** Bowling style e.g. "Right-arm fast", "Left-arm spin", etc. */
    @Column(name = "bowling_style")
    private String bowlingStyle;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) check (status in ('PENDING', 'APPROVED', 'REJECTED', 'SOLD', 'UNSOLD'))")
    @Builder.Default
    private PlayerStatus status = PlayerStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    private Tournament tournament;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = PlayerStatus.PENDING;
    }
}

