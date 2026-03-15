package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String teamNumber;
    private String name;
    private String ownerName;
    private String mobileNumber;
    private Long tournamentId;
    private String logoUrl;
}

