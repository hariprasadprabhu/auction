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

    @Column(name = "photo")
    private String photo;

    @Column(name = "payment_proof")
    private String paymentProof;

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

