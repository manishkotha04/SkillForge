
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.ProjectUpdateRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectManagementServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectManagementService service;

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Project project(AppUser owner) {
        Project project = new Project(
                "Old project",
                "Old description",
                "Java",
                owner
        );

        ReflectionTestUtils.setField(project, "id", 10L);
        return project;
    }

    private ProjectUpdateRequest request(String name, int teamSize) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName(name);
        request.setDescription("Updated description");
        request.setRequiredSkillsText("Java, SQL");
        request.setMaxTeamSize(teamSize);
        request.setDeadline(LocalDate.of(2027, 1, 31));
        return request;
    }

    @Test
    void ownerCanUpdateProjectAndDeadline() {
        AppUser owner = user(1L);
        Project project = project(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.countByProject_Id(10L))
                .thenReturn(1L);
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project updated = service.updateProject(
                10L, request("Updated name", 4));

        assertEquals("Updated name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("Java, SQL", updated.getRequiredSkillsText());
        assertEquals(4, updated.getMaxTeamSize());
        assertEquals(LocalDate.of(2027, 1, 31), updated.getDeadline());
    }

    @Test
    void appointedLeaderCanUpdateProject() {
        AppUser owner = user(1L);
        AppUser leader = user(2L);
        Project project = project(owner);

        ProjectMember leaderMembership = new ProjectMember(
                project, leader, ProjectMemberRole.LEADER);

        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(10L))
                .thenReturn(List.of(leaderMembership));
        when(projectMemberRepository.countByProject_Id(10L))
                .thenReturn(2L);
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project updated = service.updateProject(
                10L, request("Leader update", 4));

        assertEquals("Leader update", updated.getName());
    }

    @Test
    void regularMemberCannotUpdateProject() {
        AppUser owner = user(1L);
        AppUser member = user(2L);
        Project project = project(owner);

        ProjectMember membership = new ProjectMember(
                project, member, ProjectMemberRole.MEMBER);

        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(10L))
                .thenReturn(List.of(membership));

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateProject(
                        10L, request("No access", 4))
        );

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void nonMemberCannotUpdateProject() {
        AppUser owner = user(1L);
        AppUser outsider = user(3L);
        Project project = project(owner);

        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(10L))
                .thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateProject(
                        10L, request("No access", 4))
        );
    }

    @Test
    void archivedProjectCannotBeEdited() {
        AppUser owner = user(1L);
        Project project = project(owner);
        project.setArchived(true);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalStateException.class,
                () -> service.updateProject(
                        10L, request("No edit", 4))
        );

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void blankProjectNameIsRejected() {
        AppUser owner = user(1L);
        Project project = project(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProject(
                        10L, request("   ", 4))
        );
    }

    @Test
    void teamSizeCannotBeReducedBelowCurrentMembership() {
        AppUser owner = user(1L);
        Project project = project(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.countByProject_Id(10L))
                .thenReturn(3L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProject(
                        10L, request("Valid name", 2))
        );
    }
}
