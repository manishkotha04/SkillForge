package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.LeaveRequest;
import com.skillforge.skillforge.entity.OwnershipTransfer;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.ProjectMember;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceMembershipTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long REQUEST_ID = 200L;
    private static final Long TRANSFER_ID = 300L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OwnershipTransferRepository ownershipTransferRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private ProjectService projectService;

   private AppUser mockUser(Long id) {
    AppUser user = mock(AppUser.class);
    lenient().when(user.getId()).thenReturn(id);
    return user;
}

    private Project createProject(AppUser owner) {
        Project project = new Project(
                "SkillForge Test Project",
                "Test project description",
                "Java, Spring Boot",
                owner
        );

        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    @Test
    void memberCanSubmitLeaveRequest() {
        AppUser owner = mockUser(1L);
        AppUser member = mockUser(2L);
        Project project = createProject(owner);

        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository
                .existsByProject_IdAndUser_Id(PROJECT_ID, 2L))
                .thenReturn(true);
        when(leaveRequestRepository
                .existsByProject_IdAndRequestedBy_IdAndStatus(
                        PROJECT_ID, 2L, LeaveRequestStatus.PENDING))
                .thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest result =
                projectService.requestToLeaveProject(PROJECT_ID);

        assertEquals(LeaveRequestStatus.PENDING, result.getStatus());
        assertSame(project, result.getProject());
        assertSame(member, result.getRequestedBy());

        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void projectOwnerCannotSubmitLeaveRequest() {
        AppUser owner = mockUser(1L);
        Project project = createProject(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.requestToLeaveProject(PROJECT_ID)
        );

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void nonMemberCannotSubmitLeaveRequest() {
        AppUser owner = mockUser(1L);
        AppUser student = mockUser(2L);
        Project project = createProject(owner);

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository
                .existsByProject_IdAndUser_Id(PROJECT_ID, 2L))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> projectService.requestToLeaveProject(PROJECT_ID)
        );

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void ownerCanApproveMemberLeaveRequest() {
        AppUser owner = mockUser(1L);
        AppUser member = mockUser(2L);
        Project project = createProject(owner);

        LeaveRequest request = new LeaveRequest(project, member);

        ProjectMember membership = new ProjectMember(
                project, member, ProjectMemberRole.MEMBER
        );

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(leaveRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(request));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(membership));

        LeaveRequest result =
                projectService.approveLeaveRequest(REQUEST_ID);

        assertEquals(LeaveRequestStatus.APPROVED, result.getStatus());
        assertSame(owner, result.getDecidedBy());
        assertNotNull(result.getDecidedAt());

        verify(projectMemberRepository).delete(membership);
    }

    @Test
    void ownerCanRejectMemberLeaveRequest() {
        AppUser owner = mockUser(1L);
        AppUser member = mockUser(2L);
        Project project = createProject(owner);

        LeaveRequest request = new LeaveRequest(project, member);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(leaveRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(request));

        LeaveRequest result =
                projectService.rejectLeaveRequest(REQUEST_ID);

        assertEquals(LeaveRequestStatus.REJECTED, result.getStatus());
        assertSame(owner, result.getDecidedBy());
        assertNotNull(result.getDecidedAt());

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void ownerCanRequestOwnershipTransferToExistingMember() {
        AppUser owner = mockUser(1L);
        AppUser nominee = mockUser(2L);
        Project project = createProject(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository
                .existsByProject_IdAndUser_Id(PROJECT_ID, 2L))
                .thenReturn(true);
        when(ownershipTransferRepository
                .existsByProject_IdAndStatus(
                        PROJECT_ID, OwnershipTransferStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(nominee));
        when(ownershipTransferRepository.save(
                any(OwnershipTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnershipTransfer result =
                projectService.requestOwnershipTransfer(PROJECT_ID, 2L);

        assertEquals(
                OwnershipTransferStatus.PENDING,
                result.getStatus()
        );
        assertSame(owner, result.getRequestedBy());
        assertSame(nominee, result.getNominatedUser());
        assertSame(project, result.getProject());

        verify(ownershipTransferRepository)
                .save(any(OwnershipTransfer.class));
    }

    @Test
    void nominatedMemberCanAcceptOwnershipTransfer() {
        AppUser previousOwner = mockUser(1L);
        AppUser nominatedMember = mockUser(2L);
        Project project = createProject(previousOwner);

        OwnershipTransfer transfer = new OwnershipTransfer(
                project, previousOwner, nominatedMember
        );

        ProjectMember oldOwnerMembership = new ProjectMember(
                project, previousOwner, ProjectMemberRole.OWNER
        );

        ProjectMember newOwnerMembership = new ProjectMember(
                project, nominatedMember, ProjectMemberRole.MEMBER
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(nominatedMember);
        when(ownershipTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(transfer));
        when(projectMemberRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(
                        oldOwnerMembership,
                        newOwnerMembership
                ));

        OwnershipTransfer result =
                projectService.acceptOwnershipTransfer(TRANSFER_ID);

        assertSame(nominatedMember, project.getOwner());

        assertEquals(
                ProjectMemberRole.MEMBER,
                oldOwnerMembership.getRole()
        );

        assertEquals(
                ProjectMemberRole.OWNER,
                newOwnerMembership.getRole()
        );

        assertEquals(
                OwnershipTransferStatus.ACCEPTED,
                result.getStatus()
        );

        assertNotNull(result.getDecidedAt());
    }

    @Test
    void nonNominatedMemberCannotAcceptOwnershipTransfer() {
        AppUser owner = mockUser(1L);
        AppUser nominee = mockUser(2L);
        AppUser anotherMember = mockUser(3L);

        Project project = createProject(owner);

        OwnershipTransfer transfer = new OwnershipTransfer(
                project, owner, nominee
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(anotherMember);
        when(ownershipTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(transfer));

        assertThrows(
                AccessDeniedException.class,
                () -> projectService.acceptOwnershipTransfer(TRANSFER_ID)
        );
    }

    @Test
    void nominatedMemberCanRejectOwnershipTransfer() {
        AppUser owner = mockUser(1L);
        AppUser nominee = mockUser(2L);
        Project project = createProject(owner);

        OwnershipTransfer transfer = new OwnershipTransfer(
                project, owner, nominee
        );

        when(currentUserService.getCurrentUser()).thenReturn(nominee);
        when(ownershipTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(transfer));

        OwnershipTransfer result =
                projectService.rejectOwnershipTransfer(TRANSFER_ID);

        assertEquals(
                OwnershipTransferStatus.REJECTED,
                result.getStatus()
        );

        assertNotNull(result.getDecidedAt());
    }
    @Test
void ownerCannotRejectLeaveRequestForArchivedProject() {
    AppUser owner = mockUser(1L);
    AppUser member = mockUser(2L);

    Project project = createProject(owner);
    project.setArchived(true);

    LeaveRequest request = new LeaveRequest(project, member);

    when(currentUserService.getCurrentUser()).thenReturn(owner);
    when(leaveRequestRepository.findById(REQUEST_ID))
            .thenReturn(Optional.of(request));

    assertThrows(
            IllegalArgumentException.class,
            () -> projectService.rejectLeaveRequest(REQUEST_ID)
    );

    assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
    assertNull(request.getDecidedAt());
}

@Test
void nomineeCannotRejectOwnershipTransferForArchivedProject() {
    AppUser owner = mockUser(1L);
    AppUser nominee = mockUser(2L);

    Project project = createProject(owner);
    project.setArchived(true);

    OwnershipTransfer transfer =
            new OwnershipTransfer(project, owner, nominee);

    when(currentUserService.getCurrentUser()).thenReturn(nominee);
    when(ownershipTransferRepository.findById(TRANSFER_ID))
            .thenReturn(Optional.of(transfer));

    assertThrows(
            IllegalArgumentException.class,
            () -> projectService.rejectOwnershipTransfer(TRANSFER_ID)
    );

    assertEquals(
            OwnershipTransferStatus.PENDING,
            transfer.getStatus()
    );

    assertNull(transfer.getDecidedAt());
}
}