package com.projectmgmttool.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.projectmgmttool.backend.entity.enums.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    @JsonBackReference
    private Project project;

    private LocalDateTime joinedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Role role;  // Project-specific role (e.g. MEMBER or MANAGER)

    // Constructors
    public ProjectMember() {}

    public ProjectMember(User user, Project project, Role role) {
        this.user = user;
        this.project = project;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }
}
