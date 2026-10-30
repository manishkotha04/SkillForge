
package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.LeaveRequest;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    boolean existsByProject_IdAndRequestedBy_IdAndStatus(
            Long projectId,
            Long userId,
            LeaveRequestStatus status);

    @EntityGraph(attributePaths = {"project", "requestedBy"})
    List<LeaveRequest> findByProject_Owner_IdAndStatus(
            Long ownerId,
            LeaveRequestStatus status);

    @EntityGraph(attributePaths = {"project"})
    List<LeaveRequest> findByRequestedBy_IdAndStatus(
            Long userId,
            LeaveRequestStatus status);
}
