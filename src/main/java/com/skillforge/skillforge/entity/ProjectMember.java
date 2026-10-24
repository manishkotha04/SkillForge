package com.skillforge.skillforge.entity;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.enums.ProjectMemberRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_project_member_user",
            columnNames = {"project_id", "user_id"}
        )
    }
)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, AppUser user,
                         ProjectMemberRole role) {
        this.project = project;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ProjectMemberRole getRole() {
        return role;
    }

    public void setRole(ProjectMemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public Project getProject() {
        return project;
    }

    public AppUser getUser() {
        return user;
    }
}
