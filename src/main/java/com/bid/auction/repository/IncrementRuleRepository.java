package com.bid.auction.repository;

import com.bid.auction.entity.IncrementRule;
import com.bid.auction.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncrementRuleRepository extends JpaRepository<IncrementRule, Long> {
    List<IncrementRule> findByTournamentOrderByFromAmountAsc(Tournament tournament);
    List<IncrementRule> findByTournamentIdOrderByFromAmountAsc(Long tournamentId);
}

