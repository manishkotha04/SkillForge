package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository
        extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<PortfolioItem> findByIdAndUser_Id(Long id, Long userId);
}