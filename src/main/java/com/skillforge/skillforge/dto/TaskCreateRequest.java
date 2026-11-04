package com.skillforge.skillforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskCreateRequest {
    @NotBlank
    @Size(max = 150)
    private String title;
    private String description;
    private LocalDate dueDate;
    private List<Long> assigneeIds = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public List<Long> getAssigneeIds() { return assigneeIds; }
    public void setAssigneeIds(List<Long> assigneeIds) {
        this.assigneeIds = assigneeIds == null ? new ArrayList<>() : new ArrayList<>(assigneeIds);
    }
    // Kept for compatibility with existing callers and tests.
    public Long getAssigneeId() { return assigneeIds == null || assigneeIds.isEmpty() ? null : assigneeIds.get(0); }
    public void setAssigneeId(Long assigneeId) {
        this.assigneeIds = new ArrayList<>();
        if (assigneeId != null) this.assigneeIds.add(assigneeId);
    }
}
