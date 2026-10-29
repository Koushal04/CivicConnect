package com.civicconnect.api.controller;

import com.civicconnect.api.dto.CreateGrievanceRequest;
import com.civicconnect.api.dto.GrievanceResponse;
import com.civicconnect.api.security.CivicUserPrincipal;
import com.civicconnect.api.service.GrievanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
@RequestMapping("/api/grievances")
public class GrievanceController {

    private final GrievanceService grievanceService;

    public GrievanceController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @PostMapping
    public ResponseEntity<GrievanceResponse> submit(
            @Valid @RequestBody CreateGrievanceRequest request,
            @AuthenticationPrincipal CivicUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grievanceService.submit(request, principal));
    }

    @GetMapping
    public List<GrievanceResponse> list(@AuthenticationPrincipal CivicUserPrincipal principal) {
        return grievanceService.listForCitizen(principal);
    }

    @GetMapping("/{trackingId}")
    public GrievanceResponse getByTrackingId(
            @PathVariable String trackingId,
            @AuthenticationPrincipal CivicUserPrincipal principal) {
        return grievanceService.findOwnedGrievance(trackingId, principal);
    }
}
