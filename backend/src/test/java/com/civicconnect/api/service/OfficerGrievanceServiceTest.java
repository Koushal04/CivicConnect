package com.civicconnect.api.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficerGrievanceServiceTest {

    @Mock private GrievanceRepository grievanceRepository;
    @Mock private GrievanceStatusHistoryRepository statusHistoryRepository;
    @Mock private UserRepository userRepository;

    @Test
    void assignedOfficerResolvesGrievanceAndCreatesHistory() {
        User officer = officer();
        Grievance grievance = grievance(GrievanceStatus.IN_PROGRESS);
        when(userRepository.findById(8L)).thenReturn(Optional.of(officer));
        when(grievanceRepository.findByTrackingIdAndAssignedOfficerId("CC-456", 8L)).thenReturn(Optional.of(grievance));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OfficerGrievanceService service = service();

        var response = service.updateStatus(
                "CC-456", new UpdateGrievanceStatusRequest(GrievanceStatus.RESOLVED, "Pothole repaired."), principal());

        assertThat(response.status()).isEqualTo(GrievanceStatus.RESOLVED);
        assertThat(grievance.getResolutionNotes()).isEqualTo("Pothole repaired.");
        assertThat(grievance.getResolvedAt()).isNotNull();
        ArgumentCaptor<GrievanceStatusHistory> historyCaptor = ArgumentCaptor.forClass(GrievanceStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getPreviousStatus()).isEqualTo(GrievanceStatus.IN_PROGRESS);
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(GrievanceStatus.RESOLVED);
        assertThat(historyCaptor.getValue().getChangedBy()).isSameAs(officer);
    }

    @Test
    void rejectsUpdateForGrievanceNotAssignedToOfficer() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(officer()));
        when(grievanceRepository.findByTrackingIdAndAssignedOfficerId("CC-OTHER", 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().updateStatus(
                "CC-OTHER", new UpdateGrievanceStatusRequest(GrievanceStatus.IN_PROGRESS, null), principal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidTransitionAndMissingResolutionNote() {
        User officer = officer();
        Grievance submitted = grievance(GrievanceStatus.SUBMITTED);
        when(userRepository.findById(8L)).thenReturn(Optional.of(officer));
        when(grievanceRepository.findByTrackingIdAndAssignedOfficerId("CC-456", 8L)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> service().updateStatus(
                "CC-456", new UpdateGrievanceStatusRequest(GrievanceStatus.IN_PROGRESS, null), principal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        submitted.setStatus(GrievanceStatus.ASSIGNED);
        assertThatThrownBy(() -> service().updateStatus(
                "CC-456", new UpdateGrievanceStatusRequest(GrievanceStatus.RESOLVED, " "), principal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private OfficerGrievanceService service() {
        return new OfficerGrievanceService(grievanceRepository, statusHistoryRepository, userRepository);
    }

    private CivicUserPrincipal principal() {
        return new CivicUserPrincipal(8L, "officer@example.com", UserRole.OFFICER);
    }

    private User officer() {
        User officer = new User();
        officer.setRole(UserRole.OFFICER);
        officer.setEnabled(true);
        return officer;
    }

    private Grievance grievance(GrievanceStatus status) {
        Grievance grievance = new Grievance();
        grievance.setTrackingId("CC-456");
        grievance.setStatus(status);
        return grievance;
    }
}
