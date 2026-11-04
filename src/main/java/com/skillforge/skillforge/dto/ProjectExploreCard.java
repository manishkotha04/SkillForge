package com.skillforge.skillforge.dto;

import com.skillforge.skillforge.entity.Project;

public class ProjectExploreCard {

    private final Project project;
    private final String ownerName;
    private final long currentMemberCount;
    private final boolean requestPending;

    public ProjectExploreCard(
            Project project,
            long currentMemberCount,
            boolean requestPending) {

        this.project = project;
        this.currentMemberCount = currentMemberCount;
        this.requestPending = requestPending;

        // Capture owner details while the service transaction is active.
        this.ownerName = project.getOwner() != null
                && project.getOwner().getFullName() != null
                ? project.getOwner().getFullName()
                : "Unknown owner";
    }

    public Project getProject() {
        return project;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getCurrentMemberCount() {
        return currentMemberCount;
    }

    public boolean isRequestPending() {
        return requestPending;
    }
}