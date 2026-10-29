package com.civicconnect.api.controller;

import com.civicconnect.api.dto.GrievanceResponse;
import com.civicconnect.api.dto.UpdateGrievanceStatusRequest;
import com.civicconnect.api.security.CivicUserPrincipal;
import com.civicconnect.api.service.OfficerGrievanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
@RequestMapping("/api/officers/me/grievances")
public class OfficerGrievanceController {

    private final OfficerGrievanceService grievanceService;

    public OfficerGrievanceController(OfficerGrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @GetMapping
    public List<GrievanceResponse> listAssigned(@AuthenticationPrincipal CivicUserPrincipal principal) {
        return grievanceService.listAssigned(principal);
    }

    @PatchMapping("/{trackingId}/status")
    public GrievanceResponse updateStatus(
            @PathVariable String trackingId,
            @Valid @RequestBody UpdateGrievanceStatusRequest request,
            @AuthenticationPrincipal CivicUserPrincipal principal) {
        return grievanceService.updateStatus(trackingId, request, principal);
    }
}
