package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.PortfolioItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository
        extends JpaRepository<PortfolioItem, Long> {

    @EntityGraph(attributePaths = {"project"})
    List<PortfolioItem> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"project"})
    Optional<PortfolioItem> findByIdAndUser_Id(Long id, Long userId);
}