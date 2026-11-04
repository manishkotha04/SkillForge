package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.entity.TaskMessage;
import com.skillforge.skillforge.repository.TaskMessageRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TaskMessageService {
    private final TaskMessageRepository messageRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public TaskMessageService(TaskMessageRepository messageRepository,
                              TaskRepository taskRepository,
                              CurrentUserService currentUserService) {
        this.messageRepository = messageRepository;
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Task getTaskForChat(Long taskId) {
        AppUser current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireSharedAssignee(task, current);
        return task;
    }

    @Transactional(readOnly = true)
    public List<TaskMessage> getMessages(Long taskId) {
        AppUser current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireSharedAssignee(task, current);
        return messageRepository.findByTask_IdOrderByCreatedAtAsc(taskId);
    }

    @Transactional
    public TaskMessage sendMessage(Long taskId, String content) {
        AppUser current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireSharedAssignee(task, current);
        if (task.getProject().isArchived()) {
            throw new IllegalStateException("Messages cannot be added to a task in an archived project.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content is required.");
        }
        if (content.trim().length() > 2000) {
            throw new IllegalArgumentException("Messages cannot exceed 2,000 characters.");
        }
        return messageRepository.save(new TaskMessage(task, current, content.trim()));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void requireSharedAssignee(Task task, AppUser current) {
        if (task.getAssignees().size() < 2) {
            throw new IllegalStateException("Shared chat is available when a task has at least two assignees.");
        }
        boolean assigned = task.getAssignees().stream()
                .anyMatch(user -> user.getId().equals(current.getId()));
        if (!assigned) {
            throw new AccessDeniedException("Only students assigned to this shared task can access its chat.");
        }
    }
}
