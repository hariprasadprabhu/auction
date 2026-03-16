package com.bid.auction.service;
import com.bid.auction.dto.response.TeamPurseResponse;
import com.bid.auction.entity.Team;
import com.bid.auction.entity.TeamPurse;
import com.bid.auction.entity.Tournament;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.TeamPurseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class TeamPurseService {
    private final TeamPurseRepository teamPurseRepository;
    @Transactional
    public TeamPurse initializePurse(Team team, Tournament tournament) {
        teamPurseRepository.deleteByTeamIdAndTournamentId(team.getId(), tournament.getId());
        // Tournament purse IS the individual team purse (not divided)
        Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 
                ? tournament.getPurseAmount() : 1000000L;
        Integer playersPerTeam = tournament.getPlayersPerTeam() != null ? tournament.getPlayersPerTeam() : 11;
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        // Reserved = (remainingSlots - 1) × Base price
        Long reserved = (long) (playersPerTeam - 1) * basePrice;
        // Max bid per player = current purse - reserved
        Long maxBid = teamPurse - reserved;
        TeamPurse tp = TeamPurse.builder().team(team).tournament(tournament).initialPurse(teamPurse)
                .currentPurse(teamPurse).purseUsed(0L).maxBidPerPlayer(maxBid)
                .reservedFund(reserved).availableForBidding(maxBid)
                .playersBought(0).remainingSlots(playersPerTeam).build();
        return teamPurseRepository.save(tp);
    }
    @Transactional
    public TeamPurse updatePurseOnPlayerSold(Team team, Tournament tournament, Long soldPrice) {
        TeamPurse tp = findByTeamAndTournament(team.getId(), tournament.getId());
        tp.setPurseUsed(tp.getPurseUsed() + soldPrice);
        tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
        tp.setPlayersBought(tp.getPlayersBought() + 1);
        tp.setRemainingSlots(tp.getRemainingSlots() - 1);
        // Recalculate reserved fund based on new remainingSlots
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        Long reserved = (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);
        tp.setAvailableForBidding(Math.max(0L, tp.getCurrentPurse() - reserved));
        return teamPurseRepository.save(tp);
    }
    @Transactional
    public TeamPurse updatePurseOnPlayerUnsold(Team team, Tournament tournament, Long unsolvedPrice) {
        TeamPurse tp = findByTeamAndTournament(team.getId(), tournament.getId());
        tp.setPurseUsed(Math.max(0L, tp.getPurseUsed() - unsolvedPrice));
        tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
        tp.setPlayersBought(Math.max(0, tp.getPlayersBought() - 1));
        tp.setRemainingSlots(tp.getRemainingSlots() + 1);
        // Recalculate reserved fund based on new remainingSlots
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        Long reserved = (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);
        tp.setAvailableForBidding(Math.max(0L, tp.getCurrentPurse() - reserved));
        return teamPurseRepository.save(tp);
    }
    @Transactional
    public void recalculateAllTeamPurses(Tournament tournament) {
        List<TeamPurse> purses = teamPurseRepository.findByTournamentId(tournament.getId());
        // Tournament purse IS the individual team purse (not divided)
        Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 
                ? tournament.getPurseAmount() : 1000000L;
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        for (TeamPurse tp : purses) {
            // Reserved = (remainingSlots - 1) × Base price
            Long reserved = (long) (tp.getRemainingSlots() - 1) * basePrice;
            // Max bid per player = current purse - reserved
            Long maxBid = tp.getCurrentPurse() - reserved;
            tp.setInitialPurse(teamPurse);
            tp.setMaxBidPerPlayer(maxBid);
            tp.setReservedFund(reserved);
            tp.setAvailableForBidding(Math.max(0L, maxBid));
            tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
            teamPurseRepository.save(tp);
        }
    }
    public TeamPurseResponse getPurse(Long teamId, Long tournamentId) {
        return toResponse(findByTeamAndTournament(teamId, tournamentId));
    }
    public List<TeamPurseResponse> getAllTeamPurses(Long tournamentId) {
        return teamPurseRepository.findByTournamentIdOrderByTeamNumber(tournamentId)
                .stream().map(this::toResponse).toList();
    }
    public List<TeamPurseResponse> getTeamPurseAcrossTournaments(Long teamId) {
        return teamPurseRepository.findByTeamId(teamId).stream().map(this::toResponse).toList();
    }
    public TeamPurse findByTeamAndTournament(Long teamId, Long tournamentId) {
        return teamPurseRepository.findByTeamIdAndTournamentId(teamId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Team purse not found"));
    }
    // ...existing code...
    private TeamPurseResponse toResponse(TeamPurse tp) {
        return TeamPurseResponse.builder()
                .id(tp.getId()).teamId(tp.getTeam().getId()).teamNumber(tp.getTeam().getTeamNumber())
                .teamName(tp.getTeam().getName()).tournamentId(tp.getTournament().getId())
                .initialPurse(tp.getInitialPurse()).currentPurse(tp.getCurrentPurse())
                .purseUsed(tp.getPurseUsed()).maxBidPerPlayer(tp.getMaxBidPerPlayer())
                .reservedFund(tp.getReservedFund()).availableForBidding(tp.getAvailableForBidding())
                .playersBought(tp.getPlayersBought()).remainingSlots(tp.getRemainingSlots()).build();
    }
    @Transactional
    public void deleteTeamPursesForTournament(Long tournamentId) {
        teamPurseRepository.deleteByTournamentId(tournamentId);
    }

    @Transactional
    public void deleteTeamPurseInTournament(Long teamId, Long tournamentId) {
        // Delete purse record for a specific team in a specific tournament
        // Teams are tournament-specific, so this is the only purse record for this team
        teamPurseRepository.deleteByTeamIdAndTournamentId(teamId, tournamentId);
    }
}
