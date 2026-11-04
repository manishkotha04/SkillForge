package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.ProjectJoinRequest;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectJoinRequestRepository extends JpaRepository<ProjectJoinRequest, Long> {

    boolean existsByProject_IdAndRequestedBy_IdAndStatus(
            Long projectId, Long requestedById, ProjectJoinRequestStatus status);

    @EntityGraph(attributePaths = {"project", "requestedBy"})
    List<ProjectJoinRequest> findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
            Long ownerId, ProjectJoinRequestStatus status);

    @EntityGraph(attributePaths = {"project", "decidedBy"})
    List<ProjectJoinRequest> findByRequestedBy_IdOrderByCreatedAtDesc(Long userId);

    long countByProject_Owner_IdAndStatus(Long ownerId, ProjectJoinRequestStatus status);
}
