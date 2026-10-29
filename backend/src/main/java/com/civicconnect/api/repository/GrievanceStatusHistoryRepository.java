package com.civicconnect.api.repository;

import com.civicconnect.api.entity.GrievanceStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrievanceStatusHistoryRepository extends JpaRepository<GrievanceStatusHistory, Long> {
    List<GrievanceStatusHistory> findByGrievanceIdOrderByCreatedAtAsc(Long grievanceId);
}
