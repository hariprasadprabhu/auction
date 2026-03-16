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
     * Bulk delete by player id — using @Modifying/@Query avoids the Hibernate
     * "collection with orphanRemoval was no longer referenced" error that can
     * occur with a derived-delete when the parent Tournament is already in the
     * session and its auctionPlayers collection carries orphanRemoval=true.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AuctionPlayer ap WHERE ap.soldToTeam.id = :teamId")
    void deleteBySoldToTeamId(@Param("teamId") Long teamId);

    @Query("SELECT COALESCE(MAX(ap.sortOrder), 0) FROM AuctionPlayer ap WHERE ap.tournament.id = :tournamentId")
    Integer findMaxSortOrderByTournamentId(@Param("tournamentId") Long tournamentId);

    List<AuctionPlayer> findByPlayerId(Long playerId);
}
