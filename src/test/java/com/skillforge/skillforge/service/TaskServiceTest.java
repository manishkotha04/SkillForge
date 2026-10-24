package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import com.skillforge.skillforge.dto.TaskCreateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.enums.TaskStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private final TaskRepository taskRepository =
            mock(TaskRepository.class);

    private final ProjectRepository projectRepository =
            mock(ProjectRepository.class);

    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);

    private final CurrentUserService currentUserService =
            mock(CurrentUserService.class);

    private final UserRepository userRepository =
            mock(UserRepository.class);

    private final TaskService taskService =
            new TaskService(
                    taskRepository,
                    projectRepository,
                    projectMemberRepository,
                    currentUserService,
                    userRepository);

    @Test
    void getTasksForProject_returnsTasksForProjectMember() {

        Long projectId = 1L;
        Long userId = 10L;

        AppUser currentUser = mock(AppUser.class);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(currentUser.getId())
                .thenReturn(userId);

        when(projectMemberRepository
                .existsByProject_IdAndUser_Id(projectId, userId))
                .thenReturn(true);

        Task task = mock(Task.class);

        when(taskRepository
                .findByProjectIdOrderByDueDateAsc(projectId))
                .thenReturn(List.of(task));

        List<Task> result =
                taskService.getTasksForProject(projectId);

        assertEquals(1, result.size());
        assertSame(task, result.get(0));

        verify(taskRepository)
                .findByProjectIdOrderByDueDateAsc(projectId);
    }

    @Test
    void getTasksForProject_rejectsNonMember() {

        Long projectId = 1L;
        Long userId = 20L;

        AppUser currentUser = mock(AppUser.class);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(currentUser.getId())
                .thenReturn(userId);

        when(projectMemberRepository
                .existsByProject_IdAndUser_Id(projectId, userId))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> taskService.getTasksForProject(projectId));

        verify(taskRepository, never())
                .findByProjectIdOrderByDueDateAsc(projectId);
    }
    @Test
void createTask_rejectsAssigneeWhoIsNotProjectMember() {

    Long projectId = 1L;
    Long currentUserId = 10L;
    Long assigneeId = 20L;

    AppUser currentUser = mock(AppUser.class);
    Project project = mock(Project.class);

    when(currentUserService.getCurrentUser())
            .thenReturn(currentUser);

    when(currentUser.getId())
            .thenReturn(currentUserId);

    when(projectRepository.findById(projectId))
            .thenReturn(java.util.Optional.of(project));

    when(projectMemberRepository
            .existsByProject_IdAndUser_Id(projectId, currentUserId))
            .thenReturn(true);

    when(projectMemberRepository
            .existsByProject_IdAndUser_Id(projectId, assigneeId))
            .thenReturn(false);

    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("Test task");
    request.setAssigneeId(assigneeId);

    assertThrows(
            IllegalArgumentException.class,
            () -> taskService.createTask(projectId, request));

    verify(taskRepository, never()).save(any(Task.class));
}
    @Test
void createTask_createsTaskForProjectMember() {

    Long projectId = 1L;
    Long currentUserId = 10L;
    Long assigneeId = 20L;

    AppUser currentUser = mock(AppUser.class);
    AppUser assignee = mock(AppUser.class);
    Project project = mock(Project.class);

    when(currentUserService.getCurrentUser())
            .thenReturn(currentUser);

    when(currentUser.getId())
            .thenReturn(currentUserId);

    when(projectRepository.findById(projectId))
            .thenReturn(java.util.Optional.of(project));

    when(projectMemberRepository
            .existsByProject_IdAndUser_Id(projectId, currentUserId))
            .thenReturn(true);

    when(projectMemberRepository
            .existsByProject_IdAndUser_Id(projectId, assigneeId))
            .thenReturn(true);

    when(userRepository.findById(assigneeId))
            .thenReturn(java.util.Optional.of(assignee));

    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("Complete documentation");
    request.setDescription("Finish the project documentation.");
    request.setAssigneeId(assigneeId);
    
    when(taskRepository.save(any(Task.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Task savedTask = taskService.createTask(projectId, request);

    assertNotNull(savedTask);
    assertEquals("Complete documentation", savedTask.getTitle());
    assertEquals("Finish the project documentation.",
            savedTask.getDescription());
    assertSame(project, savedTask.getProject());
    assertSame(assignee, savedTask.getAssignee());
    assertEquals(TaskStatus.TODO, savedTask.getStatus());

    verify(taskRepository).save(any(Task.class));
}
}