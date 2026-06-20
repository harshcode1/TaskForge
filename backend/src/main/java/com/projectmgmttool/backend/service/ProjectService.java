package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public Project createProject(String name, String description, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setOwner(owner); // Set the user as the owner of the project
        project.setCreatedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

    public List<Project> getProjectsForUser(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        return projectRepository.findAllProjectsVisibleToUser(user.getId());
    }

    public Project getProjectById(UUID projectId, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        boolean isOwner = project.getOwner().getEmail().equals(requesterEmail);
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getUser().getEmail().equals(requesterEmail));

        if (!isOwner && !isMember) {
            throw new CustomApiException("Access denied - you don't have permission to view this project", 403);
        }

        return project;
    }

    public Project updateProject(UUID projectId, String name, String description, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        if (!project.getOwner().getEmail().equals(requesterEmail)) {
            throw new CustomApiException("Only the owner can update the project", 403);
        }

        project.setName(name);
        project.setDescription(description);
        return projectRepository.save(project);
    }

    public void deleteProject(UUID projectId, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        if (!project.getOwner().getEmail().equals(requesterEmail)) {
            throw new CustomApiException("Only the owner can delete the project", 403);
        }

        projectRepository.delete(project);
    }
}
