package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.dto.TaskCreateRequest;
import com.skillforge.skillforge.dto.TaskDto;
import com.skillforge.skillforge.dto.TaskUpdateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ProjectActivityService activityService;

    @Autowired
    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository,
                       CurrentUserService currentUserService,
                       UserRepository userRepository,
                       ProjectActivityService activityService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.activityService = activityService;
    }

    // Compatibility constructor retained for existing unit tests/callers.
    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository,
                       CurrentUserService currentUserService,
                       UserRepository userRepository) {
        this(taskRepository, projectRepository, projectMemberRepository,
                currentUserService, userRepository, null);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForProject(Long projectId) {
        requireProjectMember(projectId);
        return taskRepository.findByProjectIdOrderByDueDateAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<Task> getMyTasksForProject(Long projectId) {
        AppUser current = currentUserService.getCurrentUser();
        return getTasksForProject(projectId).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getAssignees().stream()
                        .anyMatch(user -> user.getId().equals(current.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Task> getCompletedTasksForProject(Long projectId) {
        return getTasksForProject(projectId).stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .sorted((a, b) -> {
                    if (a.getCompletedAt() == null) return 1;
                    if (b.getCompletedAt() == null) return -1;
                    return b.getCompletedAt().compareTo(a.getCompletedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Project getProjectForTaskPage(Long projectId) {
        requireProjectMember(projectId);
        return getProject(projectId);
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        return currentUserService.getCurrentUser().getId();
    }

    @Transactional
    public Task createTask(Long projectId, TaskCreateRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = getProject(projectId);
        requireProjectMember(projectId, currentUser);
        requireActiveProject(project);

        List<AppUser> assignees = resolveAssignees(projectId, request.getAssigneeIds());
        Task task = new Task(request.getTitle().trim(), request.getDescription(),
                request.getDueDate(), project, null);
        task.setAssignees(assignees);
        Task saved = taskRepository.save(task);
        record(project, currentUser, "TASK_CREATED",
                currentUser.getFullName() + " created task “" + saved.getTitle() + "”.");
        if (!assignees.isEmpty()) {
            record(project, currentUser, "TASK_ASSIGNED",
                    "Task “" + saved.getTitle() + "” assigned to " + names(assignees) + ".");
        }
        return saved;
    }

    @Transactional
    public Task updateTask(Long taskId, TaskUpdateRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }
        AppUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireProjectMember(task.getProject().getId(), currentUser);
        requireActiveProject(task.getProject());

        Set<Long> beforeAssigneeIds = task.getAssignees().stream()
                .map(AppUser::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<AppUser> assignees = resolveAssignees(task.getProject().getId(), request.getAssigneeIds());

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setAssignees(assignees);
        if (request.getStatus() != null) {
            applyStatus(task, request.getStatus(), currentUser);
        }
        Task saved = taskRepository.save(task);

        Set<Long> afterAssigneeIds = assignees.stream().map(AppUser::getId).collect(Collectors.toSet());
        if (!beforeAssigneeIds.equals(afterAssigneeIds)) {
            record(task.getProject(), currentUser, "TASK_ASSIGNED",
                    "Assignees updated for task “" + task.getTitle() + "”: " + names(assignees) + ".");
        }
        return saved;
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus status) {
        if (status == null) throw new IllegalArgumentException("Choose a task status.");
        AppUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireProjectMember(task.getProject().getId(), currentUser);
        requireActiveProject(task.getProject());
        applyStatus(task, status, currentUser);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getTask(Long taskId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireProjectMember(task.getProject().getId(), currentUser);
        return task;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getProjectMembers(Long projectId) {
        requireProjectMember(projectId);
        return projectMemberRepository.findByProject_Id(projectId).stream()
                .map(ProjectMember::getUser).toList();
    }

    private void applyStatus(Task task, TaskStatus next, AppUser actor) {
        TaskStatus previous = task.getStatus();
        if (previous == next) return;
        task.setStatus(next);
        if (next == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
            task.setCompletedBy(actor);
            record(task.getProject(), actor, "TASK_COMPLETED",
                    actor.getFullName() + " completed task “" + task.getTitle() + "”.");
        } else if (previous == TaskStatus.DONE) {
            task.setCompletedAt(null);
            task.setCompletedBy(null);
            record(task.getProject(), actor, "TASK_REOPENED",
                    actor.getFullName() + " reopened task “" + task.getTitle() + "”.");
        } else {
            record(task.getProject(), actor, "TASK_STATUS_CHANGED",
                    actor.getFullName() + " changed task “" + task.getTitle() + "” to " + next.name().replace('_', ' ').toLowerCase() + ".");
        }
    }

    private List<AppUser> resolveAssignees(Long projectId, List<Long> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) return List.of();
        Map<Long, AppUser> resolved = new LinkedHashMap<>();
        for (Long assigneeId : assigneeIds) {
            if (assigneeId == null || resolved.containsKey(assigneeId)) continue;
            if (!projectMemberRepository.existsByProject_IdAndUser_Id(projectId, assigneeId)) {
                throw new IllegalArgumentException("Every task assignee must be a member of this project.");
            }
            AppUser assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("Assigned user does not exist."));
            resolved.put(assigneeId, assignee);
        }
        return new ArrayList<>(resolved.values());
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private void requireProjectMember(Long projectId, AppUser user) {
        if (!projectMemberRepository.existsByProject_IdAndUser_Id(projectId, user.getId())) {
            throw new IllegalStateException("User is not a member of this project.");
        }
    }

    private void requireProjectMember(Long projectId) {
        requireProjectMember(projectId, currentUserService.getCurrentUser());
    }

    private void requireActiveProject(Project project) {
        if (project.isArchived()) {
            throw new IllegalStateException("Tasks cannot be changed because this project is archived.");
        }
    }

    private String names(List<AppUser> users) {
        if (users == null || users.isEmpty()) return "no one";
        return users.stream().map(AppUser::getFullName).collect(Collectors.joining(", "));
    }

    private void record(Project project, AppUser actor, String type, String message) {
        if (activityService != null) activityService.record(project.getId(), actor.getId(), type, message);
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
            dto.setAssigneeDisplayName(task.getAssignee().getFullName());
        }
        return dto;
    }
}
