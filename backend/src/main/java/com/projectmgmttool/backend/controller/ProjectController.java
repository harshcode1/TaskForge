package com.projectmgmttool.backend.controller;


import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.dto.ProjectRequest;
import com.projectmgmttool.backend.dto.ProjectResponseDTO;
import com.projectmgmttool.backend.dto.UserDTO;
import com.projectmgmttool.backend.service.ProjectService;
import com.projectmgmttool.backend.repository.ProjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Projects", description = "Endpoints for project management")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Operation(summary = "Create a new project", description = "Creates a new project with the provided details.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Project.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Project created = projectService.createProject(
                request.getName(), request.getDescription(), userDetails.getUsername());

        UserDTO owner = new UserDTO(
                created.getOwner().getId(),
                created.getOwner().getName(),
                created.getOwner().getEmail()
        );

        ProjectResponseDTO response = new ProjectResponseDTO(
                created.getId(),
                created.getName(),
                created.getDescription(),
                created.getCreatedAt(),
                owner
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user's projects", description = "Retrieves all projects associated with the authenticated user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Project.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getUserProjects(@AuthenticationPrincipal UserDetails userDetails) {
        List<Project> projects = projectService.getProjectsForUser(userDetails.getUsername());
        List<ProjectResponseDTO> response = projects.stream()
                .map(p -> new ProjectResponseDTO(
                        p.getId(), p.getName(), p.getDescription(), p.getCreatedAt(),
                        new UserDTO(p.getOwner().getId(), p.getOwner().getName(), p.getOwner().getEmail())))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get project by ID", description = "Retrieves a specific project by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Project project = projectService.getProjectById(id, userDetails.getUsername());

        UserDTO owner = new UserDTO(
                project.getOwner().getId(),
                project.getOwner().getName(),
                project.getOwner().getEmail()
        );

        ProjectResponseDTO response = new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                owner
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update a project", description = "Updates a project by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Project.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Project updated = projectService.updateProject(id, request.getName(), request.getDescription(), userDetails.getUsername());
        UserDTO owner = new UserDTO(
                updated.getOwner().getId(), updated.getOwner().getName(), updated.getOwner().getEmail());
        return ResponseEntity.ok(new ProjectResponseDTO(
                updated.getId(), updated.getName(), updated.getDescription(), updated.getCreatedAt(), owner));
    }

    @Operation(summary = "Delete project", description = "Delete a specific project")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        projectService.deleteProject(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
