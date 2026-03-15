package com.bid.auction.service;

import com.bid.auction.dto.request.TournamentRequest;
import com.bid.auction.dto.response.TournamentResponse;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.enums.TournamentStatus;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    // ── List ──────────────────────────────────────────────────────────────────
    public List<TournamentResponse> getAll(User user) {
        return tournamentRepository.findByCreatedBy(user)
                .stream().map(this::toResponse).toList();
    }

    // ── Get single ────────────────────────────────────────────────────────────
    public TournamentResponse getById(Long id, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        return toResponse(t);
    }

    // ── Create ────────────────────────────────────────────────────────────────
    public TournamentResponse create(TournamentRequest req, User user) {
        Tournament t = Tournament.builder()
                .name(req.getName())
                .date(req.getDate())
                .sport(req.getSport())
                .totalTeams(req.getTotalTeams())
                .totalPlayers(req.getTotalPlayers())
                .purseAmount(req.getPurseAmount())
                .playersPerTeam(req.getPlayersPerTeam())
                .basePrice(req.getBasePrice())
                .status(parseStatus(req.getStatus(), TournamentStatus.UPCOMING))
                .createdBy(user)
                .build();

        setLogo(t, req.getLogo());
        return toResponse(tournamentRepository.save(t));
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public TournamentResponse update(Long id, TournamentRequest req, User user) {
        Tournament t = findAndVerifyOwner(id, user);

        t.setName(req.getName());
        t.setDate(req.getDate());
        t.setSport(req.getSport());
        t.setTotalTeams(req.getTotalTeams());
        t.setTotalPlayers(req.getTotalPlayers());
        t.setPurseAmount(req.getPurseAmount());
        t.setPlayersPerTeam(req.getPlayersPerTeam());
        t.setBasePrice(req.getBasePrice());
        if (req.getStatus() != null) {
            t.setStatus(parseStatus(req.getStatus(), t.getStatus()));
        }
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            setLogo(t, req.getLogo());
        }

        return toResponse(tournamentRepository.save(t));
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void delete(Long id, User user) {
        Tournament t = findAndVerifyOwner(id, user);
        tournamentRepository.delete(t);
    }

    // ── Logo bytes ────────────────────────────────────────────────────────────
    public byte[] getLogo(Long id) {
        Tournament t = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
        if (t.getLogo() == null) throw new ResourceNotFoundException("Logo not found for tournament: " + id);
        return t.getLogo();
    }

    public String getLogoContentType(Long id) {
        Tournament t = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
        return t.getLogoContentType() != null ? t.getLogoContentType() : "image/jpeg";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public Tournament findAndVerifyOwner(Long id, User user) {
        Tournament t = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
        if (!t.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this tournament");
        }
        return t;
    }

    /** Used by other services that only need the entity (no ownership check). */
    public Tournament findById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }

    private void setLogo(Tournament t, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                t.setLogo(file.getBytes());
                t.setLogoContentType(file.getContentType());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read logo file");
            }
        }
    }

    private TournamentStatus parseStatus(String statusStr, TournamentStatus defaultVal) {
        if (statusStr == null || statusStr.isBlank()) return defaultVal;
        try {
            return TournamentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }

    public TournamentResponse toResponse(Tournament t) {
        return TournamentResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .date(t.getDate())
                .sport(t.getSport())
                .totalTeams(t.getTotalTeams())
                .totalPlayers(t.getTotalPlayers())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .purseAmount(t.getPurseAmount())
                .playersPerTeam(t.getPlayersPerTeam())
                .basePrice(t.getBasePrice())
                .logoUrl(t.getLogo() != null ? "/api/tournaments/" + t.getId() + "/logo" : null)
                .build();
    }
}

