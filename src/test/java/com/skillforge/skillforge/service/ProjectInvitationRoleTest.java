package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
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
class ProjectInvitationRoleTest {

    private static final Long PROJECT_ID = 80L;

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectInvitationRepository projectInvitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private OwnershipTransferRepository ownershipTransferRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks private ProjectService service;

    private AppUser user(Long id) {
        AppUser value = mock(AppUser.class);
        lenient().when(value.getId()).thenReturn(id);
        return value;
    }

    private Project project(AppUser owner) {
        Project value = new Project("Invitation role test", "Description", "Java", owner);
        value.setMaxTeamSize(5);
        ReflectionTestUtils.setField(value, "id", PROJECT_ID);
        return value;
    }

    private void prepareInvitationChecks(Project project, AppUser actor, AppUser invitee) {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        when(projectMemberRepository.countByProject_Id(PROJECT_ID)).thenReturn(2L);
        when(projectInvitationRepository.countByProject_IdAndStatusAndExpiresAtAfter(
                eq(PROJECT_ID), eq(InvitationStatus.PENDING), any())).thenReturn(0L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(PROJECT_ID, 3L)).thenReturn(false);
        when(projectInvitationRepository.existsByProject_IdAndInvitedUser_IdAndStatus(
                PROJECT_ID, 3L, InvitationStatus.PENDING)).thenReturn(false);
        when(projectInvitationRepository.save(any(ProjectInvitation.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void ownerCanInviteStudent() {
        AppUser owner = user(1L);
        AppUser invitee = user(3L);
        Project project = project(owner);
        prepareInvitationChecks(project, owner, invitee);

        ProjectInvitation result = service.inviteUser(PROJECT_ID, 3L);

        assertSame(owner, result.getInvitedBy());
        assertSame(invitee, result.getInvitedUser());
        assertEquals(InvitationStatus.PENDING, result.getStatus());
    }

    @Test
    void appointedLeaderCanInviteStudent() {
        AppUser owner = user(1L);
        AppUser leader = user(2L);
        AppUser invitee = user(3L);
        Project project = project(owner);
        ProjectMember leaderMembership = new ProjectMember(project, leader, ProjectMemberRole.LEADER);
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(leaderMembership));
        prepareInvitationChecks(project, leader, invitee);

        ProjectInvitation result = service.inviteUser(PROJECT_ID, 3L);

        assertSame(leader, result.getInvitedBy());
        verify(projectInvitationRepository).save(any(ProjectInvitation.class));
    }

    @Test
    void regularMemberCannotInviteStudent() {
        AppUser owner = user(1L);
        AppUser member = user(2L);
        Project project = project(owner);
        ProjectMember membership = new ProjectMember(project, member, ProjectMemberRole.MEMBER);
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(membership));

        assertThrows(AccessDeniedException.class, () -> service.inviteUser(PROJECT_ID, 3L));
        verify(projectInvitationRepository, never()).save(any());
    }

    @Test
    void nonMemberCannotInviteStudent() {
        AppUser owner = user(1L);
        AppUser outsider = user(4L);
        Project project = project(owner);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> service.inviteUser(PROJECT_ID, 3L));
        verify(projectInvitationRepository, never()).save(any());
    }
}