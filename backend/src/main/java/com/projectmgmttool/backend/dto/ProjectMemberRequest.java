package com.projectmgmttool.backend.dto;

import com.projectmgmttool.backend.entity.enums.Role;
import jakarta.validation.constraints.*;

import java.util.UUID;

public class ProjectMemberRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String userEmail;

    @NotNull(message = "Role is required")
    private Role role;

    public ProjectMemberRequest(UUID projectId, String userEmail, Role role) {
        this.projectId = projectId;
        this.userEmail = userEmail;
        this.role = role;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Role getRole() {
        return role;
    }
}
