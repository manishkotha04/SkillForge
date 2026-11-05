package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProjectArchiveServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProjectActivityService activityService = mock(ProjectActivityService.class);
    private final ProjectArchiveService service = new ProjectArchiveService(
            projectRepository, currentUserService, activityService);

    private AppUser user(long id, String name) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(name);
        return user;
    }

    private Project project(AppUser owner, boolean archived) {
        Project project = new Project("FR-12 test project", "Description", "Java", owner);
        ReflectionTestUtils.setField(project, "id", 12L);
        project.setArchived(archived);
        return project;
    }

    @Test
    void currentOwnerCanArchiveProjectAndActivityIsRecorded() {
        AppUser owner = user(1L, "Owner");
        Project project = project(owner, false);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project result = service.archiveProject(12L);

        assertTrue(result.isArchived());
        verify(projectRepository).save(project);
        verify(activityService).record(
                eq(12L), eq(1L), eq("PROJECT_ARCHIVED"), contains("history have been preserved"));
    }

    @Test
    void regularMemberCannotArchiveProject() {
        AppUser owner = user(1L, "Owner");
        AppUser member = user(2L, "Member");
        Project project = project(owner, false);
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> service.archiveProject(12L));
        assertFalse(project.isArchived());
        verify(projectRepository, never()).save(any(Project.class));
        verifyNoInteractions(activityService);
    }

    @Test
    void appointedLeaderCannotArchiveProject() {
        AppUser owner = user(1L, "Owner");
        AppUser leader = user(3L, "Leader");
        Project project = project(owner, false);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> service.archiveProject(12L));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void unrelatedStudentCannotArchiveProject() {
        AppUser owner = user(1L, "Owner");
        AppUser outsider = user(4L, "Outsider");
        Project project = project(owner, false);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> service.archiveProject(12L));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void alreadyArchivedProjectCannotBeArchivedAgain() {
        AppUser owner = user(1L, "Owner");
        Project project = project(owner, true);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));

        assertThrows(IllegalStateException.class, () -> service.archiveProject(12L));
        verify(projectRepository, never()).save(any(Project.class));
        verifyNoInteractions(activityService);
    }
}
