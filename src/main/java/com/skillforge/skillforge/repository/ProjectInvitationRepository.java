package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectInvitationRepository
        extends JpaRepository<ProjectInvitation, Long> {

    List<ProjectInvitation> findByProject_Id(Long projectId);

    List<ProjectInvitation> findByInvitedUser_Id(Long userId);

    boolean existsByProject_IdAndInvitedUser_IdAndStatus(
            Long projectId,
            Long invitedUserId,
            InvitationStatus status
    );
}
