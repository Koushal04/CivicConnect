package com.civicconnect.api.service;

import com.civicconnect.api.dto.AssignGrievanceRequest;
import com.civicconnect.api.dto.GrievanceAssignmentResponse;
import com.civicconnect.api.entity.Department;
import com.civicconnect.api.entity.Grievance;
import com.civicconnect.api.entity.GrievanceStatusHistory;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.GrievanceStatus;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.DepartmentRepository;
import com.civicconnect.api.repository.GrievanceRepository;
import com.civicconnect.api.repository.GrievanceStatusHistoryRepository;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.CivicUserPrincipal;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("mysql")
public class AdminAssignmentService {

    private final GrievanceRepository grievanceRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final GrievanceStatusHistoryRepository statusHistoryRepository;

    public AdminAssignmentService(
            GrievanceRepository grievanceRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            GrievanceStatusHistoryRepository statusHistoryRepository) {
        this.grievanceRepository = grievanceRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public GrievanceAssignmentResponse assign(
            String trackingId, AssignGrievanceRequest request, CivicUserPrincipal principal) {
        User admin = findVerifiedAdmin(principal);
        Grievance grievance = grievanceRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grievance not found"));
        rejectTerminalGrievance(grievance);

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        if (!department.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is inactive");
        }

        User officer = request.officerId() == null ? null : findEligibleOfficer(request.officerId(), department);
        grievance.setDepartment(department);
        grievance.setAssignedOfficer(officer);

        if (grievance.getStatus() == GrievanceStatus.SUBMITTED || grievance.getStatus() == GrievanceStatus.ACKNOWLEDGED) {
            GrievanceStatus previousStatus = grievance.getStatus();
            grievance.setStatus(GrievanceStatus.ASSIGNED);
            recordStatusChange(grievance, previousStatus, admin, assignmentNote(department, officer));
        }

        grievanceRepository.save(grievance);
        return new GrievanceAssignmentResponse(
                grievance.getTrackingId(), department.getCode(), officer == null ? null : officer.getId(), grievance.getStatus());
    }

    private User findVerifiedAdmin(CivicUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findById(principal.userId())
                .filter(User::isEnabled)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required"));
    }

    private User findEligibleOfficer(Long officerId, Department department) {
        User officer = userRepository.findById(officerId)
                .filter(User::isEnabled)
                .filter(user -> user.getRole() == UserRole.OFFICER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer not found"));
        if (officer.getDepartment() == null || !Objects.equals(officer.getDepartment().getId(), department.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Officer does not belong to the selected department");
        }
        return officer;
    }

    private void rejectTerminalGrievance(Grievance grievance) {
        if (grievance.getStatus() == GrievanceStatus.RESOLVED
                || grievance.getStatus() == GrievanceStatus.CLOSED
                || grievance.getStatus() == GrievanceStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Terminal grievances cannot be assigned");
        }
    }

    private void recordStatusChange(Grievance grievance, GrievanceStatus previousStatus, User changedBy, String note) {
        GrievanceStatusHistory history = new GrievanceStatusHistory();
        history.setGrievance(grievance);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(grievance.getStatus());
        history.setChangedBy(changedBy);
        history.setNote(note);
        statusHistoryRepository.save(history);
    }

    private String assignmentNote(Department department, User officer) {
        return officer == null
                ? "Assigned to department " + department.getCode()
                : "Assigned to department " + department.getCode() + " and an officer";
    }
}
