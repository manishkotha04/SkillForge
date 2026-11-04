package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.entity.TaskMessage;
import com.skillforge.skillforge.repository.TaskMessageRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskMessageServiceTest {
    private final TaskMessageRepository messages = mock(TaskMessageRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final TaskMessageService service = new TaskMessageService(messages, tasks, currentUsers);

    private AppUser user(long id) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn("User " + id);
        return user;
    }

    private Task sharedTask(AppUser first, AppUser second, boolean archived) {
        Project project = new Project("P", "", "Java", first);
        project.setArchived(archived);
        Task task = new Task("Shared task", "", null, project, null);
        ReflectionTestUtils.setField(task, "id", 12L);
        Set<AppUser> assignees = new LinkedHashSet<>();
        assignees.add(first);
        assignees.add(second);
        task.setAssignees(assignees);
        return task;
    }

    @Test
    void assigneeCanSendMessageToSharedTaskChat() {
        AppUser first = user(1);
        AppUser second = user(2);
        Task task = sharedTask(first, second, false);
        when(currentUsers.getCurrentUser()).thenReturn(first);
        when(tasks.findById(12L)).thenReturn(Optional.of(task));
        when(messages.save(any(TaskMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskMessage saved = service.sendMessage(12L, "I will handle the API");

        assertEquals("I will handle the API", saved.getContent());
        assertSame(first, saved.getAuthor());
        verify(messages).save(any(TaskMessage.class));
    }

    @Test
    void nonAssigneeCannotAccessSharedTaskChat() {
        AppUser first = user(1);
        AppUser second = user(2);
        AppUser outsider = user(3);
        Task task = sharedTask(first, second, false);
        when(currentUsers.getCurrentUser()).thenReturn(outsider);
        when(tasks.findById(12L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class, () -> service.getMessages(12L));
    }

    @Test
    void archivedProjectBlocksNewChatMessages() {
        AppUser first = user(1);
        AppUser second = user(2);
        Task task = sharedTask(first, second, true);
        when(currentUsers.getCurrentUser()).thenReturn(first);
        when(tasks.findById(12L)).thenReturn(Optional.of(task));

        assertThrows(IllegalStateException.class, () -> service.sendMessage(12L, "late message"));
        verify(messages, never()).save(any(TaskMessage.class));
    }
}
