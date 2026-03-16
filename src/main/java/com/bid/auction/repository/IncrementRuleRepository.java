package com.bid.auction.repository;

import com.bid.auction.entity.IncrementRule;
import com.bid.auction.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IncrementRuleRepository extends JpaRepository<IncrementRule, Long> {
    List<IncrementRule> findByTournamentOrderByFromAmountAsc(Tournament tournament);
    List<IncrementRule> findByTournamentIdOrderByFromAmountAsc(Long tournamentId);
    
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM IncrementRule ir WHERE ir.tournament.id = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") Long tournamentId);
}

