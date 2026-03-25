package com.bid.auction.repository;

import com.bid.auction.entity.Sponsor;
import com.bid.auction.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    List<Sponsor> findByTournament(Tournament tournament);
    List<Sponsor> findByTournamentId(Long tournamentId);
    void deleteByTournamentId(Long tournamentId);
}

