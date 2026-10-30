
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.List;
import com.skillforge.skillforge.entity.OwnershipTransfer;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import com.skillforge.skillforge.entity.LeaveRequest;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final OwnershipTransferRepository ownershipTransferRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectInvitationRepository projectInvitationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            OwnershipTransferRepository ownershipTransferRepository,
            LeaveRequestRepository leaveRequestRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectInvitationRepository = projectInvitationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.ownershipTransferRepository = ownershipTransferRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional
    public Project createProject(
            String name,
            String description,
            String requiredSkillsText,
            Integer maxTeamSize) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name is required.");
        }

        if (maxTeamSize == null || maxTeamSize < 1) {
            throw new IllegalArgumentException(
                    "Maximum team size must be at least 1.");
        }

        AppUser owner = currentUserService.getCurrentUser();

        Project project = new Project(
                name.trim(), description, requiredSkillsText, owner);

        project.setMaxTeamSize(maxTeamSize);

        Project savedProject = projectRepository.save(project);

        ProjectMember ownerMembership = new ProjectMember(
                savedProject, owner, ProjectMemberRole.OWNER);

        projectMemberRepository.save(ownerMembership);

        return savedProject;
    }


@Transactional(readOnly = true)
public List<Project> getMyProjects() {
    AppUser currentUser = currentUserService.getCurrentUser();

    List<Project> ownedProjects =
            projectRepository.findByOwner_Id(currentUser.getId());

    List<ProjectMember> memberships =
            projectMemberRepository.findByUser_Id(currentUser.getId());

    List<Project> myProjects = new ArrayList<>(ownedProjects);

    for (ProjectMember membership : memberships) {
        Project project = membership.getProject();

        boolean alreadyIncluded = myProjects.stream()
                .anyMatch(existing ->
                        existing.getId().equals(project.getId()));

        if (!alreadyIncluded) {
            myProjects.add(project);
        }
    }

    return myProjects;
}

@Transactional(readOnly = true)
public List<Project> getMyOwnedProjects() {
    AppUser currentUser = currentUserService.getCurrentUser();

    return projectRepository.findByOwner_Id(currentUser.getId());
}


    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    @Transactional(readOnly = true)
        public Project getProjectForCurrentUser(Long projectId) {
                AppUser currentUser = currentUserService.getCurrentUser();

                Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

                boolean isOwner = project.getOwner().getId()
            .equals(currentUser.getId());

                 boolean isMember =
            projectMemberRepository.existsByProject_IdAndUser_Id(
                    projectId, currentUser.getId());

         if (!isOwner && !isMember) {
        throw new AccessDeniedException(
                "You are not authorized to view this project.");
    }

    return project;
}

    @Transactional
    public ProjectInvitation inviteUser(Long projectId, Long invitedUserId) {
        if (invitedUserId == null) {
            throw new IllegalArgumentException(
                    "Invited user ID is required.");
        }

        AppUser currentUser = currentUserService.getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.isArchived()) {
            throw new IllegalArgumentException(
                    "Cannot invite users to an archived project.");
        }

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Only the project owner can invite users.");
        }


// CHECK TEAM CAPACITY BEFORE SENDING AN INVITATION
Integer maxTeamSize = project.getMaxTeamSize();

if (maxTeamSize != null) {
    long currentMemberCount =
            projectMemberRepository.countByProject_Id(projectId);

    long pendingInvitationCount =
            projectInvitationRepository
                    .countByProject_IdAndStatusAndExpiresAtAfter(
                            projectId,
                            InvitationStatus.PENDING,
                            LocalDateTime.now());

    if (currentMemberCount + pendingInvitationCount >= maxTeamSize) {
        throw new IllegalArgumentException(
                "The team is full or all available team slots "
                + "are reserved by pending invitations. "
                + "No more invitations can be sent.");
    }
}

