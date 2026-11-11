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

    @Autowired
    private ProjectAccessService projectAccessService;

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
        return projectAccessService.requireMember(projectId, requesterEmail);
    }

    public Project updateProject(UUID projectId, String name, String description, String requesterEmail) {
        Project project = projectAccessService.requireOwner(projectId, requesterEmail);

        project.setName(name);
        project.setDescription(description);
        return projectRepository.save(project);
    }

    public void deleteProject(UUID projectId, String requesterEmail) {
        Project project = projectAccessService.requireOwner(projectId, requesterEmail);

        projectRepository.delete(project);
    }
}
