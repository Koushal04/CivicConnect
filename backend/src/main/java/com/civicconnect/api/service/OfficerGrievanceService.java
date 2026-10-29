package com.civicconnect.api.service;

import com.civicconnect.api.dto.GrievanceResponse;
import com.civicconnect.api.dto.UpdateGrievanceStatusRequest;
import com.civicconnect.api.entity.Grievance;
import com.civicconnect.api.entity.GrievanceStatusHistory;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.GrievanceStatus;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.GrievanceRepository;
import com.civicconnect.api.repository.GrievanceStatusHistoryRepository;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.CivicUserPrincipal;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("mysql")
public class OfficerGrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    public OfficerGrievanceService(
            GrievanceRepository grievanceRepository,
            GrievanceStatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository) {
        this.grievanceRepository = grievanceRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> listAssigned(CivicUserPrincipal principal) {
        findVerifiedOfficer(principal);
        return grievanceRepository.findByAssignedOfficerIdOrderByCreatedAtDesc(principal.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GrievanceResponse updateStatus(
            String trackingId, UpdateGrievanceStatusRequest request, CivicUserPrincipal principal) {
        User officer = findVerifiedOfficer(principal);
        Grievance grievance = grievanceRepository.findByTrackingIdAndAssignedOfficerId(trackingId, principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grievance not found"));

        validateTransition(grievance.getStatus(), request.status(), request.note());
        GrievanceStatus previousStatus = grievance.getStatus();
        grievance.setStatus(request.status());
        if (request.status() == GrievanceStatus.RESOLVED) {
            grievance.setResolutionNotes(request.note().trim());
            grievance.setResolvedAt(Instant.now());
        }

        grievanceRepository.save(grievance);
        GrievanceStatusHistory history = new GrievanceStatusHistory();
        history.setGrievance(grievance);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(request.status());
        history.setChangedBy(officer);
        history.setNote(blankToNull(request.note()));
        statusHistoryRepository.save(history);
        return toResponse(grievance);
    }

    private User findVerifiedOfficer(CivicUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findById(principal.userId())
                .filter(User::isEnabled)
                .filter(user -> user.getRole() == UserRole.OFFICER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Officer access is required"));
    }

    private void validateTransition(GrievanceStatus current, GrievanceStatus requested, String note) {
        boolean valid = (current == GrievanceStatus.ASSIGNED && requested == GrievanceStatus.IN_PROGRESS)
                || ((current == GrievanceStatus.ASSIGNED || current == GrievanceStatus.IN_PROGRESS)
                && requested == GrievanceStatus.RESOLVED);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid grievance status transition");
        }
        if (requested == GrievanceStatus.RESOLVED && (note == null || note.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution notes are required when resolving a grievance");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private GrievanceResponse toResponse(Grievance grievance) {
        return new GrievanceResponse(
                grievance.getTrackingId(),
                grievance.getCategory(),
                grievance.getTitle(),
                grievance.getDescription(),
                grievance.getAddress(),
                grievance.getLatitude(),
                grievance.getLongitude(),
                grievance.getPriority(),
                grievance.getStatus(),
                grievance.getCreatedAt(),
                grievance.getUpdatedAt());
    }
}
