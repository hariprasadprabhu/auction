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

    // ── Helper: compute reserved + maxBid from the three inputs ──────────────
    // reserved          = (remainingSlots - 1) × basePrice
    // maxBidPerPlayer   = currentPurse - reserved  (≥ 0)
    // availableForBid   = maxBidPerPlayer
    private static long calcReserved(int remainingSlots, long basePrice) {
        return remainingSlots <= 1 ? 0L : (long) (remainingSlots - 1) * basePrice;
    }

    private static long calcMaxBid(long currentPurse, long reserved) {
        return Math.max(0L, currentPurse - reserved);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TeamPurse initializePurse(Team team, Tournament tournament) {
        teamPurseRepository.deleteByTeamIdAndTournamentId(team.getId(), tournament.getId());

        long teamPurse     = tournament.getPurseAmount()    != null && tournament.getPurseAmount()    > 0 ? tournament.getPurseAmount()    : 1_000_000L;
        int  playersPerTeam = tournament.getPlayersPerTeam() != null                                      ? tournament.getPlayersPerTeam()  : 11;
        long basePrice     = tournament.getBasePrice()      != null                                      ? tournament.getBasePrice()       : 5_000L;

        long reserved = calcReserved(playersPerTeam, basePrice);
        long maxBid   = calcMaxBid(teamPurse, reserved);

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

        long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5_000L;
        long reserved  = calcReserved(tp.getRemainingSlots(), basePrice);
        long maxBid    = calcMaxBid(tp.getCurrentPurse(), reserved);

        tp.setReservedFund(reserved);
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

        long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5_000L;
        long reserved  = calcReserved(tp.getRemainingSlots(), basePrice);
        long maxBid    = calcMaxBid(tp.getCurrentPurse(), reserved);

        tp.setReservedFund(reserved);
        tp.setMaxBidPerPlayer(maxBid);
        tp.setAvailableForBidding(maxBid);
        return teamPurseRepository.save(tp);
    }

    @Transactional(timeout = 45)
    public void recalculateAllTeamPurses(Tournament tournament) {
        List<TeamPurse> purses = teamPurseRepository.findByTournamentId(tournament.getId());

        long newInitialPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 ? tournament.getPurseAmount() : 1_000_000L;
        long basePrice       = tournament.getBasePrice()   != null                                    ? tournament.getBasePrice()   : 5_000L;

        for (TeamPurse tp : purses) {
            long newCurrentPurse = Math.max(0L, newInitialPurse - tp.getPurseUsed());
            long reserved        = calcReserved(tp.getRemainingSlots(), basePrice);
            long maxBid          = calcMaxBid(newCurrentPurse, reserved);

            tp.setInitialPurse(newInitialPurse);
            tp.setCurrentPurse(newCurrentPurse);
            tp.setReservedFund(reserved);
            tp.setMaxBidPerPlayer(maxBid);
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
