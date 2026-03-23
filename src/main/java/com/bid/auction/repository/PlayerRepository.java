package com.bid.auction.repository;

import com.bid.auction.entity.Player;
import com.bid.auction.entity.Tournament;
import com.bid.auction.enums.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTournament(Tournament tournament);
    List<Player> findByTournamentAndStatus(Tournament tournament, PlayerStatus status);
    List<Player> findByTournamentId(Long tournamentId);
    long countByTournamentId(Long tournamentId);
    long countByTournamentAndStatus(Tournament tournament, PlayerStatus status);

    @Modifying
    @Query("UPDATE Player p SET p.status = :status WHERE p.id = :id")
    void updateStatusById(@Param("id") Long id, @Param("status") PlayerStatus status);
}

