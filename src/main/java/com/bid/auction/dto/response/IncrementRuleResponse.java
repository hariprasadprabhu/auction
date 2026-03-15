package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncrementRuleResponse {
    private Long id;
    private Long fromAmount;
    private Long toAmount;
    private Long incrementBy;
    private Long tournamentId;
}

