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
        
        // Calculate reserved purse dynamically
        // If remainingSlots <= 1, no reservation needed (use all available amount)
        // Otherwise reserve for remaining slots minus 1 (for current bid)
        Long reserved = playersPerTeam <= 1 ? 0L : (long) (playersPerTeam - 1) * basePrice;
        
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

        // Deduct the sold price from purse
        tp.setPurseUsed(tp.getPurseUsed() + soldPrice);
        tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
        
        // Update player counts
        tp.setPlayersBought(tp.getPlayersBought() + 1);
        tp.setRemainingSlots(tp.getRemainingSlots() - 1);
        
        // Recalculate reserved fund based on new remainingSlots
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        // If remainingSlots <= 1, no reservation needed (can use all remaining purse)
        Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);
        
        // Recalculate availableForBidding
        tp.setAvailableForBidding(Math.max(0L, tp.getCurrentPurse() - reserved));
        
        // Recalculate maxBidPerPlayer based on new availableForBidding
        Long maxBid = Math.max(0L, tp.getCurrentPurse() - reserved);
        tp.setMaxBidPerPlayer(maxBid);
        
        return teamPurseRepository.save(tp);
    }
    @Transactional
    public TeamPurse updatePurseOnPlayerUnsold(Team team, Tournament tournament, Long unsolvedPrice) {
        TeamPurse tp = findByTeamAndTournament(team.getId(), tournament.getId());
        
        // Credit back the sold amount
        tp.setPurseUsed(Math.max(0L, tp.getPurseUsed() - unsolvedPrice));
        tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
        
        // Update player counts
        tp.setPlayersBought(Math.max(0, tp.getPlayersBought() - 1));
        tp.setRemainingSlots(tp.getRemainingSlots() + 1);
        
        // Recalculate reserved fund based on new remainingSlots
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        // If remainingSlots <= 1, no reservation needed (can use all remaining purse)
        Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);
        
        // Recalculate availableForBidding
        tp.setAvailableForBidding(Math.max(0L, tp.getCurrentPurse() - reserved));
        
        // Recalculate maxBidPerPlayer based on new availableForBidding
        Long maxBid = Math.max(0L, tp.getCurrentPurse() - reserved);
        tp.setMaxBidPerPlayer(maxBid);
        
        return teamPurseRepository.save(tp);
    }
    @Transactional(timeout = 45)
    public void recalculateAllTeamPurses(Tournament tournament) {
        List<TeamPurse> purses = teamPurseRepository.findByTournamentId(tournament.getId());
        // Tournament purse IS the individual team purse (not divided)
        Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 
                ? tournament.getPurseAmount() : 1000000L;
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        for (TeamPurse tp : purses) {
            // Calculate reserved dynamically - if remainingSlots <= 1, no reservation
            Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
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
        Team team = tp.getTeam();
        String logo = team.getLogo();

        return TeamPurseResponse.builder()
                .id(tp.getId()).teamId(team.getId()).teamNumber(team.getTeamNumber())
                .teamName(team.getName()).tournamentId(tp.getTournament().getId())
                .logoUrl(logo)
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
