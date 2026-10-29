package com.civicconnect.api.dto;

import com.civicconnect.api.enums.GrievanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGrievanceStatusRequest(
        @NotNull GrievanceStatus status,
        @Size(max = 1000) String note
) { }
