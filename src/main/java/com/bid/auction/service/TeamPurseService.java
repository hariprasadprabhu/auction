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
        Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0
                ? tournament.getPurseAmount() : 1000000L;
        Integer playersPerTeam = tournament.getPlayersPerTeam() != null ? tournament.getPlayersPerTeam() : 11;
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;

        // reservedFund is informational: shows how much is "earmarked" for remaining slots at base price.
        // It does NOT constrain maxBidPerPlayer — doing so caused maxBid=0 when
        // purseAmount ≤ (playersPerTeam-1) × basePrice (a common, valid tournament setup).
        Long reserved = playersPerTeam <= 1 ? 0L : (long) (playersPerTeam - 1) * basePrice;

        // A team can bid up to their full purse on any single player.
        // The natural spending constraint comes from their remaining currentPurse.
        Long maxBid = teamPurse;

        TeamPurse tp = TeamPurse.builder()
                .team(team).tournament(tournament)
                .initialPurse(teamPurse).currentPurse(teamPurse).purseUsed(0L)
                .maxBidPerPlayer(maxBid).reservedFund(reserved).availableForBidding(maxBid)
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

        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);

        // maxBidPerPlayer = remaining purse (team can bid up to whatever they have left)
        Long maxBid = Math.max(0L, tp.getCurrentPurse());
        tp.setMaxBidPerPlayer(maxBid);
        tp.setAvailableForBidding(maxBid);

        return teamPurseRepository.save(tp);
    }
    @Transactional
    public TeamPurse updatePurseOnPlayerUnsold(Team team, Tournament tournament, Long unsolvedPrice) {
        TeamPurse tp = findByTeamAndTournament(team.getId(), tournament.getId());

        tp.setPurseUsed(Math.max(0L, tp.getPurseUsed() - unsolvedPrice));
        tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
        tp.setPlayersBought(Math.max(0, tp.getPlayersBought() - 1));
        tp.setRemainingSlots(tp.getRemainingSlots() + 1);

        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
        tp.setReservedFund(reserved);

        Long maxBid = Math.max(0L, tp.getCurrentPurse());
        tp.setMaxBidPerPlayer(maxBid);
        tp.setAvailableForBidding(maxBid);

        return teamPurseRepository.save(tp);
    }
    @Transactional(timeout = 45)
    public void recalculateAllTeamPurses(Tournament tournament) {
        List<TeamPurse> purses = teamPurseRepository.findByTournamentId(tournament.getId());
        Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0
                ? tournament.getPurseAmount() : 1000000L;
        Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
        for (TeamPurse tp : purses) {
            Long newCurrentPurse = Math.max(0L, teamPurse - tp.getPurseUsed());
            Long reserved = tp.getRemainingSlots() <= 1 ? 0L : (long) (tp.getRemainingSlots() - 1) * basePrice;
            // maxBid = full remaining purse (not reduced by reservation)
            Long maxBid = newCurrentPurse;
            tp.setInitialPurse(teamPurse);
            tp.setCurrentPurse(newCurrentPurse);
            tp.setMaxBidPerPlayer(maxBid);
            tp.setReservedFund(reserved);
            tp.setAvailableForBidding(maxBid);
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
