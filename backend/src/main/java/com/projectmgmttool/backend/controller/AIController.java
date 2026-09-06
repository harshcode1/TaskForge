package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.AIResponse;
import com.projectmgmttool.backend.dto.AIStatusResponse;
import com.projectmgmttool.backend.dto.DashboardResponse;
import com.projectmgmttool.backend.dto.TaskDescriptionRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.service.AIService;
import com.projectmgmttool.backend.service.DashboardService;
import com.projectmgmttool.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "AI", description = "Optional AI-assisted features — no-op (503) unless OPENAI_API_KEY is configured")
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TaskRepository taskRepository;

    @Operation(summary = "Whether AI features are available", description = "Lets the frontend hide AI affordances entirely rather than show a button that always fails.")
    @GetMapping("/status")
    public ResponseEntity<AIStatusResponse> status() {
        return ResponseEntity.ok(new AIStatusResponse(aiService.isEnabled()));
    }

    @Operation(summary = "Generate a task description from its title")
    @PostMapping("/task-description")
    public ResponseEntity<AIResponse> generateTaskDescription(@Valid @RequestBody TaskDescriptionRequest request) {
        String description = aiService.generateTaskDescription(request.getTitle(), request.getProjectName());
        return ResponseEntity.ok(new AIResponse(description));
    }

    @Operation(summary = "Generate a plain-English status summary for a project",
            description = "Reuses ProjectService's own owner-or-member check, same as every other project-scoped endpoint.")
    @GetMapping("/project-summary/{projectId}")
    public ResponseEntity<AIResponse> projectSummary(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Project project = projectService.getProjectById(projectId, userDetails.getUsername());
        DashboardResponse stats = dashboardService.getDashboardData(projectId, userDetails.getUsername());

        List<String> overdueTitles = taskRepository.findByProjectId(projectId).stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(LocalDate.now())
                        && t.getStatus() != TaskStatus.DONE)
                .map(Task::getTitle)
                .limit(5)
                .collect(Collectors.toList());

        String summary = aiService.generateProjectSummary(
                project.getName(),
                stats.getTotalTasks(),
                stats.getCompletedTasks(),
                stats.getInProgressTasks(),
                stats.getOverdueTasks(),
                stats.getCompletionRate(),
                overdueTitles
        );
        return ResponseEntity.ok(new AIResponse(summary));
    }
}
