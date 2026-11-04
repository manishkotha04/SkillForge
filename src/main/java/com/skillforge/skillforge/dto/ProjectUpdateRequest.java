package com.skillforge.skillforge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ProjectUpdateRequest {

    @NotBlank(message = "Project name is required.")
    @Size(max = 150, message = "Project name cannot exceed 150 characters.")
    private String name;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters.")
    private String description;

    @Size(max = 2000, message = "Required skills cannot exceed 2000 characters.")
    private String requiredSkillsText;

    @NotNull(message = "Maximum team size is required.")
    @Min(value = 1, message = "Maximum team size must be at least 1.")
    private Integer maxTeamSize;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkillsText() { return requiredSkillsText; }
    public void setRequiredSkillsText(String requiredSkillsText) { this.requiredSkillsText = requiredSkillsText; }
    public Integer getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(Integer maxTeamSize) { this.maxTeamSize = maxTeamSize; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}
