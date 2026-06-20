package com.projectmgmttool.backend.dto;

import com.projectmgmttool.backend.entity.enums.Role;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private UUID userId;
    private String name;
    private String email;
    private Role role;

    public AuthResponse() {}

    public AuthResponse(String token, UUID userId, String name, String email, Role role) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

