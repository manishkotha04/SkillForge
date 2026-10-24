package com.skillforge.skillforge.entity;

import com.skillforge.skillforge.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_skills_text", columnDefinition = "TEXT")
    private String requiredSkillsText;

    @Column(name = "max_team_size")
    private Integer maxTeamSize;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    protected Project() {
    }

public Project(String name, String description,
               String requiredSkillsText, AppUser owner) {
    this.name = name;
    this.description = description;
    this.requiredSkillsText = requiredSkillsText;
    this.owner = owner;
    this.archived = false;
}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkillsText() {
        return requiredSkillsText;
    }

    public void setRequiredSkillsText(String requiredSkillsText) {
        this.requiredSkillsText = requiredSkillsText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
    
    public Integer getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(Integer maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
