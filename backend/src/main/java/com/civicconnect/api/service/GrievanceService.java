package com.civicconnect.api.service;

import com.civicconnect.api.dto.CreateGrievanceRequest;
import com.civicconnect.api.dto.GrievanceResponse;
import com.civicconnect.api.entity.Grievance;
import com.civicconnect.api.entity.GrievanceStatusHistory;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.GrievancePriority;
import com.civicconnect.api.enums.GrievanceStatus;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.GrievanceRepository;
import com.civicconnect.api.repository.GrievanceStatusHistoryRepository;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.CivicUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("mysql")
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    public GrievanceService(
            GrievanceRepository grievanceRepository,
            GrievanceStatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository) {
        this.grievanceRepository = grievanceRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GrievanceResponse submit(CreateGrievanceRequest request, CivicUserPrincipal principal) {
        User citizen = findVerifiedCitizen(principal);
        Grievance grievance = new Grievance();
        grievance.setTrackingId(newTrackingId());
        grievance.setCitizen(citizen);
        grievance.setCategory(request.category());
        grievance.setTitle(request.title().trim());
        grievance.setDescription(request.description().trim());
        grievance.setAddress(request.address().trim());
        grievance.setLatitude(request.latitude());
        grievance.setLongitude(request.longitude());
        grievance.setPriority(GrievancePriority.MEDIUM);
        grievance.setStatus(GrievanceStatus.SUBMITTED);

        Grievance savedGrievance = grievanceRepository.save(grievance);
        GrievanceStatusHistory statusHistory = new GrievanceStatusHistory();
        statusHistory.setGrievance(savedGrievance);
        statusHistory.setNewStatus(GrievanceStatus.SUBMITTED);
        statusHistory.setChangedBy(citizen);
        statusHistoryRepository.save(statusHistory);
        return toResponse(savedGrievance);
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> listForCitizen(CivicUserPrincipal principal) {
        findVerifiedCitizen(principal);
        return grievanceRepository.findByCitizenIdOrderByCreatedAtDesc(principal.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GrievanceResponse findOwnedGrievance(String trackingId, CivicUserPrincipal principal) {
        findVerifiedCitizen(principal);
        return grievanceRepository.findByTrackingIdAndCitizenId(trackingId, principal.userId())
                .map(this::toResponse)
                // Returning the same response avoids revealing another citizen's grievance.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grievance not found"));
    }

    private User findVerifiedCitizen(CivicUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findById(principal.userId())
                .filter(User::isEnabled)
                .filter(user -> user.getRole() == UserRole.CITIZEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Citizen access is required"));
    }

    private String newTrackingId() {
        return "CC-" + UUID.randomUUID();
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
