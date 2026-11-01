package com.skillforge.skillforge.dto;

import java.time.LocalDate;

public class AchievementViewDto {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDate achievedDate;

    public AchievementViewDto(
            Long id,
            String title,
            String description,
            LocalDate achievedDate) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.achievedDate = achievedDate;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getAchievedDate() {
        return achievedDate;
    }
}