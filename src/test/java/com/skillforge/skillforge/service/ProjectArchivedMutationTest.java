package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectJoinRequest;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectArchivedMutationTest {

    private AppUser user(long id, String name) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(name);
        return user;
    }

    private Project archivedProject(AppUser owner) {
        Project project = new Project("Archived", "History", "Java", owner);
        ReflectionTestUtils.setField(project, "id", 21L);
        project.setArchived(true);
        return project;
    }

    @Test
    void archivedInvitationCannotBeAcceptedOrExpiredAsASideEffect() {
        AppUser owner = user(1L, "Owner");
        AppUser invitee = user(2L, "Invitee");
        Project project = archivedProject(owner);
        ProjectInvitation invitation = new ProjectInvitation(project, invitee, owner);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        ProjectInvitationRepository invitations = mock(ProjectInvitationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        OwnershipTransferRepository transfers = mock(OwnershipTransferRepository.class);
        LeaveRequestRepository leaves = mock(LeaveRequestRepository.class);
        ProjectService service = new ProjectService(
                projects, members, invitations, users, current, transfers, leaves);

        when(current.getCurrentUser()).thenReturn(invitee);
        when(invitations.findById(2L)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class, () -> service.acceptInvitation(2L));
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        verify(members, never()).save(any(ProjectMember.class));
        verify(invitations, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void invitationCannotBeRejectedAfterProjectIsArchived() {
        AppUser owner = user(1L, "Owner");
        AppUser invitee = user(2L, "Invitee");
        Project project = archivedProject(owner);
        ProjectInvitation invitation = new ProjectInvitation(project, invitee, owner);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        ProjectInvitationRepository invitations = mock(ProjectInvitationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        OwnershipTransferRepository transfers = mock(OwnershipTransferRepository.class);
        LeaveRequestRepository leaves = mock(LeaveRequestRepository.class);
        ProjectService service = new ProjectService(
                projects, members, invitations, users, current, transfers, leaves);

        when(current.getCurrentUser()).thenReturn(invitee);
        when(invitations.findById(3L)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class, () -> service.rejectInvitation(3L));
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        verify(invitations, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void archivedJoinRequestCannotBeApprovedEvenIfStudentIsAlreadyMember() {
        AppUser owner = user(1L, "Owner");
        AppUser member = user(2L, "Member");
        Project project = archivedProject(owner);
        ProjectJoinRequest request = new ProjectJoinRequest(project, member);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        ProjectJoinRequestRepository requests = mock(ProjectJoinRequestRepository.class);
        ProjectInvitationRepository invitations = mock(ProjectInvitationRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        ProjectDiscoveryService service = new ProjectDiscoveryService(
                projects, members, requests, invitations, current);

        when(current.getCurrentUser()).thenReturn(owner);
        when(requests.findById(4L)).thenReturn(Optional.of(request));
        when(members.existsByProject_IdAndUser_Id(21L, 2L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.approveJoinRequest(4L));
        assertEquals(ProjectJoinRequestStatus.PENDING, request.getStatus());
        verify(requests, never()).save(any(ProjectJoinRequest.class));
        verify(members, never()).save(any(ProjectMember.class));
    }

    @Test
    void archivedJoinRequestCannotBeRejected() {
        AppUser owner = user(1L, "Owner");
        AppUser requester = user(2L, "Requester");
        Project project = archivedProject(owner);
        ProjectJoinRequest request = new ProjectJoinRequest(project, requester);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        ProjectJoinRequestRepository requests = mock(ProjectJoinRequestRepository.class);
        ProjectInvitationRepository invitations = mock(ProjectInvitationRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        ProjectDiscoveryService service = new ProjectDiscoveryService(
                projects, members, requests, invitations, current);

        when(current.getCurrentUser()).thenReturn(owner);
        when(requests.findById(5L)).thenReturn(Optional.of(request));
        when(members.existsByProject_IdAndUser_Id(21L, 2L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.rejectJoinRequest(5L));
        assertEquals(ProjectJoinRequestStatus.PENDING, request.getStatus());
        verify(requests, never()).save(any(ProjectJoinRequest.class));
    }

    @Test
    void notificationReconciliationLeavesArchivedInvitationAndJoinRequestUntouched() {
        AppUser owner = user(1L, "Owner");
        AppUser invited = user(2L, "Invited");
        Project project = archivedProject(owner);
        ProjectInvitation invitation = new ProjectInvitation(project, invited, owner);
        ProjectJoinRequest request = new ProjectJoinRequest(project, invited);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        ProjectJoinRequestRepository requests = mock(ProjectJoinRequestRepository.class);
        ProjectInvitationRepository invitations = mock(ProjectInvitationRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        ProjectDiscoveryService service = new ProjectDiscoveryService(
                projects, members, requests, invitations, current);

        when(current.getCurrentUser()).thenReturn(invited);
        when(invitations.findByInvitedUser_IdAndStatus(2L, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));
        when(members.existsByProject_IdAndUser_Id(21L, 2L)).thenReturn(true);
        when(requests.findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
                2L, ProjectJoinRequestStatus.PENDING)).thenReturn(List.of());
        when(requests.findByRequestedBy_IdOrderByCreatedAtDesc(2L)).thenReturn(List.of(request));

        service.reconcileExistingMembershipsForCurrentUser();

        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertEquals(ProjectJoinRequestStatus.PENDING, request.getStatus());
        verify(invitations, never()).save(any(ProjectInvitation.class));
        verify(requests, never()).save(any(ProjectJoinRequest.class));
    }
}
