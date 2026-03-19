package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk approve/reject player operations.
 * Accepts a list of player IDs to be approved or rejected.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPlayerActionRequest {

    @NotNull(message = "Player IDs list cannot be null")
    @NotEmpty(message = "At least one player ID is required")
    private List<Long> playerIds;
}

