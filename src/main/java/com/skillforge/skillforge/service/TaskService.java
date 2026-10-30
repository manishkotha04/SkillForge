package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.dto.TaskCreateRequest;
import com.skillforge.skillforge.dto.TaskDto;
import com.skillforge.skillforge.dto.TaskUpdateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForProject(Long projectId) {

        requireProjectMember(projectId);

        return taskRepository.findByProjectIdOrderByDueDateAsc(projectId);
    }

    @Transactional
    public Task createTask(
            Long projectId,
            TaskCreateRequest request) {

        AppUser currentUser = currentUserService.getCurrentUser();

        Project project = getProject(projectId);

        requireProjectMember(projectId, currentUser);

        AppUser assignee =
                resolveAssignee(
                        projectId,
                        request.getAssigneeId());

        Task task = new Task(
                request.getTitle().trim(),
                request.getDescription(),
                request.getDueDate(),
                project,
                assignee);

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(
            Long taskId,
            TaskUpdateRequest request) {

        AppUser currentUser = currentUserService.getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException(taskId));

        requireProjectMember(
                task.getProject().getId(),
                currentUser);

        AppUser assignee =
                resolveAssignee(
                        task.getProject().getId(),
                        request.getAssigneeId());

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setAssignee(assignee);

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTaskStatus(
            Long taskId,
            TaskStatus status) {

        AppUser currentUser = currentUserService.getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException(taskId));

        requireProjectMember(
                task.getProject().getId(),
                currentUser);

        task.setStatus(status);

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getTask(Long taskId) {

        AppUser currentUser = currentUserService.getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException(taskId));

        requireProjectMember(
                task.getProject().getId(),
                currentUser);

        return task;
    }
    
    @Transactional(readOnly = true)
    public List<AppUser> getProjectMembers(Long projectId) {
        requireProjectMember(projectId);

        return projectMemberRepository.findByProject_Id(projectId)
                .stream()
                .map(ProjectMember::getUser)
                .toList();
    }
    private Project getProject(Long projectId) {

        return projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ProjectNotFoundException(projectId));
    }

    private void requireProjectMember(
            Long projectId,
            AppUser user) {

        if (!projectMemberRepository
                .existsByProject_IdAndUser_Id(
                        projectId,
                        user.getId())) {

            throw new IllegalStateException(
                    "User is not a member of this project.");
        }
    }

    private void requireProjectMember(Long projectId) {

        requireProjectMember(
                projectId,
                currentUserService.getCurrentUser());
    }

    private AppUser resolveAssignee(
            Long projectId,
            Long assigneeId) {

        if (assigneeId == null) {
            return null;
        }

        if (!projectMemberRepository
                .existsByProject_IdAndUser_Id(
                        projectId,
                        assigneeId)) {

            throw new IllegalArgumentException(
                    "Task assignee must be a member of the project.");
        }

        return userRepository.findById(assigneeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assigned user does not exist."));
    }

    public TaskDto toDto(Task task) {

        TaskDto dto = new TaskDto();

        dto.setId(task.getId());
        dto.setProjectId(task.getProject().getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDueDate(task.getDueDate());
        dto.setStatus(task.getStatus());

        if (task.getAssignee() != null) {
            dto.setAssigneeId(task.getAssignee().getId());
            dto.setAssigneeDisplayName(
                    task.getAssignee().getFullName());
        }

        return dto;
    }
}