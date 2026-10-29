package com.civicconnect.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignGrievanceRequest(
        @NotNull @Positive Long departmentId,
        @Positive Long officerId
) { }
