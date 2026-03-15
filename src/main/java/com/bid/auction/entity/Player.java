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

    @Column(name = "photo", columnDefinition = "bytea")
    private byte[] photo;
    private String photoContentType;

    @Column(name = "payment_proof", columnDefinition = "bytea")
    private byte[] paymentProof;
    private String paymentProofContentType;

    @Enumerated(EnumType.STRING)
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

