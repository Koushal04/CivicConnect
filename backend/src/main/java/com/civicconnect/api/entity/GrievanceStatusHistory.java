package com.civicconnect.api.entity;

import com.civicconnect.api.enums.GrievanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "grievance_status_history")
public class GrievanceStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grievance_id", nullable = false)
    private Grievance grievance;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private GrievanceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private GrievanceStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GrievanceStatusHistory() { }

    public Long getId() { return id; }
    public Grievance getGrievance() { return grievance; }
    public void setGrievance(Grievance grievance) { this.grievance = grievance; }
    public GrievanceStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(GrievanceStatus previousStatus) { this.previousStatus = previousStatus; }
    public GrievanceStatus getNewStatus() { return newStatus; }
    public void setNewStatus(GrievanceStatus newStatus) { this.newStatus = newStatus; }
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
