package com.bid.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores financial details for each team in a tournament.
 * Updated whenever:
 * - A player is sold to the team
 * - A player is marked unsold
 * - Tournament details are modified
 * - Team details are modified
 */
@Entity
@Table(name = "team_purse", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "tournament_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamPurse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @ToString.Exclude
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    private Tournament tournament;

    /** Initial purse amount allocated to the team (from tournament purse) */
    @Column(nullable = false)
    private Long initialPurse;

    /** Current remaining purse available for bidding */
    @Column(nullable = false)
    private Long currentPurse;

    /** Total amount spent on players so far */
    @Column(nullable = false)
    @Builder.Default
    private Long purseUsed = 0L;

    /** Maximum bid allowed for a single player */
    @Column(nullable = false)
    private Long maxBidPerPlayer;

    /** Reserved fund for ensuring minimum squad (e.g., for remaining slots) */
    @Column(nullable = false)
    private Long reservedFund;

    /** Available purse after reserving minimum squad fund */
    @Column(nullable = false)
    private Long availableForBidding;

    /** Number of players already purchased */
    @Column(nullable = false)
    @Builder.Default
    private Integer playersBought = 0;

    /** Number of remaining slots to be filled */
    @Column(nullable = false)
    private Integer remainingSlots;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

