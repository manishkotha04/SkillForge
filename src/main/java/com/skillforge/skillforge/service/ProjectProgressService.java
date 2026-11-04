package com.skillforge.skillforge.service;

import com.skillforge.skillforge.dto.ProjectProgressSummary;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectProgressService {

    private final TaskRepository taskRepository;

    public ProjectProgressService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Calculates project completion from persisted task statuses.
     * A project with no tasks has 0% progress. Each task is counted once,
     * regardless of whether it has one assignee or multiple assignees.
     */
    @Transactional(readOnly = true)
    public ProjectProgressSummary calculateProgress(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required.");
        }

        long totalTasks = taskRepository.countByProject_Id(projectId);
        long completedTasks = taskRepository.countByProject_IdAndStatus(
                projectId, TaskStatus.DONE);

        int percentage = totalTasks == 0
                ? 0
                : (int) Math.round((completedTasks * 100.0) / totalTasks);

        return new ProjectProgressSummary(totalTasks, completedTasks, percentage);
    }
}