// YOUR EXISTING CODE CONTINUES BELOW

        if (currentUser.getId().equals(invitedUserId)) {
            throw new IllegalArgumentException(
                    "You cannot invite yourself to your own project.");
        }

        AppUser invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invited user was not found."));

        if (projectMemberRepository.existsByProject_IdAndUser_Id(
                projectId, invitedUserId)) {
            throw new IllegalArgumentException(
                    "This user is already a project member.");
        }

        if (projectInvitationRepository
                .existsByProject_IdAndInvitedUser_IdAndStatus(
                        projectId, invitedUserId, InvitationStatus.PENDING)) {
            throw new IllegalArgumentException(
                    "This user already has a pending invitation.");
        }

        ProjectInvitation invitation = new ProjectInvitation(
                project, invitedUser, currentUser);

        return projectInvitationRepository.save(invitation);
    }

    @Transactional
    public ProjectMember acceptInvitation(Long invitationId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation was not found."));

        if (!invitation.getInvitedUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Only the invited student can accept this invitation.");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This invitation is no longer pending.");
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            throw new IllegalArgumentException(
                    "This invitation has expired.");
        }

        Project project = invitation.getProject();

        if (project.isArchived()) {
            throw new IllegalArgumentException(
                    "Cannot join an archived project.");
        }

        Long projectId = project.getId();
        Long userId = currentUser.getId();

        if (projectMemberRepository.existsByProject_IdAndUser_Id(
                projectId, userId)) {
            throw new IllegalArgumentException(
                    "You are already a member of this project.");
        }

        Integer maxTeamSize = project.getMaxTeamSize();

        if (maxTeamSize != null) {
            if (maxTeamSize < 1) {
                throw new IllegalStateException(
                        "Project team size must be at least 1.");
            }

            long currentMemberCount =
                    projectMemberRepository.countByProject_Id(projectId);

            if (currentMemberCount >= maxTeamSize) {
                throw new IllegalArgumentException(
                        "This project has reached its maximum team size.");
            }
        }

        ProjectMember membership = new ProjectMember(
                project, currentUser, ProjectMemberRole.MEMBER);

        projectMemberRepository.save(membership);
        invitation.setStatus(InvitationStatus.ACCEPTED);

        return membership;
    }

    @Transactional
    public ProjectInvitation rejectInvitation(Long invitationId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation was not found."));

        if (!invitation.getInvitedUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Only the invited student can reject this invitation.");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This invitation is no longer pending.");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        return invitation;
    }

@Transactional(readOnly = true)
public boolean isProjectOwner(Long projectId) {
    AppUser currentUser = currentUserService.getCurrentUser();

    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

    return project.getOwner().getId().equals(currentUser.getId());
}

@Transactional(readOnly = true)
public List<ProjectTeamSummary> getMyProjectTeamSummaries() {
    List<Project> projects = getMyProjects();

    return projects.stream()
            .map(project -> {
                List<ProjectMember> members =
                        projectMemberRepository.findByProject_Id(
                                project.getId());

                List<String> names = members.stream()
                        .map(ProjectMember::getUser)
                        .map(AppUser::getFullName)
                        .toList();

                return new ProjectTeamSummary(
                        project.getId(),
                        members.size(),
                        names
                );
            })
            .toList();
}

@Transactional
public OwnershipTransfer requestOwnershipTransfer(
        Long projectId,
        Long nominatedUserId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

    if (project.isArchived()) {
        throw new IllegalArgumentException(
                "Archived projects cannot transfer ownership.");
    }

    if (!project.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the current owner can request ownership transfer.");
    }

    if (nominatedUserId == null
            || nominatedUserId.equals(currentUser.getId())) {
        throw new IllegalArgumentException(
                "Select another project member as the new owner.");
    }

    if (!projectMemberRepository.existsByProject_IdAndUser_Id(
            projectId, nominatedUserId)) {
        throw new IllegalArgumentException(
                "The nominated user must already be a project member.");
    }

    if (ownershipTransferRepository.existsByProject_IdAndStatus(
            projectId, OwnershipTransferStatus.PENDING)) {
        throw new IllegalArgumentException(
                "This project already has a pending ownership transfer.");
    }

    AppUser nominatedUser = userRepository.findById(nominatedUserId)
            .orElseThrow(() -> new NoSuchElementException(
                    "The nominated user was not found."));

    OwnershipTransfer transfer = new OwnershipTransfer(
            project, currentUser, nominatedUser);

    return ownershipTransferRepository.save(transfer);
}

@Transactional(readOnly = true)
public List<OwnershipTransfer> getMyPendingOwnershipTransfers() {

    AppUser currentUser = currentUserService.getCurrentUser();

    return ownershipTransferRepository.findByNominatedUser_IdAndStatus(
            currentUser.getId(),
            OwnershipTransferStatus.PENDING);
}

