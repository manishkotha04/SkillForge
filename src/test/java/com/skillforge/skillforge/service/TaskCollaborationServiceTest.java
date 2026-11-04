package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.dto.TaskCreateRequest;
import com.skillforge.skillforge.dto.TaskUpdateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskCollaborationServiceTest {
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectActivityService activityService = mock(ProjectActivityService.class);
    private final TaskService service = new TaskService(taskRepository, projectRepository,
            projectMemberRepository, currentUserService, userRepository, activityService);

    private AppUser user(long id, String name) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(name);
        return user;
    }

    private Project project(AppUser owner, boolean archived) {
        Project project = new Project("Test project", "Description", "Java", owner);
        ReflectionTestUtils.setField(project, "id", 5L);
        project.setArchived(archived);
        return project;
    }

    @Test
    void oneTaskCanBeAssignedToTwoProjectMembers() {
        AppUser owner = user(1, "Owner");
        AppUser first = user(2, "First");
        AppUser second = user(3, "Second");
        Project project = project(owner, false);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 1L)).thenReturn(true);
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 2L)).thenReturn(true);
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 3L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(first));
        when(userRepository.findById(3L)).thenReturn(Optional.of(second));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Shared task");
        request.setAssigneeIds(List.of(2L, 3L));

        Task created = service.createTask(5L, request);

        assertEquals(2, created.getAssignees().size());
        assertTrue(created.getAssignees().contains(first));
        assertTrue(created.getAssignees().contains(second));
        assertEquals(TaskStatus.TODO, created.getStatus());
    }

    @Test
    void markingTaskDoneRecordsCompletionTimeAndCompletingUser() {
        AppUser owner = user(1, "Owner");
        Project project = project(owner, false);
        Task task = new Task("Finish", "", null, project, null);
        ReflectionTestUtils.setField(task, "id", 9L);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(task));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 1L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task done = service.updateTaskStatus(9L, TaskStatus.DONE);

        assertNotNull(done.getCompletedAt());
        assertSame(owner, done.getCompletedBy());
        verify(activityService).record(eq(5L), eq(1L), eq("TASK_COMPLETED"), contains("Finish"));
    }

    @Test
    void reopeningCompletedTaskClearsCurrentCompletionStamp() {
        AppUser owner = user(1, "Owner");
        Project project = project(owner, false);
        Task task = new Task("Finish", "", null, project, null);
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(java.time.LocalDateTime.now().minusHours(1));
        task.setCompletedBy(owner);
        ReflectionTestUtils.setField(task, "id", 9L);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(task));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 1L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task reopened = service.updateTaskStatus(9L, TaskStatus.IN_PROGRESS);

        assertNull(reopened.getCompletedAt());
        assertNull(reopened.getCompletedBy());
        verify(activityService).record(eq(5L), eq(1L), eq("TASK_REOPENED"), contains("Finish"));
    }

    @Test
    void archivedProjectBlocksTaskCreation() {
        AppUser owner = user(1, "Owner");
        Project project = project(owner, true);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(5L, 1L)).thenReturn(true);
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Should not create");

        assertThrows(IllegalStateException.class, () -> service.createTask(5L, request));
        verify(taskRepository, never()).save(any(Task.class));
    }
}
