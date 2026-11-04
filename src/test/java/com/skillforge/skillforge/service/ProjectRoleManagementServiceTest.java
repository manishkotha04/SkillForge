package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectRoleManagementServiceTest {

    private static final Long PROJECT_ID = 70L;

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private ProjectService projectService;

    @InjectMocks private ProjectManagementService service;

    private AppUser user(Long id) {
        AppUser value = mock(AppUser.class);
        lenient().when(value.getId()).thenReturn(id);
        return value;
    }

    private Project project(AppUser owner) {
        Project value = new Project("Role test", "Description", "Java", owner);
        ReflectionTestUtils.setField(value, "id", PROJECT_ID);
        return value;
    }

    @Test
    void ownerCanAppointCurrentMemberAsLeader() {
        AppUser owner = user(1L);
        AppUser memberUser = user(2L);
        Project project = project(owner);
        ProjectMember membership = new ProjectMember(project, memberUser, ProjectMemberRole.MEMBER);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(membership));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(i -> i.getArgument(0));

        ProjectMember result = service.appointLeader(PROJECT_ID, 2L);

        assertEquals(ProjectMemberRole.LEADER, result.getRole());
        verify(projectMemberRepository).save(membership);
    }

    @Test
    void ownerCanRemoveLeaderRoleWithoutRemovingMembership() {
        AppUser owner = user(1L);
        AppUser leaderUser = user(2L);
        Project project = project(owner);
        ProjectMember membership = new ProjectMember(project, leaderUser, ProjectMemberRole.LEADER);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(membership));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(i -> i.getArgument(0));

        ProjectMember result = service.removeLeader(PROJECT_ID, 2L);

        assertEquals(ProjectMemberRole.MEMBER, result.getRole());
        assertSame(leaderUser, result.getUser());
        verify(projectMemberRepository).save(membership);
    }

    @Test
    void regularMemberCannotAppointLeader() {
        AppUser owner = user(1L);
        AppUser member = user(2L);
        Project project = project(owner);
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> service.appointLeader(PROJECT_ID, 3L));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void nonMemberCannotBeAppointedLeader() {
        AppUser owner = user(1L);
        Project project = project(owner);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.appointLeader(PROJECT_ID, 9L));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void ownerCannotAppointThemselvesAsLeader() {
        AppUser owner = user(1L);
        Project project = project(owner);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> service.appointLeader(PROJECT_ID, 1L));
    }

    @Test
    void archivedProjectCannotChangeLeaderRoles() {
        AppUser owner = user(1L);
        Project project = project(owner);
        project.setArchived(true);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThrows(IllegalStateException.class, () -> service.appointLeader(PROJECT_ID, 2L));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void ownerCanInviteToProjectButOrdinaryMemberCannot() {
        AppUser owner = user(1L);
        Project project = project(owner);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        assertTrue(service.canInviteToProject(PROJECT_ID));

        AppUser member = user(2L);
        ProjectMember membership = new ProjectMember(project, member, ProjectMemberRole.MEMBER);
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(membership));
        assertFalse(service.canInviteToProject(PROJECT_ID));
    }

    @Test
    void secondLeaderCannotBeAppointedWhileAnotherLeaderExists() {
        AppUser owner = user(1L);
        AppUser currentLeaderUser = user(2L);
        AppUser nextLeaderUser = user(3L);
        Project project = project(owner);
        ProjectMember currentLeader = new ProjectMember(
                project, currentLeaderUser, ProjectMemberRole.LEADER);
        ProjectMember nextLeader = new ProjectMember(
                project, nextLeaderUser, ProjectMemberRole.MEMBER);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(currentLeader, nextLeader));

        assertThrows(IllegalStateException.class,
                () -> service.appointLeader(PROJECT_ID, 3L));
        assertEquals(ProjectMemberRole.LEADER, currentLeader.getRole());
        assertEquals(ProjectMemberRole.MEMBER, nextLeader.getRole());
        verify(projectMemberRepository, never()).save(any());
    }

}