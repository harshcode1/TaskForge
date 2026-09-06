package com.projectmgmttool.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class TaskDescriptionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String projectName;

    public TaskDescriptionRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
