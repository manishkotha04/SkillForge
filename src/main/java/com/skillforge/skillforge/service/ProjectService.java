
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

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

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

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectInvitationRepository projectInvitationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectInvitationRepository = projectInvitationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
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

}
