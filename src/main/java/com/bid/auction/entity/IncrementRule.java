package com.bid.auction.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "increment_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncrementRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromAmount;

    @Column(nullable = false)
    private Long toAmount;

    @Column(nullable = false)
    private Long incrementBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    private Tournament tournament;
}

