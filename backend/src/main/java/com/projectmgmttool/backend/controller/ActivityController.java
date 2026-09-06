package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.ActivityLogDTO;
import com.projectmgmttool.backend.entity.ActivityLog;
import com.projectmgmttool.backend.repository.ActivityLogRepository;
import com.projectmgmttool.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Activity", description = "Persisted history of task changes for a project — the audit-log counterpart to the live WebSocket feed")
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "Recent activity for a project", description = "Last 50 task create/update/delete events, newest first. Reuses ProjectService's owner-or-member check, same as every other project-scoped endpoint.")
    @GetMapping("/{projectId}")
    public ResponseEntity<List<ActivityLogDTO>> getActivity(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Throws 403/404 if the requester isn't allowed to see this project —
        // same guard AIController and every other project-scoped endpoint uses.
        projectService.getProjectById(projectId, userDetails.getUsername());

        List<ActivityLog> logs = activityLogRepository.findTop50ByProjectIdOrderByCreatedAtDesc(projectId);
        List<ActivityLogDTO> dtos = logs.stream()
                .map(l -> new ActivityLogDTO(l.getId(), l.getType(), l.getTaskId(), l.getTaskTitle(), l.getActorName(), l.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
