package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.PortfolioItemRequest;
import com.skillforge.skillforge.entity.PortfolioItem;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.repository.PortfolioItemRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PortfolioService portfolioService;

    private AppUser currentUser;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(AppUser.class);
        otherUser = mock(AppUser.class);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }

    @Test
    void createPortfolioItemWithoutProject() {
        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("My Portfolio Work");
        request.setDescription("A sample portfolio item.");
        request.setExternalUrl("https://example.com");

        PortfolioItem savedItem = mock(PortfolioItem.class);

        when(portfolioItemRepository.save(any(PortfolioItem.class)))
                .thenReturn(savedItem);

        PortfolioItem result =
                portfolioService.createPortfolioItem(request);

        assertSame(savedItem, result);

        verify(projectRepository, never())
                .findById(anyLong());

        verify(portfolioItemRepository)
                .save(any(PortfolioItem.class));
    }

    @Test
    void createPortfolioItemWithOwnProject() {
        Project myProject = mock(Project.class);

        when(myProject.getOwner())
                .thenReturn(currentUser);

        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setTitle("SkillForge Project");
        request.setProjectId(10L);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(myProject));

        PortfolioItem savedItem = mock(PortfolioItem.class);

        when(portfolioItemRepository.save(any(PortfolioItem.class)))
                .thenReturn(savedItem);

        PortfolioItem result =
                portfolioService.createPortfolioItem(request);

        assertSame(savedItem, result);

        verify(projectRepository)
                .findById(10L);

        verify(portfolioItemRepository)
                .save(any(PortfolioItem.class));
    }

    @Test
void rejectsProjectOwnedByAnotherUser() {
    when(currentUser.getId())
            .thenReturn(1L);

    when(otherUser.getId())
            .thenReturn(2L);

    Project otherProject = mock(Project.class);

    when(otherProject.getOwner())
            .thenReturn(otherUser);

    PortfolioItemRequest request = new PortfolioItemRequest();
    request.setTitle("Invalid Project Link");
    request.setProjectId(20L);

    when(projectRepository.findById(20L))
            .thenReturn(Optional.of(otherProject));

    IllegalArgumentException exception =
            assertThrows(
                    IllegalArgumentException.class,
                    () -> portfolioService
                            .createPortfolioItem(request)
            );

    assertEquals(
            "You can only associate your own projects with portfolio items.",
            exception.getMessage()
    );

    verify(projectRepository)
            .findById(20L);

    verify(portfolioItemRepository, never())
            .save(any(PortfolioItem.class));
}

    @Test
    void retrievesCurrentUsersPortfolioItem() {
        when(currentUser.getId())
                .thenReturn(1L);

        PortfolioItem item = mock(PortfolioItem.class);

        when(portfolioItemRepository
                .findByIdAndUser_Id(5L, 1L))
                .thenReturn(Optional.of(item));

        PortfolioItem result =
                portfolioService.getMyPortfolioItem(5L);

        assertSame(item, result);

        verify(portfolioItemRepository)
                .findByIdAndUser_Id(5L, 1L);
    }

    @Test
    void rejectsAccessToAnotherUsersPortfolioItem() {
        when(currentUser.getId())
                .thenReturn(1L);

        when(portfolioItemRepository
                .findByIdAndUser_Id(99L, 1L))
                .thenReturn(Optional.empty());

        PortfolioItemNotFoundException exception =
                assertThrows(
                        PortfolioItemNotFoundException.class,
                        () -> portfolioService
                                .getMyPortfolioItem(99L)
                );

        assertEquals(
                "Portfolio item not found: 99",
                exception.getMessage()
        );

        verify(portfolioItemRepository)
                .findByIdAndUser_Id(99L, 1L);
    }
}