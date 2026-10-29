package com.civicconnect.api.dto;

import com.civicconnect.api.enums.GrievanceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateGrievanceRequest(
        @NotNull GrievanceCategory category,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 500) String address,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude
) { }
