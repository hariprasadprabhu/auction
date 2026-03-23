package com.bid.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PlayerRegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    @NotBlank(message = "Role is required")
    private String role;

    private String photo;
    private String paymentProof;
}

