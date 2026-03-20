package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ResetAuctionPlayersRequest {

    @NotEmpty(message = "Player IDs list cannot be empty")
    private List<Long> playerIds;
}

