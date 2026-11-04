package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectActivity;
import com.skillforge.skillforge.repository.ProjectActivityRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProjectActivityService {
    private final ProjectActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectActivityService(ProjectActivityRepository activityRepository,
                                  ProjectRepository projectRepository,
                                  UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectActivity record(Long projectId, Long actorId, String eventType, String message) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        AppUser actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        return activityRepository.save(new ProjectActivity(project, actor, eventType, message));
    }

    @Transactional(readOnly = true)
    public List<ProjectActivity> getForProject(Long projectId) {
        return activityRepository.findByProject_IdOrderByCreatedAtDesc(projectId);
    }
}
