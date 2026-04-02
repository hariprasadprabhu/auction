package com.bid.auction.service;

import com.bid.auction.dto.request.SponsorRequest;
import com.bid.auction.dto.response.SponsorResponse;
import com.bid.auction.entity.Sponsor;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final TournamentService tournamentService;

    /**
     * Add multiple sponsors to a tournament.
     * Only the tournament owner can add sponsors.
     */
    @Transactional
    public List<SponsorResponse> addSponsors(Long tournamentId, List<SponsorRequest> requests, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);

        List<Sponsor> sponsors = requests.stream()
                .map(req -> Sponsor.builder()
                        .name(req.getName())
                        .personName(req.getPersonName())
                        .personImageUrl(req.getPersonImageUrl())
                        .tournament(tournament)
                        .build())
                .toList();

        List<Sponsor> saved = sponsorRepository.saveAll(sponsors);
        return saved.stream().map(this::toResponse).toList();
    }

    /**
     * Get all sponsors for a specific tournament.
     * Only the tournament owner can view sponsors.
     */
    public List<SponsorResponse> getByTournament(Long tournamentId, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);
        return sponsorRepository.findByTournament(tournament)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Get all sponsors for a tournament (public endpoint).
     * Used by unauthenticated users to view tournament sponsors.
     */
    public List<SponsorResponse> getPublicSponsors(Long tournamentId) {
        // Verify tournament exists
        tournamentService.findById(tournamentId);
        return sponsorRepository.findByTournamentId(tournamentId)
                .stream().map(this::toResponse).toList();
    }

    private SponsorResponse toResponse(Sponsor sponsor) {
        return SponsorResponse.builder()
                .id(sponsor.getId())
                .name(sponsor.getName())
                .personName(sponsor.getPersonName())
                .personImageUrl(sponsor.getPersonImageUrl())
                .tournamentId(sponsor.getTournament().getId())
                .build();
    }

    /**
     * Update a sponsor's details.
     * Only the tournament owner can update sponsors.
     */
    @Transactional
    public SponsorResponse updateSponsor(Long tournamentId, Long sponsorId, SponsorRequest request, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new IllegalArgumentException("Sponsor not found: " + sponsorId));
        if (!sponsor.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Sponsor does not belong to the specified tournament");
        }
        sponsor.setName(request.getName());
        sponsor.setPersonName(request.getPersonName());
        sponsor.setPersonImageUrl(request.getPersonImageUrl());
        Sponsor updated = sponsorRepository.save(sponsor);
        return toResponse(updated);
    }

    /**
     * Delete a sponsor from a tournament.
     * Only the tournament owner can delete sponsors.
     */
    @Transactional
    public void deleteSponsor(Long tournamentId, Long sponsorId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new IllegalArgumentException("Sponsor not found: " + sponsorId));
        if (!sponsor.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Sponsor does not belong to the specified tournament");
        }
        sponsorRepository.delete(sponsor);
    }
}
