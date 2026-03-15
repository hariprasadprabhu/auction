package com.bid.auction.repository;

import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByCreatedBy(User createdBy);
    Optional<Tournament> findByIdAndCreatedBy(Long id, User createdBy);
}

