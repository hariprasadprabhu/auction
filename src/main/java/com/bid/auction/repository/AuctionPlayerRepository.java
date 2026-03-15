package com.bid.auction.repository;

import com.bid.auction.entity.AuctionPlayer;
import com.bid.auction.entity.Team;
import com.bid.auction.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuctionPlayerRepository extends JpaRepository<AuctionPlayer, Long> {
    List<AuctionPlayer> findByTournamentIdOrderBySortOrder(Long tournamentId);
    List<AuctionPlayer> findByTournamentId(Long tournamentId);
    List<AuctionPlayer> findBySoldToTeam(Team team);
    List<AuctionPlayer> findBySoldToTeamId(Long teamId);
    List<AuctionPlayer> findByTournamentIdAndAuctionStatus(Long tournamentId, AuctionStatus status);
    long countBySoldToTeamId(Long teamId);
    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);

    @Query("SELECT COALESCE(MAX(ap.sortOrder), 0) FROM AuctionPlayer ap WHERE ap.tournament.id = :tournamentId")
    Integer findMaxSortOrderByTournamentId(@Param("tournamentId") Long tournamentId);
}

