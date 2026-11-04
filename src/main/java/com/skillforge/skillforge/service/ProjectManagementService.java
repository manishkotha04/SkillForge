package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.ProjectUpdateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectManagementService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final ProjectService projectService;

    public ProjectManagementService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CurrentUserService currentUserService,
            ProjectService projectService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public Project getProjectForEditing(Long projectId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = findProject(projectId);
        requireEditor(project, currentUser);
        return project;
    }

    @Transactional(readOnly = true)
    public boolean canEditProject(Long projectId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null || project.isArchived()) {
            return false;
        }
        return isOwnerOrLeader(project, currentUser);
    }

    @Transactional
    public Project updateProject(Long projectId, ProjectUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Project update details are required.");
        }

        Project project = getProjectForEditing(projectId);
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (name.length() > 150) {
            throw new IllegalArgumentException("Project name cannot exceed 150 characters.");
        }
        if (request.getMaxTeamSize() == null || request.getMaxTeamSize() < 1) {
            throw new IllegalArgumentException("Maximum team size must be at least 1.");
        }

        long currentMemberCount = projectMemberRepository.countByProject_Id(projectId);
        if (request.getMaxTeamSize() < currentMemberCount) {
            throw new IllegalArgumentException(
                    "Maximum team size cannot be lower than the current number of project members.");
        }

        project.setName(name);
        project.setDescription(request.getDescription());
        project.setRequiredSkillsText(request.getRequiredSkillsText());
        project.setMaxTeamSize(request.getMaxTeamSize());
        project.setDeadline(request.getDeadline());
        return projectRepository.save(project);
    }

    @Transactional
    public Project createProjectWithDeadline(
            String name,
            String description,
            String requiredSkillsText,
            Integer maxTeamSize,
            LocalDate deadline) {
        Project project = projectService.createProject(
                name, description, requiredSkillsText, maxTeamSize);
        project.setDeadline(deadline);
        return projectRepository.save(project);
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private void requireEditor(Project project, AppUser currentUser) {
        if (project.isArchived()) {
            throw new IllegalStateException("Archived projects cannot be edited.");
        }
        if (!isOwnerOrLeader(project, currentUser)) {
            throw new AccessDeniedException(
                    "Only the project owner or an appointed project leader can edit this project.");
        }
    }

    private boolean isOwnerOrLeader(Project project, AppUser currentUser) {
        if (project.getOwner().getId().equals(currentUser.getId())) {
            return true;
        }
        List<ProjectMember> members =
                projectMemberRepository.findByProject_Id(project.getId());
        return members.stream().anyMatch(member ->
                member.getUser().getId().equals(currentUser.getId())
                        && member.getRole() == ProjectMemberRole.LEADER);
    }

    @Transactional(readOnly = true)
    public boolean canInviteToProject(Long projectId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null || project.isArchived()) {
            return false;
        }
        return isOwnerOrLeader(project, currentUser);
    }

    @Transactional
    public ProjectMember appointLeader(Long projectId, Long memberUserId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = findProject(projectId);
        requireProjectOwner(project, currentUser);
        requireActiveProject(project);

        if (memberUserId == null || memberUserId.equals(project.getOwner().getId())) {
            throw new IllegalArgumentException("Select an existing project member other than the owner.");
        }

        ProjectMember member = findMembership(projectId, memberUserId);
        if (member.getRole() == ProjectMemberRole.LEADER) {
            throw new IllegalArgumentException("This student is already a project leader.");
        }
        if (member.getRole() != ProjectMemberRole.MEMBER) {
            throw new IllegalArgumentException("Only a current regular member can be appointed as leader.");
        }

        boolean anotherLeaderExists = projectMemberRepository.findByProject_Id(projectId).stream()
                .anyMatch(existing -> existing.getRole() == ProjectMemberRole.LEADER
                        && !existing.getUser().getId().equals(memberUserId));
        if (anotherLeaderExists) {
            throw new IllegalStateException(
                    "This project already has a leader. Remove the current leader role before appointing another.");
        }

        member.setRole(ProjectMemberRole.LEADER);
        return projectMemberRepository.save(member);
    }

    @Transactional
    public ProjectMember removeLeader(Long projectId, Long memberUserId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = findProject(projectId);
        requireProjectOwner(project, currentUser);
        requireActiveProject(project);

        if (memberUserId == null || memberUserId.equals(project.getOwner().getId())) {
            throw new IllegalArgumentException("The project owner cannot be removed as a leader.");
        }

        ProjectMember member = findMembership(projectId, memberUserId);
        if (member.getRole() != ProjectMemberRole.LEADER) {
            throw new IllegalArgumentException("This project member is not currently a leader.");
        }

        member.setRole(ProjectMemberRole.MEMBER);
        return projectMemberRepository.save(member);
    }

    private void requireProjectOwner(Project project, AppUser currentUser) {
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the project owner can appoint or remove project leaders.");
        }
    }

    private void requireActiveProject(Project project) {
        if (project.isArchived()) {
            throw new IllegalStateException("Archived projects cannot change team roles.");
        }
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProject_Id(projectId).stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The selected student is not a current project member."));
    }


    @Transactional(readOnly = true)
    public boolean hasProjectLeader(Long projectId) {
        return projectMemberRepository.findByProject_Id(projectId).stream()
                .anyMatch(member -> member.getRole() == ProjectMemberRole.LEADER);
    }
}
