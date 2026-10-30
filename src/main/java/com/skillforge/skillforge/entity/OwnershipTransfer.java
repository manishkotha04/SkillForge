package com.skillforge.skillforge.entity;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ownership_transfers")
public class OwnershipTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OwnershipTransferStatus status =
            OwnershipTransferStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private AppUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nominated_user_id", nullable = false)
    private AppUser nominatedUser;

    protected OwnershipTransfer() {
    }

    public OwnershipTransfer(
            Project project,
            AppUser requestedBy,
            AppUser nominatedUser) {
        this.project = project;
        this.requestedBy = requestedBy;
        this.nominatedUser = nominatedUser;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public OwnershipTransferStatus getStatus() {
        return status;
    }

    public void setStatus(OwnershipTransferStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Project getProject() {
        return project;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public AppUser getNominatedUser() {
        return nominatedUser;
    }
}