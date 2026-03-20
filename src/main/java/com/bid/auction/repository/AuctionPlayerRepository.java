package com.bid.auction.repository;

import com.bid.auction.entity.AuctionPlayer;
import com.bid.auction.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AuctionPlayerRepository extends JpaRepository<AuctionPlayer, Long> {
    List<AuctionPlayer> findByTournamentIdOrderBySortOrder(Long tournamentId);
    List<AuctionPlayer> findByTournamentId(Long tournamentId);
    List<AuctionPlayer> findBySoldToTeamId(Long teamId);
    List<AuctionPlayer> findByTournamentIdAndAuctionStatus(Long tournamentId, AuctionStatus status);
    long countBySoldToTeamId(Long teamId);
    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);

    /**
     * Delete all auction players linked to a specific player.
     * Using custom query to handle null player references properly.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.player IS NOT NULL AND ap.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.soldToTeam.id = :teamId")
    void deleteBySoldToTeamId(@Param("teamId") Long teamId);

    @Query("SELECT COALESCE(MAX(ap.sortOrder), 0) FROM AuctionPlayer ap WHERE ap.tournament.id = :tournamentId")
    Integer findMaxSortOrderByTournamentId(@Param("tournamentId") Long tournamentId);

    List<AuctionPlayer> findByPlayerId(Long playerId);
}
