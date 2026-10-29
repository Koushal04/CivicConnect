package com.civicconnect.api.service;

import com.civicconnect.api.dto.CreateGrievanceRequest;
import com.civicconnect.api.entity.Grievance;
import com.civicconnect.api.entity.GrievanceStatusHistory;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.GrievanceCategory;
import com.civicconnect.api.enums.GrievancePriority;
import com.civicconnect.api.enums.GrievanceStatus;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.GrievanceRepository;
import com.civicconnect.api.repository.GrievanceStatusHistoryRepository;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.CivicUserPrincipal;
import java.util.List;
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
class GrievanceServiceTest {

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private GrievanceStatusHistoryRepository statusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void submitsForPersistedCitizenAndCreatesInitialStatusHistory() {
        User citizen = citizen();
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GrievanceService service = service();

        var response = service.submit(request(), principal());

        ArgumentCaptor<Grievance> grievanceCaptor = ArgumentCaptor.forClass(Grievance.class);
        verify(grievanceRepository).save(grievanceCaptor.capture());
        Grievance grievance = grievanceCaptor.getValue();
        assertThat(grievance.getCitizen()).isSameAs(citizen);
        assertThat(grievance.getTrackingId()).startsWith("CC-").hasSize(39);
        assertThat(grievance.getStatus()).isEqualTo(GrievanceStatus.SUBMITTED);
        assertThat(grievance.getPriority()).isEqualTo(GrievancePriority.MEDIUM);
        assertThat(response.trackingId()).isEqualTo(grievance.getTrackingId());

        ArgumentCaptor<GrievanceStatusHistory> historyCaptor = ArgumentCaptor.forClass(GrievanceStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getGrievance()).isSameAs(grievance);
        assertThat(historyCaptor.getValue().getPreviousStatus()).isNull();
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(GrievanceStatus.SUBMITTED);
        assertThat(historyCaptor.getValue().getChangedBy()).isSameAs(citizen);
    }

    @Test
    void listsOnlyGrievancesReturnedForAuthenticatedCitizen() {
        User citizen = citizen();
        Grievance grievance = grievance("CC-123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(grievanceRepository.findByCitizenIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(grievance));
        GrievanceService service = service();

        var responses = service.listForCitizen(principal());

        assertThat(responses).extracting(response -> response.trackingId()).containsExactly("CC-123");
        verify(grievanceRepository).findByCitizenIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void doesNotRevealGrievanceOwnedByAnotherCitizen() {
        User citizen = citizen();
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(grievanceRepository.findByTrackingIdAndCitizenId("CC-OTHER", 1L)).thenReturn(Optional.empty());
        GrievanceService service = service();

        assertThatThrownBy(() -> service.findOwnedGrievance("CC-OTHER", principal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private GrievanceService service() {
        return new GrievanceService(grievanceRepository, statusHistoryRepository, userRepository);
    }

    private CivicUserPrincipal principal() {
        return new CivicUserPrincipal(1L, "citizen@example.com", UserRole.CITIZEN);
    }

    private User citizen() {
        User citizen = new User();
        citizen.setEmail("citizen@example.com");
        citizen.setRole(UserRole.CITIZEN);
        citizen.setEnabled(true);
        return citizen;
    }

    private CreateGrievanceRequest request() {
        return new CreateGrievanceRequest(
                GrievanceCategory.POTHOLE,
                "Large pothole",
                "A large pothole is obstructing traffic.",
                "Main Street near the bus stop",
                null,
                null);
    }

    private Grievance grievance(String trackingId) {
        Grievance grievance = new Grievance();
        grievance.setTrackingId(trackingId);
        grievance.setCategory(GrievanceCategory.POTHOLE);
        grievance.setTitle("Large pothole");
        grievance.setDescription("A large pothole is obstructing traffic.");
        grievance.setAddress("Main Street near the bus stop");
        grievance.setPriority(GrievancePriority.MEDIUM);
        grievance.setStatus(GrievanceStatus.SUBMITTED);
        return grievance;
    }
}
