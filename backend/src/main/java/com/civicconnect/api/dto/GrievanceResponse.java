package com.civicconnect.api.dto;

import com.civicconnect.api.enums.GrievanceCategory;
import com.civicconnect.api.enums.GrievancePriority;
import com.civicconnect.api.enums.GrievanceStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record GrievanceResponse(
        String trackingId,
        GrievanceCategory category,
        String title,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        GrievancePriority priority,
        GrievanceStatus status,
        Instant createdAt,
        Instant updatedAt
) { }
