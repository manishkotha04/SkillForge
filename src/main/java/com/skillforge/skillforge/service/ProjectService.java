package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Project createProject(
            String name,
            String description,
            String requiredSkillsText) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }

        AppUser owner = currentUserService.getCurrentUser();

        Project project = new Project(
                name.trim(), description, requiredSkillsText, owner);

        Project savedProject = projectRepository.save(project);

        ProjectMember ownerMembership = new ProjectMember(
                savedProject, owner, ProjectMemberRole.OWNER);

        projectMemberRepository.save(ownerMembership);

        return savedProject;
    }

    @Transactional(readOnly = true)
    public List<Project> getMyProjects() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return projectRepository.findByOwner_Id(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
