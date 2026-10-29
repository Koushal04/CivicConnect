package com.civicconnect.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCitizenRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^[+0-9() -]{7,30}$", message = "phone must be a valid phone number") String phone,
        @NotBlank @Size(min = 12, max = 72) String password
) { }
