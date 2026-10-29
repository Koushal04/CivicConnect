package com.civicconnect.api.controller;

import com.civicconnect.api.dto.AssignGrievanceRequest;
import com.civicconnect.api.dto.GrievanceAssignmentResponse;
import com.civicconnect.api.security.CivicUserPrincipal;
import com.civicconnect.api.service.AdminAssignmentService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
@RequestMapping("/api/admin/grievances")
public class AdminGrievanceController {

    private final AdminAssignmentService assignmentService;

    public AdminGrievanceController(AdminAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PatchMapping("/{trackingId}/assignment")
    public GrievanceAssignmentResponse assign(
            @PathVariable String trackingId,
            @Valid @RequestBody AssignGrievanceRequest request,
            @AuthenticationPrincipal CivicUserPrincipal principal) {
        return assignmentService.assign(trackingId, request, principal);
    }
}
