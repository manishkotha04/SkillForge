package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.PortfolioItemRequest;
import com.skillforge.skillforge.entity.PortfolioItem;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.repository.PortfolioItemRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private PortfolioItemRepository portfolioItemRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private PortfolioService portfolioService;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(AppUser.class);
        lenient().when(currentUser.getId()).thenReturn(1L);
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createPortfolioItemWithoutProject() {
        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("My Portfolio Work");
        request.setDescription("A sample portfolio item.");
        request.setExternalUrl("https://example.com");
        PortfolioItem savedItem = mock(PortfolioItem.class);
        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenReturn(savedItem);

        assertSame(savedItem, portfolioService.createPortfolioItem(request));
        verify(projectRepository, never()).findById(anyLong());
        verify(projectMemberRepository, never()).existsByProject_IdAndUser_Id(anyLong(), anyLong());
    }

    @Test
    void ownerCanLinkOwnProject() {
        Project project = mock(Project.class);
        when(project.getOwner()).thenReturn(currentUser);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(portfolioItemRepository.save(any(PortfolioItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("My Project");
        request.setProjectId(10L);

        PortfolioItem result = portfolioService.createPortfolioItem(request);
        assertSame(project, result.getProject());
        verify(projectMemberRepository).existsByProject_IdAndUser_Id(10L, 1L);
    }

    @Test
    void currentProjectMemberCanLinkSharedProject() {
        AppUser projectOwner = mock(AppUser.class);
        when(projectOwner.getId()).thenReturn(9L);
        Project project = mock(Project.class);
        when(project.getOwner()).thenReturn(projectOwner);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(20L, 1L))
                .thenReturn(true);
        when(portfolioItemRepository.save(any(PortfolioItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("Team Contribution");
        request.setProjectId(20L);

        PortfolioItem result = portfolioService.createPortfolioItem(request);
        assertSame(project, result.getProject());
    }

    @Test
    void nonMemberCannotLinkAnotherUsersProject() {
        AppUser projectOwner = mock(AppUser.class);
        when(projectOwner.getId()).thenReturn(2L);
        Project project = mock(Project.class);
        when(project.getOwner()).thenReturn(projectOwner);
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(30L, 1L))
                .thenReturn(false);

        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("Invalid Link");
        request.setProjectId(30L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> portfolioService.createPortfolioItem(request));
        assertEquals(
                "You can only associate projects you currently belong to with portfolio items.",
                exception.getMessage());
        verify(portfolioItemRepository, never()).save(any(PortfolioItem.class));
    }

    @Test
    void getPortfolioForStudentCanReadAnotherStudentsItems() {
        List<PortfolioItem> items = List.of(mock(PortfolioItem.class));
        when(portfolioItemRepository.findByUser_IdOrderByCreatedAtDesc(22L))
                .thenReturn(items);

        assertSame(items, portfolioService.getPortfolioForStudent(22L));
        verify(portfolioItemRepository).findByUser_IdOrderByCreatedAtDesc(22L);
    }

    @Test
    void retrievesOnlyCurrentUsersPortfolioItemForEditing() {
        PortfolioItem item = mock(PortfolioItem.class);
        when(portfolioItemRepository.findByIdAndUser_Id(5L, 1L))
                .thenReturn(Optional.of(item));

        assertSame(item, portfolioService.getMyPortfolioItem(5L));
        verify(portfolioItemRepository).findByIdAndUser_Id(5L, 1L);
    }

    @Test
    void rejectsEditingAnotherUsersPortfolioItem() {
        when(portfolioItemRepository.findByIdAndUser_Id(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(PortfolioItemNotFoundException.class,
                () -> portfolioService.getMyPortfolioItem(99L));
        verify(portfolioItemRepository).findByIdAndUser_Id(99L, 1L);
    }
}
