package com.bid.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teamNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ownerName;

    private String mobileNumber;

    @Column(name = "logo", columnDefinition = "bytea")
    private byte[] logo;
    private String logoContentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    private Tournament tournament;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

