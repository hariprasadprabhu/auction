package com.bid.auction.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SponsorResponse {

    private Long id;

    private String name;

    private String personName;

    private String personImageUrl;

    private Long tournamentId;
}

