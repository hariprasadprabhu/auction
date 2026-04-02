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
    
    // Eager fetch soldToTeam to prevent lazy loading errors
    @Query("SELECT ap FROM AuctionPlayer ap LEFT JOIN FETCH ap.soldToTeam WHERE ap.tournament.id = :tournamentId ORDER BY ap.sortOrder ASC")
    List<AuctionPlayer> findByTournamentIdOrderBySortOrder(@Param("tournamentId") Long tournamentId);
    
    // Eager fetch soldToTeam to prevent lazy loading errors
    @Query("SELECT ap FROM AuctionPlayer ap LEFT JOIN FETCH ap.soldToTeam WHERE ap.tournament.id = :tournamentId")
    List<AuctionPlayer> findByTournamentId(@Param("tournamentId") Long tournamentId);
    
    List<AuctionPlayer> findBySoldToTeamId(Long teamId);
    
    @Query("SELECT ap FROM AuctionPlayer ap LEFT JOIN FETCH ap.soldToTeam WHERE ap.tournament.id = :tournamentId AND ap.auctionStatus = :status")
    List<AuctionPlayer> findByTournamentIdAndAuctionStatus(@Param("tournamentId") Long tournamentId, @Param("status") AuctionStatus status);
    
    long countBySoldToTeamId(Long teamId);
    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);

    /**
     * Delete all auction players linked to a specific player.
     * Using custom query to handle null player references properly.
     *
     * flushAutomatically = true  →  flushes all pending JPA changes (e.g. TeamPurse refund UPDATE)
     *                               to the DB BEFORE the bulk DELETE runs.
     * clearAutomatically = true  →  clears the first-level cache after the DELETE so subsequent
     *                               reads don't return stale data.
     *
     * Without flushAutomatically the sequence was:
     *   1. updatePurseOnPlayerUnsold() marks TeamPurse dirty in the first-level cache
     *   2. deleteByPlayerId() fires the bulk DELETE without flushing first
     *   3. clearAutomatically evicts all managed entities, including the dirty TeamPurse
     *   4. The TeamPurse UPDATE is never sent to the DB → refund is silently lost
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.player IS NOT NULL AND ap.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.soldToTeam.id = :teamId")
    void deleteBySoldToTeamId(@Param("teamId") Long teamId);

    /** Bulk-delete all auction players for a tournament in one shot. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.tournament.id = :tournamentId")
    void deleteAllByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT COALESCE(MAX(ap.sortOrder), 0) FROM AuctionPlayer ap WHERE ap.tournament.id = :tournamentId")
    Integer findMaxSortOrderByTournamentId(@Param("tournamentId") Long tournamentId);

    List<AuctionPlayer> findByPlayerId(Long playerId);
}
