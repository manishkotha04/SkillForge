package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.ProjectExploreCard;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectJoinRequest;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectDiscoveryServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectJoinRequestRepository joinRequestRepository;
    @Mock private ProjectInvitationRepository invitationRepository;
    @Mock private CurrentUserService currentUserService;
    @InjectMocks private ProjectDiscoveryService service;

    private AppUser user(long id) {
        AppUser user = mock(AppUser.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Project project(long id, AppUser owner, int capacity) {
        Project project = new Project("Project " + id, "Collaborative project", "Java, SQL", owner);
        ReflectionTestUtils.setField(project, "id", id);
        project.setMaxTeamSize(capacity);
        return project;
    }

    @Test
    void discoveryShowsOnlyActiveAvailableProjectsNotOwnedOrJoinedByCurrentUser() {
        AppUser current = user(1);
        Project own = project(10, current, 5);
        Project available = project(11, user(2), 5);
        Project archived = project(12, user(3), 5);
        archived.setArchived(true);
        Project full = project(13, user(4), 1);
        Project alreadyJoined = project(14, user(5), 5);

        when(currentUserService.getCurrentUser()).thenReturn(current);
        when(projectRepository.findAll()).thenReturn(List.of(own, available, archived, full, alreadyJoined));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(11L)).thenReturn(1L);
        when(joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                11L, 1L, ProjectJoinRequestStatus.PENDING)).thenReturn(false);
        when(projectMemberRepository.existsByProject_IdAndUser_Id(13L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(13L)).thenReturn(1L);
        when(projectMemberRepository.existsByProject_IdAndUser_Id(14L, 1L)).thenReturn(true);

        List<ProjectExploreCard> result = service.findAvailableProjects("");

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getProject().getId());
    }

    @Test
    void discoverySearchMatchesProjectNameDescriptionOrSkillsIgnoringCase() {
        AppUser current = user(1);
        Project matching = project(11, user(2), 4);
        when(currentUserService.getCurrentUser()).thenReturn(current);
        when(projectRepository.findAll()).thenReturn(List.of(matching));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(11L)).thenReturn(1L);
        when(joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                11L, 1L, ProjectJoinRequestStatus.PENDING)).thenReturn(false);

        assertEquals(1, service.findAvailableProjects("JAVA").size());
        assertTrue(service.findAvailableProjects("unknown phrase").isEmpty());
    }

    @Test
    void studentCanSendJoinRequest() {
        AppUser owner = user(2);
        AppUser student = user(1);
        Project project = project(11, owner, 5);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(11L)).thenReturn(1L);
        when(joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                11L, 1L, ProjectJoinRequestStatus.PENDING)).thenReturn(false);
        when(joinRequestRepository.save(any(ProjectJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectJoinRequest result = service.requestToJoin(11L);
        assertEquals(ProjectJoinRequestStatus.PENDING, result.getStatus());
        assertSame(student, result.getRequestedBy());
        assertSame(project, result.getProject());
    }

    @Test
    void duplicatePendingJoinRequestIsRejected() {
        AppUser student = user(1);
        Project project = project(11, user(2), 5);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(joinRequestRepository.existsByProject_IdAndRequestedBy_IdAndStatus(
                11L, 1L, ProjectJoinRequestStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.requestToJoin(11L));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void ownerCanApproveRequestAndAddStudentAsMember() {
        AppUser owner = user(2);
        AppUser student = user(1);
        Project project = project(11, owner, 5);
        ProjectJoinRequest request = new ProjectJoinRequest(project, student);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(joinRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(11L)).thenReturn(2L);
        when(joinRequestRepository.save(any(ProjectJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectJoinRequest result = service.approveJoinRequest(100L);
        assertEquals(ProjectJoinRequestStatus.APPROVED, result.getStatus());
        assertSame(owner, result.getDecidedBy());
        assertNotNull(result.getDecidedAt());
        verify(projectMemberRepository).save(argThat(member ->
                member.getRole() == ProjectMemberRole.MEMBER
                        && member.getProject() == project
                        && member.getUser() == student));
    }

    @Test
    void nonOwnerCannotApproveJoinRequest() {
        AppUser owner = user(2);
        AppUser another = user(3);
        Project project = project(11, owner, 5);
        ProjectJoinRequest request = new ProjectJoinRequest(project, user(1));
        when(currentUserService.getCurrentUser()).thenReturn(another);
        when(joinRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () -> service.approveJoinRequest(100L));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void archivedProjectDoesNotAcceptJoinRequests() {
        AppUser student = user(1);
        Project project = project(11, user(2), 5);
        project.setArchived(true);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> service.requestToJoin(11L));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void pendingOwnerInvitationPreventsDuplicateJoinRequest() {
        AppUser student = user(1);
        Project project = project(11, user(2), 5);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(invitationRepository.existsByProject_IdAndInvitedUser_IdAndStatus(
                11L, 1L, InvitationStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.requestToJoin(11L));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void approvingJoinRequestClosesMatchingPendingOwnerInvitation() {
        AppUser owner = user(2);
        AppUser student = user(1);
        Project project = project(11, owner, 5);
        ProjectJoinRequest request = new ProjectJoinRequest(project, student);
        ProjectInvitation invitation = new ProjectInvitation(project, student, owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(joinRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(false);
        when(projectMemberRepository.countByProject_Id(11L)).thenReturn(2L);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(joinRequestRepository.save(any(ProjectJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.findByProject_Id(11L)).thenReturn(List.of(invitation));
        when(invitationRepository.save(any(ProjectInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectJoinRequest result = service.approveJoinRequest(100L);

        assertEquals(ProjectJoinRequestStatus.APPROVED, result.getStatus());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void legacyPendingInvitationIsClosedWhenStudentIsAlreadyMember() {
        AppUser student = user(1);
        AppUser owner = user(2);
        Project project = project(11, owner, 5);
        ProjectInvitation invitation = new ProjectInvitation(project, student, owner);

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(invitationRepository.findByInvitedUser_IdAndStatus(1L, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(11L, 1L)).thenReturn(true);
        when(joinRequestRepository.findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
                1L, ProjectJoinRequestStatus.PENDING)).thenReturn(List.of());
        when(joinRequestRepository.findByRequestedBy_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());
        when(invitationRepository.save(any(ProjectInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reconcileExistingMembershipsForCurrentUser();

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

}
