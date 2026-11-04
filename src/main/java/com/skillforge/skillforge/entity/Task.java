package com.skillforge.skillforge.entity;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.enums.TaskStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private AppUser completedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /*
     * Keep the original assignee_id relation for compatibility with existing
     * database rows. New assignments are stored in task_assignees. The legacy
     * assignee is included by getAssignees(), so existing tasks remain visible.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_task_assignee", columnNames = {"task_id", "user_id"})
    )
    private Set<AppUser> assignedUsers = new LinkedHashSet<>();

    protected Task() {
    }

    public Task(String title, String description, LocalDate dueDate,
                Project project, AppUser assignee) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.project = project;
        this.status = TaskStatus.TODO;
        if (assignee != null) {
            this.assignee = assignee;
            this.assignedUsers.add(assignee);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = TaskStatus.TODO;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public AppUser getCompletedBy() { return completedBy; }
    public void setCompletedBy(AppUser completedBy) { this.completedBy = completedBy; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    /** Backward-compatible primary assignee accessor. */
    public AppUser getAssignee() {
        if (assignee != null) return assignee;
        return assignedUsers == null || assignedUsers.isEmpty()
                ? null : assignedUsers.iterator().next();
    }

    /** Backward-compatible primary assignee setter. */
    public void setAssignee(AppUser assignee) {
        this.assignee = assignee;
        if (assignedUsers == null) assignedUsers = new LinkedHashSet<>();
        if (assignee != null) assignedUsers.add(assignee);
    }

    /** Returns all assignees, including a legacy assignee from older rows. */
    public Set<AppUser> getAssignees() {
        Set<AppUser> result = new LinkedHashSet<>();
        if (assignee != null) result.add(assignee);
        if (assignedUsers != null) result.addAll(assignedUsers);
        return result;
    }

    public void setAssignees(Collection<AppUser> users) {
        if (assignedUsers == null) assignedUsers = new LinkedHashSet<>();
        assignedUsers.clear();
        if (users != null) assignedUsers.addAll(users);
        this.assignee = assignedUsers.isEmpty() ? null : assignedUsers.iterator().next();
    }
}
