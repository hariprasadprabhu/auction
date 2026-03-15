package com.bid.auction.entity;

import com.bid.auction.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auction_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerNumber;

    @Column(nullable = false)
    private String firstName;

    private String lastName;
    private Integer age;
    private String city;
    private String battingStyle;
    private String bowlingStyle;

    @Column(nullable = false)
    private String role;

    private Long basePrice;

    @Column(name = "photo", columnDefinition = "bytea")
    private byte[] photo;
    private String photoContentType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuctionStatus auctionStatus = AuctionStatus.UPCOMING;

    /** Link back to the registered Player this auction slot was created from.
     *  Null for auction players added directly (admin-created without prior registration). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @ToString.Exclude
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_to_team_id")
    @ToString.Exclude
    private Team soldToTeam;

    private Long soldPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    private Tournament tournament;

    private Integer sortOrder;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (auctionStatus == null) auctionStatus = AuctionStatus.UPCOMING;
    }
}

