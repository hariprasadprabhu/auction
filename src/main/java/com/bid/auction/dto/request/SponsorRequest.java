package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SponsorRequest {

    @NotBlank(message = "Sponsor name is required")
    private String name;

    private String personName;

    private String personImageUrl;
}

