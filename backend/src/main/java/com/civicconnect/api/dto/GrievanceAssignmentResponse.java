package com.civicconnect.api.dto;

import com.civicconnect.api.enums.GrievanceStatus;

public record GrievanceAssignmentResponse(
        String trackingId,
        String departmentCode,
        Long assignedOfficerId,
        GrievanceStatus status
) { }
