package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectArchiveService {

    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final ProjectActivityService projectActivityService;

    public ProjectArchiveService(ProjectRepository projectRepository,
                                 CurrentUserService currentUserService,
                                 ProjectActivityService projectActivityService) {
        this.projectRepository = projectRepository;
        this.currentUserService = currentUserService;
        this.projectActivityService = projectActivityService;
    }

    /**
     * Archives a project without deleting its project, membership, task,
     * discussion, invitation, portfolio, or activity records.
     * Only the current project owner can perform this irreversible action.
     */
    @Transactional
    public Project archiveProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required.");
        }

        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.getOwner() == null
                || !project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Only the current project owner can archive this project.");
        }

        if (project.isArchived()) {
            throw new IllegalStateException("This project has already been archived.");
        }

        project.setArchived(true);
        Project archivedProject = projectRepository.save(project);

        projectActivityService.record(
                projectId,
                currentUser.getId(),
                "PROJECT_ARCHIVED",
                currentUser.getFullName() + " archived project “" + project.getName() +
                        "”. The project and its history have been preserved.");

        return archivedProject;
    }
}
