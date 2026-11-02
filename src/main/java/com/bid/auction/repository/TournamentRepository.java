package com.bid.auction.repository;

import com.bid.auction.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByCreatedById(Long createdById);
    Optional<Tournament> findByIdAndCreatedById(Long id, Long createdById);
}
