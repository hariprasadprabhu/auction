package com.bid.auction.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must be at least 2 characters")
    @Pattern(regexp = "^[a-zA-Z .\\-]+$",
             message = "Name can only contain letters, spaces, dots, and hyphens")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        message = "Password must have at least 1 uppercase letter, 1 digit, and 1 special character"
    )
    private String password;

    @NotBlank(message = "Phone country code is required")
    private String phoneCountryCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{6,15}$", message = "Phone number must be 6-15 digits")
    private String phoneNumber;

    private String organisation;

    @NotBlank(message = "Sport is required")
    private String sport;

    private Integer numberOfTeams;
}

