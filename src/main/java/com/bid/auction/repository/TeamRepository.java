package com.bid.auction.repository;

import com.bid.auction.entity.Team;
import com.bid.auction.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByTournament(Tournament tournament);
    List<Team> findByTournamentId(Long tournamentId);
    Optional<Team> findByIdAndTournamentId(Long id, Long tournamentId);
    long countByTournamentId(Long tournamentId);
}

