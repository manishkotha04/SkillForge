package com.skillforge.skillforge.dto;

/**
 * Read-only progress values for one project.
 * The percentage is based on tasks, not assignees, so a shared task is counted once.
 */
public class ProjectProgressSummary {

    private final long totalTasks;
    private final long completedTasks;
    private final int percentage;

    public ProjectProgressSummary(long totalTasks, long completedTasks, int percentage) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.percentage = percentage;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public int getPercentage() {
        return percentage;
    }
}
