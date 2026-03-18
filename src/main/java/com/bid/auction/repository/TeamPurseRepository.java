package com.bid.auction.repository;

import com.bid.auction.entity.TeamPurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamPurseRepository extends JpaRepository<TeamPurse, Long> {

    Optional<TeamPurse> findByTeamIdAndTournamentId(Long teamId, Long tournamentId);

    List<TeamPurse> findByTournamentId(Long tournamentId);

    List<TeamPurse> findByTeamId(Long teamId);

    @Query("SELECT tp FROM TeamPurse tp WHERE tp.tournament.id = :tournamentId ORDER BY tp.team.teamNumber ASC")
    List<TeamPurse> findByTournamentIdOrderByTeamNumber(@Param("tournamentId") Long tournamentId);

    void deleteByTeamIdAndTournamentId(Long teamId, Long tournamentId);

    void deleteByTournamentId(Long tournamentId);
}

