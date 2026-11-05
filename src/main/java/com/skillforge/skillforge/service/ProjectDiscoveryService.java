package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.ProjectExploreCard;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectJoinRequest;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class ProjectDiscoveryService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectJoinRequestRepository joinRequestRepository;
    private final ProjectInvitationRepository invitationRepository;
    private final CurrentUserService currentUserService;

    public ProjectDiscoveryService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectJoinRequestRepository joinRequestRepository,
            ProjectInvitationRepository invitationRepository,
            CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.invitationRepository = invitationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ProjectExploreCard> findAvailableProjects(String query) {
        AppUser currentUser = currentUserService.getCurrentUser();
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return projectRepository.findAll().stream()
                .filter(project -> !project.isArchived())
                .filter(project -> project.getOwner() != null
                        && !project.getOwner().getId().equals(currentUser.getId()))
                .filter(project -> !projectMemberRepository.existsByProject_IdAndUser_Id(
                        project.getId(), currentUser.getId()))
                .map(project -> new ProjectExploreCard(
                        project,
                        projectMemberRepository.countByProject_Id(project.getId()),
                        joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                                project.getId(), currentUser.getId(), ProjectJoinRequestStatus.PENDING)))
                .filter(card -> card.getProject().getMaxTeamSize() == null
                        || card.getCurrentMemberCount() < card.getProject().getMaxTeamSize())
                .filter(card -> matches(card.getProject(), term))
                .toList();
    }

    private boolean matches(Project project, String term) {
        if (term.isBlank()) return true;
        return contains(project.getName(), term)
                || contains(project.getDescription(), term)
                || contains(project.getRequiredSkillsText(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    @Transactional
    public ProjectJoinRequest requestToJoin(Long projectId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = findProject(projectId);

        if (project.isArchived()) {
            throw new IllegalArgumentException("Archived projects are not accepting join requests.");
        }
        if (project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You already own this project.");
        }
        if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, currentUser.getId())) {
            throw new IllegalArgumentException("You are already a member of this project.");
        }
        if (invitationRepository.existsByProject_IdAndInvitedUser_IdAndStatus(
                projectId, currentUser.getId(), InvitationStatus.PENDING)) {
            throw new IllegalArgumentException(
                    "The project owner has already invited you. Please respond to that invitation in Notifications instead of sending a separate join request.");
        }
        if (joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                projectId, currentUser.getId(), ProjectJoinRequestStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending join request for this project.");
        }
        ensureCapacity(project);

        return joinRequestRepository.save(new ProjectJoinRequest(project, currentUser));
    }

    @Transactional(readOnly = true)
    public boolean hasPendingJoinRequest(Long projectId, Long studentId) {
        return joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                projectId, studentId, ProjectJoinRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ProjectJoinRequest> getPendingRequestsForCurrentOwner() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return joinRequestRepository.findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
                currentUser.getId(), ProjectJoinRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ProjectJoinRequest> getMyJoinRequests() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return joinRequestRepository.findByRequestedBy_IdOrderByCreatedAtDesc(currentUser.getId());
    }

    /**
     * Reconciles legacy duplicate records created when the same student had
     * both a pending invitation and a pending join request for one project.
     * Membership is the source of truth: a pending request/invitation is closed
     * once the student is already a member.
     */
    @Transactional
    public void reconcileExistingMembershipsForCurrentUser() {
        AppUser currentUser = currentUserService.getCurrentUser();
        Long userId = currentUser.getId();

        List<ProjectInvitation> pendingInvitations =
                invitationRepository.findByInvitedUser_IdAndStatus(userId, InvitationStatus.PENDING);
        for (ProjectInvitation invitation : pendingInvitations) {
            Long projectId = invitation.getProject().getId();
            if (invitation.getProject().isArchived()) {
                continue;
            }
            if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, userId)) {
                invitation.setStatus(InvitationStatus.ACCEPTED);
                invitationRepository.save(invitation);
            }
        }

        List<ProjectJoinRequest> requestsReceived =
                joinRequestRepository.findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
                        userId, ProjectJoinRequestStatus.PENDING);
        for (ProjectJoinRequest request : requestsReceived) {
            reconcileRequestIfAlreadyMember(request);
        }

        List<ProjectJoinRequest> requestsSent =
                joinRequestRepository.findByRequestedBy_IdOrderByCreatedAtDesc(userId);
        for (ProjectJoinRequest request : requestsSent) {
            reconcileRequestIfAlreadyMember(request);
        }
    }

    /** Call after a student accepts an owner invitation. */
    @Transactional
    public void reconcilePendingJoinRequestsAfterInvitationAccepted(Long projectId, Long studentId) {
        List<ProjectJoinRequest> requests = joinRequestRepository
                .findByRequestedBy_IdOrderByCreatedAtDesc(studentId);
        for (ProjectJoinRequest request : requests) {
            if (request.getProject().getId().equals(projectId)
                    && !request.getProject().isArchived()
                    && request.getStatus() == ProjectJoinRequestStatus.PENDING) {
                markApproved(request, request.getProject().getOwner());
                joinRequestRepository.save(request);
            }
        }
    }

    @Transactional
    public ProjectJoinRequest approveJoinRequest(Long requestId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        ProjectJoinRequest request = findJoinRequest(requestId);
        Project project = request.getProject();
        requireOwner(project, currentUser);
        requirePending(request);
        if (project.isArchived()) {
            throw new IllegalStateException("Archived projects are read-only. Join requests cannot be reviewed.");
        }

        // Repair a legacy/stale request instead of trying to add the same student twice.
        if (projectMemberRepository.existsByProject_IdAndUser_Id(
                project.getId(), request.getRequestedBy().getId())) {
            markApproved(request, currentUser);
            closePendingInvitations(project.getId(), request.getRequestedBy().getId());
            return joinRequestRepository.save(request);
        }

        ensureCapacity(project);

        projectMemberRepository.save(new ProjectMember(
                project, request.getRequestedBy(), ProjectMemberRole.MEMBER));
        markApproved(request, currentUser);
        closePendingInvitations(project.getId(), request.getRequestedBy().getId());
        return joinRequestRepository.save(request);
    }

    @Transactional
    public ProjectJoinRequest rejectJoinRequest(Long requestId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        ProjectJoinRequest request = findJoinRequest(requestId);
        requireOwner(request.getProject(), currentUser);
        requirePending(request);
        if (request.getProject().isArchived()) {
            throw new IllegalStateException("Archived projects are read-only. Join requests cannot be reviewed.");
        }

        // A rejection is not a member-removal operation.
        if (projectMemberRepository.existsByProject_IdAndUser_Id(
                request.getProject().getId(), request.getRequestedBy().getId())) {
            markApproved(request, currentUser);
            closePendingInvitations(request.getProject().getId(), request.getRequestedBy().getId());
            return joinRequestRepository.save(request);
        }

        request.setStatus(ProjectJoinRequestStatus.REJECTED);
        request.setDecidedBy(currentUser);
        request.setDecidedAt(LocalDateTime.now());
        return joinRequestRepository.save(request);
    }

    private void reconcileRequestIfAlreadyMember(ProjectJoinRequest request) {
        if (!request.getProject().isArchived()
                && request.getStatus() == ProjectJoinRequestStatus.PENDING
                && projectMemberRepository.existsByProject_IdAndUser_Id(
                        request.getProject().getId(), request.getRequestedBy().getId())) {
            markApproved(request, request.getProject().getOwner());
            joinRequestRepository.save(request);
            closePendingInvitations(request.getProject().getId(), request.getRequestedBy().getId());
        }
    }

    private void markApproved(ProjectJoinRequest request, AppUser owner) {
        request.setStatus(ProjectJoinRequestStatus.APPROVED);
        request.setDecidedBy(owner);
        request.setDecidedAt(LocalDateTime.now());
    }

    private void closePendingInvitations(Long projectId, Long studentId) {
        for (ProjectInvitation invitation : invitationRepository.findByProject_Id(projectId)) {
            if (invitation.getProject().isArchived()) {
                continue;
            }
            if (invitation.getInvitedUser().getId().equals(studentId)
                    && invitation.getStatus() == InvitationStatus.PENDING) {
                invitation.setStatus(InvitationStatus.ACCEPTED);
                invitationRepository.save(invitation);
            }
        }
    }

    private void ensureCapacity(Project project) {
        Integer maxTeamSize = project.getMaxTeamSize();
        if (maxTeamSize != null
                && projectMemberRepository.countByProject_Id(project.getId()) >= maxTeamSize) {
            throw new IllegalArgumentException("This project has reached its maximum team size.");
        }
    }

    private void requireOwner(Project project, AppUser currentUser) {
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the project owner can review join requests.");
        }
    }

    private void requirePending(ProjectJoinRequest request) {
        if (request.getStatus() != ProjectJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("This join request has already been reviewed.");
        }
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project was not found."));
    }

    private ProjectJoinRequest findJoinRequest(Long requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Join request was not found."));
    }
}
