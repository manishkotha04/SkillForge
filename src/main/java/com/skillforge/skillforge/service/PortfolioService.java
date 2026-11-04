package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.PortfolioItemDto;
import com.skillforge.skillforge.dto.PortfolioItemRequest;
import com.skillforge.skillforge.entity.PortfolioItem;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.repository.PortfolioItemRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;

    public PortfolioService(
            PortfolioItemRepository portfolioItemRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CurrentUserService currentUserService) {

        this.portfolioItemRepository = portfolioItemRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<PortfolioItem> getMyPortfolio() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return getPortfolioForStudent(currentUser.getId());
    }

    /**
     * Read-only portfolio lookup used by the authenticated student-discovery view.
     * Portfolio editing remains restricted to the portfolio owner.
     */
    @Transactional(readOnly = true)
    public List<PortfolioItem> getPortfolioForStudent(Long studentUserId) {
        if (studentUserId == null) {
            throw new IllegalArgumentException("Student ID is required.");
        }
        return portfolioItemRepository
                .findByUser_IdOrderByCreatedAtDesc(studentUserId);
    }

    @Transactional
    public PortfolioItem createPortfolioItem(PortfolioItemRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = resolveProject(request.getProjectId(), currentUser);

        PortfolioItem item = new PortfolioItem(
                request.getTitle().trim(),
                request.getDescription(),
                currentUser,
                project,
                request.getExternalUrl());

        return portfolioItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public PortfolioItem getMyPortfolioItem(Long itemId) {
        AppUser currentUser = currentUserService.getCurrentUser();
        return portfolioItemRepository
                .findByIdAndUser_Id(itemId, currentUser.getId())
                .orElseThrow(() -> new PortfolioItemNotFoundException(itemId));
    }

    @Transactional
    public PortfolioItem updatePortfolioItem(
            Long itemId,
            PortfolioItemRequest request) {

        PortfolioItem item = getMyPortfolioItem(itemId);
        AppUser currentUser = currentUserService.getCurrentUser();
        Project project = resolveProject(request.getProjectId(), currentUser);

        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription());
        item.setProject(project);
        item.setExternalUrl(request.getExternalUrl());

        return portfolioItemRepository.save(item);
    }

    private Project resolveProject(Long projectId, AppUser currentUser) {
        if (projectId == null) {
            return null;
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected project does not exist."));

        boolean isOwner = project.getOwner() != null
                && project.getOwner().getId() != null
                && project.getOwner().getId().equals(currentUser.getId());
        boolean isMember = projectMemberRepository
                .existsByProject_IdAndUser_Id(projectId, currentUser.getId());

        if (!isOwner && !isMember) {
            throw new IllegalArgumentException(
                    "You can only associate projects you currently belong to with portfolio items.");
        }

        return project;
    }

    public PortfolioItemDto toDto(PortfolioItem item) {
        PortfolioItemDto dto = new PortfolioItemDto();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setExternalUrl(item.getExternalUrl());
        dto.setCreatedAt(item.getCreatedAt());

        if (item.getProject() != null) {
            dto.setProjectId(item.getProject().getId());
            dto.setProjectName(item.getProject().getName());
        }

        return dto;
    }
}
