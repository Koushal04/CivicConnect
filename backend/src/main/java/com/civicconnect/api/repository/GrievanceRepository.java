package com.civicconnect.api.repository;

import com.civicconnect.api.entity.Grievance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    Optional<Grievance> findByTrackingId(String trackingId);
    Optional<Grievance> findByTrackingIdAndCitizenId(String trackingId, Long citizenId);
    Optional<Grievance> findByTrackingIdAndAssignedOfficerId(String trackingId, Long officerId);
    List<Grievance> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);
    List<Grievance> findByAssignedOfficerIdOrderByCreatedAtDesc(Long officerId);
    boolean existsByTrackingId(String trackingId);
}
