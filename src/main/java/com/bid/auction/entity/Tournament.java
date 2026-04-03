package com.bid.auction.entity;

import com.bid.auction.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDate date;
    private String sport;
    private Integer totalTeams;
    private Integer totalPlayers;
    
    @Setter(AccessLevel.PACKAGE)
    private Integer teamAllowed;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TournamentStatus status = TournamentStatus.UPCOMING;

    private Long purseAmount;
    private Integer playersPerTeam;
    private Long basePrice;
    private Long initialIncrement;

    @Column(name = "logo")
    private String logo;

    @Column(name = "payment_proof_required")
    private Boolean paymentProofRequired;

    /**
     * Controls whether players can self-register for this tournament.
     * Set to {@code true} by default on creation.
     * Owner can toggle this via PUT /tournaments/{id}.
     */
    @Column(name = "player_registration_open", nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean playerRegistrationOpen = true;

    /**
     * Tracks whether the auction date (tournament date) can still be edited.
     * <p>
     * Lifecycle:
     * <ul>
     *   <li>Set to {@code true} automatically on creation (counts as edit #1).</li>
     *   <li>After the first post-creation change to the date, set to {@code false} (edit #2 used).</li>
     *   <li>Once {@code false}, the date is permanently locked and cannot be updated again.</li>
     * </ul>
     * This field is <strong>not</strong> settable via the public API.
     */
    @Column(name = "can_edit_auction_date", nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean canEditAuctionDate = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<AuctionPlayer> auctionPlayers = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<IncrementRule> incrementRules = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<TeamPurse> teamPurses = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Sponsor> sponsors = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TournamentStatus.UPCOMING;
        if (canEditAuctionDate == null) canEditAuctionDate = true;
        if (playerRegistrationOpen == null) playerRegistrationOpen = true;
    }
}

