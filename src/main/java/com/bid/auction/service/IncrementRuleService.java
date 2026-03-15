package com.bid.auction.service;

import com.bid.auction.dto.request.IncrementRuleRequest;
import com.bid.auction.dto.response.IncrementRuleResponse;
import com.bid.auction.entity.IncrementRule;
import com.bid.auction.entity.Tournament;
import com.bid.auction.entity.User;
import com.bid.auction.exception.ResourceNotFoundException;
import com.bid.auction.repository.IncrementRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncrementRuleService {

    private final IncrementRuleRepository incrementRuleRepository;
    private final TournamentService tournamentService;

    public List<IncrementRuleResponse> getAllByTournament(Long tournamentId, User user) {
        tournamentService.findAndVerifyOwner(tournamentId, user);
        return incrementRuleRepository.findByTournamentIdOrderByFromAmountAsc(tournamentId)
                .stream().map(this::toResponse).toList();
    }

    public IncrementRuleResponse create(Long tournamentId, IncrementRuleRequest req, User user) {
        Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);

        IncrementRule rule = IncrementRule.builder()
                .fromAmount(req.getFromAmount())
                .toAmount(resolveToAmount(req.getToAmount()))
                .incrementBy(req.getIncrementBy())
                .tournament(tournament)
                .build();

        return toResponse(incrementRuleRepository.save(rule));
    }

    public IncrementRuleResponse update(Long id, IncrementRuleRequest req, User user) {
        IncrementRule rule = findRule(id);
        tournamentService.findAndVerifyOwner(rule.getTournament().getId(), user);

        rule.setFromAmount(req.getFromAmount());
        rule.setToAmount(resolveToAmount(req.getToAmount()));
        rule.setIncrementBy(req.getIncrementBy());

        return toResponse(incrementRuleRepository.save(rule));
    }

    public void delete(Long id, User user) {
        IncrementRule rule = findRule(id);
        tournamentService.findAndVerifyOwner(rule.getTournament().getId(), user);
        incrementRuleRepository.delete(rule);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private IncrementRule findRule(Long id) {
        return incrementRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Increment rule not found: " + id));
    }

    /** 0 or null → Long.MAX_VALUE (means "and above") */
    private Long resolveToAmount(Long toAmount) {
        return (toAmount == null || toAmount == 0L) ? Long.MAX_VALUE : toAmount;
    }

    private IncrementRuleResponse toResponse(IncrementRule rule) {
        return IncrementRuleResponse.builder()
                .id(rule.getId())
                .fromAmount(rule.getFromAmount())
                .toAmount(rule.getToAmount())
                .incrementBy(rule.getIncrementBy())
                .tournamentId(rule.getTournament().getId())
                .build();
    }
}

