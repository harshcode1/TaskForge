package com.projectmgmttool.backend.dto;

import com.projectmgmttool.backend.entity.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProfileDTO {
    private UUID id;
    private String name;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    public ProfileDTO(UUID id, String name, String email, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
