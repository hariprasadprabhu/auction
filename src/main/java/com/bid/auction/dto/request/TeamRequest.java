package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TeamRequest {

    // teamNumber is auto-generated, no need to provide it
    private String teamNumber;

    @NotBlank(message = "Team name is required")
    private String name;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private MultipartFile logo;
}

