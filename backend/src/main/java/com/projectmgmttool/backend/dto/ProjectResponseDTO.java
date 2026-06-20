package com.projectmgmttool.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private UserDTO owner;

    public ProjectResponseDTO(UUID id, String name, String description, LocalDateTime createdAt, UserDTO owner) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDTO getOwner() {
        return owner;
    }

    public void setOwner(UserDTO owner) {
        this.owner = owner;
    }
}
