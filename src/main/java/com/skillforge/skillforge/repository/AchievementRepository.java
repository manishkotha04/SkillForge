package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository
        extends JpaRepository<Achievement, Long> {

    List<Achievement> findByUserIdOrderByAchievedDateDesc(Long userId);

    Optional<Achievement> findByIdAndUserId(
            Long id,
            Long userId
    );
}