@Transactional
public OwnershipTransfer acceptOwnershipTransfer(Long transferId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    OwnershipTransfer transfer = ownershipTransferRepository.findById(transferId)
            .orElseThrow(() -> new NoSuchElementException(
                    "Ownership transfer request was not found."));

    if (!transfer.getNominatedUser().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the nominated member can accept this request.");
    }

    if (transfer.getStatus() != OwnershipTransferStatus.PENDING) {
        throw new IllegalArgumentException(
                "This ownership transfer is no longer pending.");
    }

    Project project = transfer.getProject();

    if (project.isArchived()) {
        throw new IllegalArgumentException(
                "Archived projects cannot transfer ownership.");
    }

    if (!project.getOwner().getId()
            .equals(transfer.getRequestedBy().getId())) {
        throw new IllegalStateException(
                "The project owner has changed. This request is no longer valid.");
    }

    ProjectMember previousOwnerMembership =
            projectMemberRepository.findByProject_Id(project.getId())
                    .stream()
                    .filter(member -> member.getUser().getId()
                            .equals(transfer.getRequestedBy().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Previous owner membership was not found."));

    ProjectMember newOwnerMembership =
            projectMemberRepository.findByProject_Id(project.getId())
                    .stream()
                    .filter(member -> member.getUser().getId()
                            .equals(currentUser.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "New owner membership was not found."));

    project.setOwner(currentUser);
    previousOwnerMembership.setRole(ProjectMemberRole.MEMBER);
    newOwnerMembership.setRole(ProjectMemberRole.OWNER);

    transfer.setStatus(OwnershipTransferStatus.ACCEPTED);
    transfer.setDecidedAt(LocalDateTime.now());

    return transfer;
}

@Transactional
public OwnershipTransfer rejectOwnershipTransfer(Long transferId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    OwnershipTransfer transfer = ownershipTransferRepository.findById(transferId)
            .orElseThrow(() -> new NoSuchElementException(
                    "Ownership transfer request was not found."));

    if (!transfer.getNominatedUser().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the nominated member can reject this request.");
    }

    if (transfer.getStatus() != OwnershipTransferStatus.PENDING) {
        throw new IllegalArgumentException(
                "This ownership transfer is no longer pending.");
    }

    transfer.setStatus(OwnershipTransferStatus.REJECTED);
    transfer.setDecidedAt(LocalDateTime.now());

    return transfer;
}
@Transactional(readOnly = true)
public List<ProjectMember> getProjectMembers(Long projectId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

    if (!project.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the project owner can view transfer nominees.");
    }

    return projectMemberRepository.findByProject_Id(projectId);
}

@Transactional
public LeaveRequest requestToLeaveProject(Long projectId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

    if (project.isArchived()) {
        throw new IllegalArgumentException(
                "You cannot request to leave an archived project.");
    }

    if (project.getOwner().getId().equals(currentUser.getId())) {
        throw new IllegalArgumentException(
                "Project owners must transfer ownership before leaving.");
    }

    if (!projectMemberRepository.existsByProject_IdAndUser_Id(
            projectId, currentUser.getId())) {
        throw new AccessDeniedException(
                "You are not a member of this project.");
    }

    if (leaveRequestRepository
            .existsByProject_IdAndRequestedBy_IdAndStatus(
                    projectId,
                    currentUser.getId(),
                    LeaveRequestStatus.PENDING)) {
        throw new IllegalArgumentException(
                "You already have a pending leave request.");
    }

    LeaveRequest request = new LeaveRequest(project, currentUser);

    return leaveRequestRepository.save(request);
}

@Transactional(readOnly = true)
public List<LeaveRequest> getPendingLeaveRequestsForOwner() {

    AppUser currentUser = currentUserService.getCurrentUser();

    return leaveRequestRepository.findByProject_Owner_IdAndStatus(
            currentUser.getId(),
            LeaveRequestStatus.PENDING);
}

@Transactional
public LeaveRequest approveLeaveRequest(Long requestId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    LeaveRequest request = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException(
                    "Leave request was not found."));

    Project project = request.getProject();

    if (!project.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the project owner can approve leave requests.");
    }

    if (request.getStatus() != LeaveRequestStatus.PENDING) {
        throw new IllegalArgumentException(
                "This leave request is no longer pending.");
    }

    if (project.isArchived()) {
        throw new IllegalArgumentException(
                "Leave requests cannot be approved for archived projects.");
    }

    if (request.getRequestedBy().getId().equals(project.getOwner().getId())) {
        throw new IllegalStateException(
                "The project owner cannot leave without transferring ownership.");
    }

    ProjectMember membership = projectMemberRepository
            .findByProject_Id(project.getId())
            .stream()
            .filter(member -> member.getUser().getId()
                    .equals(request.getRequestedBy().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "The requesting member is no longer part of this project."));

    projectMemberRepository.delete(membership);

    request.setStatus(LeaveRequestStatus.APPROVED);
    request.setDecidedBy(currentUser);
    request.setDecidedAt(LocalDateTime.now());

    return request;
}

@Transactional
public LeaveRequest rejectLeaveRequest(Long requestId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    LeaveRequest request = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException(
                    "Leave request was not found."));

    Project project = request.getProject();

    if (!project.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException(
                "Only the project owner can reject leave requests.");
    }

    if (request.getStatus() != LeaveRequestStatus.PENDING) {
        throw new IllegalArgumentException(
                "This leave request is no longer pending.");
    }

    request.setStatus(LeaveRequestStatus.REJECTED);
    request.setDecidedBy(currentUser);
    request.setDecidedAt(LocalDateTime.now());

    return request;
}
@Transactional(readOnly = true)
public boolean hasPendingLeaveRequest(Long projectId) {

    AppUser currentUser = currentUserService.getCurrentUser();

    return leaveRequestRepository
            .existsByProject_IdAndRequestedBy_IdAndStatus(
                    projectId,
                    currentUser.getId(),
                    LeaveRequestStatus.PENDING);
}
}
