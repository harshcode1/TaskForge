package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.dto.TaskRequest;
import com.projectmgmttool.backend.dto.TaskDTO;
import com.projectmgmttool.backend.service.TaskService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Tasks", description = "Endpoints for task management")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    private TaskDTO toDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getEmail() : null,
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.getDueDate()
        );
    }

    @Operation(summary = "Create a new task", description = "Creates a new task within a project.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDTO.class))),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Task created = taskService.createTask(request, userDetails.getUsername());
        return ResponseEntity.ok(toDTO(created));
    }

    @Operation(summary = "Get tasks for a project", description = "Retrieves tasks for a specific project, optionally filtered by status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasks(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status) {
        List<Task> tasks = taskService.getTasksForProject(projectId, Optional.ofNullable(status));
        return ResponseEntity.ok(tasks.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "Get tasks assigned to me", description = "Returns all tasks assigned to the current user.")
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getMyTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<Task> tasks = taskService.getTasksForUser(userDetails.getUsername());
        return ResponseEntity.ok(tasks.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "Update a task", description = "Updates a task by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Task updated = taskService.updateTask(id, request, userDetails.getUsername());
        return ResponseEntity.ok(toDTO(updated));
    }

    @Operation(summary = "Delete a task", description = "Deletes a task by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
