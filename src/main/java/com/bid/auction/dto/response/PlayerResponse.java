package com.bid.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {
    private Long id;
    private String playerNumber;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String role;
    private String status;
    private Long tournamentId;
    private String photoUrl;
    private String paymentProofUrl;
}

