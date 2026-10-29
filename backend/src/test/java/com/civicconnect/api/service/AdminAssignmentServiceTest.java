package com.civicconnect.api.service;

import com.civicconnect.api.dto.AssignGrievanceRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAssignmentServiceTest {

    @Mock private GrievanceRepository grievanceRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private GrievanceStatusHistoryRepository statusHistoryRepository;

    @Test
    void adminAssignsEligibleOfficerAndRecordsAssignedStatus() {
        User admin = user(UserRole.ADMIN);
        Department department = department();
        User officer = user(UserRole.OFFICER);
        officer.setDepartment(department);
        Grievance grievance = grievance(GrievanceStatus.SUBMITTED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(grievanceRepository.findByTrackingId("CC-123")).thenReturn(Optional.of(grievance));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(department));
        when(userRepository.findById(7L)).thenReturn(Optional.of(officer));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminAssignmentService service = service();

        var response = service.assign("CC-123", new AssignGrievanceRequest(4L, 7L), adminPrincipal());

        assertThat(grievance.getDepartment()).isSameAs(department);
        assertThat(grievance.getAssignedOfficer()).isSameAs(officer);
        assertThat(grievance.getStatus()).isEqualTo(GrievanceStatus.ASSIGNED);
        assertThat(response.departmentCode()).isEqualTo("ROADS");
        ArgumentCaptor<GrievanceStatusHistory> historyCaptor = ArgumentCaptor.forClass(GrievanceStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getPreviousStatus()).isEqualTo(GrievanceStatus.SUBMITTED);
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(GrievanceStatus.ASSIGNED);
        assertThat(historyCaptor.getValue().getChangedBy()).isSameAs(admin);
    }

    @Test
    void rejectsAssignmentWhenOfficerBelongsToAnotherDepartment() {
        User admin = user(UserRole.ADMIN);
        Department selectedDepartment = activeDepartmentWithoutCode();
        Department otherDepartment = mock(Department.class);
        when(otherDepartment.getId()).thenReturn(99L);
        User officer = user(UserRole.OFFICER);
        officer.setDepartment(otherDepartment);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(grievanceRepository.findByTrackingId("CC-123")).thenReturn(Optional.of(grievance(GrievanceStatus.SUBMITTED)));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(selectedDepartment));
        when(userRepository.findById(7L)).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> service().assign("CC-123", new AssignGrievanceRequest(4L, 7L), adminPrincipal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsNonAdminPrincipalEvenWhenRequestTargetsValidResources() {
        User officer = user(UserRole.OFFICER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> service().assign("CC-123", new AssignGrievanceRequest(4L, null), adminPrincipal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private AdminAssignmentService service() {
        return new AdminAssignmentService(grievanceRepository, departmentRepository, userRepository, statusHistoryRepository);
    }

    private CivicUserPrincipal adminPrincipal() {
        return new CivicUserPrincipal(1L, "admin@example.com", UserRole.ADMIN);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Department department() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(4L);
        when(department.getCode()).thenReturn("ROADS");
        when(department.isActive()).thenReturn(true);
        return department;
    }

    private Department activeDepartmentWithoutCode() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(4L);
        when(department.isActive()).thenReturn(true);
        return department;
    }

    private Grievance grievance(GrievanceStatus status) {
        Grievance grievance = new Grievance();
        grievance.setTrackingId("CC-123");
        grievance.setStatus(status);
        return grievance;
    }
}
