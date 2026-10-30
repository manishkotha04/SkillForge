package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.enums.InvitationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectInvitationRepository
        extends JpaRepository<ProjectInvitation, Long> {

    List<ProjectInvitation> findByProject_Id(Long projectId);

    @EntityGraph(attributePaths = {"project", "invitedBy"})
    @Query("""
           SELECT i
           FROM ProjectInvitation i
           WHERE i.invitedUser.id = :userId
           AND i.status = :status
           """)
    List<ProjectInvitation> findByInvitedUser_IdAndStatus(
            @Param("userId") Long userId,
            @Param("status") InvitationStatus status
    );

    long countByInvitedUser_IdAndStatus(
            Long userId,
            InvitationStatus status
    );

    boolean existsByProject_IdAndInvitedUser_IdAndStatus(
            Long projectId,
            Long invitedUserId,
            InvitationStatus status
    );

    long countByProject_IdAndStatusAndExpiresAtAfter(
            Long projectId,
            InvitationStatus status,
            LocalDateTime now
    );
}